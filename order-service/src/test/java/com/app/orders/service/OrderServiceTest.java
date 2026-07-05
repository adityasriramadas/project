package com.app.orders.service;

import com.app.common.exception.BadRequestException;
import com.app.common.exception.BusinessConflictException;
import com.app.common.exception.DownstreamServiceException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.orders.client.InventoryClient;
import com.app.orders.dto.CreateOrderRequest;
import com.app.orders.mapper.OrderMapper;
import com.app.orders.model.OrderStatus;
import com.app.orders.repository.OrderRepository;
import com.app.orders.repository.entity.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderSavesAndConfirmsWhenInventoryReservationSucceeds() {
        var request = request("PROD-1", 2);
        var pending = order(null, "PROD-1", 2, null);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(request)).thenReturn(pending);
        when(orderRepository.saveAndFlush(pending)).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(inventoryClient).reserveStock("PROD-1", 2);
        when(orderRepository.save(pending)).thenReturn(pending);

        var result = orderService.createOrder(request, "key-1");

        assertThat(result.getCreated()).isTrue();
        assertThat(result.getOrder().getId()).isNotBlank();
        assertThat(result.getOrder().getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getOrder().getIdempotencyKey()).isEqualTo("key-1");
        verify(inventoryClient).reserveStock("PROD-1", 2);
    }

    @Test
    void createOrderReturnsExistingOrderForSameIdempotentRequest() {
        var request = request("PROD-1", 2);
        var existing = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        var result = orderService.createOrder(request, "key-1");

        assertThat(result.getCreated()).isFalse();
        assertThat(result.getOrder()).isSameAs(existing);
        verifyNoInteractions(inventoryClient);
        verify(orderRepository, never()).saveAndFlush(any());
    }

    @Test
    void createOrderRejectsReusedIdempotencyKeyWithDifferentPayload() {
        var request = request("PROD-2", 2);
        var existing = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderService.createOrder(request, "key-1"))
                .isInstanceOf(BusinessConflictException.class);
    }

    @Test
    void createOrderRequiresIdempotencyKey() {
        assertThatThrownBy(() -> orderService.createOrder(request("PROD-1", 1), " "))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(orderMapper, inventoryClient);
    }

    @Test
    void createOrderHandlesConcurrentDuplicateInsert() {
        var request = request("PROD-1", 2);
        var pending = order(null, "PROD-1", 2, null);
        var existing = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existing));
        when(orderMapper.toEntity(request)).thenReturn(pending);
        when(orderRepository.saveAndFlush(pending)).thenThrow(new DataIntegrityViolationException("duplicate"));

        var result = orderService.createOrder(request, "key-1");

        assertThat(result.getCreated()).isFalse();
        assertThat(result.getOrder()).isSameAs(existing);
    }

    @Test
    void createOrderRejectsAndRethrowsWhenInventoryHasBusinessConflict() {
        var request = request("PROD-1", 2);
        var pending = order(null, "PROD-1", 2, null);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(request)).thenReturn(pending);
        when(orderRepository.saveAndFlush(pending)).thenReturn(pending);
        doThrow(new BusinessConflictException(com.app.common.exception.CommonApiErrorCode.INSUFFICIENT_INVENTORY, "no stock"))
                .when(inventoryClient).reserveStock("PROD-1", 2);
        when(orderRepository.save(pending)).thenReturn(pending);

        assertThatThrownBy(() -> orderService.createOrder(request, "key-1"))
                .isInstanceOf(BusinessConflictException.class);

        assertThat(pending.getStatus()).isEqualTo(OrderStatus.REJECTED);
        verify(orderRepository).save(pending);
    }

    @Test
    void createOrderCompensatesWhenUnexpectedReservationErrorOccurs() {
        var request = request("PROD-1", 2);
        var pending = order(null, "PROD-1", 2, null);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(request)).thenReturn(pending);
        when(orderRepository.saveAndFlush(pending)).thenReturn(pending);
        doThrow(new IllegalStateException("boom")).when(inventoryClient).reserveStock("PROD-1", 2);

        assertThatThrownBy(() -> orderService.createOrder(request, "key-1"))
                .isInstanceOf(IllegalStateException.class);

        verify(inventoryClient).releaseStock("PROD-1", 2);
    }

    @Test
    void createOrderMarksRejectedWhenDownstreamUnavailable() {
        var request = request("PROD-1", 2);
        var pending = order(null, "PROD-1", 2, null);
        when(orderRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(orderMapper.toEntity(request)).thenReturn(pending);
        when(orderRepository.saveAndFlush(pending)).thenReturn(pending);
        doThrow(new DownstreamServiceException("down")).when(inventoryClient).reserveStock("PROD-1", 2);
        when(orderRepository.save(pending)).thenReturn(pending);

        assertThatThrownBy(() -> orderService.createOrder(request, "key-1"))
                .isInstanceOf(DownstreamServiceException.class);

        assertThat(pending.getStatus()).isEqualTo(OrderStatus.REJECTED);
    }

    @Test
    void listOrdersUsesFindAllWhenNoFiltersProvided() {
        var pageable = Pageable.unpaged();
        var page = new PageImpl<>(List.of(order("ORDER-1", "PROD-1", 1, OrderStatus.CONFIRMED)));
        when(orderRepository.findAll(pageable)).thenReturn(page);

        var result = orderService.listOrders(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(pageable);
    }

    @Test
    void listOrdersUsesStatusRepositoryMethodWhenOnlyStatusProvided() {
        var pageable = Pageable.unpaged();
        var page = new PageImpl<>(List.of(order("ORDER-1", "PROD-1", 1, OrderStatus.CONFIRMED)));
        when(orderRepository.findByStatus(OrderStatus.CONFIRMED, pageable)).thenReturn(page);

        var result = orderService.listOrders(OrderStatus.CONFIRMED, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findByStatus(OrderStatus.CONFIRMED, pageable);
    }

    @Test
    void listOrdersUsesSpecificationWhenProductFilterProvided() {
        var pageable = Pageable.unpaged();
        var page = new PageImpl<>(List.of(order("ORDER-1", "PROD-1", 1, OrderStatus.CONFIRMED)));
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        var result = orderService.listOrders(OrderStatus.CONFIRMED, "PROD-1", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void getOrderByIdReturnsOrderWhenFound() {
        var order = order("ORDER-1", "PROD-1", 1, OrderStatus.CONFIRMED);
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order));

        assertThat(orderService.getOrderById("ORDER-1")).isSameAs(order);
    }

    @Test
    void getOrderByIdThrowsWhenMissing() {
        when(orderRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MISSING");
    }

    @Test
    void cancelOrderReleasesInventoryAndMarksCancelled() {
        var order = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderRepository.findById("ORDER-1")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertThat(orderService.cancelOrder("ORDER-1")).isTrue();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryClient).releaseStock("PROD-1", 2);
        verify(orderRepository).save(order);
    }

    @Test
    void cancelOrderThrowsWhenOrderMissing() {
        when(orderRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder("MISSING"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrderRejectsNonConfirmedOrder() {
        when(orderRepository.findById("ORDER-1"))
                .thenReturn(Optional.of(order("ORDER-1", "PROD-1", 2, OrderStatus.REJECTED)));

        assertThatThrownBy(() -> orderService.cancelOrder("ORDER-1"))
                .isInstanceOf(BusinessConflictException.class);

        verify(inventoryClient, never()).releaseStock(any(), any(Integer.class));
    }

    private static CreateOrderRequest request(String productId, int quantity) {
        return CreateOrderRequest.builder()
                .productId(productId)
                .quantity(quantity)
                .customerId("CUST-1")
                .unitPrice(BigDecimal.TEN)
                .currency("USD")
                .build();
    }

    private static Order order(String id, String productId, int quantity, OrderStatus status) {
        return Order.builder()
                .id(id)
                .productId(productId)
                .quantity(quantity)
                .status(status)
                .unitPrice(BigDecimal.TEN)
                .currency("USD")
                .build();
    }
}
