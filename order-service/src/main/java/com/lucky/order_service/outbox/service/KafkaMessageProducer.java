package com.lucky.order_service.outbox.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void send(String topic, String jsonMessage) {
        try {
            kafkaTemplate.send(topic, jsonMessage).get();
            log.info("Kafka 전송 완료");
        } catch (Exception e) {
            log.error("Kafka 전송 실패", e);
            throw new RuntimeException("메시지 발행 중 오류 발생", e);
        }
    }
}
