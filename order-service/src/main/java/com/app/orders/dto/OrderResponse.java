package com.app.orders.dto;

import com.app.orders.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "OrderResponse", description = "Response payload for order information")
public class OrderResponse {

    @Schema(description = "Unique identifier for the order", example = "ORDER-001")
    private String id;

    @Schema(description = "Unique identifier of the ordered product", example = "PROD-001")
    private String productId;

    @Schema(description = "Quantity of the product ordered", example = "2")
    private Integer quantity;

    @Schema(description = "Customer identifier", example = "CUST-001")
    private String customerId;

    @Schema(description = "Customer name", example = "Aditya")
    private String customerName;

    @Schema(description = "Customer email address", example = "aditya@example.com")
    private String customerEmail;

    @Schema(description = "Shipping address", example = "221B Baker Street, London")
    private String shippingAddress;

    @Schema(description = "Unit price captured at order creation", example = "49.99")
    private BigDecimal unitPrice;

    @Schema(description = "Total order amount", example = "99.98")
    private BigDecimal totalAmount;

    @Schema(description = "ISO currency code", example = "USD")
    private String currency;

    @Schema(description = "Additional order notes", example = "Deliver during business hours")
    private String notes;

    @Schema(description = "Current status of the order", example = "PENDING")
    private OrderStatus status;

    @Schema(description = "Idempotency key for preventing duplicate orders", example = "uuid-key-123")
    private String idempotencyKey;

    @Schema(description = "Timestamp when the order was created", example = "2025-01-01T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the order was last updated", example = "2025-01-01T11:00:00Z")
    private Instant updatedAt;
}
