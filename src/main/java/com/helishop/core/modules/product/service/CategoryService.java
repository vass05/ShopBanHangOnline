package com.helishop.core.modules.product.service;

import com.helishop.core.common.exception.ResourceNotFoundException;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.mapper.CategoryMapper;
import com.helishop.core.modules.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    /**
     * Lấy toàn bộ ID của danh mục cha và tất cả danh mục con/cháu đệ quy
     * Thử truy vấn đệ quy CTE trước, nếu không hỗ trợ sẽ dùng BFS traversal
     */
    @Transactional(readOnly = true)
    public List<Long> getAllDescendantCategoryIds(Long categoryId) {
        if (categoryId == null) {
            return List.of();
        }

        try {
            List<Long> ids = categoryRepository.findDescendantCategoryIdsNative(categoryId);
            if (ids != null && !ids.isEmpty()) {
                return ids;
            }
        } catch (Exception e) {
            log.debug("CTE query không khả dụng hoặc lỗi, chuyển sang BFS traversal: {}", e.getMessage());
        }

        // BFS traversal đệ quy qua quan hệ cha-con
        Set<Long> result = new HashSet<>();
        result.add(categoryId);

        Queue<Long> queue = new LinkedList<>();
        queue.add(categoryId);

        while (!queue.isEmpty()) {
            Long currentId = queue.poll();
            List<Category> children = categoryRepository.findByParentId(currentId);
            if (children != null) {
                for (Category child : children) {
                    if (child.getId() != null && result.add(child.getId())) {
                        queue.add(child.getId());
                    }
                }
            }
        }

        return new ArrayList<>(result);
    }

    /**
     * Lấy danh sách cây danh mục phân cấp đa tầng (Category Tree)
     */
    @Cacheable(value = "categories", key = "'tree'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        List<Category> rootCategories = categoryRepository.findByParentIsNull();
        return rootCategories.stream()
                .map(this::buildCategoryNode)
                .toList();
    }

    private CategoryResponse buildCategoryNode(Category category) {
        CategoryResponse response = categoryMapper.toResponse(category);
        List<Category> children = categoryRepository.findByParentId(category.getId());
        if (children != null && !children.isEmpty()) {
            List<CategoryResponse> childResponses = children.stream()
                    .map(this::buildCategoryNode)
                    .toList();
            response.setChildren(new ArrayList<>(childResponses));
        } else {
            response.setChildren(new ArrayList<>());
        }
        return response;
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(categoryMapper::toResponse)
                .toList();
    }
}
