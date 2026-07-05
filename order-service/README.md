# Order Service

`order-service` stores customer orders and coordinates inventory reservation/release with `inventory-service`.

## Responsibilities

- Create orders with idempotency support.
- List orders with optional status/product filters.
- Fetch one order by ID.
- Cancel confirmed orders.
- Reserve stock during order creation.
- Release stock during order cancellation.

## API Base Path

The controller base path is:

```text
/order
```

In local development, call it through the gateway:

```text
http://localhost:8080/order
```

## Endpoints

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/order` | Requires `Idempotency-Key` header |
| `GET` | `/order` | Lists orders |
| `GET` | `/order?status=CONFIRMED` | Filters by status |
| `GET` | `/order?productId=PROD-001` | Filters by product |
| `GET` | `/order/{id}` | Gets one order |
| `POST` | `/order/{id}/cancel` | Cancels a confirmed order |

## Create Order Request

```json
{
  "productId": "PROD-001",
  "quantity": 2,
  "customerId": "CUST-001",
  "customerName": "Test User",
  "customerEmail": "test@example.com",
  "shippingAddress": "Local test address",
  "unitPrice": 49.99,
  "currency": "USD",
  "notes": "Deliver during business hours"
}
```

## Business Rules

- New orders start as `PENDING`.
- If inventory reservation succeeds, the order becomes `CONFIRMED`.
- If inventory reservation fails, the order becomes `REJECTED`.
- Cancellation is allowed only for `CONFIRMED` orders.
- Reusing the same `Idempotency-Key` with the same request returns the existing order.
- Reusing the same `Idempotency-Key` with different order details returns a conflict.

## Data

The service uses:

- Database: `orderdb`
- Main table: `orders`
- Schema migration: Liquibase

## Swagger

Use the gateway Swagger URL:

```text
http://localhost:8080/order/swagger-ui/index.html
```

See [../local_setup.md](../local_setup.md) for setup and local commands.
