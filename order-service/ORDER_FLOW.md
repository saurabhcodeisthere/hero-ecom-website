# Order Service — Complete Object Flow

> **What this document covers:**
> The complete journey of every object from the moment a customer sends
> `POST /api/v1/orders` to the moment the `201 Created` response lands back.
> Every intermediate conversion, every inter-service call, every DB write.

---

## Sample Request

```
POST /api/v1/orders
Authorization: Bearer eyJhbGci...
Content-Type: application/json

{
  "shippingAddress": "123 MG Road, Bangalore",
  "items": [
    { "bikeId": 1, "quantity": 2 },
    { "bikeId": 2, "quantity": 1 }
  ]
}
```

---

## Layer 1 — Spring Security (JWT Filter)

Reads `Authorization: Bearer ...` from the header.
Calls Keycloak JWK endpoint to verify the token signature.
Converts the raw token string into a `Jwt` object.

```
Jwt {
  subject  : "d43d639a-cf18-4d20-9cd9-66919f07d429"   ← userId
  email    : "vaibhav@hero.com"
  roles    : ["ROLE_USER"]
  expiry   : 2026-03-18T10:00:00
}
```

Stored in `SecurityContext`. Made available via `@AuthenticationPrincipal Jwt jwt`.

---

## Layer 2 — Jackson (JSON → Java Object)

Jackson reads the raw JSON body and maps it into `PlaceOrderRequest`.

```
Raw JSON                              PlaceOrderRequest
────────────────────────────────      ────────────────────────────────────────
"shippingAddress": "123 MG Road"  →   String shippingAddress = "123 MG Road"
"items": [ ... ]                  →   List<OrderItemRequest> items
  { "bikeId":1, "quantity":2 }          OrderItemRequest { bikeId=1, qty=2 }
  { "bikeId":2, "quantity":1 }          OrderItemRequest { bikeId=2, qty=1 }
```

**`@Valid` runs here:**

| Field            | Constraint              | Result |
|------------------|-------------------------|--------|
| shippingAddress  | @NotBlank, @Size(max=255) | ✅     |
| items            | @NotEmpty, @Size(max=10) | ✅     |
| each item.bikeId | @NotNull, @Positive      | ✅     |
| each item.qty    | @Min(1), @Max(10)        | ✅     |

---

## Layer 3 — Order Controller

Receives two objects and passes them straight to the service. No logic here.

```java
PlaceOrderRequest request   ← from JSON body (Layer 2)
Jwt jwt                     ← from SecurityContext (Layer 1)

orderService.placeOrder(request, jwt)
```

---

## Layer 4 — Order Service — Extract JWT Data

Pulls identity out of the Jwt object. Builds a skeleton Order entity.

```
Jwt object                         Plain Strings
──────────────────────────────     ────────────────────────────────────
jwt.getSubject()               →   String userId    = "d43d639a-..."
jwt.getClaimAsString("email")  →   String userEmail = "vaibhav@hero.com"
```

**Skeleton Order (in memory — no DB write yet):**

```
Order {
  id             : null                       ← DB will generate
  orderNumber    : "ORD-20260318-A3F9C1B2"
  userId         : "d43d639a-..."             ← from JWT, NOT request body
  userEmail      : "vaibhav@hero.com"
  status         : PENDING
  shippingAddress: "123 MG Road, Bangalore"
  totalAmount    : 0.00
  items          : []                         ← empty, filled in loop below
  createdAt      : null                       ← DB will set this
  version        : null                       ← DB will set this to 0
}
```

---

## Layer 5 — Item Loop (runs once per item in the request)

### Iteration 1 — bikeId=1, quantity=2

#### Step 5A — Call bike-service

```
bikeServiceClient.getBikeById(1L)
        ↓
HTTP Interface Proxy builds:
  GET http://bike-service/api/v1/bikes/1

LoadBalancerInterceptor asks Eureka → gets real IP
  GET http://172.18.0.5:8081/api/v1/bikes/1

bike-service returns FULL BikeResponse JSON:
{
  "id": 1,
  "modelName": "Hero Splendor Plus",   ← kept
  "price": 77000.00,                   ← kept
  "type": "COMMUTER",                  ← IGNORED by Jackson
  "engine": { ... },                   ← IGNORED by Jackson
  "suspension": { ... },               ← IGNORED by Jackson
  "dimensions": { ... },               ← IGNORED by Jackson
  "description": "...",                ← IGNORED by Jackson
  "imageUrl": "..."                    ← IGNORED by Jackson
}

Jackson maps ONLY matching fields → BikeClientResponse:
BikeClientResponse {
  id        : 1
  modelName : "Hero Splendor Plus"
  price     : 77000.00
}
```

