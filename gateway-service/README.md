# Gateway Service

`gateway-service` is the only public HTTP entry point for the application.

## Responsibilities

- Route client requests to downstream services.
- Enforce JWT resource-server security for business APIs.
- Add/pass correlation IDs.
- Expose health and monitoring endpoints.
- Proxy Swagger/OpenAPI endpoints for order and inventory services.

## Routes

| Gateway Path | Destination |
| --- | --- |
| `/order/**` | `order-service` |
| `/orders/**` | `order-service` compatibility route |
| `/inventory/**` | `inventory-service` |
| `/order/swagger-ui/**`, `/order/api-docs/**` | order Swagger/OpenAPI |
| `/inventory/swagger-ui/**`, `/inventory/api-docs/**` | inventory Swagger/OpenAPI |

Use `/order` for order business APIs. The controller base path in `order-service` is `/order`.

## Local URLs

- Gateway: `http://localhost:8080`
- Health: `http://localhost:8080/actuator/health`
- Order Swagger: `http://localhost:8080/order/swagger-ui/index.html`
- Inventory Swagger: `http://localhost:8080/inventory/swagger-ui/index.html`

## Security

Swagger/OpenAPI and health endpoints are public. Business endpoints require:

```text
Authorization: Bearer <jwt>
```

The local Docker secret is:

```text
dev-jwt-signing-secret-change-me-32chars-minimum
```

See [../local_setup.md](../local_setup.md) for local token generation and run commands.
