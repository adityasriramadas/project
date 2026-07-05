package com.app.orders.model;

import org.springframework.util.Assert;

import java.util.Arrays;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public enum OrderStatus {

  PENDING("PENDING", "Order is pending processing"),
  CONFIRMED("CONFIRMED", "Order has been confirmed and processed"),
  REJECTED("REJECTED", "Order has been rejected due to insufficient inventory or other issues"),
  CANCELLED("CANCELLED", "Order has been cancelled by the customer");

  public final String value;
  public final String description;

  OrderStatus(String value, String description) {
    this.value = value;
    this.description = description;
  }

  /**
   * Get enum by value
   */
  public static OrderStatus getByValue(String value) {
    var result = Arrays.stream(values())
        .filter(t -> t.value.equals(value))
        .findFirst();

    Assert.isTrue(result.isPresent(),
        "OrderStatus cannot be resolved for value: " + value);

    return result.get();
  }

  @Override
  public String toString() {
    return this.value;
  }

  public String getValue() {
    return this.value;
  }
}
