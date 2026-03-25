# Notification Service — Complete Documentation

> **What this document covers:**
> Architecture, message flow, RabbitMQ design, handler pattern,
> Dead Letter Queue, email setup, SOLID principles applied,
> and every class's single responsibility.

---

## What This Service Does

Listens to RabbitMQ for order lifecycle events published by order-service
and sends the appropriate email to the customer via Mailtrap (dev) or real SMTP (prod).

```
order-service                  RabbitMQ                  notification-service
     │                            │                              │
     │── publishes event ────────►│                              │
     │   routing key:             │── delivers to queue ────────►│
     │   "order.confirmed"        │   notification.queue         │
     │                            │                              │── sends email
     │                            │                              │   to customer
```

---

## Architecture — Before vs After RabbitMQ

### Before (HTTP — Disabled, Kept for Reference)

```
order-service
  NotificationAsyncSender (@Async, @Component DISABLED)
      │
      │── POST /api/v1/notifications/order-event ──► notification-service
      │                                               NotificationController (@RestController DISABLED)
      │
      PROBLEM: if notification-service is down → email lost forever
```

### After (RabbitMQ — Current)

```
order-service
  RabbitMQEventPublisher
      │
      │── rabbitTemplate.convertAndSend() ──► order.events exchange
      │                                              │
      │                                    notification.queue
      │                                              │
      │                                    NotificationListener (@RabbitListener)
      │                                              │
      │                                    NotificationService (routes to handler)
      │                                              │
      │                                    OrderPlacedHandler / OrderConfirmedHandler / ...
      │                                              │
      │                                    EmailService → Mailtrap / real SMTP

      BENEFIT: if notification-service is down → message stays in queue
               delivered when service comes back up ✅
```

---

## RabbitMQ Infrastructure

### Exchange

```
Name:  order.events
Type:  Topic
Owner: declared by both order-service and notification-service (idempotent)

WHY TOPIC?
  Supports wildcard routing key patterns (* and #).
  Future services can subscribe selectively:
    notification-service → "order.#"          (all order events)
    loyalty-service      → "order.delivered"  (only deliveries)
    inventory-service    → "order.cancelled"  (only cancellations)
  Adding a new consumer = new queue + new binding. ZERO changes to exchange.
```

### Queues

```
notification.queue
  durable: true                          → survives RabbitMQ restart
  x-dead-letter-exchange: notification   → on rejection, route to DLQ exchange
                          .dlq.exchange

notification.dlq
  durable: true                          → failed messages never lost
  purpose: stores messages that failed   → inspect and re-publish manually
           after processing

```

### Binding

```
exchange:     order.events
queue:        notification.queue
routing key:  "order.#"

Matches:
  order.placed     ✅
  order.confirmed  ✅
  order.shipped    ✅
  order.delivered  ✅
  order.cancelled  ✅
  payment.placed   ❌ (different prefix — not routed here)
```

### How Routing Key is Derived (order-service side)

```
OrderEventType enum → routing key
──────────────────────────────────
ORDER_PLACED    → "order.placed"
ORDER_CONFIRMED → "order.confirmed"
ORDER_SHIPPED   → "order.shipped"
ORDER_DELIVERED → "order.delivered"
ORDER_CANCELLED → "order.cancelled"

Code: event.getType().name().toLowerCase().replace("_", ".")
```

---

## Message Flow — Step by Step

