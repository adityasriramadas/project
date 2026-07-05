package com.app.orders.controller;

import com.app.common.dto.Response;
import com.app.orders.constants.OrderFields;
import com.app.orders.dto.CreateOrderRequest;
import com.app.orders.dto.PageResponse;
import com.app.orders.dto.OrderResponse;
import com.app.orders.mapper.OrderMapper;
import com.app.orders.model.OrderStatus;
import com.app.orders.service.OrderService;
import com.app.orders.web.OrderApiPaths;
import com.app.orders.web.OrderQueryParams;
import com.app.orders.web.OrderRequestHeaders;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@RestController
@RequestMapping(OrderApiPaths.BASE)
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing customer orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    @PostMapping
    @Operation(summary = "Create a new order",
            description = "Creates a new order with idempotency support to prevent duplicate orders")
    public Response<OrderResponse> createOrder(
            @Parameter(description = "Idempotency key to prevent duplicate orders", example = "uuid-key-123", required = true)
            @RequestHeader(name = OrderRequestHeaders.IDEMPOTENCY_KEY, required = true) String idempotencyKey,
            @Parameter(description = "Order details", required = true)
            @Valid @RequestBody CreateOrderRequest request) {
        var result = orderService.createOrder(request, idempotencyKey);
        return Response.success(orderMapper.toResponse(result.getOrder()));
    }

    @GetMapping
    @Operation(summary = "Get all orders",
            description = "Retrieves a paginated list of orders with optional filtering by status and product ID")
    public Response<PageResponse<OrderResponse>> listOrders(
            @Parameter(description = "Filter by order status", example = "PENDING")
            @RequestParam(name = OrderQueryParams.STATUS, required = false) OrderStatus status,
            @Parameter(description = "Filter by product ID", example = "PROD-001")
            @RequestParam(name = OrderQueryParams.PRODUCT_ID, required = false) String productId,
            @Parameter(hidden = true) @PageableDefault(size = 20, sort = OrderFields.CREATED_AT, direction = Sort.Direction.DESC) Pageable pageable) {
        return Response.success(orderMapper.toPageResponse(orderService.listOrders(status, productId, pageable)));
    }

    @GetMapping(OrderApiPaths.BY_ID)
    @Operation(summary = "Get order by ID",
            description = "Retrieves order information using its unique identifier")
    public Response<OrderResponse> getOrderById(
            @PathVariable("id") @Parameter(description = "Order ID", example = "ORDER-001", required = true) String id) {
        return Response.success(orderMapper.toResponse(orderService.getOrderById(id)));
    }

    @PostMapping(OrderApiPaths.CANCEL)
    @Operation(summary = "Cancel an order",
            description = "Cancels an existing order and releases reserved inventory")
    public Response<Boolean> cancelOrder(
            @PathVariable("id") @Parameter(description = "Order ID", example = "ORDER-001", required = true) String id) {
        return Response.success(orderService.cancelOrder(id));
    }
}
