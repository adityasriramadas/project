package com.app.orders.repository.entity;

import com.app.orders.model.OrderStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Order", description = "Order entity representing customer orders")
public class Order {

  @Id
  @Schema(description = "Unique identifier for the order", example = "ORDER-001")
  private String id;

  @NotBlank(message = "Product ID cannot be blank")
  @Column(name = "product_id")
  @Schema(description = "Unique identifier of the ordered product", example = "PROD-001")
  private String productId;

  @Min(value = 1, message = "Quantity must be greater than 0")
  @Schema(description = "Quantity of the product ordered", example = "2")
  private Integer quantity;

  @Column(name = "customer_id", length = 80)
  private String customerId;

  @Column(name = "customer_name", length = 120)
  private String customerName;

  @Column(name = "customer_email", length = 160)
  private String customerEmail;

  @Column(name = "shipping_address", length = 500)
  private String shippingAddress;

  @Column(name = "unit_price", precision = 12, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "total_amount", precision = 12, scale = 2)
  private BigDecimal totalAmount;

  @Column(length = 3)
  private String currency;

  @Column(length = 500)
  private String notes;

  @Enumerated(EnumType.STRING)
  @Schema(description = "Current status of the order", example = "PENDING")
  private OrderStatus status;

  @NotBlank(message = "Idempotency key cannot be blank")
  @Column(name = "idempotency_key", unique = true)
  @Schema(description = "Idempotency key for preventing duplicate orders", example = "uuid-key-123")
  private String idempotencyKey;

  @Column(name = "created_at", updatable = false)
  @Schema(description = "Timestamp when the order was created", example = "2025-01-01T10:00:00Z")
  private Instant createdAt;

  @Column(name = "updated_at")
  @Schema(description = "Timestamp when the order was last updated", example = "2025-01-01T11:00:00Z")
  private Instant updatedAt;

  @Version
  @Schema(description = "Version for optimistic locking", example = "1")
  private Long version;

  @PrePersist
  void onCreate() {
    var now = Instant.now();
    if (this.currency == null || this.currency.isBlank()) {
      this.currency = "USD";
    }
    if (this.unitPrice != null && this.totalAmount == null) {
      this.totalAmount = this.unitPrice.multiply(BigDecimal.valueOf(this.quantity));
    }
    this.createdAt = now;
    this.updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
