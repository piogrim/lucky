package com.lucky.inventory_service.outbox.service;

import com.lucky.inventory_service.outbox.domain.OutboxEvent;
import com.lucky.inventory_service.outbox.domain.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public void save(OutboxEvent event) {
        outboxEventRepository.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveError(OutboxEvent event) {
        outboxEventRepository.save(event);
    }
}
