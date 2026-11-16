package com.helishop.core.modules.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.product.dto.*;
import com.helishop.core.modules.product.entity.*;
import com.helishop.core.modules.product.mapper.ProductMapper;
import com.helishop.core.modules.product.repository.*;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;

@Service @RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerProductService {
    private final UserRepository users;
    private final ShopRepository shops;
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final CategoryRepository categories;
    private final ProductMapper mapper;
    private final ProductWriteLock locks;
    private final ApplicationEventPublisher events;
    private final ObjectMapper json;

    private Shop shop(Long userId) {
        var user = users.findById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        if (user.getRole() != UserRole.ROLE_SELLER || user.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED);
        Shop shop = shops.findByOwnerId(userId).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Bạn cần mở gian hàng trước"));
        if (shop.getStatus() != ShopStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED, "Gian hàng đang ngừng hoạt động hoặc bị khóa");
        return shop;
    }

    public PageResponse<ProductResponse> list(Long userId, int page, int size, String keyword, ProductStatus status) {
        Shop shop = shop(userId);
        if (page < 1 || size < 1 || size > 50 || (keyword != null && keyword.length() > 255)) throw invalid("Bộ lọc không hợp lệ");
        var spec = ProductSpecification.filter(keyword == null ? null : keyword.trim(), null, shop.getId(), status, null, null, null);
        var result = products.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id")));
        // Collections are fetched only when the seller opens an individual product.
        return PageResponse.from(result.map(p -> ProductResponse.builder().id(p.getId()).name(p.getName()).slug(p.getSlug())
                .mainImageUrl(p.getMainImageUrl()).price(p.getPrice()).stockQuantity(p.getStockQuantity()).status(p.getStatus())
                .shopId(shop.getId()).shopName(shop.getShopName()).categoryId(p.getCategory().getId()).categoryName(p.getCategory().getName()).build()));
    }

    public ProductResponse detail(Long userId, Long id) {
        Shop shop = shop(userId);
        Product product = products.findById(id).orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        own(product, shop);
        return mapper.toResponse(product);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProductResponse create(Long userId, SellerProductCreateRequest request) {
        Shop shop = shop(userId);
        Product product = Product.builder().shop(shop).slug("product-" + UUID.randomUUID()).build();
        metadata(product, request.product());
        for (SellerSkuRequest sku : request.skus()) {
            ProductSku entity = ProductSku.builder().product(product).build();
            skuFields(product, entity, sku);
            product.getSkus().add(entity);
        }
        aggregate(product);
        try { products.saveAndFlush(product); }
        catch (DataIntegrityViolationException error) { throw conflict("Mã SKU đã được sử dụng"); }
        changed(product);
        return mapper.toResponse(product);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProductResponse update(Long userId, Long id, SellerProductRequest request) {
        Shop shop = shop(userId);
        Product product = locks.acquire(id);
        own(product, shop);
        metadata(product, request);
        aggregate(product);
        products.flush(); changed(product);
        return mapper.toResponse(product);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ProductResponse saveSku(Long userId, Long id, Long skuId, SellerSkuRequest request) {
        Shop shop = shop(userId);
        Product product = locks.acquire(id);
        own(product, shop);
        ProductSku sku;
        if (skuId == null) {
            if (product.getSkus().size() >= 100) throw invalid("Mỗi sản phẩm tối đa 100 SKU");
            sku = ProductSku.builder().product(product).build();
        } else {
            sku = product.getSkus().stream().filter(item -> item.getId().equals(skuId)).findFirst()
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "SKU không thuộc sản phẩm này"));
            if (request.version() == null || !request.version().equals(sku.getVersion())) {
                throw conflict("SKU đã thay đổi, có thể vừa có đơn mua. Hãy tải lại dữ liệu trước khi sửa");
            }
        }
        skuFields(product, sku, request);
        if (skuId == null) product.getSkus().add(sku);
        aggregate(product);
        try { products.saveAndFlush(product); }
        catch (DataIntegrityViolationException error) { throw conflict("Mã SKU đã được sử dụng"); }
        changed(product);
        return mapper.toResponse(product);
    }

    private void own(Product product, Shop shop) {
        if (!product.getShop().getId().equals(shop.getId())) throw new AppException(ErrorCode.UNAUTHORIZED, "Sản phẩm thuộc gian hàng khác");
    }

    private void metadata(Product product, SellerProductRequest request) {
        String name = request.name().strip();
        if (name.isEmpty()) throw invalid("Tên sản phẩm không được trống");
        var category = categories.findById(request.categoryId()).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Danh mục không tồn tại"));
        product.setName(name); product.setCategory(category); product.setDescription(request.description() == null ? null : request.description().strip());
        product.setMainImageUrl(image(request.mainImageUrl())); product.setStatus(request.status());
    }

    private void skuFields(Product product, ProductSku sku, SellerSkuRequest request) {
        String code = request.skuCode().strip().toUpperCase(Locale.ROOT);
        if (code.isEmpty()) throw invalid("Mã SKU không được trống");
        if (request.originalPrice() != null && request.originalPrice().compareTo(request.price()) < 0) throw invalid("Giá gốc phải lớn hơn hoặc bằng giá bán");
        skus.findBySkuCodeIgnoreCase(code).filter(existing -> !Objects.equals(existing.getId(), sku.getId()))
                .ifPresent(existing -> { throw conflict("Mã SKU đã được sử dụng"); });
        Map<String, String> attributes = new TreeMap<>();
        if (request.attributes() != null) request.attributes().forEach((key, value) -> {
            String cleanKey = key.strip(), cleanValue = value.strip();
            if (cleanKey.isEmpty() || cleanValue.isEmpty() || attributes.put(cleanKey, cleanValue) != null) throw invalid("Thuộc tính SKU không hợp lệ");
        });
        for (ProductSku other : product.getSkus()) {
            if (other == sku) continue;
            if (code.equalsIgnoreCase(other.getSkuCode())) throw conflict("Mã SKU đã được sử dụng");
            Map<String, String> otherAttributes = attributes(other.getSkuAttributes());
            if (!otherAttributes.keySet().equals(attributes.keySet())) throw invalid("Các SKU phải dùng cùng tên thuộc tính (tối đa hai thuộc tính)");
            if (otherAttributes.equals(attributes)) throw conflict("Tổ hợp thuộc tính SKU đã tồn tại");
        }
        sku.setSkuCode(code); sku.setPrice(request.price()); sku.setOriginalPrice(request.originalPrice());
        sku.setStockQuantity(request.stockQuantity()); sku.setSkuImageUrl(image(request.skuImageUrl()));
        try { sku.setSkuAttributes(json.writeValueAsString(attributes)); }
        catch (JsonProcessingException error) { throw invalid("Thuộc tính SKU không hợp lệ"); }
    }

    private Map<String, String> attributes(String value) {
        try { return value == null || value.isBlank() ? Map.of() : json.readValue(value, new TypeReference<Map<String, String>>() {}); }
        catch (JsonProcessingException error) { throw invalid("SKU hiện có chứa thuộc tính không hợp lệ; hãy sửa trước khi thêm biến thể"); }
    }

    private String image(String value) {
        if (value == null || value.isBlank()) return null;
        String trimmed = value.strip();
        try {
            var uri = java.net.URI.create(trimmed);
            if ((("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) && uri.getHost() != null && uri.getUserInfo() == null)
                    || (trimmed.startsWith("/uploads/") && !trimmed.contains(".."))) return trimmed;
        } catch (IllegalArgumentException error) { throw invalid("Đường dẫn ảnh không hợp lệ"); }
        throw invalid("Ảnh cần URL http/https hoặc đường dẫn /uploads/");
    }

    private void aggregate(Product product) {
        if (product.getSkus().isEmpty()) {
            if (product.getStatus() == ProductStatus.ACTIVE) throw invalid("Cần thêm SKU trước khi mở bán");
            product.setPrice(null); product.setStockQuantity(0); return;
        }
        product.setPrice(product.getSkus().stream().map(ProductSku::getPrice).min(BigDecimal::compareTo).orElseThrow());
        product.setStockQuantity(Math.toIntExact(product.getSkus().stream().mapToLong(ProductSku::getStockQuantity).sum()));
    }

    private void changed(Product product) { events.publishEvent(new ProductStockChanged(Set.of(product.getId()))); }
    private AppException invalid(String message) { return new AppException(ErrorCode.INVALID_REQUEST, message); }
    private AppException conflict(String message) { return new AppException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, message); }
}
