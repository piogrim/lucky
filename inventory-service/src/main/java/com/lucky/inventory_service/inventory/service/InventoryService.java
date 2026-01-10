package com.lucky.inventory_service.inventory.service;

import com.lucky.inventory_service.inventory.domain.Inventory;
import com.lucky.inventory_service.inventory.domain.InventoryRepository;
import com.lucky.inventory_service.inventory.dto.OrderKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order_create", groupId = "inventory-group")
    @Transactional
    public void consume(String message) {
        try {
            OrderKafkaDto orderDto = objectMapper.readValue(message, OrderKafkaDto.class);
            log.info("Consumer: 주문 받음 orderId={}, productId={}, qty={}",
                    orderDto.getOrderId(), orderDto.getProductId(), orderDto.getQuantity());

            Inventory inventory = inventoryRepository.findByProductId(orderDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("상품이 존재하지 않습니다."));

            inventory.decrease(orderDto.getQuantity());

        } catch (Exception e) {
            log.error("재고 차감 실패: {}", e.getMessage());
        }
    }
}
