package com.lucky.order_service.order.service;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.OrderCreateRequestDto;
import com.lucky.order_service.order.dto.OrderKafkaDto;
import com.lucky.order_service.order.dto.OrderResponseDto;
import com.lucky.order_service.order.domain.OrderItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public OrderResponseDto saveOrder(String userId, OrderCreateRequestDto requestDto) {
        Order order = createOrderEntity(userId, requestDto.getItems());

        orderRepository.save(order);

        try {
            sendOrderEvents(order, requestDto.getItems());
        } catch (Exception e) {
            log.error("Kafka 전송 실패로 인한 롤백: {}", e.getMessage());
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다.");
        }

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
        for (OrderCreateRequestDto.OrderItemDto itemDto : itemDtos) {
            OrderKafkaDto kafkaDto = new OrderKafkaDto(
                    order.getId(),
                    itemDto.getProductId(),
                    itemDto.getQuantity(),
                    itemDto.getProductPrice()
            );

            try {
                String jsonMessage = objectMapper.writeValueAsString(kafkaDto);
                kafkaTemplate.send("order_create", jsonMessage).get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
