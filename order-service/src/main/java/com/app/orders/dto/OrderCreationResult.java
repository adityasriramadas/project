package com.app.orders.dto;

import com.app.orders.repository.entity.Order;
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
@Schema(name = "OrderCreationResult", description = "Result of order creation operation")
public class OrderCreationResult {

    @Schema(description = "The order object that was created or retrieved")
    private Order order;

    @Schema(description = "Whether a new order was created (true) or an existing one was retrieved (false)", example = "true")
    private Boolean created;
}


