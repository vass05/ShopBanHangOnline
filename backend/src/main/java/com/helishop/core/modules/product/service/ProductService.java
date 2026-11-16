package com.helishop.core.modules.product.service;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.constants.ProductStatus;
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
    private final ProductWriteLock writeLock;
    private final org.springframework.context.ApplicationEventPublisher events;

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
        if (criteria.getPage() < 1 || criteria.getSize() < 1 || criteria.getSize() > 100
                || criteria.getSortBy() == null || !java.util.Set.of("id", "price", "createdAt", "rating").contains(criteria.getSortBy())
                || (criteria.getMinPrice() != null && criteria.getMaxPrice() != null && criteria.getMinPrice().compareTo(criteria.getMaxPrice()) > 0)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bộ lọc hoặc phân trang không hợp lệ");
        }
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        if (!admin) criteria.setStatus(ProductStatus.ACTIVE);
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
        if (!admin) spec = spec.and((root, query, builder) -> builder.equal(root.get("shop").get("status"), com.helishop.core.common.constants.ShopStatus.ACTIVE));

        // productRepository.findAll với @EntityGraph(attributePaths = {"category", "shop"}) giải quyết N+1 query
        Page<Product> page = productRepository.findAll(spec, pageable);
        // List responses use the already-fetched shop/category and main image; collections are loaded only on the detail endpoint.
        Page<ProductResponse> dtoPage = page.map(product -> ProductResponse.builder()
                .id(product.getId()).name(product.getName()).slug(product.getSlug()).description(product.getDescription())
                .price(product.getPrice()).rating(product.getRating()).stockQuantity(product.getStockQuantity()).status(product.getStatus())
                .mainImageUrl(product.getMainImageUrl()).shopId(product.getShop().getId()).shopName(product.getShop().getShopName()).shopStatus(product.getShop().getStatus())
                .categoryId(product.getCategory().getId()).categoryName(product.getCategory().getName())
                .skus(List.of()).images(List.of()).createdAt(product.getCreatedAt()).updatedAt(product.getUpdatedAt()).build());
        return PageResponse.from(dtoPage);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Shop shop = shopRepository.findById(request.getShopId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Shop không tồn tại"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh mục không tồn tại"));

        Product product = productMapper.toEntity(request);
        if (request.getStatus() == ProductStatus.ACTIVE) throw new AppException(ErrorCode.INVALID_REQUEST, "Tạo sản phẩm kèm SKU bằng API seller trước khi mở bán");
        if (product.getStatus() == null) product.setStatus(ProductStatus.DRAFT);
        if (product.getSlug() == null || product.getSlug().isBlank()) product.setSlug("product-" + java.util.UUID.randomUUID());
        product.setRating(null);
        product.setShop(shop);
        product.setCategory(category);

        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = writeLock.acquire(id);
        if (!product.getSkus().isEmpty() && (request.getPrice() != null || request.getStockQuantity() != null)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Hãy cập nhật giá/tồn kho trên từng SKU, không sửa tổng sản phẩm");
        }
        if (request.getStatus() == ProductStatus.ACTIVE && product.getSkus().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Cần thêm SKU trước khi mở bán");
        }
        if (request.getRating() != null) throw new AppException(ErrorCode.INVALID_REQUEST, "Điểm đánh giá không được nhập thủ công");

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getMainImageUrl() != null) product.setMainImageUrl(request.getMainImageUrl());
        if (request.getStatus() != null) product.setStatus(request.getStatus());

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh mục không tồn tại"));
            product.setCategory(category);
        }

        Product updatedProduct = productRepository.save(product);
        events.publishEvent(new ProductStockChanged(java.util.Set.of(id)));
        log.info("Updated product ID '{}'; cache will be evicted after commit", id);
        return productMapper.toResponse(updatedProduct);
    }
}
