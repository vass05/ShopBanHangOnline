package com.helishop.core.modules.payment.repository;

import com.helishop.core.common.constants.PaymentStatus;
import com.helishop.core.modules.order.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    List<PaymentTransaction> findByOrderId(Long orderId);

    boolean existsByTransactionCode(String transactionCode);

    boolean existsByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
