package com.helishop.core.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu thêm sản phẩm vào giỏ hàng")
public class AddToCartRequest {

    @NotNull(message = "Mã SKU không được để trống")
    @Schema(description = "ID biến thể sản phẩm (SKU ID)", example = "105")
    private Long skuId;

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng thêm vào giỏ phải lớn hơn hoặc bằng 1")
    @Schema(description = "Số lượng cần thêm", example = "2")
    private Integer quantity;
}
