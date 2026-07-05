CREATE INDEX idx_orders_product_id
    ON orders (product_id);

CREATE INDEX idx_orders_status
    ON orders (status);

CREATE INDEX idx_orders_created_at
    ON orders (created_at);

CREATE INDEX idx_orders_product_status
    ON orders (product_id, status);

CREATE INDEX idx_orders_customer_id
    ON orders (customer_id);

CREATE INDEX idx_orders_customer_email
    ON orders (customer_email);
