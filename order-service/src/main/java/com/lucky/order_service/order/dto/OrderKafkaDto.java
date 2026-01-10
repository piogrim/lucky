package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderKafkaDto {

    private Long orderId;
    private Long productId;
    private Long quantity;

    public OrderKafkaDto(Long orderId, Long productId, Long quantity) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
    }
}
