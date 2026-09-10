package com.helishop.core.payment;

import com.helishop.core.modules.payment.util.VnPayUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnPayUtilsTest {

    private final String secretKey = "404E635266556A586E3272357538782F";

    @Test
    @DisplayName("HMAC-SHA512 sinh ra chuỗi băm hexa 128 ký tự chuẩn xác")
    void testHmacSHA512() {
        String data = "vnp_Amount=10000000&vnp_Command=pay&vnp_TmnCode=HELI0001";
        String hash = VnPayUtils.hmacSHA512(secretKey, data);

        assertThat(hash).isNotNull();
        assertThat(hash).hasSize(128); // 64 bytes = 128 hex chars
        assertThat(hash).matches("^[a-f0-9]{128}$");
    }

    @Test
    @DisplayName("hashAllFields tự động sắp xếp tham số theo bảng chữ cái trước khi băm")
    void testHashAllFieldsSorting() {
        Map<String, String> params1 = new HashMap<>();
        params1.put("vnp_TmnCode", "HELI0001");
        params1.put("vnp_Amount", "50000000");
        params1.put("vnp_Command", "pay");

        Map<String, String> params2 = new HashMap<>();
        params2.put("vnp_Command", "pay");
        params2.put("vnp_TmnCode", "HELI0001");
        params2.put("vnp_Amount", "50000000");

        String hash1 = VnPayUtils.hashAllFields(params1, secretKey);
        String hash2 = VnPayUtils.hashAllFields(params2, secretKey);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    @DisplayName("buildQueryUrl mã hóa chuẩn URL và sắp xếp tham số")
    void testBuildQueryUrl() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "ORD-123");
        params.put("vnp_Amount", "25000000");

        String query = VnPayUtils.buildQueryUrl(params);

        assertThat(query).startsWith("vnp_Amount=25000000&vnp_TxnRef=ORD-123");
    }

    @Test
    @DisplayName("getIpAddress trích xuất đúng IP thực từ các proxy headers")
    void testGetIpAddress() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18");

        String ip = VnPayUtils.getIpAddress(request);
        assertThat(ip).isEqualTo("203.0.113.195");
    }
}
