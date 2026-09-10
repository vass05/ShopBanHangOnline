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
@Schema(description = "Nhóm các sản phẩm trong giỏ hàng theo từng Cửa hàng (Shop)")
public class CartShopGroupResponse {

    @Schema(description = "ID của Cửa hàng", example = "3")
    private Long shopId;

    @Schema(description = "Tên Cửa hàng", example = "Anker Official Store")
    private String shopName;

    @Builder.Default
    @Schema(description = "Danh sách các sản phẩm thuộc Cửa hàng này")
    private List<CartItemDto> items = new ArrayList<>();

    @Schema(description = "Tổng tiền phụ của các sản phẩm thuộc Cửa hàng này", example = "500000.00")
    private BigDecimal shopTotal;

    @Schema(description = "Tổng số lượng sản phẩm thuộc Cửa hàng này", example = "2")
    private Integer shopItemCount;
}
