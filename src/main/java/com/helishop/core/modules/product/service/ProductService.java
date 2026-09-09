package com.helishop.core.modules.product.service;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.product.dto.ProductFilterCriteria;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.mapper.ProductMapper;
import com.helishop.core.modules.product.repository.CategoryRepository;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.repository.ProductSpecification;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.repository.ShopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;
    private final ShopRepository shopRepository;
    private final ProductMapper productMapper;

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        log.info("Querying product from DB for ID: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchProducts(ProductFilterCriteria criteria) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(
                Math.max(0, criteria.getPage() - 1),
                criteria.getSize(),
                Sort.by(direction, criteria.getSortBy())
        );

        // Lấy tất cả ID danh mục con cháu nếu có truyền categoryId
        List<Long> categoryIds = null;
        if (criteria.getCategoryId() != null) {
            categoryIds = categoryService.getAllDescendantCategoryIds(criteria.getCategoryId());
        }

        var spec = ProductSpecification.filter(
                criteria.getKeyword(),
                categoryIds,
                criteria.getShopId(),
                criteria.getStatus(),
                criteria.getMinPrice(),
                criteria.getMaxPrice(),
                criteria.getMinRating()
        );

        // productRepository.findAll với @EntityGraph(attributePaths = {"category", "shop"}) giải quyết N+1 query
        Page<Product> page = productRepository.findAll(spec, pageable);
        Page<ProductResponse> dtoPage = page.map(productMapper::toResponse);
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh mục không tồn tại"));

        Product product = productMapper.toEntity(request);
        product.setShop(shop);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getRating() != null) product.setRating(request.getRating());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getMainImageUrl() != null) product.setMainImageUrl(request.getMainImageUrl());
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh mục không tồn tại"));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Updated product ID '{}' and evicted from Redis cache", id);
        return productMapper.toResponse(updatedProduct);
    }
}
