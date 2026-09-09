package com.helishop.core.modules.product.repository;

import com.helishop.core.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Override
    @EntityGraph(attributePaths = {"category", "shop"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "shop", "skus", "images"})
    Optional<Product> findBySlug(String slug);

    @Override
    @EntityGraph(attributePaths = {"category", "shop", "skus", "images"})
    Optional<Product> findById(Long id);

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByShopId(Long shopId);
}
