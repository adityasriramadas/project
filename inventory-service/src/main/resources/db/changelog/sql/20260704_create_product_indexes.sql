CREATE INDEX idx_product_quantity
    ON product (quantity);

CREATE INDEX idx_product_updated_at
    ON product (updated_at);

CREATE INDEX idx_product_category
    ON product (category);

CREATE INDEX idx_product_active
    ON product (active);

CREATE INDEX idx_product_low_stock
    ON product (quantity, reorder_threshold);