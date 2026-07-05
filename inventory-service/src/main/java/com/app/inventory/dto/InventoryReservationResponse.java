package com.app.inventory.dto;

import com.project.inventory.model.InventoryReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "InventoryReservationResponse", description = "Response payload for inventory reservation")
public class InventoryReservationResponse {

    @Schema(description = "Unique identifier of the product", example = "PROD-001")
    private String productId;

    @Schema(description = "Quantity of the product reserved", example = "5")
    private Integer quantity;

    @Schema(description = "Status of the reservation", example = "RESERVED")
    private InventoryReservationStatus status;
}


