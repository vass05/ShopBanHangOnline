package com.helishop.core.modules.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Yêu cầu xóa danh sách sản phẩm khỏi giỏ hàng")
public class RemoveCartItemsRequest {

    @NotEmpty(message = "Danh sách mã SKU cần xóa không được rỗng")
    @Schema(description = "Danh sách ID các SKU cần xóa", example = "[101, 105]")
    private List<Long> skuIds;
}
