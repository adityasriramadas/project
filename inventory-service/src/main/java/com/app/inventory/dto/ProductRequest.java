package com.app.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ProductRequest", description = "Request payload for creating or updating a product")
public class ProductRequest {

    @Schema(description = "Unique identifier for the product", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String id;

    @Schema(description = "Name of the product", example = "Wireless Mouse", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product name is required")
    @Size(min = 2, max = 100, message = "Product name must be between 2 and 100 characters")
    private String name;

    @Schema(description = "Detailed product description", example = "Ergonomic wireless mouse with USB-C charging")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Schema(description = "Product category", example = "Electronics")
    @Size(max = 80, message = "Category must not exceed 80 characters")
    private String category;

    @Schema(description = "Product brand", example = "Logitech")
    @Size(max = 80, message = "Brand must not exceed 80 characters")
    private String brand;

    @Schema(description = "Available quantity of the product", example = "100", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    @Schema(description = "Minimum stock level before the product is treated as low stock", example = "10", minimum = "0")
    @PositiveOrZero(message = "Reorder threshold must be zero or positive")
    private Integer reorderThreshold;

    @Schema(description = "Unit price of the product", example = "49.99", minimum = "0")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    @Schema(description = "ISO currency code", example = "USD")
    @Size(min = 3, max = 3, message = "Currency must be a 3 character ISO code")
    private String currency;

    @Schema(description = "Whether product can be ordered", example = "true")
    private Boolean active;
}

