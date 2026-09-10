package com.helishop.core.modules.product.mapper;

import com.helishop.core.modules.product.dto.ProductImageResponse;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.dto.ProductSkuResponse;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductImage;
import com.helishop.core.modules.product.entity.ProductSku;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    @Mapping(source = "shop.id", target = "shopId")
    @Mapping(source = "shop.shopName", target = "shopName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(target = "skus", expression = "java(mapSkus(product.getSkus()))")
    @Mapping(target = "images", expression = "java(mapImages(product.getImages()))")
    ProductResponse toResponse(Product product);

    @Mapping(target = "shop", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "skus", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductRequest request);

    default List<ProductSkuResponse> mapSkus(List<ProductSku> skus) {
        if (skus == null) return Collections.emptyList();
        return skus.stream().map(sku -> ProductSkuResponse.builder()
                .id(sku.getId())
                .productId(sku.getProduct() != null ? sku.getProduct().getId() : null)
                .productName(sku.getProduct() != null ? sku.getProduct().getName() : null)
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .originalPrice(sku.getOriginalPrice())
                .stockQuantity(sku.getStockQuantity())
                .skuAttributes(sku.getSkuAttributes())
                .skuImageUrl(sku.getSkuImageUrl())
                .version(sku.getVersion())
                .createdAt(sku.getCreatedAt())
                .updatedAt(sku.getUpdatedAt())
                .build()).toList();
    }

    default List<ProductImageResponse> mapImages(List<ProductImage> images) {
        if (images == null) return Collections.emptyList();
        return images.stream().map(img -> ProductImageResponse.builder()
                .id(img.getId())
                .imageUrl(img.getImageUrl())
                .isThumbnail(img.getIsThumbnail())
                .displayOrder(img.getDisplayOrder())
                .build()).toList();
    }
}
