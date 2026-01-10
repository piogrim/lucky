package com.lucky.order_service.order.service;

import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderResponseDto;

public interface OrderService {

    OrderResponseDto saveOrder(String userId, OrderCreateRequestDto requestDto);
}
