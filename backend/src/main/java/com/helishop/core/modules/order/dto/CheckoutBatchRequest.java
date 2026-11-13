package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Báo giá hoặc đặt hàng nhiều shop bằng địa chỉ đã lưu và SKU thực")
public class CheckoutBatchRequest {
    @NotNull private Long addressId;
    @Builder.Default @Pattern(regexp = "standard|express|economy") private String shippingMethod = "standard";
    @Size(max = 50) private String voucherCode;
    @NotNull private PaymentMethod paymentMethod;
    @Size(max = 500) private String note;
    @NotEmpty @Size(max = 100) @Valid private List<CheckoutRequest.ItemRequest> items;
}
