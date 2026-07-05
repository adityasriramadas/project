package com.app.common.security;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public class CommonSecurityConstants {
    public static final String ACTUATOR_HEALTH = "/actuator/health";
    public static final String ACTUATOR_HEALTH_CHILDREN = "/actuator/health/**";
    public static final String API_DOCS = "/api-docs";
    public static final String API_DOCS_CHILDREN = "/api-docs/**";
    public static final String SWAGGER_UI = "/swagger-ui.html";
    public static final String SWAGGER_UI_CHILDREN = "/swagger-ui/**";
    public static final String JWT_SECRET_PROPERTY = "${security.jwt.secret:dev-jwt-signing-secret-change-me-32chars-minimum}";
    public static final String HMAC_SHA_256 = "HmacSHA256";

    protected CommonSecurityConstants() {
    }
}
