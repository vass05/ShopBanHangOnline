package com.helishop.core.modules.order.dto;

import com.helishop.core.common.constants.PaymentMethod;
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
public class CheckoutRequest {

    private Long customerId;

    private Long shopId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;

    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddressSnapshot;

    private String note;

    @NotEmpty(message = "Danh sách sản phẩm mua không được để trống")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ItemRequest {
        @NotNull(message = "SKU ID không được để trống")
        private Long skuId;

        @NotNull(message = "Số lượng mua không được để trống")
        private Integer quantity;
    }
}
