package com.lucky.order_service.outbox.domain;

public enum EventStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
}
