package com.helishop.core.modules.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class VnPayConfig {

    @Value("${app.vnpay.tmn-code:HELI0001}")
    private String tmnCode;

    @Value("${app.vnpay.hash-secret:404E635266556A586E3272357538782F}")
    private String hashSecret;

    @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
    private String payUrl;

    @Value("${app.vnpay.return-url:http://localhost:8080/api/v1/payments/vnpay-return}")
    private String returnUrl;

    @Value("${app.vnpay.ipn-url:http://localhost:8080/api/v1/payments/vnpay-ipn}")
    private String ipnUrl;
}
