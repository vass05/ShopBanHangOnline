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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ShopRepository shopRepository;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
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

        var spec = ProductSpecification.filter(
                criteria.getKeyword(),
                criteria.getCategoryId(),
                criteria.getShopId(),
                criteria.getStatus()
        );

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
}
