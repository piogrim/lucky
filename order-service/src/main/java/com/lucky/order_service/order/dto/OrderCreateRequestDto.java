package com.lucky.order_service.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class OrderCreateRequestDto {

    private List<OrderItemDto>items;

    @Setter
    @Getter
    @NoArgsConstructor
    public static class OrderItemDto{
        private Long productId;
        private Long productPrice;
        private Long quantity;
    }
}
