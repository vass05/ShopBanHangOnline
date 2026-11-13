package com.helishop.core.order;

import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.order.dto.*;
import com.helishop.core.modules.order.service.CheckoutService;
import com.helishop.core.security.JwtUtils;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc
class CheckoutApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JwtUtils jwt;
    @MockBean CheckoutService checkout;
    @MockBean RefreshTokenService refresh;
    @MockBean StringRedisTemplate redis;
    static final String BODY = """
            {"addressId":5,"shippingMethod":"standard","paymentMethod":"COD","items":[{"skuId":10,"quantity":1}]}
            """;
    String token() { return "Bearer " + jwt.generateAccessToken("buyer@test.com", 1L, "ROLE_CUSTOMER", "Buyer"); }

    @Test void quoteUsesTokenIdentityAndStandardEnvelope() throws Exception {
        when(checkout.quote(eq(1L), any())).thenReturn(new CheckoutQuoteResponse(BigDecimal.valueOf(100000), BigDecimal.valueOf(30000), BigDecimal.ZERO, BigDecimal.valueOf(130000), List.of()));
        mvc.perform(post("/api/v1/orders/quote").param("customerId", "999").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data.finalAmount").value(130000));
        verify(checkout).quote(eq(1L), any());
    }

    @Test void missingIdempotencyHeaderIsBadRequest() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout-batch").header("Authorization", token()).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1010));
        verifyNoInteractions(checkout);
    }

    @Test void nonpositiveQuantityIsRejectedBeforeCheckout() throws Exception {
        mvc.perform(post("/api/v1/orders/checkout-batch").header("Authorization", token()).header("Idempotency-Key", "checkout-request-0001")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY.replace("\"quantity\":1", "\"quantity\":0")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(1010));
        verifyNoInteractions(checkout);
    }

    @Test void validCheckoutPassesKeyAndReturnsServerOrders() throws Exception {
        when(checkout.checkout(eq(1L), any(), eq("checkout-request-0001"))).thenReturn(List.of(OrderResponse.builder().id(50L).orderCode("ORD-SERVER").build()));
        mvc.perform(post("/api/v1/orders/checkout-batch").header("Authorization", token()).header("Idempotency-Key", "checkout-request-0001")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data[0].orderCode").value("ORD-SERVER"));
        verify(checkout).checkout(eq(1L), any(), eq("checkout-request-0001"));
    }
}
