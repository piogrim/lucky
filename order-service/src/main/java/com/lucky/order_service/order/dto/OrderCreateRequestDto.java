package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderCreateRequestDto {

    private Long productId;
    private Long quantity;
    private Long totalPrice;
}
