package com.app.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
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
@Schema(name = "CreateOrderRequest", description = "Request payload for creating a new order")
public class CreateOrderRequest {

    @Schema(description = "Unique identifier of the product to order", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product ID is required")
    @Size(min = 2, max = 50, message = "Product ID must be between 2 and 50 characters")
    private String productId;

    @Schema(description = "Quantity of the product to order", example = "2", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @Schema(description = "Customer identifier", example = "CUST-001")
    @Size(max = 80, message = "Customer ID must not exceed 80 characters")
    private String customerId;

    @Schema(description = "Customer name", example = "Aditya")
    @Size(max = 120, message = "Customer name must not exceed 120 characters")
    private String customerName;

    @Schema(description = "Customer email address", example = "aditya@example.com")
    @Email(message = "Customer email must be valid")
    @Size(max = 160, message = "Customer email must not exceed 160 characters")
    private String customerEmail;

    @Schema(description = "Shipping address", example = "221B Baker Street, London")
    @Size(max = 500, message = "Shipping address must not exceed 500 characters")
    private String shippingAddress;

    @Schema(description = "Unit price captured at order creation", example = "49.99", minimum = "0")
    @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be zero or positive")
    private BigDecimal unitPrice;

    @Schema(description = "ISO currency code", example = "USD")
    @Size(min = 3, max = 3, message = "Currency must be a 3 character ISO code")
    private String currency;

    @Schema(description = "Additional order notes", example = "Deliver during business hours")
    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

