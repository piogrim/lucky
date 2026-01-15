package com.lucky.order_service.order.dto;

import com.lucky.order_service.order.domain.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class OrderResponseDto {

    private Long orderId;
    private Long totalPrice;
    private OrderStatus orderStatus;

    public OrderResponseDto(Long orderId, Long totalPrice, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.totalPrice = totalPrice;
        this.orderStatus = orderStatus;
    }
}
