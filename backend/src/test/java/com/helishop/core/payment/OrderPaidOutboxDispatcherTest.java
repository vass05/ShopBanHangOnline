package com.helishop.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.modules.payment.event.*;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitOperations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderPaidOutboxDispatcherTest {
    @Test void failedBrokerPublishRemainsPendingAndIsRetriedLater() throws Exception {
        var repository = mock(OrderPaidOutboxRepository.class);
        var rabbit = mock(RabbitTemplate.class);
        var json = new ObjectMapper().findAndRegisterModules();
        var entry = OrderPaidOutbox.builder().orderId(10L).payload(json.writeValueAsString(OrderPaidEvent.builder().orderId(10L).shopId(20L).build())).build();
        when(repository.findByIdWithLock(10L)).thenReturn(java.util.Optional.of(entry));
        when(rabbit.invoke(any())).thenThrow(new AmqpException("Broker unavailable"));
        var worker = new OrderPaidOutboxDelivery(repository, json, rabbit);
        worker.deliver(10L);
        assertFalse(entry.isEmailPublished()); assertFalse(entry.isSellerPublished()); assertNotNull(entry.getLastError());
        var operations = mock(RabbitOperations.class);
        doAnswer(invocation -> {
            RabbitOperations.OperationsCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(operations);
        }).when(rabbit).invoke(any());
        worker.deliver(10L);
        assertTrue(entry.isEmailPublished()); assertTrue(entry.isSellerPublished()); assertNull(entry.getLastError());
        verify(operations, times(2)).waitForConfirmsOrDie(5000);
    }

    @Test void eventIsPersistedBeforeAnyBrokerAttempt() {
        var repository = mock(OrderPaidOutboxRepository.class);
        var publisher = new OrderEventPublisher(repository, new ObjectMapper().findAndRegisterModules());
        publisher.publishOrderPaidEvent(OrderPaidEvent.builder().orderId(10L).orderCode("ORD-TEST").build());
        verify(repository).save(argThat(entry -> entry.getOrderId().equals(10L) && entry.getPayload().contains("ORD-TEST") && !entry.isEmailPublished()));
    }
}
