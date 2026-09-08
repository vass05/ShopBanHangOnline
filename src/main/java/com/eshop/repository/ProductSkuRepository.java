package com.eshop.repository;

import com.eshop.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {
    Optional<ProductSku> findBySkuCode(String skuCode);
}
