package com.helishop.core.modules.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor
public class OrderEventPublisher {
    private final OrderPaidOutboxRepository outbox;
    private final ObjectMapper json;

    @Transactional
    public void publishOrderPaidEvent(OrderPaidEvent event) {
        try {
            outbox.save(OrderPaidOutbox.builder().orderId(event.getOrderId()).payload(json.writeValueAsString(event))
                    .sellerPublished(event.getShopId() == null).build());
        } catch (JsonProcessingException exception) {
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION, "Không thể lưu sự kiện thanh toán");
        }
    }
}