> **Why only 3 fields?** `BikeClientResponse` is a consumer-driven contract.
> Order-service declares only what it needs. Jackson silently ignores the rest.
> If bike-service adds 10 new fields tomorrow — order-service is unaffected.

---

#### Step 5B — Call inventory-service (check stock)

```
inventoryServiceClient.getInventoryByBikeId(1L)
        ↓
GET http://172.18.0.6:8082/api/v1/inventories/bike/1

inventory-service returns:
{
  "id": 1,
  "bikeId": 1,
  "stockQuantity": 150,
  "active": true
}

Jackson maps → InventoryClientResponse:
InventoryClientResponse {
  id            : 1
  bikeId        : 1
  stockQuantity : 150
  active        : true
}

Stock check: 150 >= 2  ✅  enough stock — continue
```

If stock was insufficient (e.g. 1 < 2):
- Throws `InsufficientStockException`
- Circuit breaker fallback re-throws it
- `GlobalExceptionHandler` returns `400 BAD REQUEST`

---

#### Step 5C — Call inventory-service (reduce stock)

```
inventoryServiceClient.reduceStock(1L, 2)
        ↓
PATCH http://172.18.0.6:8082/api/v1/inventories/bike/1/reduce?quantity=2

inventory-service deducts atomically: 150 - 2 = 148
Uses @Version (optimistic locking) to prevent race conditions
Returns updated InventoryClientResponse (result not used here)
```

---

#### Step 5D — Build OrderItem (snapshot from bike-service data)

```
BikeClientResponse    +    OrderItemRequest
──────────────────         ────────────────
id        = 1              bikeId   = 1
modelName = "Splendor"     quantity = 2
price     = 77000.00

subtotal = 77000.00 × 2 = 154000.00

        ↓  combined into ↓

OrderItem {
  id        : null                         ← DB will generate
  order     : <Order ref>                  ← set by addItem() helper
  bikeId    : 1
  bikeName  : "Hero Splendor Plus"         ← SNAPSHOT at order time
  bikePrice : 77000.00                     ← SNAPSHOT at order time
  quantity  : 2
  subtotal  : 154000.00
}

order.addItem(item):
  items.add(item)       ← item goes into Order's list
  item.setOrder(this)   ← bidirectional link maintained
```

> **Why snapshot?** If the bike's price changes next month, old orders
> must still show the original price the customer paid — not the new price.

---

### Iteration 2 — bikeId=2, quantity=1

Same 4 steps repeated:

```
BikeClientResponse      { id:2, modelName:"Hero Xpulse 200 4V", price:149000.00 }
InventoryClientResponse { stockQuantity:50, active:true }
Stock check: 50 >= 1 ✅
reduceStock(2L, 1) → stock becomes 49
subtotal = 149000.00 × 1 = 149000.00

OrderItem {
  bikeId    : 2
  bikeName  : "Hero Xpulse 200 4V"    ← SNAPSHOT
  bikePrice : 149000.00               ← SNAPSHOT
  quantity  : 1
  subtotal  : 149000.00
}
```

---

## Layer 6 — Total Calculation

```
order.getItems() stream:
  item1.subtotal = 154000.00
  item2.subtotal = 149000.00
                 ──────────
  total          = 303000.00

order.setTotalAmount(303000.00)
```

**Order fully built in memory (no DB write yet):**

```
Order {
  id             : null
  orderNumber    : "ORD-20260318-A3F9C1B2"
  userId         : "d43d639a-..."
  userEmail      : "vaibhav@hero.com"
  status         : PENDING
  shippingAddress: "123 MG Road, Bangalore"
  totalAmount    : 303000.00
  items: [
    OrderItem { bikeId:1, bikeName:"Hero Splendor Plus",  qty:2, subtotal:154000.00 }
    OrderItem { bikeId:2, bikeName:"Hero Xpulse 200 4V",  qty:1, subtotal:149000.00 }
  ]
}
```

---

## Layer 7 — JPA / Hibernate (Java Object → SQL → DB)

```
orderRepository.save(order)
```

Hibernate translates the Order into SQL:

