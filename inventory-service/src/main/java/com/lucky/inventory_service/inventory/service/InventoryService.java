package com.lucky.inventory_service.inventory.service;

import com.lucky.inventory_service.inventory.domain.*;
import com.lucky.inventory_service.inventory.dto.InventoryKafkaDto;
import com.lucky.inventory_service.inventory.dto.OrderKafkaDto;
import com.lucky.inventory_service.outbox.domain.OutboxEvent;
import com.lucky.inventory_service.outbox.domain.OutboxEventRepository;
import com.lucky.inventory_service.outbox.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";

    private static final String INVENTORY_DEDUCTION_RESULT = "inventory_result";
    private static final String INVENTORY_DEDUCTION = "inventory_deduction";

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final OutboxEventService outboxEventService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order_create", groupId = "inventory-group")
    @Transactional
    public void consume(String message) {
        OrderKafkaDto orderDto = null;
        try {
            orderDto = objectMapper.readValue(message, OrderKafkaDto.class);

            InventoryHistory history = inventoryHistoryRepository.findByOrderId(orderDto.getOrderId());

            if(history != null && history.getStatus() == HistoryStatus.DEDUCTED) {
                log.info("이미 처리된 주문입니다.");


                saveEvent(INVENTORY_DEDUCTION_RESULT, INVENTORY_DEDUCTION,
                        objectMapper.writeValueAsString(
                                new InventoryKafkaDto(orderDto.getOrderId(), SUCCESS)
                        )
                );

                return;
            }

            log.info("Consumer: 주문 수신 OrderId={}", orderDto.getOrderId());

            orderDto.getItems().sort(Comparator.comparing(OrderKafkaDto.OrderItemDto::getProductId));

            for (OrderKafkaDto.OrderItemDto item : orderDto.getItems()) {

                Inventory inventory = inventoryRepository.findByProductIdWithLock(item.getProductId())
                        .orElseThrow(() -> new RuntimeException("상품 없음"));

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

            saveEvent(INVENTORY_DEDUCTION_RESULT, INVENTORY_DEDUCTION,
                    objectMapper.writeValueAsString(
                            new InventoryKafkaDto(orderDto.getOrderId(), SUCCESS)
                    )
            );

            log.info("주문({}) 재고 차감 완료", orderDto.getOrderId());
        } catch (Exception e) {
            log.error("재고 차감 처리 실패");

            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();

            if (orderDto != null) {
                //롤백되더라도 실패 이벤트는 남겨야 하므로 별도의 트랜잭션으로 처리
                outboxEventService.saveError(
                        new OutboxEvent(
                                INVENTORY_DEDUCTION_RESULT,
                                INVENTORY_DEDUCTION,
                                objectMapper.writeValueAsString(
                                        new InventoryKafkaDto(orderDto.getOrderId(), FAIL)
                                )
                        )
                );
            }
        }
    }

    private void saveEvent(String topic, String type, String payload) {
        OutboxEvent event = new OutboxEvent(topic, type, payload);
        outboxEventService.save(event);
    }

    //추후 재고 복구가 필요한 경우를 대비한 롤백 처리 리스너
    @KafkaListener(topics = "inventory_rollback", groupId = "inventory-group")
    @Transactional
    public void rollback(String message) {
        try {
            Long orderId = Long.parseLong(message);

            List<InventoryHistory> histories = inventoryHistoryRepository.findAllByOrderIdAndStatus(orderId, HistoryStatus.DEDUCTED);

            for (InventoryHistory history : histories) {
                Inventory inventory = inventoryRepository.findByProductId(history.getProductId())
                        .orElseThrow();

                if(history.getStatus() == HistoryStatus.DEDUCTED) {
                    inventory.increase(history.getQuantity());
                    history.restore();
                }

                log.info("상품 {}에 대한 재고 복구 완료", history.getProductId());
            }
        } catch (Exception e) {
            log.error("롤백 처리 중 에러 발생", e);
        }
    }
}
