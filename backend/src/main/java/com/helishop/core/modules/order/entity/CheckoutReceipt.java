package com.helishop.core.modules.order.entity;

import com.helishop.core.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "checkout_receipts", uniqueConstraints = @UniqueConstraint(columnNames = {"customer_id", "request_key"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CheckoutReceipt extends BaseEntity {
    @Column(name = "customer_id", nullable = false) private Long customerId;
    @Column(name = "request_key", nullable = false, length = 80) private String requestKey;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(nullable = false, columnDefinition = "TEXT") private String orderIds;
}
