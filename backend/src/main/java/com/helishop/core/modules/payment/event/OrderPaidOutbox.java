package com.helishop.core.modules.payment.event;

import com.helishop.core.common.base.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Entity @Table(name = "order_paid_outbox")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderPaidOutbox extends BaseEntity {
    @Column(nullable = false, unique = true) private Long orderId;
    @Column(nullable = false, columnDefinition = "TEXT") private String payload;
    @Builder.Default @Column(nullable = false) private boolean emailPublished = false;
    @Builder.Default @Column(nullable = false) private boolean sellerPublished = false;
    @Builder.Default @Column(nullable = false) private int attempts = 0;
    @Column(length = 200) private String lastError;
}
