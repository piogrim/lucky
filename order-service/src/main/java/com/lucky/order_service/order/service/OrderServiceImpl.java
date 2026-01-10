package com.lucky.order_service.order.service;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderKafkaDto;
import com.lucky.order_service.order.dto.OrderResponseDto;
import com.lucky.order_service.orderItem.domain.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderKafkaDto> kafkaTemplate;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository, KafkaTemplate<String, OrderKafkaDto> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Transactional
    public OrderResponseDto saveOrder(String userId, OrderCreateRequestDto requestDto) {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.pending);
        order.setUserId(Long.valueOf(userId));
        order.setTotalPrice(requestDto.getTotalPrice());

        OrderItem item = new OrderItem(
                requestDto.getProductId(),
                requestDto.getQuantity(),
                order
        );
        order.addOrderItem(item);

        orderRepository.save(order);

        OrderKafkaDto kafkaDto = new OrderKafkaDto(
                order.getId(),
                requestDto.getProductId(),
                requestDto.getQuantity()
        );

        kafkaTemplate.send("order_create", kafkaDto)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("카프카 전송 성공");
                    } else {
                        log.info("카프카 전송 실패");
                    }
                });

        return new OrderResponseDto(order.getId(), order.getTotalPrice());
    }
}
