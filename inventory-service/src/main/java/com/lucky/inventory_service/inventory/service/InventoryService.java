package com.lucky.inventory_service.inventory.service;

import com.lucky.inventory_service.inventory.domain.*;
import com.lucky.inventory_service.inventory.dto.InventoryKafkaDto;
import com.lucky.inventory_service.inventory.dto.OrderKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order_create", groupId = "inventory-group")
    @Transactional
    public void consume(String message) {
        OrderKafkaDto orderDto = null;
        try {
            orderDto = objectMapper.readValue(message, OrderKafkaDto.class);
            log.info("Consumer: 주문 수신 OrderId={}", orderDto.getOrderId());

            for (OrderKafkaDto.OrderItemDto item : orderDto.getItems()) {

                Inventory inventory = inventoryRepository.findById(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다. ID=" + item.getProductId()));

                inventory.decrease(item.getQuantity());

                inventoryHistoryRepository.save(
                        new InventoryHistory(
                                orderDto.getOrderId(),
                                item.getProductId(),
                                item.getQuantity(),
                                HistoryStatus.DEDUCTED
                        )
                );
            }

            InventoryKafkaDto successDto = new InventoryKafkaDto(orderDto.getOrderId(), "SUCCESS");
            kafkaTemplate.send("inventory_result", objectMapper.writeValueAsString(successDto));
            log.info("주문({}) 재고 차감 완료", orderDto.getOrderId());

        } catch (Exception e) {
            log.error("재고 차감 처리 실패");

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            if (orderDto != null) {
                sendFailMessage(orderDto.getOrderId());
            }
        }
    }

    private void sendFailMessage(Long orderId) {
        try {
            InventoryKafkaDto failDto = new InventoryKafkaDto(orderId, "FAIL");
            String jsonResult = objectMapper.writeValueAsString(failDto);
            kafkaTemplate.send("inventory_result", jsonResult);
            log.info("주문({}) 실패 메시지 전송 완료", orderId);
        } catch (Exception e) {
            log.error("실패 메시지 전송 중 에러", e);
        }
    }

    @KafkaListener(topics = "inventory_rollback", groupId = "inventory-group")
    @Transactional
    public void rollback(String message) {
        try {
            Long orderId = Long.parseLong(message);

            List<InventoryHistory> histories = inventoryHistoryRepository.findAllByOrderIdAndStatus(orderId, HistoryStatus.DEDUCTED);

            for (InventoryHistory history : histories) {
                Inventory inventory = inventoryRepository.findByProductId(history.getProductId())
                        .orElseThrow();
                inventory.increase(history.getQuantity());

                history.restore();
                log.info("상품 {}에 대한 재고 복구 완료", history.getProductId());
            }
        } catch (Exception e) {
            log.error("롤백 처리 중 에러 발생", e);
        }
    }
}
