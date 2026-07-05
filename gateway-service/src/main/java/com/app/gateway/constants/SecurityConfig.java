package com.app.gateway.constants;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import javax.crypto.spec.SecretKeySpec;

import com.app.common.constants.ApiErrorFields;
import com.app.common.constants.ApiMessages;
import com.app.common.security.CommonSecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.ServerAuthenticationEntryPoint;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        .pathMatchers(
                            CommonSecurityConstants.ACTUATOR_HEALTH,
                            CommonSecurityConstants.ACTUATOR_HEALTH_CHILDREN,
                            "/order/swagger-ui.html",
                            "/order/swagger-ui/**",
                            "/order/api-docs",
                            "/order/api-docs/**",
                            "/inventory/swagger-ui.html",
                            "/inventory/swagger-ui/**",
                            "/inventory/api-docs",
                            "/inventory/api-docs/**")
                        .permitAll()
                        .anyExchange().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(forbiddenHandler()))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .build();
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder(
            @Value(CommonSecurityConstants.JWT_SECRET_PROPERTY) String secret) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), CommonSecurityConstants.HMAC_SHA_256);
        return NimbusReactiveJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private ServerAuthenticationEntryPoint unauthorizedEntryPoint() {
        return (exchange, ex) -> writeError(
                exchange,
                HttpStatus.UNAUTHORIZED,
                GatewayApiErrorCode.UNAUTHORIZED.code(),
                ApiMessages.AUTHENTICATION_FAILED);
    }

    private ServerAccessDeniedHandler forbiddenHandler() {
        return (exchange, ex) -> writeError(
                exchange,
                HttpStatus.FORBIDDEN,
                GatewayApiErrorCode.FORBIDDEN.code(),
                ApiMessages.ACCESS_DENIED);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        try {
            var body = objectMapper.writeValueAsBytes(Map.of(
                    ApiErrorFields.TIMESTAMP, Instant.now(),
                    ApiErrorFields.STATUS, status.value(),
                    ApiErrorFields.ERROR, code,
                    ApiErrorFields.MESSAGE, message,
                    ApiErrorFields.PATH, exchange.getRequest().getPath().value()
            ));
            var buffer = exchange.getResponse().bufferFactory().wrap(body);
            return exchange.getResponse().writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return exchange.getResponse().setComplete();
        }
    }
}

