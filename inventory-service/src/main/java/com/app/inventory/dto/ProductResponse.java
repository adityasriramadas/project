package com.app.inventory.dto;

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
@Schema(name = "ProductResponse", description = "Response payload for product information")
public class ProductResponse {

    @Schema(description = "Unique identifier for the product", example = "PROD-001")
    private String id;

    @Schema(description = "Name of the product", example = "Wireless Mouse")
    private String name;

    @Schema(description = "Detailed product description", example = "Ergonomic wireless mouse with USB-C charging")
    private String description;

    @Schema(description = "Product category", example = "Electronics")
    private String category;

    @Schema(description = "Product brand", example = "Logitech")
    private String brand;

    @Schema(description = "Available quantity of the product", example = "100")
    private Integer quantity;

    @Schema(description = "Minimum stock level before the product is treated as low stock", example = "10")
    private Integer reorderThreshold;

    @Schema(description = "Whether the product is low stock", example = "false")
    private Boolean lowStock;

    @Schema(description = "Unit price of the product", example = "49.99")
    private BigDecimal unitPrice;

    @Schema(description = "ISO currency code", example = "USD")
    private String currency;

    @Schema(description = "Whether product can be ordered", example = "true")
    private Boolean active;

    @Schema(description = "Timestamp when the product was created", example = "2025-01-01T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Timestamp when the product was last updated", example = "2025-01-01T11:00:00Z")
    private Instant updatedAt;
}

