import { expect, test } from '@playwright/test';
import { createHmac } from 'node:crypto';

test.describe.configure({ mode: 'serial' });

const jwtSecret = process.env.E2E_JWT_SECRET ?? 'dev-jwt-signing-secret-change-me-32chars-minimum';

function base64Url(value: string | Buffer) {
  return Buffer.from(value)
    .toString('base64')
    .replace(/=/g, '')
    .replace(/\+/g, '-')
    .replace(/\//g, '_');
}

function createJwt() {
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = base64Url(JSON.stringify({
    sub: 'playwright-e2e',
    iat: now,
    exp: now + 60 * 30,
  }));
  const signature = base64Url(createHmac('sha256', jwtSecret).update(`${header}.${payload}`).digest());

  return `${header}.${payload}.${signature}`;
}

test('all inventory and order APIs pass through the gateway', async ({ request, baseURL }) => {
  expect(baseURL, 'GATEWAY_BASE_URL must point to the API gateway').toBeTruthy();
  const gatewayUrl = new URL(baseURL!);
  expect(['8081', '8082'], 'Do not run E2E tests against backing services directly').not.toContain(gatewayUrl.port);

  const authHeaders = { Authorization: `Bearer ${createJwt()}` };
  const runId = Date.now();
  const productId = `E2E-${runId}`;
  const idempotencyKey = `e2e-${runId}`;

  const health = await request.get('/actuator/health');
  expect(health.ok()).toBeTruthy();

  const createProduct = await request.post('/inventory/products', {
    headers: authHeaders,
    data: {
      id: productId,
      name: 'E2E Gateway Product',
      description: 'Created by Playwright gateway API test',
      category: 'E2E',
      brand: 'Gateway',
      quantity: 12,
      reorderThreshold: 10,
      unitPrice: 19.99,
      currency: 'USD',
      active: true,
    },
  });
  expect(createProduct.status()).toBe(201);
  const createdProduct = await createProduct.json();
  expect(createdProduct).toMatchObject({
    success: true,
    data: {
      id: productId,
      name: 'E2E Gateway Product',
      quantity: 12,
      reorderThreshold: 10,
      currency: 'USD',
      active: true,
      lowStock: false,
    },
  });

  const getProduct = await request.get(`/inventory/products/${productId}`, { headers: authHeaders });
  expect(getProduct.ok()).toBeTruthy();
  expect(await getProduct.json()).toMatchObject({ data: { id: productId } });

  const listProducts = await request.get('/inventory/products?size=100', { headers: authHeaders });
  expect(listProducts.ok()).toBeTruthy();
  const listedProducts = await listProducts.json();
  expect(listedProducts.data.content.some((product: { id: string }) => product.id === productId)).toBeTruthy();

  const updateProduct = await request.put(`/inventory/products/${productId}`, {
    headers: authHeaders,
    data: {
      name: 'E2E Gateway Product Updated',
      description: 'Updated by Playwright gateway API test',
      category: 'E2E',
      brand: 'Gateway',
      quantity: 4,
      reorderThreshold: 10,
      unitPrice: 19.99,
      currency: 'USD',
      active: true,
    },
  });
  expect(updateProduct.ok()).toBeTruthy();
  expect(await updateProduct.json()).toMatchObject({
    data: {
      id: productId,
      name: 'E2E Gateway Product Updated',
      quantity: 4,
      lowStock: true,
    },
  });

  const listLowStock = await request.get('/inventory/products?lowStockOnly=true&size=100', { headers: authHeaders });
  expect(listLowStock.ok()).toBeTruthy();
  const lowStockProducts = await listLowStock.json();
  expect(lowStockProducts.data.content.some((product: { id: string; lowStock: boolean }) =>
    product.id === productId && product.lowStock === true)).toBeTruthy();

  const reserveStock = await request.post('/inventory/reserve', {
    headers: authHeaders,
    data: { productId, quantity: 1 },
  });
  expect(reserveStock.ok()).toBeTruthy();
  expect(await reserveStock.json()).toMatchObject({
    data: { productId, quantity: 1, status: 'RESERVED' },
  });

  const releaseStock = await request.post('/inventory/release', {
    headers: authHeaders,
    data: { productId, quantity: 1 },
  });
  expect(releaseStock.ok()).toBeTruthy();
  expect(await releaseStock.json()).toMatchObject({
    data: { productId, quantity: 1, status: 'RELEASED' },
  });

  const createOrder = await request.post('/orders', {
    headers: {
      ...authHeaders,
      'Idempotency-Key': idempotencyKey,
    },
    data: {
      productId,
      quantity: 2,
      customerId: `CUST-${runId}`,
      customerName: 'Playwright Customer',
      customerEmail: 'playwright@example.com',
      shippingAddress: '1 Gateway Test Road',
      unitPrice: 19.99,
      currency: 'USD',
      notes: 'gateway-only e2e test',
    },
  });
  expect(createOrder.status()).toBe(201);
  const createdOrder = await createOrder.json();
  expect(createdOrder).toMatchObject({
    productId,
    quantity: 2,
    status: 'CONFIRMED',
    idempotencyKey,
    currency: 'USD',
  });

  const orderId = createdOrder.id;
  expect(orderId).toBeTruthy();

  const replayOrder = await request.post('/orders', {
    headers: {
      ...authHeaders,
      'Idempotency-Key': idempotencyKey,
    },
    data: {
      productId,
      quantity: 2,
      customerId: `CUST-${runId}`,
      customerName: 'Playwright Customer',
      customerEmail: 'playwright@example.com',
      shippingAddress: '1 Gateway Test Road',
      unitPrice: 19.99,
      currency: 'USD',
      notes: 'gateway-only e2e test',
    },
  });
  expect(replayOrder.status()).toBe(200);
  expect(await replayOrder.json()).toMatchObject({ id: orderId, idempotencyKey });

  const listOrders = await request.get(`/orders?status=CONFIRMED&productId=${productId}&size=100`, { headers: authHeaders });
  expect(listOrders.ok()).toBeTruthy();
  const listedOrders = await listOrders.json();
  expect(listedOrders.content.some((order: { id: string }) => order.id === orderId)).toBeTruthy();

  const getOrder = await request.get(`/orders/${orderId}`, { headers: authHeaders });
  expect(getOrder.ok()).toBeTruthy();
  expect(await getOrder.json()).toMatchObject({ id: orderId, productId, status: 'CONFIRMED' });

  const cancelOrder = await request.post(`/orders/${orderId}/cancel`, { headers: authHeaders });
  expect(cancelOrder.ok()).toBeTruthy();
  expect(await cancelOrder.json()).toMatchObject({ message: 'cancelled' });

  const getCancelledOrder = await request.get(`/orders/${orderId}`, { headers: authHeaders });
  expect(getCancelledOrder.ok()).toBeTruthy();
  expect(await getCancelledOrder.json()).toMatchObject({ id: orderId, status: 'CANCELLED' });
});
