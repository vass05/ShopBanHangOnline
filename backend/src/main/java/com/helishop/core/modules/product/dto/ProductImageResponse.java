package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin chi tiết hình ảnh sản phẩm")
public class ProductImageResponse {

    @Schema(description = "ID hình ảnh", example = "1")
    private Long id;

    @Schema(description = "Đường dẫn URL của hình ảnh", example = "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=800")
    private String imageUrl;

    @Schema(description = "Có phải ảnh đại diện thumbnail không", example = "true")
    private Boolean isThumbnail;

    @Schema(description = "Thứ tự sắp xếp hiển thị", example = "1")
    private Integer displayOrder;
}
