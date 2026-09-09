package com.helishop.core.modules.product.dto;

import com.helishop.core.common.constants.ProductStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductFilterCriteria {

    private String keyword;
    private Long categoryId;
    private Long shopId;
    private ProductStatus status;

    @Builder.Default
    private int page = 1;

    @Builder.Default
    private int size = 20;

    @Builder.Default
    private String sortBy = "id";

    @Builder.Default
    private String sortDirection = "DESC";
}
