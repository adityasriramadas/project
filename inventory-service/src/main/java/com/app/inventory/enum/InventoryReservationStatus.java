package com.project.inventory.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * Status of inventory reservation.
 *
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
@Getter
public enum InventoryReservationStatus {
  RESERVED("INVENTORY-0001", "Product quantity has been reserved."),
  RELEASED("INVENTORY-0002", "Product reservation has been released.");

  private final String code;
  private final String message;

  InventoryReservationStatus(String code, String message) {
    this.code = code;
    this.message = message;
  }

    @Override
  public String toString() {
    return message;
  }
}