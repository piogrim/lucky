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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaMessageProducer kafkaMessageProducer;

    //0.01초마다 대기중인 Outbox 이벤트를 조회하여 Kafka로 메세지 발행
    @Scheduled(fixedDelay = 10)
    public void publishOutboxEvents() {
        Pageable pageable = PageRequest.of(0, 100);
        List<OutboxEvent> events = outboxEventRepository.findAllByStatusOrderByIdAsc(EventStatus.PENDING, pageable);

        if (events.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (OutboxEvent event : events) {
            CompletableFuture<Void> future = kafkaMessageProducer.send(event.getTopic(), event.getPayloadJson())
                    .thenAccept(result -> { // 성공 시 (thenAccept는 예외 발생 시 실행 안 됨)
                        event.markSent();
                        outboxEventRepository.save(event);
                        log.info("전송 성공: {}", event.getId());
                    })
                    .exceptionally(ex -> { // 실패 시
                        log.error("전송 실패: {}", event.getId(), ex);
                        return null; // 예외 처리 후 넘어가기
                    });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
