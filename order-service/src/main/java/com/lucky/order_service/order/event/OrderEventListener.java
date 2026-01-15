package com.lucky.order_service.order.event;

import com.lucky.order_service.order.domain.Order;
import com.lucky.order_service.order.domain.OrderRepository;
import com.lucky.order_service.order.domain.OrderStatus;
import com.lucky.order_service.order.dto.InventoryKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderEventListener {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "inventory_result", groupId = "order-group")
    @Transactional
    public void handleStockResult(String message) {
        InventoryKafkaDto inventoryKafkaDto = objectMapper.readValue(message, InventoryKafkaDto.class);

        Order order = orderRepository.findById(inventoryKafkaDto.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));

        if(order.getOrderStatus() == OrderStatus.CANCELED) {
            log.info("이미 실패한 주문입니다. 주문 Id : {}", order.getId());
            return;
        }

        if("FAIL".equals(inventoryKafkaDto.getResult())) {
            log.info("재고 부족 -> 주문 Id : {}, 제품 Id : {}", order.getId(), inventoryKafkaDto.getProductId());
            order.setOrderStatus(OrderStatus.CANCELED);
            orderRepository.save(order);

            try {
                kafkaTemplate.send("inventory_rollback", String.valueOf(order.getId()));
                log.info("재고 롤백 요청 전송: OrderId={}", order.getId());
            } catch (Exception e) {
                log.error("롤백 요청 전송 실패", e);
            }

            return;
        }

        log.info("재고 확보 성공 -> : 주문 Id : {}, 제품 Id : {}", order.getId(), inventoryKafkaDto.getProductId());
        order.setOrderStatus(OrderStatus.SUCCESS);
        orderRepository.save(order);
    }
}
