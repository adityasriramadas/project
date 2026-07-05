# Local Setup

This file contains the local setup and run instructions for the order/inventory microservices.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker Desktop with Docker Compose
- Optional: DBeaver or another PostgreSQL client
- Optional: Postman/curl/PowerShell for API testing

## Ports

| Component | Host Port | Notes |
| --- | --- | --- |
| Gateway | `8080` | Use this for API calls |
| PostgreSQL | `5432` | Exposed by `docker-compose.override.yml` |
| Redis | `6379` | Exposed by `docker-compose.override.yml` |

The order and inventory containers listen internally on `8081` and `8082`, but only the gateway is exposed for normal API use.

## Start The Stack

From the repository root:

```bash
docker compose up -d --build
```

Check status:

```bash
docker compose ps
```

Follow logs:

```bash
docker compose logs -f gateway-service order inventory
```

Stop containers:

```bash
docker compose down
```

Stop containers and delete the local database volume:

```bash
docker compose down -v
```

## Rebuild Or Redeploy Services

Rebuild and recreate everything:

```bash
docker compose up -d --build
```

Recreate application containers using already-built images:

```bash
docker compose up -d --force-recreate inventory order gateway-service
```

Rebuild one service:

```bash
docker compose build inventory
docker compose up -d inventory
```

Use `order` or `gateway-service` in place of `inventory` for the other services.

## Database Setup

`docker-compose.override.yml` mounts:

```text
./db/init.sql:/docker-entrypoint-initdb.d/init.sql
```

On the first PostgreSQL startup, that script creates:

- `inventorydb`
- `orderdb`
- `appuser` with password `app_pass`

Liquibase creates service tables when the Java services start.

## PostgreSQL Connection For DBeaver

Use:

```text
Host: localhost
Port: 5432
Database: orderdb or inventorydb
Username: appuser
Password: app_pass
```

The local PostgreSQL timezone is configured as `Asia/Kolkata`.

If DBeaver sends the old timezone ID `Asia/Calcutta`, change the DBeaver driver/JVM timezone to:

```text
Asia/Kolkata
```

## Swagger URLs

- Order Swagger UI: `http://localhost:8080/order/swagger-ui/index.html`
- Order OpenAPI JSON: `http://localhost:8080/order/api-docs`
- Inventory Swagger UI: `http://localhost:8080/inventory/swagger-ui/index.html`
- Inventory OpenAPI JSON: `http://localhost:8080/inventory/api-docs`

## Generate A Local JWT

Business APIs require a Bearer token. The local Docker secret is:

```text
dev-jwt-signing-secret-change-me-32chars-minimum
```

PowerShell token generator:

```powershell
$secret = 'dev-jwt-signing-secret-change-me-32chars-minimum'
function B64Url([byte[]]$bytes) {
  [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_')
}
function JsonB64($obj) {
  B64Url ([Text.Encoding]::UTF8.GetBytes(($obj | ConvertTo-Json -Compress)))
}
$header = @{ alg='HS256'; typ='JWT' }
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$payload = @{ sub='local-user'; iss='local'; iat=$now; exp=($now + 3600); scope='api' }
$unsigned = "$(JsonB64 $header).$(JsonB64 $payload)"
$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
$token = "$unsigned.$(B64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned))))"
$token
```

Use the result as:

```text
Authorization: Bearer <token>
```

## Smoke Test Commands

Set variables in PowerShell:

```powershell
$base = 'http://localhost:8080'
$headers = @{ Authorization = "Bearer $token" }
```

Create or update a product:

```powershell
$product = @{
  id = 'PROD-001'
  name = 'Wireless Mouse'
  description = 'Local test product'
  category = 'Electronics'
  brand = 'Logitech'
  quantity = 100
  reorderThreshold = 10
  unitPrice = 49.99
  currency = 'USD'
  active = $true
}
Invoke-RestMethod "$base/inventory/product" -Method Post -Headers $headers -ContentType 'application/json' -Body ($product | ConvertTo-Json)
```

List products:

```powershell
Invoke-RestMethod "$base/inventory/product" -Headers $headers
```

Create an order:

```powershell
$order = @{
  productId = 'PROD-001'
  quantity = 2
  customerId = 'CUST-001'
  customerName = 'Local User'
  customerEmail = 'local@example.com'
  shippingAddress = 'Local test address'
  unitPrice = 49.99
  currency = 'USD'
  notes = 'Local smoke test'
}
$orderHeaders = $headers.Clone()
$orderHeaders['Idempotency-Key'] = [guid]::NewGuid().ToString()
Invoke-RestMethod "$base/order" -Method Post -Headers $orderHeaders -ContentType 'application/json' -Body ($order | ConvertTo-Json)
```

List orders:

```powershell
Invoke-RestMethod "$base/order" -Headers $headers
```

## Maven Commands

Build all modules:

```bash
mvn clean package
```

Run order and inventory tests:

```bash
mvn -pl inventory-service,order-service -am test
```

Build one service and dependencies:

```bash
mvn -pl gateway-service -am package
```

Replace `gateway-service` with `order-service` or `inventory-service` as needed.

## Troubleshooting

If the gateway returns `401`, the route is working but the request needs a Bearer JWT.

If the gateway returns `404`, confirm the path:

- Order API base path is `/order`
- Inventory product path is `/inventory/product`

If a service takes time after restart, wait for the Swagger/OpenAPI URL to return `200`.

If local DB state is broken and can be deleted:

```bash
docker compose down -v
docker compose up -d --build
```
