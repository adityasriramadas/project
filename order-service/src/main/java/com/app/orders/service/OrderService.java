package com.app.orders.service;

import com.app.orders.client.InventoryClient;
import com.app.orders.constants.OrderFields;
import com.app.orders.constants.OrderMessages;
import com.app.orders.dto.CacheNames;
import com.app.orders.dto.CreateOrderRequest;
import com.app.orders.dto.OrderCreationResult;
import com.app.common.exception.BadRequestException;
import com.app.common.exception.BusinessConflictException;
import com.app.common.exception.CommonApiErrorCode;
import com.app.common.exception.DownstreamServiceException;
import com.app.common.exception.ResourceNotFoundException;
import com.app.orders.mapper.OrderMapper;
import com.app.orders.repository.entity.Order;
import com.app.orders.model.OrderStatus;
import com.app.orders.repository.OrderRepository;
import com.app.common.util.LoggerUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;

    @CacheEvict(cacheNames = CacheNames.ORDERS_BY_ID, allEntries = true)
    @Transactional(noRollbackFor = {BusinessConflictException.class, DownstreamServiceException.class})
    public OrderCreationResult createOrder(CreateOrderRequest request, String idempotencyKey) {

        LoggerUtil.info("Creating order with idempotency key: {}", idempotencyKey);

        validateIdempotencyKey(idempotencyKey);

        var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existingOrder.isPresent()) {
            LoggerUtil.info("Found existing order for idempotency key: {}, returning previous result", idempotencyKey);
            validateIdempotentReplay(existingOrder.get(), request);
            return new OrderCreationResult(existingOrder.get(), false);
        }

        var order = buildOrder(request, idempotencyKey);

        try {
            orderRepository.saveAndFlush(order);
            LoggerUtil.debug("Saved order with id: {}", order.getId());
        } catch (DataIntegrityViolationException ex) {
            return handleDuplicateRequest(request, idempotencyKey);
        }

        return reserveInventory(order);
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            LoggerUtil.error("Idempotency key is required");
            throw new BadRequestException(OrderMessages.IDEMPOTENCY_KEY_REQUIRED);
        }
    }

    private Order buildOrder(CreateOrderRequest request, String idempotencyKey) {
        var order = orderMapper.toEntity(request);
        order.setId(UUID.randomUUID().toString());
        order.setStatus(OrderStatus.PENDING);
        order.setIdempotencyKey(idempotencyKey);
        return order;
    }

    private OrderCreationResult handleDuplicateRequest(CreateOrderRequest request, String idempotencyKey) {
        LoggerUtil.warn("Duplicate request detected for idempotency key: {}", idempotencyKey);

        var existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> {
                    LoggerUtil.error("Duplicate request but no order found for idempotency key: {}", idempotencyKey);
                    return new BusinessConflictException(
                            CommonApiErrorCode.DUPLICATE_ORDER,
                            OrderMessages.DUPLICATE_ORDER_REQUEST);
                });

        validateIdempotentReplay(existingOrder, request);
        return new OrderCreationResult(existingOrder, false);
    }

    private OrderCreationResult reserveInventory(Order order) {
        try {
            LoggerUtil.debug("Reserving inventory for product: {}, quantity: {}",
                    order.getProductId(), order.getQuantity());

            inventoryClient.reserveStock(order.getProductId(), order.getQuantity());

            order.setStatus(OrderStatus.CONFIRMED);
            var saved = orderRepository.save(order);

            LoggerUtil.info("Order {} confirmed and inventory reserved", saved.getId());

            return new OrderCreationResult(saved, true);

        } catch (BusinessConflictException | DownstreamServiceException ex) {

            order.setStatus(OrderStatus.REJECTED);
            orderRepository.save(order);

            LoggerUtil.warn("Order {} rejected: {}", order.getId(), ex.getMessage());
            throw ex;

        } catch (RuntimeException ex) {

            LoggerUtil.error("Unexpected error during order processing for order: {}", order.getId(), ex);

            compensate(order);
            throw ex;
        }
    }

    public Page<Order> listOrders(OrderStatus status, String productId, Pageable pageable) {
        LoggerUtil.debug("Listing orders with status: {}, productId: {}", status, productId);
        
        if (status == null && (productId == null || productId.isBlank())) {
            return orderRepository.findAll(pageable);
        }

        if (status != null && (productId == null || productId.isBlank())) {
            return orderRepository.findByStatus(status, pageable);
        }

        return orderRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (status != null) {
                predicates.add(cb.equal(root.get(OrderFields.STATUS), status));
            }
            if (!productId.isBlank()) {
                predicates.add(cb.equal(root.get(OrderFields.PRODUCT_ID), productId));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable);
    }

    @Cacheable(cacheNames = CacheNames.ORDERS_BY_ID, key = "#p0")
    public Order getOrderById(String id) {
        LoggerUtil.debug("Getting order with id: {} - checking cache first", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> {
                    LoggerUtil.error("Order not found with id: {}", id);
                    return new ResourceNotFoundException(OrderMessages.ORDER_NOT_FOUND_PREFIX + id);
                });
    }

    @Transactional
    @CacheEvict(cacheNames = CacheNames.ORDERS_BY_ID, key = "#p0")
    public boolean cancelOrder(String id) {
        LoggerUtil.info("Cancelling order with id: {}", id);

        var order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        OrderMessages.ORDER_NOT_FOUND_PREFIX + id));

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            LoggerUtil.warn("Cannot cancel order {} - invalid status: {}", id, order.getStatus());
            throw new BusinessConflictException(
                    CommonApiErrorCode.INVALID_ORDER_STATUS,
                    OrderMessages.INVALID_CANCEL_STATUS);
        }

        inventoryClient.releaseStock(order.getProductId(), order.getQuantity());

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        LoggerUtil.info("Order {} cancelled and inventory released", id);
        return true;
    }

    private void compensate(Order order) {
        LoggerUtil.warn("Compensating inventory for failed order: {}", order.getId());
        try {
            inventoryClient.releaseStock(order.getProductId(), order.getQuantity());
            LoggerUtil.info("Successfully compensated inventory for order: {}", order.getId());
        } catch (RuntimeException releaseFailure) {
            LoggerUtil.error("Failed to compensate inventory for order {}", order.getId(), releaseFailure);
        }
    }

    private void validateIdempotentReplay(Order existing, CreateOrderRequest request) {
        if (!existing.getProductId().equals(request.getProductId()) || !existing.getQuantity().equals(request.getQuantity())) {
            LoggerUtil.error("Idempotency key reused with different request data. Existing: productId={}, quantity={}, Requested: productId={}, quantity={}", 
                           existing.getProductId(), existing.getQuantity(), request.getProductId(), request.getQuantity());
            throw new BusinessConflictException(
                    CommonApiErrorCode.IDEMPOTENCY_KEY_REUSED,
                    OrderMessages.IDEMPOTENCY_KEY_REUSED
            );
        }
    }
}
