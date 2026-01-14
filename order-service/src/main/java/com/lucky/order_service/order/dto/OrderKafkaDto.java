package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderKafkaDto {

    private Long orderId;
    private Long productId;
    private Long quantity;
    private Long productPrice;

    public OrderKafkaDto(Long orderId, Long productId, Long quantity, Long productPrice) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.productPrice = productPrice;
    }
}
