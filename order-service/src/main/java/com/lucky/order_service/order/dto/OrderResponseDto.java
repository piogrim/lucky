package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderResponseDto {

    private Long orderId;
    private Long totalPrice;

    public OrderResponseDto(Long orderId, Long totalPrice) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
    }
}