```sql
INSERT INTO orders
  (order_number, user_id, user_email, status, shipping_address, total_amount, created_at, version)
VALUES
  ('ORD-20260318-A3F9C1B2', 'd43d639a-...', 'vaibhav@hero.com',
   'PENDING', '123 MG Road', 303000.00, NOW(), 0);
```

Because of `CascadeType.ALL`, Hibernate also inserts both items automatically:

```sql
INSERT INTO order_items (order_id, bike_id, bike_name, bike_price, quantity, subtotal)
VALUES (42, 1, 'Hero Splendor Plus', 77000.00, 2, 154000.00);

INSERT INTO order_items (order_id, bike_id, bike_name, bike_price, quantity, subtotal)
VALUES (42, 2, 'Hero Xpulse 200 4V', 149000.00, 1, 149000.00);
```

PostgreSQL writes to disk, returns generated IDs.
Hibernate fills in the generated values back into the Java object:

```
Order (saved — complete with DB-generated fields):
{
  id             : 42                          ← generated by DB
  orderNumber    : "ORD-20260318-A3F9C1B2"
  status         : PENDING
  totalAmount    : 303000.00
  createdAt      : 2026-03-18T09:36:38         ← set by @CreationTimestamp
  updatedAt      : 2026-03-18T09:36:38         ← set by @UpdateTimestamp
  version        : 0                           ← set by @Version
  items: [
    OrderItem { id:101, bikeId:1, bikeName:"Hero Splendor Plus",  qty:2, subtotal:154000.00 }
    OrderItem { id:102, bikeId:2, bikeName:"Hero Xpulse 200 4V",  qty:1, subtotal:149000.00 }
  ]
}
```

---

## Layer 8 — Order Mapper (Entity → Response DTO)

```
orderMapper.toResponse(saved)
```

Manual mapping — explicit control over what the client sees:

```
Order entity                           OrderResponse DTO
─────────────────────────────────      ─────────────────────────────────
id             : 42               →    orderId        : 42
orderNumber    : "ORD-..."        →    orderNumber    : "ORD-..."
status         : PENDING (enum)   →    status         : "PENDING" (String)
totalAmount    : 303000.00        →    totalAmount    : 303000.00
shippingAddress: "123 MG Road"    →    shippingAddress: "123 MG Road"
createdAt      : 2026-03-18...    →    createdAt      : 2026-03-18...
userId         : "d43d639a-..."   →    ✗ NOT MAPPED   ← intentionally hidden
userEmail      : "vaibhav@..."    →    ✗ NOT MAPPED   ← intentionally hidden
version        : 0                →    ✗ NOT MAPPED   ← internal field
updatedAt      : 2026-03-18...    →    ✗ NOT MAPPED   ← not needed by client
```

Each `OrderItem` entity → `OrderItemResponse`:

```
OrderItem                              OrderItemResponse
─────────────────────────────────      ─────────────────────────────────
bikeId    : 1                     →    bikeId    : 1
bikeName  : "Hero Splendor Plus"  →    bikeName  : "Hero Splendor Plus"
bikePrice : 77000.00              →    bikePrice : 77000.00
quantity  : 2                     →    quantity  : 2
subtotal  : 154000.00             →    subtotal  : 154000.00
order     : <Order ref>           →    ✗ NOT MAPPED  ← avoids circular reference
```

---

## Layer 9 — Controller Wraps in ResponseEntity

```java
ResponseEntity.status(201 CREATED).body(orderResponse)
```

---

## Layer 10 — Jackson (Java Object → JSON)

Jackson serializes `OrderResponse` into JSON and writes it to the HTTP response body.

---

## Final HTTP Response

```
HTTP/1.1 201 Created
Content-Type: application/json

{
  "orderId": 42,
  "orderNumber": "ORD-20260318-A3F9C1B2",
  "status": "PENDING",
  "totalAmount": 303000.00,
  "shippingAddress": "123 MG Road, Bangalore",
  "createdAt": "2026-03-18T09:36:38",
  "items": [
    {
      "bikeId": 1,
      "bikeName": "Hero Splendor Plus",
      "bikePrice": 77000.00,
      "quantity": 2,
      "subtotal": 154000.00
    },
    {
      "bikeId": 2,
      "bikeName": "Hero Xpulse 200 4V",
      "bikePrice": 149000.00,
      "quantity": 1,
      "subtotal": 149000.00
    }
  ]
}
```

