package com.helishop.core.security;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.cart.service.RedisCartService;
import com.helishop.core.modules.order.dto.CheckoutRequest;
import com.helishop.core.modules.order.dto.OrderResponse;
import com.helishop.core.modules.order.entity.Order;
import com.helishop.core.modules.order.repository.OrderRepository;
import com.helishop.core.modules.order.service.OrderService;
import com.helishop.core.modules.product.entity.Product;
import com.helishop.core.modules.product.repository.ProductRepository;
import com.helishop.core.modules.product.service.ProductService;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.ShopRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ResourceOwnershipIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockBean OrderRepository orderRepository;
    @MockBean ProductRepository productRepository;
    @MockBean ShopRepository shopRepository;
    @MockBean OrderService orders;
    @MockBean ProductService products;
    @MockBean RedisCartService carts;
    @MockBean RefreshTokenService refreshTokens;
    @MockBean StringRedisTemplate redis;

    private static final String CHECKOUT = """
            {"paymentMethod":"COD","shippingAddressSnapshot":"Ha Noi","items":[{"skuId":10,"quantity":1}]}
            """;

    @BeforeEach
    void resources() {
        User buyer = User.builder().build();
        buyer.setId(1L);
        User seller = User.builder().build();
        seller.setId(3L);
        Shop shop = Shop.builder().owner(seller).build();
        shop.setId(20L);
        Order order = Order.builder().customer(buyer).shop(shop).orderStatus(OrderStatus.PENDING).build();
        order.setId(10L);
        Product product = Product.builder().shop(shop).build();
        product.setId(30L);
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));
        when(orderRepository.findByOrderCode("ORD-TEST")).thenReturn(Optional.of(order));
        when(shopRepository.findById(20L)).thenReturn(Optional.of(shop));
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
    }

    private String token(long id, String role) {
        return "Bearer " + jwt.generateAccessToken("user" + id + "@test.com", id, "ROLE_" + role, "Test");
    }

    @Test
    void rejectsForgedCartIdentityOnReadAndMutation() throws Exception {
        mvc.perform(get("/api/v1/cart").param("customerId", "1").header("Authorization", token(2, "CUSTOMER")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cart/items").param("customerId", "1")
                        .header("Authorization", token(2, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skuId\":10,\"quantity\":1}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(carts);
    }

    @Test
    void checkoutUsesTokenIdentityAndRejectsForgedQueryOrBody() throws Exception {
        when(orders.checkout(eq(1L), any())).thenReturn(OrderResponse.builder().id(10L).build());
        mvc.perform(post("/api/v1/orders/checkout").header("Authorization", token(1, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(CHECKOUT))
                .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/orders/checkout").param("customerId", "2")
                        .header("Authorization", token(1, "CUSTOMER")).contentType(MediaType.APPLICATION_JSON).content(CHECKOUT))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/orders/checkout").header("Authorization", token(1, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON).content(CHECKOUT.replace("{\"paymentMethod", "{\"customerId\":2,\"paymentMethod")))
                .andExpect(status().isForbidden());
        verify(orders, times(1)).checkout(eq(1L), any(CheckoutRequest.class));
    }

    @Test
    void rejectsOtherCustomersOrdersByIdCodeAndList() throws Exception {
        for (String path : List.of("/api/v1/orders/10", "/api/v1/orders/code/ORD-TEST", "/api/v1/orders/customer/1")) {
            mvc.perform(get(path).header("Authorization", token(2, "CUSTOMER"))).andExpect(status().isForbidden());
        }
        verifyNoInteractions(orders);
    }

    @Test
    void ownerAndShopSellerCanReadButCustomerCannotAdvanceStatus() throws Exception {
        mvc.perform(get("/api/v1/orders/10").header("Authorization", token(1, "CUSTOMER"))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/10").header("Authorization", token(3, "SELLER"))).andExpect(status().isOk());
        mvc.perform(patch("/api/v1/orders/10/status").param("status", "DELIVERED")
                        .header("Authorization", token(1, "CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/orders/10/status").param("status", "CANCELLED")
                        .header("Authorization", token(2, "CUSTOMER"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/orders/10/status").param("status", "CANCELLED")
                        .header("Authorization", token(1, "CUSTOMER"))).andExpect(status().isOk());
        verify(orders).updateOrderStatus(10L, OrderStatus.CANCELLED);
    }

    @Test
    void onlyShopOwnerOrAdminCanManageProducts() throws Exception {
        String body = "{\"name\":\"Test\",\"shopId\":20,\"categoryId\":1}";
        mvc.perform(post("/api/v1/products").header("Authorization", token(4, "SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/products/30").header("Authorization", token(4, "SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/products").header("Authorization", token(3, "SELLER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/products").header("Authorization", token(9, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        verify(products, times(2)).createProduct(any());
        verify(products, never()).updateProduct(anyLong(), any());
    }

    @Test
    void publicRegistrationCannotGrantAdmin() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"Password123!\",\"role\":\"ROLE_ADMIN\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(1005));
    }
}
