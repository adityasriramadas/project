package com.app.inventory.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InventoryReservationRequest", description = "Request payload for reserving inventory")
public class InventoryReservationRequest {

    @Schema(description = "Unique identifier of the product to reserve", example = "PROD-001", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product ID is required")
    @Size(min = 2, max = 50, message = "Product ID must be between 2 and 50 characters")
    private String productId;

    @Schema(description = "Quantity of the product to reserve", example = "5", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Positive(message = "Quantity must be positive")
    private Integer quantity;
}


