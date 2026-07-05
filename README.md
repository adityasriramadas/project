# Order And Inventory Microservices

This repository contains a Spring Boot microservices system for product inventory and customer orders. API traffic is expected to go through the gateway service.

## Services

| Service | Purpose | Container Port |
| --- | --- | --- |
| `gateway-service` | Single API entry point, routing, JWT resource-server security | `8080` |
| `order-service` | Creates, lists, reads, and cancels orders | `8081` |
| `inventory-service` | Creates/updates products and reserves/releases stock | `8082` |
| `postgres` | PostgreSQL databases for order and inventory services | `5432` |
| `redis` | Cache backend | `6379` |

## Main Flow

1. Client calls `gateway-service` on `http://localhost:8080`.
2. Gateway routes `/order/**` to `order-service`.
3. Gateway routes `/inventory/**` to `inventory-service`.
4. `order-service` stores order data in `orderdb`.
5. `inventory-service` stores product data in `inventorydb`.
6. When an order is created or cancelled, `order-service` calls `inventory-service` to reserve or release stock.

## Documentation

- Local setup and commands: [local_setup.md](local_setup.md)
- Architecture overview: [Design Diagrams/architecture.png](Design%20Diagrams/architecture.png)
- Order flow diagram: [Design Diagrams/order-flow.png](Design%20Diagrams/order-flow.png)
- Product inventory flow diagram: [Design Diagrams/product-inventory-flow.png](Design%20Diagrams/product-inventory-flow.png)
- Gateway notes: [gateway-service/README.md](gateway-service/README.md)
- Order service notes: [order-service/README.md](order-service/README.md)
- Inventory service notes: [inventory-service/README.md](inventory-service/README.md)

## Gateway URLs

- Gateway base URL: `http://localhost:8080`
- Order Swagger UI: `http://localhost:8080/order/swagger-ui/index.html`
- Order OpenAPI JSON: `http://localhost:8080/order/api-docs`
- Inventory Swagger UI: `http://localhost:8080/inventory/swagger-ui/index.html`
- Inventory OpenAPI JSON: `http://localhost:8080/inventory/api-docs`

Business APIs require a Bearer JWT. Health and OpenAPI/Swagger endpoints are public.

## API Summary

### Order APIs

All order APIs should be called through the gateway with the `/order` base path:

- `POST /order`
- `GET /order`
- `GET /order?status=CONFIRMED`
- `GET /order?productId=PROD-001`
- `GET /order/{id}`
- `POST /order/{id}/cancel`

`POST /order` requires the `Idempotency-Key` header.

### Inventory APIs

All inventory APIs should be called through the gateway with the `/inventory` base path:

- `POST /inventory/product`
- `GET /inventory/product`
- `GET /inventory/product?lowStockOnly=true`
- `GET /inventory/product/{id}`
- `POST /inventory/reserve`
- `POST /inventory/release`

## Testing

Run service tests:

```bash
mvn -pl inventory-service,order-service -am test
```

Run the full Maven test suite:

```bash
mvn test
```

## Important Notes

- Use the gateway URL for business API testing.
- Direct service calls are useful only for debugging inside local development.
- Database schemas are managed by Liquibase on service startup.
- Local PostgreSQL timezone is configured as `Asia/Kolkata` in `docker-compose.override.yml`.