---

## Object Count — One Request Creates

| Object                  | Count | Purpose                                  |
|-------------------------|-------|------------------------------------------|
| `PlaceOrderRequest`     | 1     | Carries raw input from customer          |
| `OrderItemRequest`      | 2     | One per item in request                  |
| `Jwt`                   | 1     | Carries identity from Keycloak token     |
| `Order` entity          | 1     | Core DB entity                           |
| `BikeClientResponse`    | 2     | One per item — result of bike-service call |
| `InventoryClientResponse` | 2   | One per item — result of inventory-service call |
| `OrderItem` entity      | 2     | One per item — saved to DB               |
| `Order` entity (saved)  | 1     | Returned by DB with IDs filled           |
| `OrderItemResponse`     | 2     | One per item — mapper output             |
| `OrderResponse`         | 1     | Final DTO sent to customer               |
| **Total**               | **17**|                                          |

---

## Inter-Service Communication Summary

```
order-service
    │
    ├── GET  /api/v1/bikes/{id}                        → bike-service
    │        (per item: validate bike exists, get name + price)
    │
    ├── GET  /api/v1/inventories/bike/{bikeId}         → inventory-service
    │        (per item: check stock is sufficient)
    │
    └── PATCH /api/v1/inventories/bike/{bikeId}/reduce → inventory-service
             (per item: atomically deduct stock)
```

All calls go through:
1. `HttpServiceProxyFactory` — converts Java method call to HTTP request
2. `LoadBalancerInterceptor` — resolves service name to real IP via Eureka
3. `RestClient` — sends the actual HTTP request
4. Jackson — converts response JSON back to a Java object

No JWT is passed in these internal calls — services communicate within
the Docker network and trust each other at the network level.

---

## Key Design Decisions

| Decision | Reason |
|----------|--------|
| `userId` taken from JWT, not request body | Customer cannot fake their identity |
| `bikeName` and `bikePrice` stored as snapshots | Price changes don't affect old orders |
| `BikeClientResponse` has only 3 fields | Consumer-driven contract — take only what you need |
| Manual mapper instead of ModelMapper | Explicit control — internal fields never accidentally exposed |
| `CascadeType.ALL` on Order → OrderItems | Save order once, all items saved automatically |
| `orphanRemoval = true` | Remove item from list → deleted from DB, no orphan rows |
| `@Version` on Order | Optimistic locking prevents lost updates under concurrent requests |

---
---

# GET /api/v1/orders — Customer Order History

## Sample Request

```
GET /api/v1/orders
Authorization: Bearer eyJhbGci...
```

---

## Layer 1 — Spring Security

Same as POST /api/v1/orders.
JWT verified, `Jwt` object placed in `SecurityContext`.
`@PreAuthorize("hasRole('CUSTOMER')")` checks the token contains `CUSTOMER` role.
If role missing → `403 Forbidden` before controller is even reached.

---

## Layer 2 — No Request Body

This is a GET request. There is no JSON body and no `@Valid` to run.
The only input is the `Jwt` from `SecurityContext`.

---

## Layer 3 — Order Controller

```java
@GetMapping
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<List<OrderResponse>> getMyOrders(@AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(orderService.getMyOrders(jwt));
}
```

Passes the `Jwt` object to the service. Returns whatever the service gives back wrapped in `200 OK`.

---

## Layer 4 — Order Service

```java
public List<OrderResponse> getMyOrders(Jwt jwt) {
    String userId = jwt.getSubject();   // "d43d639a-cf18-4d20-9cd9-66919f07d429"
    return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(orderMapper::toResponse)
            .toList();
}
```

Extracts `userId` from JWT. Queries DB for that user's orders only.

---

## Layer 5 — Repository Query

```java
List<Order> findByUserIdOrderByCreatedAtDesc(String userId);
```

Spring Data JPA translates the method name into SQL:

```sql
SELECT * FROM orders
WHERE user_id = 'd43d639a-cf18-4d20-9cd9-66919f07d429'
ORDER BY created_at DESC;
```

Returns a `List<Order>` — each `Order` contains its `List<OrderItem>` loaded lazily.

---

## Layer 6 — Mapper (Entity → DTO, repeated per order)

```
Each Order entity → orderMapper.toResponse() → OrderResponse

Fields hidden from customer in toResponse():
  userId      ✗ NOT MAPPED
  userEmail   ✗ NOT MAPPED
  version     ✗ NOT MAPPED
  updatedAt   ✗ NOT MAPPED
```

