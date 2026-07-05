package com.app.orders.controller;

import com.app.orders.dto.CreateOrderRequest;
import com.app.orders.dto.OrderCreationResult;
import com.app.orders.dto.OrderResponse;
import com.app.orders.dto.PageResponse;
import com.app.orders.mapper.OrderMapper;
import com.app.orders.model.OrderStatus;
import com.app.orders.repository.entity.Order;
import com.app.orders.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private OrderMapper orderMapper;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new OrderController(orderService, orderMapper))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void createOrderPassesIdempotencyHeaderAndRequestBodyToService() throws Exception {
        var request = CreateOrderRequest.builder()
                .productId("PROD-1")
                .quantity(2)
                .customerId("CUST-1")
                .unitPrice(BigDecimal.TEN)
                .currency("USD")
                .build();
        var order = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        var response = orderResponse("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderService.createOrder(any(CreateOrderRequest.class), eq("idem-1")))
                .thenReturn(new OrderCreationResult(order, true));
        when(orderMapper.toResponse(order)).thenReturn(response);

        mockMvc.perform(post("/order")
                        .header("Idempotency-Key", "idem-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.id").value("ORDER-1"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        var requestCaptor = ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createOrder(requestCaptor.capture(), eq("idem-1"));
        assertThat(requestCaptor.getValue().getProductId()).isEqualTo("PROD-1");
        assertThat(requestCaptor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void listOrdersPassesFiltersAndReturnsPagedResponse() throws Exception {
        var order = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        var pageResponse = PageResponse.<OrderResponse>builder()
                .content(List.of(orderResponse("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED)))
                .page(0)
                .size(20)
                .totalElements(1L)
                .totalPages(1)
                .hasNext(false)
                .build();
        when(orderService.listOrders(eq(OrderStatus.CONFIRMED), eq("PROD-1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(order)));
        when(orderMapper.toPageResponse(any())).thenReturn(pageResponse);

        mockMvc.perform(get("/order")
                        .param("status", "CONFIRMED")
                        .param("productId", "PROD-1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value("ORDER-1"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(orderService).listOrders(eq(OrderStatus.CONFIRMED), eq("PROD-1"), any(Pageable.class));
    }

    @Test
    void getOrderByIdReadsPathVariableAndReturnsOrder() throws Exception {
        var order = order("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED);
        when(orderService.getOrderById("ORDER-1")).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse("ORDER-1", "PROD-1", 2, OrderStatus.CONFIRMED));

        mockMvc.perform(get("/order/ORDER-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("ORDER-1"))
                .andExpect(jsonPath("$.data.productId").value("PROD-1"));

        verify(orderService).getOrderById("ORDER-1");
    }

    @Test
    void cancelOrderReadsPathVariableAndReturnsBooleanResult() throws Exception {
        when(orderService.cancelOrder("ORDER-1")).thenReturn(true);

        mockMvc.perform(post("/order/ORDER-1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        verify(orderService).cancelOrder("ORDER-1");
    }

    private static Order order(String id, String productId, int quantity, OrderStatus status) {
        return Order.builder()
                .id(id)
                .productId(productId)
                .quantity(quantity)
                .status(status)
                .build();
    }

    private static OrderResponse orderResponse(String id, String productId, int quantity, OrderStatus status) {
        return OrderResponse.builder()
                .id(id)
                .productId(productId)
                .quantity(quantity)
                .status(status)
                .build();
    }
}
