package com.helishop.core.modules.payment.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface OrderPaidOutboxRepository extends JpaRepository<OrderPaidOutbox, Long> {
    @Query("select o from OrderPaidOutbox o where o.emailPublished = false or o.sellerPublished = false order by o.id")
    List<OrderPaidOutbox> findPending(Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrderPaidOutbox o where o.id = :id")
    java.util.Optional<OrderPaidOutbox> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);
    List<OrderPaidOutbox> findByOrderId(Long orderId);
}
