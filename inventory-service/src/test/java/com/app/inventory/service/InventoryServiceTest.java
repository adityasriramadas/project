package com.app.inventory.service;

import com.app.common.exception.BadRequestException;
import com.app.common.exception.BusinessConflictException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.inventory.dto.ProductRequest;
import com.app.inventory.mapper.InventoryMapper;
import com.app.inventory.repository.ProductRepository;
import com.app.inventory.repository.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMapper inventoryMapper;

    @InjectMocks
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(inventoryService, "threshold", 5);
    }

    @Test
    void createOrUpdateProductCreatesWhenProductDoesNotExist() {
        var request = ProductRequest.builder()
                .id("PROD-1")
                .name("Mouse")
                .quantity(10)
                .build();
        var mapped = product("PROD-1", 10, true);
        when(productRepository.findById("PROD-1")).thenReturn(Optional.empty());
        when(inventoryMapper.toEntity(request, 5)).thenReturn(mapped);
        when(productRepository.save(mapped)).thenReturn(mapped);

        var result = inventoryService.createOrUpdateProduct(request);

        assertThat(result).isSameAs(mapped);
        verify(productRepository).save(mapped);
    }

    @Test
    void createOrUpdateProductUpdatesWhenProductExists() {
        var request = ProductRequest.builder()
                .id("PROD-1")
                .name("Updated Mouse")
                .quantity(8)
                .build();
        var existing = product("PROD-1", 10, true);
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            existing.setName(request.getName());
            existing.setQuantity(request.getQuantity());
            return null;
        }).when(inventoryMapper).updateEntity(request, existing);
        when(productRepository.save(existing)).thenReturn(existing);

        var result = inventoryService.createOrUpdateProduct(request);

        assertThat(result.getName()).isEqualTo("Updated Mouse");
        assertThat(result.getQuantity()).isEqualTo(8);
        verify(inventoryMapper).updateEntity(request, existing);
    }

    @Test
    void listProductsReturnsAllWhenLowStockFilterIsFalse() {
        var pageable = Pageable.unpaged();
        var page = new PageImpl<>(List.of(product("PROD-1", 10, true)));
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<Product> result = inventoryService.listProducts(false, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(productRepository).findAll(pageable);
    }

    @Test
    void listProductsUsesSpecificationWhenLowStockFilterIsTrue() {
        var pageable = Pageable.unpaged();
        var page = new PageImpl<>(List.of(product("LOW-1", 2, true)));
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<Product> result = inventoryService.listProducts(true, pageable);

        assertThat(result.getContent()).extracting(Product::getId).containsExactly("LOW-1");
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getProductByIdReturnsProductWhenFound() {
        var product = product("PROD-1", 10, true);
        when(productRepository.findById("PROD-1")).thenReturn(Optional.of(product));

        assertThat(inventoryService.getProductById("PROD-1")).isSameAs(product);
    }

    @Test
    void getProductByIdThrowsWhenMissing() {
        when(productRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.getProductById("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    void reserveStockDecreasesQuantityAndSaves() {
        var product = product("PROD-1", 10, true);
        when(productRepository.findByIdForUpdate("PROD-1")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var result = inventoryService.reserveStock("PROD-1", 3);

        assertThat(result.getQuantity()).isEqualTo(7);
        verify(productRepository).save(product);
    }

    @Test
    void reserveStockRejectsInvalidQuantity() {
        assertThatThrownBy(() -> inventoryService.reserveStock("PROD-1", 0))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void reserveStockThrowsWhenProductMissing() {
        when(productRepository.findByIdForUpdate("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock("MISSING", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reserveStockThrowsWhenProductInactive() {
        when(productRepository.findByIdForUpdate("PROD-1"))
                .thenReturn(Optional.of(product("PROD-1", 10, false)));

        assertThatThrownBy(() -> inventoryService.reserveStock("PROD-1", 1))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void reserveStockThrowsWhenInventoryIsInsufficient() {
        when(productRepository.findByIdForUpdate("PROD-1"))
                .thenReturn(Optional.of(product("PROD-1", 2, true)));

        assertThatThrownBy(() -> inventoryService.reserveStock("PROD-1", 3))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void releaseStockIncreasesQuantityAndSaves() {
        var product = product("PROD-1", 7, true);
        when(productRepository.findByIdForUpdate("PROD-1")).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);

        var result = inventoryService.releaseStock("PROD-1", 3);

        assertThat(result.getQuantity()).isEqualTo(10);
        var captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualTo(10);
    }

    @Test
    void releaseStockThrowsWhenProductMissing() {
        when(productRepository.findByIdForUpdate("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.releaseStock("MISSING", 1))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Product product(String id, int quantity, boolean active) {
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .quantity(quantity)
                .reorderThreshold(5)
                .active(active)
                .build();
    }
}
