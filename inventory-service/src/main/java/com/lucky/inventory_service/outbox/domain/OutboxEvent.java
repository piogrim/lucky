package com.lucky.inventory_service.outbox.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String topic;

    @Column
    private String eventType;

    @Column
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    private EventStatus status;

    public OutboxEvent(String topic, String eventType, String payloadJson) {
        this.topic = topic;
        this.eventType = eventType;
        this.payloadJson = payloadJson;
        this.status = EventStatus.PENDING;
    }

    public void markSent() {
        this.status = EventStatus.SENT;
    }

    public void markSending() {
        this.status = EventStatus.SENDING;
    }

    public void markFailed() {
        this.status = EventStatus.FAILED;
    }
}