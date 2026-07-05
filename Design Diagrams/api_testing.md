# API Testing Guide

Use this guide to test the Order and Inventory APIs from Swagger UI through the API Gateway.

## 1. Start Or Verify Services

From the project root:

```bash
docker compose up -d --build
docker compose ps
```

All of these services should be running:

- `gateway-service`
- `order`
- `inventory`
- `postgres`
- `redis`

## 2. Open Swagger UI

Use the gateway URLs only:

- Inventory Swagger UI: `http://localhost:8080/inventory/swagger-ui/index.html`
- Order Swagger UI: `http://localhost:8080/order/swagger-ui/index.html`

Do not use direct service URLs for normal API testing.

## 3. Generate Authorization Token

Business APIs require a Bearer JWT.

Use this PowerShell script:

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
$payload = @{ sub='swagger-test-user'; iss='local'; iat=$now; exp=($now + 3600); scope='api' }
$unsigned = "$(JsonB64 $header).$(JsonB64 $payload)"
$hmac = [Security.Cryptography.HMACSHA256]::new([Text.Encoding]::UTF8.GetBytes($secret))
$token = "$unsigned.$(B64Url ($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned))))"
$token
```

In Swagger, click **Authorize** and enter only the raw token value:

```text
<token>
```

Then click **Authorize** and close the dialog. Swagger adds the `Bearer` prefix automatically.

For tools outside Swagger, such as curl or Postman, use this full HTTP header:

```text
Authorization: Bearer <token>
```

## 4. Inventory API Testing Order

Open:

```text
http://localhost:8080/inventory/swagger-ui/index.html
```

### 4.1 Create Or Update Product

Endpoint:

```text
POST /inventory/product
```

Payload:

```json
{
  "id": "PROD-SWAGGER-001",
  "name": "Swagger Test Product",
  "description": "Product created from Swagger testing",
  "category": "Testing",
  "brand": "Local",
  "quantity": 100,
  "reorderThreshold": 10,
  "unitPrice": 49.99,
  "currency": "USD",
  "active": true
}
```

Expected status:

```text
200 OK
```

Check response:

- `success` is `true`
- `data.id` is `PROD-SWAGGER-001`
- `data.quantity` is `100`

### 4.2 List Products

Endpoint:

```text
GET /inventory/product
```

Expected status:

```text
200 OK
```

Check response:

- `success` is `true`
- response contains paginated product data

### 4.3 List Low Stock Products

Endpoint:

```text
GET /inventory/product?lowStockOnly=true
```

Expected status:

```text
200 OK
```

### 4.4 Get Product By ID

Endpoint:

```text
GET /inventory/product/{id}
```

Use:

```text
PROD-SWAGGER-001
```

Expected status:

```text
200 OK
```

Check response:

- `data.id` is `PROD-SWAGGER-001`

### 4.5 Reserve Inventory

Endpoint:

```text
POST /inventory/reserve
```

Payload:

```json
{
  "productId": "PROD-SWAGGER-001",
  "quantity": 5
}
```

Expected status:

```text
200 OK
```

Check response:

- `data.productId` is `PROD-SWAGGER-001`
- `data.quantity` is `5`
- `data.status` is `RESERVED`

### 4.6 Release Inventory

Endpoint:

```text
POST /inventory/release
```

Payload:

```json
{
  "productId": "PROD-SWAGGER-001",
  "quantity": 5
}
```

Expected status:

```text
200 OK
```

Check response:

- `data.productId` is `PROD-SWAGGER-001`
- `data.quantity` is `5`
- `data.status` is `RELEASED`

## 5. Order API Testing Order

Open:

```text
http://localhost:8080/order/swagger-ui/index.html
```

Make sure `PROD-SWAGGER-001` exists before creating an order.

### 5.1 Create Order

Endpoint:

```text
POST /order
```

Required header:

```text
Idempotency-Key: any-unique-guid-or-string
```

Payload:

```json
{
  "productId": "PROD-SWAGGER-001",
  "quantity": 2,
  "customerId": "CUST-SWAGGER-001",
  "customerName": "Swagger User",
  "customerEmail": "swagger@example.com",
  "shippingAddress": "Swagger test address",
  "unitPrice": 49.99,
  "currency": "USD",
  "notes": "Created from Swagger API testing"
}
```

Expected status:

```text
200 OK
```

Check response:

- `success` is `true`
- `data.productId` is `PROD-SWAGGER-001`
- `data.status` is `CONFIRMED`
- Save `data.id` for the next tests

### 5.2 List Orders

Endpoint:

```text
GET /order
```

Expected status:

```text
200 OK
```

### 5.3 List Orders By Status

Endpoint:

```text
GET /order?status=CONFIRMED
```

Expected status:

```text
200 OK
```

### 5.4 List Orders By Product

Endpoint:

```text
GET /order?productId=PROD-SWAGGER-001
```

Expected status:

```text
200 OK
```

### 5.5 Get Order By ID

Endpoint:

```text
GET /order/{id}
```

Use the order ID returned from create order.

Expected status:

```text
200 OK
```

### 5.6 Cancel Order

Endpoint:

```text
POST /order/{id}/cancel
```

Use the order ID returned from create order.

Expected status:

```text
200 OK
```

Check response:

- `data` is `true`

### 5.7 Verify Cancelled Order

Endpoint:

```text
GET /order/{id}
```

Expected status:

```text
200 OK
```

Check response:

- `data.status` is `CANCELLED`

## 6. Common Status Checks

| Status | Meaning | Action |
| --- | --- | --- |
| `200 OK` | API call passed | Verify response body |
| `400 Bad Request` | Invalid request body/header | Check required fields and validation messages |
| `401 Unauthorized` | Missing or invalid JWT | Add the raw JWT token in Swagger Authorize |
| `404 Not Found` | Resource or path not found | Check URL path and IDs |
| `409 Conflict` | Business rule failed | Check inventory quantity, product active status, order status, or idempotency key |
| `503 Service Unavailable` | Downstream service unavailable | Check Docker containers and service logs |

## 7. Pass Criteria

API testing passes when:

- Swagger UI opens through the gateway.
- Authorization is added successfully.
- All Inventory API calls return expected statuses.
- A product can be created, read, reserved, and released.
- An order can be created, listed, read, cancelled, and verified as cancelled.
- No business API is tested through direct service URLs.
