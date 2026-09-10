package com.helishop.core.cache;

import com.helishop.core.modules.product.dto.ProductRequest;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.entity.Category;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.repository.CategoryRepository;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = "spring.main.allow-bean-definition-overriding=true")
class RedisCachingTest {

    @TestConfiguration
    static class TestCacheConfig {
        @Bean
        @Primary
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("products", "categories");
        }
    }

    @Autowired
    private ProductService productService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private ProductRepository productRepository;

    @MockBean
    private CategoryRepository categoryRepository;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        // Clear cache trước mỗi bài test
        if (cacheManager.getCache("products") != null) {
            cacheManager.getCache("products").clear();
        }

        Category category = Category.builder().name("Điện thoại").build();
        category.setId(1L);

        sampleProduct = Product.builder()
                .name("iPhone 16 Pro Max")
                .price(BigDecimal.valueOf(34000000))
                .stockQuantity(10)
                .category(category)
                .build();
        sampleProduct.setId(99L);
    }

    @Test
    @DisplayName("@Cacheable: Lần đầu truy vấn vào DB, lần thứ hai lấy từ Cache (không query DB lại)")
    void shouldCacheProductDetailsOnGetById() {
        when(productRepository.findById(99L)).thenReturn(Optional.of(sampleProduct));

        // Lần 1: Cache miss -> gọi DB
        ProductResponse response1 = productService.getById(99L);
        assertNotNull(response1);
        assertEquals("iPhone 16 Pro Max", response1.getName());

        // Lần 2: Cache hit -> không gọi vào repository
        ProductResponse response2 = productService.getById(99L);
        assertNotNull(response2);
        assertEquals("iPhone 16 Pro Max", response2.getName());

        // Xác nhận productRepository.findById chỉ được gọi đúng 1 lần duy nhất
        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("@CacheEvict: Khi cập nhật sản phẩm, cache cũ bị xóa để lần truy vấn sau lấy dữ liệu mới từ DB")
    void shouldEvictCacheWhenProductIsUpdated() {
        when(productRepository.findById(99L)).thenReturn(Optional.of(sampleProduct));
        when(productRepository.save(any(Product.class))).thenReturn(sampleProduct);

        // Lần 1: Nạp cache
        productService.getById(99L);
        verify(productRepository, times(1)).findById(99L);

        // Cập nhật sản phẩm -> trigger @CacheEvict
        ProductRequest updateRequest = ProductRequest.builder()
                .name("iPhone 16 Pro Max (Updated)")
                .price(BigDecimal.valueOf(35000000))
                .build();

        productService.updateProduct(99L, updateRequest);

        // Lần 3: Truy vấn lại sau khi evict -> bắt buộc phải gọi lại DB lần thứ hai (thêm 1 lần trong update và 1 lần truy vấn mới)
        sampleProduct.setName("iPhone 16 Pro Max (Updated)");
        ProductResponse responseAfterUpdate = productService.getById(99L);

        assertNotNull(responseAfterUpdate);
        assertEquals("iPhone 16 Pro Max (Updated)", responseAfterUpdate.getName());
        // Tổng số lần gọi findById là 3 (lần 1 getById, lần 2 bên trong updateProduct, lần 3 getById sau khi evict)
        verify(productRepository, times(3)).findById(99L);
    }
}
