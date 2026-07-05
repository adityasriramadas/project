package com.app.orders.security;

import com.app.common.constants.ApiErrorFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.app.common.constants.ApiMessages;
import com.app.common.exception.CommonApiErrorCode;
import com.app.orders.constants.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(
                                SecurityConstants.ACTUATOR_HEALTH,
                                SecurityConstants.ACTUATOR_HEALTH_CHILDREN,
                                SecurityConstants.API_DOCS,
                                SecurityConstants.API_DOCS_CHILDREN,
                                SecurityConstants.SWAGGER_UI,
                                SecurityConstants.SWAGGER_UI_CHILDREN)
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::unauthorized)
                        .accessDeniedHandler(this::forbidden))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .formLogin(form -> form.disable())
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            @Value(SecurityConstants.JWT_SECRET_PROPERTY) String secret) {
        SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), SecurityConstants.HMAC_SHA_256);
        return NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private void unauthorized(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws java.io.IOException {
        writeJsonError(response, HttpStatus.UNAUTHORIZED, CommonApiErrorCode.UNAUTHORIZED.code(), ApiMessages.AUTHENTICATION_FAILED, request.getRequestURI());
    }

    private void forbidden(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws java.io.IOException {
        writeJsonError(response, HttpStatus.FORBIDDEN, CommonApiErrorCode.FORBIDDEN.code(), ApiMessages.ACCESS_DENIED, request.getRequestURI());
    }

    private void writeJsonError(HttpServletResponse response, HttpStatus status, String code, String message, String path) throws java.io.IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(Map.of(
                ApiErrorFields.TIMESTAMP, Instant.now(),
                ApiErrorFields.STATUS, status.value(),
                ApiErrorFields.ERROR, code,
                ApiErrorFields.MESSAGE, message,
                ApiErrorFields.PATH, path
        )));
    }
}
