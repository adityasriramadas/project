# Inventory Service

`inventory-service` stores products and manages stock quantity.

## Responsibilities

- Create or update products.
- List products.
- Fetch one product by ID.
- Reserve stock for orders.
- Release stock when orders are cancelled.
- Report low-stock products.

## API Base Path

The controller base path is:

```text
/inventory
```

In local development, call it through the gateway:

```text
http://localhost:8080/inventory
```

## Endpoints

| Method | Path | Notes |
| --- | --- | --- |
| `POST` | `/inventory/product` | Creates a product, or updates it if the ID already exists |
| `GET` | `/inventory/product` | Lists products |
| `GET` | `/inventory/product?lowStockOnly=true` | Lists low-stock products |
| `GET` | `/inventory/product/{id}` | Gets one product |
| `POST` | `/inventory/reserve` | Decreases product quantity |
| `POST` | `/inventory/release` | Increases product quantity |

## Product Request

```json
{
  "id": "PROD-001",
  "name": "Wireless Mouse",
  "description": "Ergonomic wireless mouse",
  "category": "Electronics",
  "brand": "Logitech",
  "quantity": 100,
  "reorderThreshold": 10,
  "unitPrice": 49.99,
  "currency": "USD",
  "active": true
}
```

## Reserve Or Release Request

```json
{
  "productId": "PROD-001",
  "quantity": 5
}
```

## Business Rules

- Product quantity cannot be negative.
- Reserve/release quantity must be positive.
- Reserving inactive products is rejected.
- Reserving more than available quantity is rejected.
- `lowStockOnly=true` returns products with quantity less than or equal to the threshold.

## Data

The service uses:

- Database: `inventorydb`
- Main table: `product`
- Schema migration: Liquibase

## Swagger

Use the gateway Swagger URL:

```text
http://localhost:8080/inventory/swagger-ui/index.html
```

See [../local_setup.md](../local_setup.md) for setup and local commands.
