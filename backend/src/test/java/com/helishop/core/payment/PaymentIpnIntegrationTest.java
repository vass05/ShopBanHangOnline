package com.helishop.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.payment.dto.CreatePaymentUrlRequest;
import com.helishop.core.modules.payment.dto.PaymentUrlResponse;
import com.helishop.core.modules.payment.dto.VnPayIpnResponse;
import com.helishop.core.modules.payment.service.PaymentService;
import com.helishop.core.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIpnIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @MockBean
    private PaymentService paymentService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    private String customerToken;

    @BeforeEach
    void setUp() {
        customerToken = "Bearer " + jwtUtils.generateAccessToken("customer@gmail.com", 1L, "ROLE_CUSTOMER", "Nguyen Van A");
    }

    @Test
    @DisplayName("VNPAY IPN Webhook: Không cần Bearer Token vẫn truy cập được (Public Endpoint 200 OK)")
    void shouldAllowPublicAccessToVnPayIpn() throws Exception {
        when(paymentService.processVnPayIpn(any())).thenReturn(VnPayIpnResponse.success());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "ORD-123")
                        .param("vnp_Amount", "50000000")
                        .param("vnp_ResponseCode", "00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"))
                .andExpect(jsonPath("$.Message").value("Confirm Success"));
    }

    @Test
    @DisplayName("VNPAY IPN Webhook: Sai mã checksum -> Trả về JSON RspCode = 97")
    void shouldReturn97WhenChecksumInvalid() throws Exception {
        when(paymentService.processVnPayIpn(any())).thenReturn(VnPayIpnResponse.invalidChecksum());

        mockMvc.perform(get("/api/v1/payments/vnpay-ipn")
                        .param("vnp_TxnRef", "ORD-123")
                        .param("vnp_SecureHash", "INVALID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"))
                .andExpect(jsonPath("$.Message").value("Invalid Checksum"));
    }

    @Test
    @DisplayName("Tạo URL thanh toán VNPAY: Không có token -> Báo 401 Unauthorized")
    void shouldReturn401WhenAnonymousCreatePaymentUrl() throws Exception {
        CreatePaymentUrlRequest request = CreatePaymentUrlRequest.builder()
                .orderCode("ORD-123")
                .build();

        mockMvc.perform(post("/api/v1/payments/create-vnpay-url")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tạo URL thanh toán VNPAY: Đã đăng nhập -> Trả về 200 OK kèm paymentUrl")
    void shouldCreatePaymentUrlWhenAuthenticated() throws Exception {
        CreatePaymentUrlRequest request = CreatePaymentUrlRequest.builder()
                .orderCode("ORD-123")
                .bankCode("NCB")
                .build();

        PaymentUrlResponse urlResponse = PaymentUrlResponse.builder()
                .orderCode("ORD-123")
                .amount(BigDecimal.valueOf(500000))
                .paymentUrl("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=50000000")
                .expireTime("20260910153000")
                .build();

        when(paymentService.createVnPayPaymentUrl(anyLong(), any(), any())).thenReturn(urlResponse);

        mockMvc.perform(post("/api/v1/payments/create-vnpay-url")
                        .header("Authorization", customerToken)
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.orderCode").value("ORD-123"))
                .andExpect(jsonPath("$.data.paymentUrl").value("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=50000000"));
    }
}
