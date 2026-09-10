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
@Schema(description = "Yêu cầu cập nhật số lượng món hàng trong giỏ")
public class UpdateCartItemRequest {

    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 0, message = "Số lượng không được âm (bằng 0 sẽ tự động xóa khỏi giỏ hàng)")
    @Schema(description = "Số lượng cập nhật mới (nếu bằng 0 sẽ tự xóa khỏi giỏ)", example = "3")
    private Integer quantity;
}
