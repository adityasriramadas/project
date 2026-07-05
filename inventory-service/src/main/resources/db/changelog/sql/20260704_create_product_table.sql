CREATE TABLE IF NOT EXISTS product (
    id                  VARCHAR(255) PRIMARY KEY,
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(500),
    category            VARCHAR(80),
    brand               VARCHAR(80),
    quantity            INTEGER NOT NULL,
    reorder_threshold   INTEGER NOT NULL DEFAULT 10,
    unit_price          NUMERIC(12, 2),
    currency            VARCHAR(3) DEFAULT 'USD',
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0
);