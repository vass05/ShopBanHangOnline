package com.helishop.core.modules.product.repository;

import com.helishop.core.modules.product.entity.ProductSku;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findBySkuCode(String skuCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM ProductSku s WHERE s.id = :id")
    Optional<ProductSku> findByIdWithLock(@Param("id") Long id);
}
