package com.helishop.core.modules.order.repository;

import com.helishop.core.modules.order.entity.CheckoutReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CheckoutReceiptRepository extends JpaRepository<CheckoutReceipt, Long> {
    Optional<CheckoutReceipt> findByCustomerIdAndRequestKey(Long customerId, String requestKey);
}