---

## Layer 7 — Jackson + Response

```
List<OrderResponse>
        ↓
Jackson serializes to JSON array
        ↓
ResponseEntity.ok() wraps it
```

---

## Final HTTP Response

```
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "orderId": 22,
    "orderNumber": "ORD-20260318-B0A6D94F",
    "status": "PENDING",
    "totalAmount": 303000.00,
    "shippingAddress": "123 MG Road, Bangalore",
    "createdAt": "2026-03-18T09:36:38",
    "items": [ ... ]
  },
  {
    "orderId": 21,
    "orderNumber": "ORD-20260318-2006A4B8",
    ...
  }
]
```

Empty list `[]` returned if customer has no orders yet — never null, never 404.

---
---

# GET /api/v1/orders/{id} — Customer Get Single Order

## Sample Request

```
GET /api/v1/orders/22
Authorization: Bearer eyJhbGci...
```

---

## Layer 1 — Spring Security

JWT verified. `CUSTOMER` role checked via `@PreAuthorize`.

---

## Layer 2 — Path Variable Extracted

```
URL: /api/v1/orders/22
Spring extracts: Long id = 22
```

No request body. Only `id` from the path and `Jwt` from security context.

---

## Layer 3 — Order Controller

```java
@GetMapping("/{id}")
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    return ResponseEntity.ok(orderService.getOrderById(id, jwt));
}
```

---

## Layer 4 — Order Service (Ownership Check)

```java
public OrderResponse getOrderById(Long id, Jwt jwt) {
    String userId = jwt.getSubject();

    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));

    if (!order.getUserId().equals(userId)) {
        throw new ForbiddenException("You are not authorized to view this order");
    }

    return orderMapper.toResponse(order);
}
```

**Three outcomes:**

```
Outcome 1 — Order not found (id=999 doesn't exist in DB):
  findById(999) → empty Optional
  orElseThrow   → throws OrderNotFoundException
  GlobalExceptionHandler → 404 NOT FOUND

Outcome 2 — Order belongs to different user:
  order.getUserId() = "abc-123"   ← belongs to rahul
  jwt.getSubject()  = "d43d639a"  ← logged in as vaibhav
  "abc-123".equals("d43d639a") → false
  throws ForbiddenException → 403 FORBIDDEN

Outcome 3 — Order exists and belongs to this user:
  order.getUserId() = "d43d639a"
  jwt.getSubject()  = "d43d639a"
  equal → true → continue → map → return 200 OK
```

---

## Layer 5 — Repository + Mapper + Response

Same as GET /api/v1/orders but for a single order.
`toResponse()` hides `userId`, `userEmail`, `version`, `updatedAt`.

---

## Final HTTP Response

```
HTTP/1.1 200 OK

{
  "orderId": 22,
  "orderNumber": "ORD-20260318-B0A6D94F",
  "status": "PENDING",
  "totalAmount": 303000.00,
  "shippingAddress": "123 MG Road, Bangalore",
  "createdAt": "2026-03-18T09:36:38",
  "items": [ ... ]
}
```

---
---

# DELETE /api/v1/orders/{id} — Customer Cancel Order

## Sample Request

```
DELETE /api/v1/orders/22
Authorization: Bearer eyJhbGci...
```

---

## Layer 1 — Spring Security

JWT verified. `CUSTOMER` role checked.

---

## Layer 2 — Path Variable

```
URL: /api/v1/orders/22
Spring extracts: Long id = 22
```

No request body. Customer does not need to say why they are cancelling.

---

