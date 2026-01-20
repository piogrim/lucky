package com.lucky.order_service.outbox.domain;

public enum EventStatus {
    PENDING,
    SENT,
    FAILED
}
