package com.helishop.core.modules.order.repository;

import com.helishop.core.modules.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

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
