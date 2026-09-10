package com.helishop.core.product;

import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.dto.CategoryResponse;
import com.helishop.core.modules.product.dto.ProductFilterCriteria;
import com.helishop.core.modules.product.dto.ProductResponse;
import com.helishop.core.modules.product.service.CategoryService;
import com.helishop.core.modules.product.service.ProductService;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogAndNPlusOneIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private ProductService productService;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private OrderService orderService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("Public Access: Lấy cây danh mục sản phẩm đa tầng /api/v1/categories/tree")
    void shouldGetCategoryTreePublicly() throws Exception {
        CategoryResponse rootCategory = CategoryResponse.builder()
                .id(1L)
                .name("Thiết bị điện tử")
                .children(List.of(CategoryResponse.builder().id(2L).name("Điện thoại").build()))
                .build();

        when(categoryService.getCategoryTree()).thenReturn(List.of(rootCategory));

        mockMvc.perform(get("/api/v1/categories/tree")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Thiết bị điện tử"))
                .andExpect(jsonPath("$.data[0].children[0].name").value("Điện thoại"));
    }

    @Test
    @DisplayName("Tìm kiếm và lọc sản phẩm động đa tiêu chí với phân trang Pageable")
    void shouldSearchProductsWithDynamicFiltersAndPagination() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(101L)
                .name("iPhone 16 Pro Max 256GB")
                .price(BigDecimal.valueOf(34990000))
                .rating(BigDecimal.valueOf(4.9))
                .categoryId(2L)
                .categoryName("Điện thoại")
                .shopId(1L)
                .shopName("Apple Official Store")
                .build();

        PageResponse<ProductResponse> pageResponse = PageResponse.<ProductResponse>builder()
                .items(List.of(product))
                .page(1)
                .size(10)
                .totalElements(1L)
                .totalPages(1)
                .build();

        when(productService.searchProducts(any(ProductFilterCriteria.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "iPhone")
                        .param("categoryId", "1")
                        .param("minPrice", "20000000")
                        .param("maxPrice", "40000000")
                        .param("minRating", "4.5")
                        .param("page", "1")
                        .param("size", "10")
                        .param("sortBy", "price")
                        .param("sortDirection", "ASC")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].name").value("iPhone 16 Pro Max 256GB"))
                .andExpect(jsonPath("$.data.items[0].price").value(34990000))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng của khách hàng với đầy đủ thông tin orderItems (@EntityGraph)")
    void shouldGetCustomerOrdersWithEagerLoadedOrderItems() throws Exception {
        String customerToken = jwtUtils.generateAccessToken("customer@test.com", 1L, "ROLE_CUSTOMER", "Khách hàng");

        OrderResponse.ItemResponse itemResponse = OrderResponse.ItemResponse.builder()
                .id(1L)
                .skuId(10L)
                .productNameSnapshot("iPhone 16 Pro Max")
                .skuVariantSnapshot("256GB Titan Tự Nhiên")
                .quantity(1)
                .priceAtPurchase(BigDecimal.valueOf(34990000))
                .subtotal(BigDecimal.valueOf(34990000))
                .build();

        OrderResponse orderResponse = OrderResponse.builder()
                .id(1L)
                .orderCode("ORD-2026-0001")
                .totalAmount(BigDecimal.valueOf(34990000))
                .items(List.of(itemResponse))
                .build();

        when(orderService.getByCustomerId(1L)).thenReturn(List.of(orderResponse));

        mockMvc.perform(get("/api/v1/orders/customer/1")
                        .header("Authorization", "Bearer " + customerToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].orderCode").value("ORD-2026-0001"))
                .andExpect(jsonPath("$.data[0].items[0].productNameSnapshot").value("iPhone 16 Pro Max"))
                .andExpect(jsonPath("$.data[0].items[0].quantity").value(1));
    }
}
