package com.lucky.order_service.outbox.service;

import com.lucky.order_service.outbox.domain.EventStatus;
import com.lucky.order_service.outbox.domain.OutboxEvent;
import com.lucky.order_service.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaMessageProducer kafkaMessageProducer;

    //2초마다 대기중인 Outbox 이벤트를 조회하여 Kafka로 메세지 발행
    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishOutboxEvents() {
        Pageable pageable = PageRequest.of(0, 50);
        List<OutboxEvent> events = outboxEventRepository.findAllByStatusOrderByIdAsc(EventStatus.PENDING, pageable);

        for (OutboxEvent event : events) {
            try {
                log.info("Publishing outbox event {}", event.getId());
                kafkaMessageProducer.send(event.getTopic(), event.getPayloadJson());
                event.markSent();
            } catch (Exception e) {
                event.markFailed();
            }
        }
    }
}
