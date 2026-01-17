package com.lucky.order_service.order.service;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderKafkaDto;
import com.lucky.order_service.order.dto.OrderResponseDto;
import com.lucky.order_service.order.domain.OrderItem;
import com.lucky.order_service.order.kafka.OrderMessageProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl {

    private final OrderRepository orderRepository;
    private final OrderMessageProducer orderMessageProducer;

    @Transactional
    public OrderResponseDto createOrder(String userId, OrderCreateRequestDto requestDto) {
        Order order = createOrderEntity(userId, requestDto.getItems());

        orderRepository.save(order);

        sendOrderEvents(order, requestDto.getItems());

        return new OrderResponseDto(order.getId(), order.getTotalPrice(), order.getOrderStatus());
    }

    public OrderResponseDto getOrder(String userId, Long orderId) {
        Long longUserId = Long.parseLong(userId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문이 존재하지 않습니다. ID=" + orderId));

        if(!order.getUserId().equals(longUserId)){
            throw new IllegalStateException("해당 유저가 주문한 내역이 아닙니다.");
        }

        return new OrderResponseDto(order.getId(), order.getTotalPrice(), order.getOrderStatus());
    }

    private Order createOrderEntity(String userId, List<OrderCreateRequestDto.OrderItemDto> itemDtos) {
        Order order = new Order();
        order.setOrderStatus(OrderStatus.PENDING);
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
        String topic = "order_create";

        List<OrderKafkaDto.OrderItemDto> kafkaItems = itemDtos.stream()
                .map(item -> new OrderKafkaDto.OrderItemDto(
                        item.getProductId(),
                        item.getProductPrice(),
                        item.getQuantity()
                ))
                .toList();

        OrderKafkaDto kafkaDto = new OrderKafkaDto(
                order.getId(),
                kafkaItems
        );

        orderMessageProducer.sendCreateEvent(topic, kafkaDto);
    }
}
