package com.helishop.core.modules.product.repository;

import com.helishop.core.modules.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    List<Category> findByParentId(Long parentId);

    List<Category> findByParentIsNull();

    @Query(value = """
            WITH RECURSIVE cat_tree AS (
                SELECT id FROM categories WHERE id = :categoryId
                UNION ALL
                SELECT c.id FROM categories c
                JOIN cat_tree ct ON c.parent_id = ct.id
            )
            SELECT id FROM cat_tree
            """, nativeQuery = true)
    List<Long> findDescendantCategoryIdsNative(@Param("categoryId") Long categoryId);
}
