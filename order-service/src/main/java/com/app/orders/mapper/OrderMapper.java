package com.app.orders.mapper;

import com.app.orders.dto.CreateOrderRequest;
import com.app.orders.dto.OrderResponse;
import com.app.orders.dto.PageResponse;
import com.app.orders.repository.entity.Order;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Order order);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "idempotencyKey", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Order toEntity(CreateOrderRequest request);

    default PageResponse<OrderResponse> toPageResponse(Page<Order> page) {
        var content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.<OrderResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @AfterMapping
    default void normalizeCreate(CreateOrderRequest request, @MappingTarget Order order) {
        order.setProductId(trimToNull(order.getProductId()));
        order.setCustomerId(trimToNull(order.getCustomerId()));
        order.setCustomerName(trimToNull(order.getCustomerName()));
        order.setCustomerEmail(trimToNull(order.getCustomerEmail()));
        order.setShippingAddress(trimToNull(order.getShippingAddress()));
        order.setCurrency(normalizeCurrency(order.getCurrency()));
        order.setNotes(trimToNull(order.getNotes()));
        order.setTotalAmount(calculateTotal(order.getUnitPrice(), order.getQuantity()));
    }

    private BigDecimal calculateTotal(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "USD";
        }
        return currency.trim().toUpperCase();
    }
}
