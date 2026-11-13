package com.helishop.core.modules.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor @Slf4j
public class ProductStockCache {
    private final CacheManager caches;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void evict(ProductStockChanged event) {
        try {
            var cache = caches.getCache("products");
            if (cache != null) event.productIds().forEach(cache::evict);
        } catch (RuntimeException exception) {
            log.warn("Kho đã commit nhưng chưa xóa được cache sản phẩm {}", event.productIds());
        }
    }
}
