package com.lucky.inventory_service.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class InventoryKafkaDto {
    private Long orderId;
    private String result;
}
