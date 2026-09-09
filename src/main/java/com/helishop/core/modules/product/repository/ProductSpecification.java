package com.helishop.core.modules.product.repository;

import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.modules.product.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductSpecification {

    /**
     * Ghép các Predicate động phục vụ tìm kiếm và lọc đa tiêu chí
     */
    public static Specification<Product> filter(
            String keyword,
            List<Long> categoryIds,
            Long shopId,
            ProductStatus status,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            BigDecimal minRating
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Tìm kiếm từ khóa theo Tên hoặc Mô tả sản phẩm (không phân biệt hoa thường)
            if (StringUtils.hasText(keyword)) {
                String searchPattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameLike, descLike));
            }

            // 2. Lọc theo danh mục (hỗ trợ danh mục cha và toàn bộ danh mục con trực thuộc)
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // 3. Lọc theo Shop ID
            if (shopId != null) {
                predicates.add(cb.equal(root.get("shop").get("id"), shopId));
            }

            // 4. Lọc theo trạng thái
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 5. Lọc theo khoảng giá (min - max)
            if (minPrice != null && maxPrice != null) {
                predicates.add(cb.between(root.get("price"), minPrice, maxPrice));
            } else if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            } else if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            // 6. Lọc theo mức đánh giá tối thiểu (rating)
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Backward compatibility method cho 4 tham số cơ bản
     */
    public static Specification<Product> filter(String keyword, Long categoryId, Long shopId, ProductStatus status) {
        List<Long> categoryIds = categoryId != null ? List.of(categoryId) : null;
        return filter(keyword, categoryIds, shopId, status, null, null, null);
    }
}
