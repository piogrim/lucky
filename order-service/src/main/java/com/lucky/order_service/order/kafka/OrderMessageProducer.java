package com.lucky.order_service.order.kafka;

import com.lucky.order_service.order.dto.OrderKafkaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderMessageProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendCreateEvent(String topic, OrderKafkaDto kafkaDto) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(kafkaDto);
            kafkaTemplate.send(topic, jsonMessage).get();
            log.info("Kafka 전송 완료: OrderId={}", kafkaDto.getOrderId());
        } catch (Exception e) {
            log.error("Kafka 전송 실패", e);
            throw new RuntimeException("메시지 발행 중 오류 발생", e);
        }
    }
}
