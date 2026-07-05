package com.app.gateway.constants;

import com.app.common.exception.CommonApiErrorCode;
import java.util.Arrays;
import org.springframework.util.Assert;

/**
 * @author system
 * @since 2026-07-04
 */
public enum GatewayApiErrorCode {

  UNAUTHORIZED(CommonApiErrorCode.UNAUTHORIZED.code(), "Unauthorized access"),
  FORBIDDEN(CommonApiErrorCode.FORBIDDEN.code(), "Forbidden access");

  private final String code;
  private final String message;

  GatewayApiErrorCode(String code, String message) {
    this.code = code;
    this.message = message;
  }

  public String code() {
    return code;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  public static GatewayApiErrorCode getByCode(String code) {
    var result = Arrays.stream(values())
        .filter(e -> e.code.equals(code))
        .findFirst();

    Assert.isTrue(result.isPresent(),
        "GatewayApiErrorCode cannot be resolved for code: " + code);

    return result.get();
  }

  @Override
  public String toString() {
    return this.message;
  }
}