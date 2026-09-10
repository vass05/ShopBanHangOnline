package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.OrderStatus;
import com.helishop.core.common.constants.PaymentMethod;
import com.helishop.core.common.constants.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết đơn hàng")
public class OrderResponse {

    @Schema(description = "ID đơn hàng", example = "500")
    private Long id;

    @Schema(description = "Mã đơn hàng định danh công khai", example = "ORD-A1B2C3D4")
    private String orderCode;

    @Schema(description = "ID khách hàng", example = "1")
    private Long customerId;

    @Schema(description = "Họ tên khách hàng", example = "Nguyễn Văn A")
    private String customerName;

    @Schema(description = "ID gian hàng", example = "1")
    private Long shopId;

    @Schema(description = "Tổng tiền hàng ban đầu (VNĐ)", example = "40000000")
    private BigDecimal totalAmount;

    @Schema(description = "Số tiền được giảm giá voucher (VNĐ)", example = "0")
    private BigDecimal discountAmount;

    @Schema(description = "Phí vận chuyển (VNĐ)", example = "30000")
    private BigDecimal shippingFee;

    @Schema(description = "Tổng tiền thanh toán cuối cùng (VNĐ)", example = "40030000")
    private BigDecimal finalAmount;

    @Schema(description = "Trạng thái đơn hàng", example = "PENDING")
    private OrderStatus orderStatus;

    @Schema(description = "Trạng thái thanh toán", example = "UNPAID")
    private PaymentStatus paymentStatus;

    @Schema(description = "Phương thức thanh toán", example = "COD")
    private PaymentMethod paymentMethod;

    @Schema(description = "Địa chỉ nhận hàng snapshot", example = "Số 1 Đại Cồ Việt, Hai Bà Trưng, Hà Nội")
    private String shippingAddressSnapshot;

    @Schema(description = "Ghi chú đơn hàng", example = "Giao giờ hành chính")
    private String note;

    @Schema(description = "Thời gian tạo đơn")
    private LocalDateTime createdAt;

    @Schema(description = "Danh sách chi tiết các sản phẩm trong đơn")
    private List<ItemResponse> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Chi tiết mặt hàng trong đơn hàng")
    public static class ItemResponse {
        @Schema(description = "ID dòng đơn hàng", example = "1001")
        private Long id;

        @Schema(description = "ID biến thể SKU", example = "10")
        private Long skuId;

        @Schema(description = "Tên sản phẩm snapshot lúc mua", example = "iPhone 16 Pro Max 256GB")
        private String productNameSnapshot;

        @Schema(description = "Biến thể thuộc tính snapshot", example = "{\"color\":\"Titan Sa Mạc\"}")
        private String skuVariantSnapshot;

        @Schema(description = "Số lượng mua", example = "2")
        private Integer quantity;

        @Schema(description = "Đơn giá snapshot lúc mua (VNĐ)", example = "20000000")
        private BigDecimal priceAtPurchase;

        @Schema(description = "Thành tiền của dòng mặt hàng (VNĐ)", example = "40000000")
        private BigDecimal subtotal;
    }
}
