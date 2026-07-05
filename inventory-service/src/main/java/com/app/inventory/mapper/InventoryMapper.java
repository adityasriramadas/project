package com.app.inventory.mapper;

import com.app.inventory.dto.PageResponse;
import com.app.inventory.dto.ProductRequest;
import com.app.inventory.dto.ProductResponse;
import com.app.inventory.repository.entity.Product;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface InventoryMapper {

    @Mapping(target = "lowStock", expression = "java(isLowStock(product))")
    ProductResponse toResponse(Product product);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Product toEntity(ProductRequest request, @Context int defaultReorderThreshold);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(ProductRequest request, @MappingTarget Product product);

    default PageResponse<ProductResponse> toPageResponse(Page<Product> page) {
        var content = page.getContent().stream()
                .map(this::toResponse)
                .toList();
        return PageResponse.<ProductResponse>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    default boolean isLowStock(Product product) {
        return product.getQuantity() != null
                && product.getReorderThreshold() != null
                && product.getQuantity() <= product.getReorderThreshold();
    }

    @AfterMapping
    default void normalizeCreate(
            ProductRequest request,
            @Context int defaultReorderThreshold,
            @MappingTarget Product product) {
        product.setId(trimToNull(product.getId()));
        product.setName(trimToNull(product.getName()));
        product.setDescription(trimToNull(product.getDescription()));
        product.setCategory(trimToNull(product.getCategory()));
        product.setBrand(trimToNull(product.getBrand()));
        if (product.getReorderThreshold() == null) {
            product.setReorderThreshold(defaultReorderThreshold);
        }
        product.setCurrency(normalizeCurrency(product.getCurrency()));
        product.setActive(product.getActive() == null || product.getActive());
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
