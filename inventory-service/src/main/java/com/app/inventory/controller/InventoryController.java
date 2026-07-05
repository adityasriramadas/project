package com.app.inventory.controller;

import com.app.common.dto.Response;
import com.app.inventory.constants.ProductFields;
import com.app.inventory.dto.InventoryReservationRequest;
import com.app.inventory.dto.InventoryReservationResponse;
import com.app.inventory.dto.PageResponse;
import com.app.inventory.dto.ProductRequest;
import com.app.inventory.dto.ProductResponse;
import com.app.inventory.mapper.InventoryMapper;
import com.project.inventory.model.InventoryReservationStatus;
import com.app.inventory.service.InventoryService;
import com.app.inventory.web.InventoryApiPaths;
import com.app.inventory.web.InventoryQueryParams;
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
@RequestMapping(InventoryApiPaths.BASE)
@RequiredArgsConstructor
@Tag(name = "Product Inventory Management", description = "APIs for managing product inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryMapper inventoryMapper;


    @PostMapping(InventoryApiPaths.PRODUCTS)
    @Operation(summary = "Create or Update Product")
    public Response<ProductResponse> createOrUpdateProduct(@Valid @RequestBody ProductRequest request) {
        var product = inventoryService.createOrUpdateProduct(request);
        return Response.success(inventoryMapper.toResponse(product));
    }

    @GetMapping(InventoryApiPaths.PRODUCT_BY_ID)
    @Operation(summary = "Get product by ID", description = "Retrieves product information using its unique identifier")
    public Response<ProductResponse> getProduct(
            @PathVariable("id") @Parameter(description = "Product ID", example = "PROD-001", required = true) String id) {
        return Response.success(inventoryMapper.toResponse(inventoryService.getProductById(id)));
    }

    @GetMapping(InventoryApiPaths.PRODUCTS)
    @Operation(summary = "List all products", description = "Retrieves a paginated list of all products, with option to filter low stock items")
    public Response<PageResponse<ProductResponse>> listProducts(
            @Parameter(description = "Filter to show only low stock products", example = "false")
            @RequestParam(
                    name = InventoryQueryParams.LOW_STOCK_ONLY,
                    defaultValue = InventoryQueryParams.FALSE) boolean lowStockOnly,
            @Parameter(hidden = true)
            @PageableDefault(size = 20, sort = ProductFields.ID, direction = Sort.Direction.ASC) Pageable pageable) {
        var products = inventoryMapper.toPageResponse(inventoryService.listProducts(lowStockOnly, pageable));
        return Response.success(products, "Products retrieved successfully");
    }

    @PostMapping(InventoryApiPaths.RESERVE)
    @Operation(summary = "Reserve inventory", description = "Reserves a specified quantity of a product for an order")
    public Response<InventoryReservationResponse> reserveStock(
            @Parameter(description = "Inventory reservation details", required = true)
            @Valid @RequestBody InventoryReservationRequest request) {
        var product = inventoryService.reserveStock(request.getProductId(), request.getQuantity());
        var response = new InventoryReservationResponse(
                product.getId(),
                request.getQuantity(),
                InventoryReservationStatus.RESERVED);
        return Response.success(response, "Inventory reserved successfully");
    }

    @PostMapping(InventoryApiPaths.RELEASE)
    @Operation(summary = "Release inventory", description = "Releases a previously reserved quantity of a product")
    public Response<InventoryReservationResponse> releaseStock(
            @Parameter(description = "Inventory release details", required = true)
            @Valid @RequestBody InventoryReservationRequest request) {
        var product = inventoryService.releaseStock(request.getProductId(), request.getQuantity());
        var response = new InventoryReservationResponse(
                product.getId(),
                request.getQuantity(),
                InventoryReservationStatus.RELEASED);
        return Response.success(response, "Inventory released successfully");
    }
}
