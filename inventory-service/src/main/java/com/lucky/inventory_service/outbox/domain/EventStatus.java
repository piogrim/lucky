package com.lucky.inventory_service.outbox.domain;

public enum EventStatus {
    PENDING,
    SENT,
    FAILED
}
