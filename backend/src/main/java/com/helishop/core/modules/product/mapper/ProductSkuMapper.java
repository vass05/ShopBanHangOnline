package com.helishop.core.modules.product.mapper;

import com.helishop.core.modules.product.dto.ProductSkuRequest;
import com.helishop.core.modules.product.dto.ProductSkuResponse;
import com.helishop.core.modules.product.entity.ProductSku;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProductSkuMapper {

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    ProductSkuResponse toResponse(ProductSku productSku);

    @Mapping(target = "product", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductSku toEntity(ProductSkuRequest request);
}
