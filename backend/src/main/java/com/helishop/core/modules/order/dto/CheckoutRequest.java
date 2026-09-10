package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu đặt hàng và thanh toán (Checkout)")
public class CheckoutRequest {

    @Schema(description = "ID khách hàng đặt hàng (tùy chọn trong body nếu đã truyền ở query param)", example = "1")
    private Long customerId;

    @Schema(description = "ID gian hàng", example = "1")
    private Long shopId;

    @Schema(description = "Phương thức thanh toán (COD, VNPAY, MOMO, ZALOPAY)", example = "COD", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @Schema(description = "Địa chỉ nhận hàng snapshot tại thời điểm đặt", example = "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddressSnapshot;

    @Schema(description = "Ghi chú của khách hàng khi giao hàng", example = "Giao giờ hành chính giúp tôi")
    private String note;

    @Schema(description = "Danh sách các mặt hàng và biến thể đặt mua", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Danh sách sản phẩm mua không được để trống")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Chi tiết từng món hàng trong đơn đặt mua")
    public static class ItemRequest {
        @Schema(description = "ID của biến thể sản phẩm (SKU ID)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "SKU ID không được để trống")
        private Long skuId;

        @Schema(description = "Số lượng muốn mua (phải > 0)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Số lượng mua không được để trống")
        private Integer quantity;
    }
}
