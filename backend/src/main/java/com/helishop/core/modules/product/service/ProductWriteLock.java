package com.helishop.core.modules.product.service;

import com.helishop.core.common.exception.*;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class ProductWriteLock {
    private final ProductSkuRepository skus;
    private final ProductRepository products;

    // Match checkout/cancellation: all existing SKU IDs ascending, then the parent row.
    public Product acquire(Long productId) {
        for (Long id : skus.findIdsByProductId(productId)) {
            skus.findByIdWithLock(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        }
        return products.findByIdWithLock(productId).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
