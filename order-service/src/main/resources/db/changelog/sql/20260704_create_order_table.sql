CREATE TABLE IF NOT EXISTS orders (
    id                  VARCHAR(255) PRIMARY KEY,
    product_id          VARCHAR(255) NOT NULL,
    quantity            INTEGER NOT NULL,
    status              VARCHAR(50) NOT NULL,
    idempotency_key     VARCHAR(255) NOT NULL UNIQUE,
    customer_id         VARCHAR(80),
    customer_name       VARCHAR(120),
    customer_email      VARCHAR(160),
    shipping_address    VARCHAR(500),
    unit_price          NUMERIC(12, 2),
    total_amount        NUMERIC(12, 2),
    currency            VARCHAR(3) DEFAULT 'USD',
    notes               VARCHAR(500),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0
);
