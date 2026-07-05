package com.app.gateway.constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {
    public static final String HEADER = "X-Correlation-Id";
    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String requestCorrelationId = correlationId;

        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(headers -> headers.set(HEADER, requestCorrelationId)))
                .response(exchange.getResponse())
                .build();
        mutated.getResponse().beforeCommit(() -> {
            mutated.getResponse().getHeaders().set(HEADER, requestCorrelationId);
            return Mono.empty();
        });

        long start = System.currentTimeMillis();
        return chain.filter(mutated)
                .doFinally(signalType -> {
                    long durationMs = System.currentTimeMillis() - start;
                    MDC.put("correlationId", requestCorrelationId);
                    try {
                        log.info("{} {} -> {} ({} ms)",
                                mutated.getRequest().getMethod(),
                                mutated.getRequest().getURI().getPath(),
                                mutated.getResponse().getStatusCode(),
                                durationMs);
                    } finally {
                        MDC.remove("correlationId");
                    }
                })
                .contextWrite(context -> context.put(HEADER, requestCorrelationId));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}