```
Step 1 — order-service publishes to RabbitMQ
  RabbitMQEventPublisher.publish(event)
  rabbitTemplate.convertAndSend("order.events", "order.confirmed", event)
  Event serialised as JSON by JacksonJsonMessageConverter

Step 2 — RabbitMQ routes to notification.queue
  Exchange reads routing key "order.confirmed"
  Matches binding pattern "order.#"
  Delivers JSON message to notification.queue
  Message status: READY → UNACKNOWLEDGED (held until consumer confirms)

Step 3 — NotificationListener receives from queue
  @RabbitListener(queues = "notification.queue")
  Spring AMQP deserialises JSON → OrderNotificationEvent
  Calls notificationService.handleOrderEvent(event)

Step 4 — NotificationService routes to correct handler
  Looks up handler in Map<String, OrderEventHandler>:
    "ORDER_CONFIRMED" → OrderConfirmedHandler
  Calls handler.handle(event)

Step 5 — Handler sends email
  OrderConfirmedHandler.handle(event)
  emailService.sendHtmlEmail(
    to:       event.getUserEmail()
    subject:  "We Are Preparing Your Order — ORD-20260324-BC018EAA"
    template: "email/order-confirmed"
    context:  event
  )

Step 6 — EmailService renders and sends
  Thymeleaf renders HTML template with event data
  JavaMailSender sends via SMTP to Mailtrap
  Message ACKed → permanently removed from queue ✅

On Failure:
  EmailService throws MailAuthenticationException
  NotificationListener throws exception
  Spring AMQP sends NACK (defaultRequeueRejected = false)
  RabbitMQ routes to x-dead-letter-exchange → notification.dlq
  Message sits safely in DLQ for investigation ✅
```

---

## Event Payload — OrderNotificationEvent

```json
{
  "type": "ORDER_CONFIRMED",
  "orderId": 23,
  "orderNumber": "ORD-20260324-BC018EAA",
  "userEmail": "vaibhav@test.com",
  "userName": "Vaibhav",
  "occurredAt": "2026-03-24T10:05:00Z",
  "shippingAddress": "123 MG Road, Mumbai, Maharashtra - 400001",
  "totalAmount": 323000.00,
  "items": [
    {
      "bikeName": "Hero Splendor Plus",
      "quantity": 2,
      "unitPrice": 75000.00,
      "subtotal": 150000.00
    }
  ],
  "metadata": {
    "trackingId": null,
    "estimatedDelivery": "2026-03-27",
    "cancellationReason": null
  }
}
```

### Metadata Field Usage Per Event Type

| Field | Present For |
|---|---|
| `trackingId` | ORDER_SHIPPED only |
| `estimatedDelivery` | ORDER_CONFIRMED, ORDER_SHIPPED |
| `cancellationReason` | ORDER_CANCELLED only |
| All others | All event types |

---

## Handler Pattern — Open/Closed Principle

### The Interface

```java
public interface OrderEventHandler {
    OrderEventType getSupportedType();   // tells NotificationService: I handle this
    void handle(OrderNotificationEvent event);
}
```

### Handlers and Their Emails

| Handler | Event | Email Subject |
|---|---|---|
| `OrderPlacedHandler` | ORDER_PLACED | Order Confirmation |
| `OrderConfirmedHandler` | ORDER_CONFIRMED | We Are Preparing Your Order |
| `OrderShippedHandler` | ORDER_SHIPPED | Your Bike Is On The Way |
| `OrderDeliveredHandler` | ORDER_DELIVERED | Your Bike Has Arrived |
| `OrderCancelledHandler` | ORDER_CANCELLED | Your Order Has Been Cancelled |

### How NotificationService Routes

```java
// Built at startup — Spring collects all OrderEventHandler beans
Map<String, OrderEventHandler> handlers = {
  "ORDER_PLACED"    → OrderPlacedHandler,
  "ORDER_CONFIRMED" → OrderConfirmedHandler,
  "ORDER_SHIPPED"   → OrderShippedHandler,
  "ORDER_DELIVERED" → OrderDeliveredHandler,
  "ORDER_CANCELLED" → OrderCancelledHandler
}

// On each event:
handlers.get(event.getType().name()).handle(event)
```

**Adding a new event type tomorrow:**
```
✅ Create OrderReturnedHandler implements OrderEventHandler
✅ Add ORDER_RETURNED to enum
❌ NotificationService — ZERO changes
❌ NotificationListener — ZERO changes
❌ RabbitMQConfig — ZERO changes
```
This is the Open/Closed Principle — open for extension, closed for modification.

---

## Dead Letter Queue (DLQ)

### What It Is

A safety net queue that receives messages which failed processing after the listener threw an exception.

### Two Configs That Work Together

```
Config 1 — Source queue declares its dead letter exchange
  QueueBuilder.durable("notification.queue")
    .withArgument("x-dead-letter-exchange", "notification.dlq.exchange")
  Tells RabbitMQ: "if a message is rejected from me, send it here"

Config 2 — Spring factory configured to reject on failure
  factory.setDefaultRequeueRejected(false)
  Tells Spring: "when listener throws exception → NACK without requeue"

Without Config 1: message dropped forever on rejection ❌
Without Config 2: message loops forever (default requeue=true) ❌
Both together:    message safely lands in notification.dlq ✅
```

