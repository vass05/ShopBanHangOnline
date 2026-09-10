package com.helishop.core.product;

import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.repository.ProductSpecification;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductSpecificationTest {

    @Mock
    private Root<Product> root;

    @Mock
    private CriteriaQuery<?> query;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Path<String> pathName;

    @Mock
    private Path<String> pathDescription;

    @Mock
    private Path<BigDecimal> pathPrice;

    @Mock
    private Path<BigDecimal> pathRating;

    @Mock
    private Path<Object> pathCategory;

    @Mock
    private Path<Long> pathCategoryId;

    @BeforeEach
    void setUp() {
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
    }

    @Test
    @DisplayName("Tạo Predicate lọc khoảng giá: between(minPrice, maxPrice)")
    void shouldCreateBetweenPredicateForPriceRange() {
        when(root.<BigDecimal>get("price")).thenReturn(pathPrice);
        BigDecimal min = BigDecimal.valueOf(1000000);
        BigDecimal max = BigDecimal.valueOf(5000000);

        Specification<Product> spec = ProductSpecification.filter(
                null, null, null, null, min, max, null
        );

        spec.toPredicate(root, query, cb);
        verify(cb).between(eq(pathPrice), eq(min), eq(max));
    }

    @Test
    @DisplayName("Tạo Predicate tìm kiếm từ khóa keyword theo name hoặc description")
    void shouldCreateLikePredicateForKeyword() {
        when(root.<String>get("name")).thenReturn(pathName);
        when(root.<String>get("description")).thenReturn(pathDescription);
        when(cb.lower(any())).thenReturn(mock(Expression.class));
        when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));

        Specification<Product> spec = ProductSpecification.filter(
                "iphone", null, null, null, null, null, null
        );

        spec.toPredicate(root, query, cb);
        verify(cb, org.mockito.Mockito.times(2)).like(any(), eq("%iphone%"));
    }

    @Test
    @DisplayName("Tạo Predicate lọc danh mục bao gồm danh sách ID danh mục con cháu")
    void shouldCreateInPredicateForCategoryIds() {
        when(root.get("category")).thenReturn(pathCategory);
        when(pathCategory.<Long>get("id")).thenReturn(pathCategoryId);
        CriteriaBuilder.In<Long> inClause = mock(CriteriaBuilder.In.class);
        when(pathCategoryId.in(any(List.class))).thenReturn(inClause);

        Specification<Product> spec = ProductSpecification.filter(
                null, List.of(1L, 2L, 3L), null, null, null, null, null
        );

        spec.toPredicate(root, query, cb);
        verify(pathCategoryId).in(eq(List.of(1L, 2L, 3L)));
    }

    @Test
    @DisplayName("Tạo Predicate lọc rating tối thiểu")
    void shouldCreateGreaterThanOrEqualToPredicateForRating() {
        when(root.<BigDecimal>get("rating")).thenReturn(pathRating);
        BigDecimal minRating = BigDecimal.valueOf(4.5);

        Specification<Product> spec = ProductSpecification.filter(
                null, null, null, null, null, null, minRating
        );

        spec.toPredicate(root, query, cb);
        verify(cb).greaterThanOrEqualTo(eq(pathRating), eq(minRating));
    }
}