## Layer 3 — Order Controller

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<Void> cancelMyOrder(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
    orderService.cancelMyOrder(id, jwt);
    return ResponseEntity.noContent().build();   // 204 — no body in response
}
```

Returns `204 No Content` — cancel was successful, nothing to show.

---

## Layer 4 — Order Service (3 Guards + Stock Restore)

```java
public void cancelMyOrder(Long id, Jwt jwt) {
    String userId = jwt.getSubject();

    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));     // Guard 1

    if (!order.getUserId().equals(userId)) {
        throw new ForbiddenException("...");                        // Guard 2
    }

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new InvalidOrderStateException("...");                // Guard 3
    }

    restoreStockForOrder(order);    // Step A — restore stock first
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);   // Step B — then mark cancelled
}
```

**Guard 1 — Does this order exist?**
```
findById(22) → found ✅  or  throws 404
```

**Guard 2 — Does this order belong to this customer?**
```
order.userId = "d43d639a"  (vaibhav's order)
jwt.subject  = "d43d639a"  (vaibhav logged in)
equal → ✅  or  throws 403
```

**Guard 3 — Is the order still cancellable?**
```
order.status = PENDING   → ✅ customer can cancel
order.status = CONFIRMED → ❌ 400 — "Only PENDING orders can be cancelled"
order.status = SHIPPED   → ❌ 400 — contact support
order.status = DELIVERED → ❌ 400 — contact support
```

---

## Step A — restoreStockForOrder()

For every `OrderItem` in the order, calls inventory-service to add the stock back:

```
Order has 2 items:
  item1: bikeId=1, quantity=2
  item2: bikeId=2, quantity=1

Loop iteration 1:
  PATCH http://inventory-service/api/v1/inventories/bike/1/restore?quantity=2
  inventory-service: stockQuantity + 2  (e.g. 96 → 98)

Loop iteration 2:
  PATCH http://inventory-service/api/v1/inventories/bike/2/restore?quantity=1
  inventory-service: stockQuantity + 1  (e.g. 48 → 49)
```

If a restore call fails (inventory-service down):
- Error is logged
- Cancellation continues — order is still cancelled
- Stock discrepancy is resolved manually later

This is "best-effort restore" — we never block a cancellation because of a stock service failure.

---

## Step B — Save Cancelled Order

```
order.setStatus(CANCELLED)
orderRepository.save(order)
        ↓
UPDATE orders SET status='CANCELLED', updated_at=NOW() WHERE id=22;
```

---

## Final HTTP Response

```
HTTP/1.1 204 No Content
```

No body. `204` means "it worked, nothing to say". Frontend reads the status code and shows "Order cancelled successfully".

---
---

# GET /api/v1/admin/orders — Admin Get All Orders (Paginated)

## Sample Request

```
GET /api/v1/admin/orders?page=0&size=20&status=PENDING
Authorization: Bearer eyJhbGci...
```

`status` is optional. Omit it to get all orders regardless of status.

---

## Layer 1 — Spring Security

JWT verified. `@PreAuthorize("hasRole('ADMIN')")` — only ADMIN role allowed.
If a customer token is used → `403 Forbidden`.

---

## Layer 2 — Request Params Extracted

```
page   = 0     (which page, zero-indexed)
size   = 20    (how many orders per page)
status = PENDING  (optional filter — null if not provided)
```

---

## Layer 3 — Admin Controller

```java
@GetMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<PagedOrderResponse> getAllOrders(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) OrderStatus status) {
    return ResponseEntity.ok(orderService.getAllOrders(page, size, status));
}
```

Default values: `page=0`, `size=20` if not provided. `status` defaults to `null`.

---

## Layer 4 — Order Service (Conditional Query)

```java
public PagedOrderResponse getAllOrders(int page, int size, OrderStatus status) {
    Pageable pageable = PageRequest.of(page, size);

    Page<Order> orderPage = (status != null)
            ? orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
            : orderRepository.findAllByOrderByCreatedAtDesc(pageable);

    List<AdminOrderResponse> orders = orderPage.getContent()
            .stream()
            .map(orderMapper::toAdminResponse)
            .toList();

    return PagedOrderResponse.builder()
            .orders(orders)
            .page(orderPage.getNumber())
            .size(orderPage.getSize())
            .totalElements(orderPage.getTotalElements())
            .totalPages(orderPage.getTotalPages())
            .last(orderPage.isLast())
            .build();
}
```

---

## Layer 5 — Repository (Two Possible SQL Queries)

**With status filter:**
```sql
SELECT * FROM orders
WHERE status = 'PENDING'
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

**Without status filter:**
```sql
SELECT * FROM orders
ORDER BY created_at DESC
LIMIT 20 OFFSET 0;
```

Spring Data JPA also runs a COUNT query automatically for pagination metadata:
```sql
SELECT COUNT(*) FROM orders WHERE status = 'PENDING';
-- Returns: 21
-- totalPages = ceil(21 / 20) = 2
```

---

## Layer 6 — Mapper (toAdminResponse — different from customer view)

