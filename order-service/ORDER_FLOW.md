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
