package com.helishop.core.mapper;

import com.helishop.core.common.constants.ProductStatus;
import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.dto.ProductSkuResponse;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.entity.ProductSku;
import com.helishop.core.modules.product.mapper.CategoryMapper;
import com.helishop.core.modules.product.mapper.CategoryMapperImpl;
import com.helishop.core.modules.product.mapper.ProductMapper;
import com.helishop.core.modules.product.mapper.ProductMapperImpl;
import com.helishop.core.modules.product.mapper.ProductSkuMapper;
import com.helishop.core.modules.product.mapper.ProductSkuMapperImpl;
import com.helishop.core.modules.user.dto.UserResponse;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.mapper.UserMapper;
import com.helishop.core.modules.user.mapper.UserMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MapperUnitTest {

    private final ProductMapper productMapper = new ProductMapperImpl();
    private final CategoryMapper categoryMapper = new CategoryMapperImpl();
    private final ProductSkuMapper productSkuMapper = new ProductSkuMapperImpl();
    private final UserMapper userMapper = new UserMapperImpl();

    @Test
    @DisplayName("ProductMapper: Ánh xạ Product Entity sang ProductResponse chính xác")
    void shouldMapProductEntityToResponse() {
        Shop shop = Shop.builder().shopName("Apple Flagship").build();
        shop.setId(10L);

        Category category = Category.builder().name("Smartphone").build();
        category.setId(20L);

        Product product = Product.builder()
                .name("iPhone 16 Pro")
                .slug("iphone-16-pro")
                .description("Titanium flagship")
                .status(ProductStatus.ACTIVE)
                .shop(shop)
                .category(category)
                .build();
        product.setId(100L);

        ProductResponse response = productMapper.toResponse(product);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getName()).isEqualTo("iPhone 16 Pro");
        assertThat(response.getShopId()).isEqualTo(10L);
        assertThat(response.getShopName()).isEqualTo("Apple Flagship");
        assertThat(response.getCategoryId()).isEqualTo(20L);
        assertThat(response.getCategoryName()).isEqualTo("Smartphone");
    }

    @Test
    @DisplayName("ProductMapper: Ánh xạ ProductRequest DTO sang Product Entity chính xác")
    void shouldMapProductRequestToEntity() {
        ProductRequest request = ProductRequest.builder()
                .name("MacBook Pro M3")
                .slug("macbook-pro-m3")
                .description("Apple Silicon")
                .status(ProductStatus.ACTIVE)
                .shopId(5L)
                .categoryId(15L)
                .build();

        Product entity = productMapper.toEntity(request);

        assertThat(entity).isNotNull();
        assertThat(entity.getName()).isEqualTo("MacBook Pro M3");
        assertThat(entity.getSlug()).isEqualTo("macbook-pro-m3");
        assertThat(entity.getStatus()).isEqualTo(ProductStatus.ACTIVE);
    }

    @Test
    @DisplayName("CategoryMapper: Ánh xạ Category sang CategoryResponse gồm quan hệ cha con")
    void shouldMapCategoryToResponse() {
        Category parent = Category.builder().name("Điện tử").build();
        parent.setId(1L);

        Category child = Category.builder()
                .name("Điện thoại")
                .slug("dien-thoai")
                .level(2)
                .parent(parent)
                .build();
        child.setId(2L);

        CategoryResponse response = categoryMapper.toResponse(child);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("Điện thoại");
        assertThat(response.getParentId()).isEqualTo(1L);
        assertThat(response.getParentName()).isEqualTo("Điện tử");
    }

    @Test
    @DisplayName("ProductSkuMapper: Ánh xạ ProductSku sang ProductSkuResponse")
    void shouldMapProductSkuToResponse() {
        Product product = Product.builder().name("Áo Thun Nam").build();
        product.setId(50L);

        ProductSku sku = ProductSku.builder()
                .product(product)
                .skuCode("TSHIRT-WHITE-L")
                .price(BigDecimal.valueOf(199000))
                .stockQuantity(100)
                .skuAttributes("Color: White, Size: L")
                .version(0L)
                .build();
        sku.setId(500L);

        ProductSkuResponse response = productSkuMapper.toResponse(sku);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(500L);
        assertThat(response.getProductId()).isEqualTo(50L);
        assertThat(response.getProductName()).isEqualTo("Áo Thun Nam");
        assertThat(response.getSkuCode()).isEqualTo("TSHIRT-WHITE-L");
        assertThat(response.getStockQuantity()).isEqualTo(100);
        assertThat(response.getVersion()).isEqualTo(0L);
    }

    @Test
    @DisplayName("UserMapper: Ánh xạ User sang UserResponse bảo mật (không lộ mật khẩu)")
    void shouldMapUserToResponseSafely() {
        User user = User.builder()
                .email("test@example.com")
                .passwordHash("$2a$12$securehash")
                .fullName("Nguyen Van Test")
                .phone("0901234567")
                .role(UserRole.ROLE_CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();
        user.setId(77L);

        UserResponse response = userMapper.toResponse(user);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van Test");
        assertThat(response.getRole()).isEqualTo(UserRole.ROLE_CUSTOMER);
    }
}
