package com.helishop.core.modules.order.repository;

import com.helishop.core.modules.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdWithLock(@org.springframework.data.repository.query.Param("id") Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.orderCode = :code")
    Optional<Order> findByOrderCodeWithLock(@org.springframework.data.repository.query.Param("code") String code);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.productSku", "orderItems.productSku.product", "customer", "shop"})
    Optional<Order> findByOrderCode(String orderCode);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.productSku", "orderItems.productSku.product", "customer", "shop"})
    List<Order> findByCustomerId(Long customerId);

    @Override
    @EntityGraph(attributePaths = {"orderItems", "orderItems.productSku", "orderItems.productSku.product", "customer", "shop"})
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.productSku", "orderItems.productSku.product"})
    List<Order> findByShopId(Long shopId);
}
