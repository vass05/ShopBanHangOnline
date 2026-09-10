package com.helishop.core.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Phản hồi toàn bộ giỏ hàng theo chuẩn Shopee E-Commerce")
public class CartResponse {

    @Schema(description = "ID người dùng chủ sở hữu giỏ hàng", example = "1")
    private Long userId;

    @Builder.Default
    @Schema(description = "Danh sách sản phẩm được phân nhóm theo từng Cửa hàng")
    private List<CartShopGroupResponse> shops = new ArrayList<>();

    @Schema(description = "Tổng số loại mặt hàng (SKU) khác nhau trong giỏ", example = "3")
    private Integer totalItems;

    @Schema(description = "Tổng số lượng sản phẩm tất cả các loại", example = "5")
    private Integer totalQuantity;

    @Schema(description = "Tổng giá trị tiền của toàn bộ giỏ hàng", example = "1250000.00")
    private BigDecimal totalAmount;
}
