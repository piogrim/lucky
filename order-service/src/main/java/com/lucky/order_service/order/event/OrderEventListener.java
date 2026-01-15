package com.lucky.order_service.order.event;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.InventoryKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory_result", groupId = "order-group")
    @Transactional
    public void handleStockResult(String message) {
        try {
            InventoryKafkaDto inventoryKafkaDto = objectMapper.readValue(message, InventoryKafkaDto.class);
            Long orderId = inventoryKafkaDto.getOrderId();

            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. ID=" + orderId));

            if (order.getOrderStatus() == OrderStatus.CANCELED) {
                log.info("이미 취소된 주문입니다. OrderId: {}", orderId);
                return;
            }

            if ("FAIL".equals(inventoryKafkaDto.getResult())) {
                log.info("재고 차감 실패로 인한 주문 취소. OrderId: {}", orderId);

                order.setOrderStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                return;
            }

            log.info("재고 확보 성공. OrderId: {}", orderId);

            //TODO : 결제처리 추가

            order.setOrderStatus(OrderStatus.SUCCESS);
            orderRepository.save(order);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 오류 발생: {}", message, e);
        }
    }
}