### ACK vs NACK

```
Message delivered to NotificationListener
  │
  │  status: UNACKNOWLEDGED (RabbitMQ still holds it)
  │
  ├── processing succeeds → Spring sends ACK → message permanently deleted ✅
  │
  └── processing fails  → Spring sends NACK + requeue=false
                               │
                               ▼
                        RabbitMQ reads x-dead-letter-exchange
                               │
                               ▼
                        notification.dlq.exchange
                               │
                               ▼
                        notification.dlq ✅
```

### Inspecting DLQ

```
RabbitMQ Management UI → http://localhost:15672
  Queues → notification.dlq → Get Messages

Shows the full JSON payload of the failed event.
Fix the bug → re-publish the message manually → email delivered.
```

---

## Email Configuration

### Current (Development)

```yaml
# notification-service.yaml in config-server
spring:
  rabbitmq:
    host: rabbitmq
    port: 5672
    username: guest
    password: guest

  mail:
    host: sandbox.smtp.mailtrap.io
    port: 2525
    username: <mailtrap-smtp-username>
    password: <mailtrap-smtp-password>

app:
  mail:
    from: noreply@hero-bikestore.com
```

Emails sent to Mailtrap sandbox never reach real inboxes.
View them at https://mailtrap.io → Email Testing → Inboxes.

### Switching to Production

Only the `spring.mail.*` block changes. Zero code changes needed.

```yaml
# Production
spring:
  mail:
    host: live.smtp.mailtrap.io      # "live" not "sandbox"
    port: 587
    username: api
    password: <production-api-token>
```

---

## Class Responsibilities (SOLID — Single Responsibility)

| Class | Package | Single Job |
|---|---|---|
| `NotificationListener` | `listener` | Consume message from queue → delegate to service |
| `NotificationService` | `service` | Look up correct handler in map → call handler |
| `OrderPlacedHandler` | `handler` | Know subject + template for ORDER_PLACED |
| `OrderConfirmedHandler` | `handler` | Know subject + template for ORDER_CONFIRMED |
| `OrderShippedHandler` | `handler` | Know subject + template for ORDER_SHIPPED |
| `OrderDeliveredHandler` | `handler` | Know subject + template for ORDER_DELIVERED |
| `OrderCancelledHandler` | `handler` | Know subject + template for ORDER_CANCELLED |
| `EmailService` | `service` | Render Thymeleaf template → send via SMTP |
| `RabbitMQConfig` | `config` | Declare queue, exchange, binding, DLQ infrastructure |
| `SecurityConfig` | `config` | Permit all requests (internal service, no auth needed) |

---

## Disabled Code — Kept for Reference

| File | Why Disabled | How to Re-enable |
|---|---|---|
| `NotificationController.java` | Replaced by `NotificationListener` | Restore `@RestController` |

The HTTP approach is kept in the codebase as a reference for:
- Understanding the evolution from HTTP → RabbitMQ
- Reverting if RabbitMQ needs to be removed
- Learning comparison between the two approaches

---

## SOLID Principles Applied

| Principle | Where Applied |
|---|---|
| **S** — Single Responsibility | Each handler class handles exactly ONE event type |
| **O** — Open/Closed | New event type = new handler only, nothing else changes |
| **L** — Liskov Substitution | Any `OrderEventHandler` impl can replace another in the map |
| **I** — Interface Segregation | `OrderEventHandler` has only 2 methods — no forced methods |
| **D** — Dependency Inversion | `NotificationListener` depends on `NotificationService` abstraction |

---

## Ports and Endpoints

| | Value |
|---|---|
| Service port | `8085` |
| Actuator health | `http://localhost:8085/actuator/health` |
| RabbitMQ UI | `http://localhost:15672` (guest/guest) |
| Swagger UI | `http://localhost:8085/swagger-ui/index.html` |

> Notification-service is **not exposed via API Gateway** — it is an internal service.
> It receives events only from RabbitMQ, not from external HTTP clients.
