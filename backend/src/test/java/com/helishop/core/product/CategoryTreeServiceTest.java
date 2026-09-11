package com.helishop.core.product;

import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.mapper.CategoryMapper;
import com.helishop.core.modules.product.repository.CategoryRepository;
import com.helishop.core.modules.product.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryTreeServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category rootCategory;
    private Category childCategory1;
    private Category childCategory2;
    private Category grandChildCategory;

    @BeforeEach
    void setUp() {
        // Cây danh mục 3 tầng:
        // ID 1: Thời trang (Root)
        //   ├── ID 2: Quần áo nam
        //   │     └── ID 4: Áo khoác nam
        //   └── ID 3: Váy đầm nữ
        rootCategory = Category.builder().name("Thời trang").slug("thoi-trang").level(1).build();
        rootCategory.setId(1L);

        childCategory1 = Category.builder().name("Quần áo nam").slug("quan-ao-nam").parent(rootCategory).level(2).build();
        childCategory1.setId(2L);

        childCategory2 = Category.builder().name("Váy đầm nữ").slug("vay-dam-nu").parent(rootCategory).level(2).build();
        childCategory2.setId(3L);

        grandChildCategory = Category.builder().name("Áo khoác nam").slug("ao-khoac-nam").parent(childCategory1).level(3).build();
        grandChildCategory.setId(4L);
    }

    @Test
    @DisplayName("Truy vấn đệ quy cây danh mục: Search danh mục cha 'Thời trang' trả về toàn bộ ID của danh mục con/cháu")
    void shouldReturnAllDescendantCategoryIdsWhenSearchingParentCategory() {
        when(categoryRepository.findDescendantCategoryIdsNative(1L)).thenReturn(null); // Giả lập fallback sang BFS
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(childCategory1, childCategory2));
        when(categoryRepository.findByParentId(2L)).thenReturn(List.of(grandChildCategory));
        when(categoryRepository.findByParentId(3L)).thenReturn(List.of());
        when(categoryRepository.findByParentId(4L)).thenReturn(List.of());

        List<Long> allIds = categoryService.getAllDescendantCategoryIds(1L);

        assertNotNull(allIds);
        assertThat(allIds).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
    }

    @Test
    @DisplayName("Truy vấn đệ quy: Search danh mục con 'Quần áo nam' chỉ trả về [2, 4]")
    void shouldReturnSubTreeIdsWhenSearchingSubCategory() {
        when(categoryRepository.findDescendantCategoryIdsNative(2L)).thenReturn(null);
        when(categoryRepository.findByParentId(2L)).thenReturn(List.of(grandChildCategory));
        when(categoryRepository.findByParentId(4L)).thenReturn(List.of());

        List<Long> subIds = categoryService.getAllDescendantCategoryIds(2L);

        assertNotNull(subIds);
        assertThat(subIds).containsExactlyInAnyOrder(2L, 4L);
    }

    @Test
    @DisplayName("Xây dựng cây danh mục phân cấp Category Tree có cấu trúc lồng nhau")
    void shouldBuildHierarchicalCategoryTree() {
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(rootCategory));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(childCategory1));
        when(categoryRepository.findByParentId(2L)).thenReturn(List.of(grandChildCategory));
        when(categoryRepository.findByParentId(4L)).thenReturn(List.of());

        when(categoryMapper.toResponse(rootCategory))
                .thenReturn(CategoryResponse.builder().id(1L).name("Thời trang").build());
        when(categoryMapper.toResponse(childCategory1))
                .thenReturn(CategoryResponse.builder().id(2L).name("Quần áo nam").build());
        when(categoryMapper.toResponse(grandChildCategory))
                .thenReturn(CategoryResponse.builder().id(4L).name("Áo khoác nam").build());

        List<CategoryResponse> tree = categoryService.getCategoryTree();

        assertNotNull(tree);
        assertEquals(1, tree.size());
        CategoryResponse rootNode = tree.get(0);
        assertEquals(1L, rootNode.getId());
        assertEquals(1, rootNode.getChildren().size());

        CategoryResponse childNode = rootNode.getChildren().get(0);
        assertEquals(2L, childNode.getId());
        assertEquals(1, childNode.getChildren().size());

        CategoryResponse grandChildNode = childNode.getChildren().get(0);
        assertEquals(4L, grandChildNode.getId());
    }
}
