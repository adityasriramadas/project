package com.app.orders.client;

import com.app.orders.constants.OrderMessages;
import com.app.orders.dto.InventoryReservationRequest;
import com.app.common.exception.BusinessConflictException;
import com.app.common.exception.CommonApiErrorCode;
import com.app.common.exception.DownstreamServiceException;
import com.app.orders.observability.CorrelationIdHolder;
import com.app.orders.web.InventoryApiPaths;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import java.time.Duration;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Component
public class InventoryClient {

    private final WebClient webClient;
    private final Duration timeout;

    public InventoryClient(
            WebClient.Builder builder,
            @Value("${inventory.url:http://localhost:8082}") String inventoryUrl,
            @Value("${inventory.client.timeout:2s}") Duration timeout) {
        this.webClient = builder.baseUrl(inventoryUrl).build();
        this.timeout = timeout;
    }

    @Retry(name = "inventory")
    @CircuitBreaker(name = "inventory")
    public void reserveStock(String productId, int quantity) {
        callInventory(InventoryApiPaths.RESERVE, productId, quantity);
    }

    @Retry(name = "inventoryRelease")
    @CircuitBreaker(name = "inventoryRelease")
    public void releaseStock(String productId, int quantity) {
        callInventory(InventoryApiPaths.RELEASE, productId, quantity);
    }

    private void callInventory(String path, String productId, int quantity) {
        try {
            webClient.post()
                    .uri(path)
                    .headers(headers -> {
                        var correlationId = CorrelationIdHolder.get();
                        if (correlationId != null && !correlationId.isBlank()) {
                            headers.set(CorrelationIdHolder.HEADER, correlationId);
                        }
                        var authorization = CorrelationIdHolder.getAuthorization();
                        if (authorization != null && !authorization.isBlank()) {
                            headers.set(CorrelationIdHolder.AUTHORIZATION_HEADER, authorization);
                        }
                    })
                    .bodyValue(new InventoryReservationRequest(productId, quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block(timeout);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.CONFLICT) {
                throw new BusinessConflictException(
                        CommonApiErrorCode.INSUFFICIENT_INVENTORY,
                        OrderMessages.INSUFFICIENT_INVENTORY_PREFIX + productId);
            }
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessConflictException(
                        CommonApiErrorCode.PRODUCT_NOT_FOUND,
                        OrderMessages.PRODUCT_NOT_FOUND_PREFIX + productId);
            }
            throw new DownstreamServiceException(OrderMessages.DOWNSTREAM_REJECTED, ex);
        } catch (WebClientRequestException ex) {
            throw new DownstreamServiceException(OrderMessages.DOWNSTREAM_UNAVAILABLE, ex);
        } catch (BusinessConflictException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new DownstreamServiceException(OrderMessages.DOWNSTREAM_UNAVAILABLE, ex);
        }
    }
}
