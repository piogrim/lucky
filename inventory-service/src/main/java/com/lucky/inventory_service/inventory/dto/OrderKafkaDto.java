package com.lucky.inventory_service.inventory.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderKafkaDto {
    private Long orderId;
    private Long productId;
    private Long quantity;
    private Long productPrice;
}