```
Each Order entity → orderMapper.toAdminResponse() → AdminOrderResponse

Fields ADDED vs customer view:
  userId      ✅ INCLUDED  ← admin needs to know who placed the order
  userEmail   ✅ INCLUDED  ← admin needs to contact the customer
  updatedAt   ✅ INCLUDED  ← admin needs to track when status last changed
```

---

## Layer 7 — PagedOrderResponse Built

```
PagedOrderResponse {
  orders       : [ AdminOrderResponse, AdminOrderResponse, ... ]  (20 max)
  page         : 0
  size         : 20
  totalElements: 21
  totalPages   : 2
  last         : false   ← more pages exist
}
```

---

## Final HTTP Response

```
HTTP/1.1 200 OK

{
  "orders": [
    {
      "orderId": 22,
      "orderNumber": "ORD-20260318-B0A6D94F",
      "userId": "d43d639a-cf18-4d20-9cd9-66919f07d429",
      "userEmail": "vaibhav@test.com",
      "status": "PENDING",
      "totalAmount": 303000.00,
      "shippingAddress": "123 MG Road, Bangalore",
      "createdAt": "2026-03-18T09:36:38",
      "updatedAt": "2026-03-18T09:36:38",
      "items": [ ... ]
    },
    ...
  ],
  "page": 0,
  "size": 20,
  "totalElements": 21,
  "totalPages": 2,
  "last": false
}
```

---
---

# PATCH /api/v1/admin/orders/{id}/confirm — Admin Confirm Order

## Sample Request

```
PATCH /api/v1/admin/orders/21/confirm
Authorization: Bearer eyJhbGci...
```

No request body. The action is encoded in the URL (`/confirm`).

---

## Layer 1 — Spring Security

JWT verified. `ADMIN` role checked.

---

## Layer 2 — Path Variable

```
URL: /api/v1/admin/orders/21/confirm
Spring extracts: Long id = 21
```

---

## Layer 3 — Admin Controller

```java
@PatchMapping("/{id}/confirm")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<AdminOrderResponse> confirmOrder(@PathVariable Long id) {
    return ResponseEntity.ok(orderService.confirmOrder(id));
}
```

---

## Layer 4 — Service → transitionStatus()

```java
public AdminOrderResponse confirmOrder(Long id) {
    return transitionStatus(id, OrderStatus.PENDING, OrderStatus.CONFIRMED, "confirm");
}
```

Delegates to the shared `transitionStatus()` helper with:
```
from   = PENDING    ← order must currently be in this state
to     = CONFIRMED  ← change it to this state
action = "confirm"  ← used only in error messages
```

---

## Layer 5 — transitionStatus() Execution

```
Step 1: Find order 21
  orderRepository.findById(21) → Order { status=PENDING, ... }

Step 2: Guard — is current status == from?
  order.status  = PENDING
  from          = PENDING
  equal → ✅ continue

Step 3: Update status in memory
  order.setStatus(CONFIRMED)

Step 4: Save to DB
  UPDATE orders SET status='CONFIRMED', updated_at=NOW() WHERE id=21;

Step 5: Log
  INFO Order ORD-20260318-ABC transitioned: PENDING → CONFIRMED

Step 6: Map and return
  orderMapper.toAdminResponse(saved) → AdminOrderResponse
```

**If order was already CONFIRMED (invalid transition):**
```
order.status  = CONFIRMED
from          = PENDING
CONFIRMED != PENDING → ❌
throws InvalidOrderStateException:
  "Cannot confirm an order with status: CONFIRMED. Required status: PENDING"
  → 400 BAD REQUEST
```

---

## Final HTTP Response

```
HTTP/1.1 200 OK

{
  "orderId": 21,
  "orderNumber": "ORD-20260318-2006A4B8",
  "userId": "d43d639a-...",
  "userEmail": "vaibhav@test.com",
  "status": "CONFIRMED",
  "totalAmount": 77000.00,
  "shippingAddress": "123 MG Road, Bangalore",
  "createdAt": "2026-03-18T09:36:38",
  "updatedAt": "2026-03-18T18:50:33",   ← updated timestamp
  "items": [ ... ]
}
```

---

> **PATCH /ship and PATCH /deliver follow the exact same flow.**
> The only difference is the `from` and `to` values passed to `transitionStatus()`:
>
> | Endpoint  | from      | to        |
> |-----------|-----------|-----------|
> | /confirm  | PENDING   | CONFIRMED |
> | /ship     | CONFIRMED | SHIPPED   |
> | /deliver  | SHIPPED   | DELIVERED |

