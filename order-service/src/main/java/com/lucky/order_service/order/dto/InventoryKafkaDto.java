package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InventoryKafkaDto {

    private Long orderId;
    private String result;
}
