package com.lucky.order_service.order.service;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderKafkaDto;
import com.lucky.order_service.order.dto.OrderResponseDto;
import com.lucky.order_service.order.domain.OrderItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        Order order = createOrderEntity(userId, requestDto.getItems());

        orderRepository.save(order);

        sendOrderEvents(order, requestDto.getItems());

        return new OrderResponseDto(order.getId(), order.getTotalPrice());
    }

    private Order createOrderEntity(String userId, List<OrderCreateRequestDto.OrderItemDto> itemDtos) {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.pending);
        order.setUserId(Long.valueOf(userId));

        long totalPrice = 0L;

        for (OrderCreateRequestDto.OrderItemDto itemDto : itemDtos) {
            OrderItem orderItem = new OrderItem(
                    itemDto.getProductId(),
                    itemDto.getQuantity(),
                    itemDto.getProductPrice(),
                    order
            );

            order.addOrderItem(orderItem);

            totalPrice += itemDto.getProductPrice() * orderItem.getQuantity();
        }

        order.setTotalPrice(totalPrice);
        return order;
    }

    private void sendOrderEvents(Order order, List<OrderCreateRequestDto.OrderItemDto> itemDtos) {
        for (OrderCreateRequestDto.OrderItemDto itemDto : itemDtos) {
            OrderKafkaDto kafkaDto = new OrderKafkaDto(
                    order.getId(),
                    itemDto.getProductId(),
                    itemDto.getQuantity(),
                    itemDto.getProductPrice()
            );

            kafkaTemplate.send("order_create", kafkaDto)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Kafka 전송 성공: OrderId={}, ProductId={}", order.getId(), itemDto.getProductId());
                        } else {
                            log.error("Kafka 전송 실패: OrderId={}, Error={}", order.getId(), ex.getMessage());
                        }
                    });
        }
    }
}
