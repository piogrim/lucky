package com.lucky.order_service.order.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequestDto {

    private List<OrderItemDto>items;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDto{
        private Long productId;
        private Long productPrice;
        private Long quantity;
    }
}