---
---

# DELETE /api/v1/admin/orders/{id} — Admin Cancel Order

## Sample Request

```
DELETE /api/v1/admin/orders/20
Authorization: Bearer eyJhbGci...
```

---

## Layer 1 — Spring Security

JWT verified. `ADMIN` role checked.

---

## Layer 2 — Path Variable

```
URL: /api/v1/admin/orders/20
Spring extracts: Long id = 20
```

---

## Layer 3 — Admin Controller

```java
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> cancelOrder(@PathVariable Long id) {
    orderService.adminCancelOrder(id);
    return ResponseEntity.noContent().build();
}
```

---

## Layer 4 — Order Service

```java
public void adminCancelOrder(Long id) {
    Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));

    if (order.getStatus() == OrderStatus.SHIPPED    ||
        order.getStatus() == OrderStatus.DELIVERED  ||
        order.getStatus() == OrderStatus.CANCELLED) {
        throw new InvalidOrderStateException(
                "Cannot cancel an order with status: " + order.getStatus() + ". " +
                "Only PENDING and CONFIRMED orders can be cancelled."
        );
    }

    restoreStockForOrder(order);
    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);
}
```

**Admin cancel vs Customer cancel — key difference:**

```
Customer cancel:  only PENDING  → allowed
Admin cancel:     PENDING       → allowed
                  CONFIRMED     → allowed  ← admin has more power
                  SHIPPED       → NOT allowed (already dispatched)
                  DELIVERED     → NOT allowed (already delivered)
                  CANCELLED     → NOT allowed (already done)
```

---

## Layer 5 — Stock Restore (same as customer cancel)

```
For each OrderItem in the order:
  PATCH /api/v1/inventories/bike/{bikeId}/restore?quantity={quantity}

If restore fails → log error, continue, order still cancelled.
```

---

## Layer 6 — Save

```sql
UPDATE orders SET status='CANCELLED', updated_at=NOW() WHERE id=20;
```

---

## Final HTTP Response

```
HTTP/1.1 204 No Content
```

---

## Complete State Machine — All Valid Transitions

```
                  confirmOrder()           shipOrder()          deliverOrder()
                      ↓                       ↓                      ↓
[PENDING] ──────────────→ [CONFIRMED] ──────────→ [SHIPPED] ──────────→ [DELIVERED]
    │                          │
    │ cancelMyOrder()           │ adminCancelOrder()
    │ (customer — PENDING only) │ (admin — PENDING or CONFIRMED)
    ↓                          ↓
[CANCELLED]               [CANCELLED]


Guards enforced by transitionStatus():
  confirmOrder → order MUST be PENDING    → else 400
  shipOrder    → order MUST be CONFIRMED  → else 400
  deliverOrder → order MUST be SHIPPED    → else 400

Guards enforced by adminCancelOrder():
  cancel       → order MUST NOT be SHIPPED, DELIVERED, CANCELLED → else 400

Guards enforced by cancelMyOrder():
  cancel       → order MUST be PENDING → else 400
  cancel       → userId must match JWT → else 403
```

---

## All APIs — Quick Reference

| Method   | Endpoint                          | Role     | Returns | Purpose                        |
|----------|-----------------------------------|----------|---------|--------------------------------|
| POST     | /api/v1/orders                    | CUSTOMER | 201     | Place new order                |
| GET      | /api/v1/orders                    | CUSTOMER | 200     | My order history               |
| GET      | /api/v1/orders/{id}               | CUSTOMER | 200     | Single order (own only)        |
| DELETE   | /api/v1/orders/{id}               | CUSTOMER | 204     | Cancel PENDING order           |
| GET      | /api/v1/admin/orders              | ADMIN    | 200     | All orders, paginated          |
| GET      | /api/v1/admin/orders/{id}         | ADMIN    | 200     | Any order (full details)       |
| PATCH    | /api/v1/admin/orders/{id}/confirm | ADMIN    | 200     | PENDING → CONFIRMED            |
| PATCH    | /api/v1/admin/orders/{id}/ship    | ADMIN    | 200     | CONFIRMED → SHIPPED            |
| PATCH    | /api/v1/admin/orders/{id}/deliver | ADMIN    | 200     | SHIPPED → DELIVERED            |
| DELETE   | /api/v1/admin/orders/{id}         | ADMIN    | 204     | Cancel + restore stock         |
