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
        OrderKafkaDto orderDto = objectMapper.readValue(message, OrderKafkaDto.class);
        try {
            log.info("Consumer: 주문 받음 orderId={}, productId={}, qty={}",
                    orderDto.getOrderId(), orderDto.getProductId(), orderDto.getQuantity());

            Inventory inventory = inventoryRepository.findByProductId(orderDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

            inventory.decrease(orderDto.getQuantity());

            inventoryHistoryRepository.save(new InventoryHistory(
                    orderDto.getOrderId(),
                    orderDto.getProductId(),
                    orderDto.getQuantity(),
                    HistoryStatus.DEDUCTED
            ));

            InventoryKafkaDto successDto = new InventoryKafkaDto(orderDto.getOrderId(), orderDto.getProductId(), "SUCCESS");
            String jsonResult = objectMapper.writeValueAsString(successDto);

            kafkaTemplate.send("inventory_result", jsonResult);

        } catch (Exception e) {
            log.error("재고 차감 실패: {}", e.getMessage());

            if (orderDto != null) {
                try {
                    InventoryKafkaDto failDto = new InventoryKafkaDto(orderDto.getOrderId(), orderDto.getProductId(), "FAIL");
                    String jsonResult = objectMapper.writeValueAsString(failDto);

                    kafkaTemplate.send("inventory_result", jsonResult);
                } catch (Exception sendError) {
                    log.error("실패 응답 전송 중 에러 발생", sendError);
                }
            }
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
