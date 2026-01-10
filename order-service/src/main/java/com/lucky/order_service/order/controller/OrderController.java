package com.lucky.order_service.order.controller;

import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderResponseDto;
import com.lucky.order_service.order.service.OrderServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderServiceImpl orderServiceImpl;

    @Autowired
    public OrderController(OrderServiceImpl orderServiceImpl) {
        this.orderServiceImpl = orderServiceImpl;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @RequestHeader("X-User-Id") String id,
            @RequestBody OrderCreateRequestDto requestDto) {
        return ResponseEntity.ok(orderServiceImpl.saveOrder(id, requestDto));
    }
}
