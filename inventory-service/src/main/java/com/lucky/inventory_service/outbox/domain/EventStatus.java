package com.lucky.inventory_service.outbox.domain;

public enum EventStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED
}
