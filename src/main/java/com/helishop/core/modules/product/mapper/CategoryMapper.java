package com.helishop.core.modules.product.mapper;

import com.helishop.core.modules.product.dto.CategoryRequest;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(source = "parent.name", target = "parentName")
    CategoryResponse toResponse(Category category);

    @Mapping(target = "parent", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "products", ignore = true)
    Category toEntity(CategoryRequest request);
}
