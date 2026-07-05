package com.app.inventory.repository.entity;

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
@Table(name = "product")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Product", description = "Product entity representing inventory items")
public class Product {

    @Id
    @Schema(description = "Unique identifier for the product", example = "PROD-001")
    private String id;

    @NotBlank(message = "Product name cannot be blank")
    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 80)
    private String category;

    @Column(length = 80)
    private String brand;

    @Min(value = 0, message = "Quantity cannot be negative")
    @Column(nullable = false)
    private Integer quantity;

    @Min(value = 0, message = "Reorder threshold cannot be negative")
    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(length = 3)
    private String currency;

    @Column(nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Schema(description = "Timestamp when the product was created", example = "2025-01-01T10:00:00Z")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Timestamp when the product was last updated", example = "2025-01-01T11:00:00Z")
    private Instant updatedAt;

    @Version
    @Schema(description = "Version for optimistic locking", example = "1")
    private Long version;

    public Product(String id, String name, Integer quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.reorderThreshold = 10;
        this.currency = "USD";
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        var now = Instant.now();
        if (this.reorderThreshold == null) {
            this.reorderThreshold = 10;
        }
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "USD";
        }
        if (this.active == null) {
            this.active = true;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
