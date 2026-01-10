package com.lucky.order_service.orderItem.service;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.orderItem.domain.OrderItem;
import com.lucky.order_service.orderItem.domain.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    @Autowired
    public OrderItemService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    public void save(Long productId, Long quantity, Order order) {
        orderItemRepository.save(new OrderItem(productId, quantity, order));
    }
}
