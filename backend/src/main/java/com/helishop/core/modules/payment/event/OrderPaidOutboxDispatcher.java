package com.helishop.core.modules.payment.event;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @EnableScheduling
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OrderPaidOutboxDispatcher {
    private final OrderPaidOutboxRepository outbox;
    private final OrderPaidOutboxDelivery delivery;

    @Scheduled(fixedDelayString = "${app.outbox.delay-ms:5000}", initialDelayString = "${app.outbox.delay-ms:5000}")
    public void dispatch() {
        // Each ID is locked in a separate transaction; broker latency must not hold a range lock on new inserts.
        for (var entry : outbox.findPending(PageRequest.of(0, 20))) delivery.deliver(entry.getId());
    }
}
