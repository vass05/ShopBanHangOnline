package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
@Schema(description = "Yêu cầu tạo mới hoặc cập nhật danh mục sản phẩm")
public class CategoryRequest {

    @Schema(description = "Tên danh mục", example = "Điện thoại & Phụ kiện", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;

    @Schema(description = "Đường dẫn SEO (Slug)", example = "dien-thoai-phu-kien")
    private String slug;

    @Schema(description = "Ảnh minh họa danh mục", example = "https://cdn.helishop.com/cat-phone.png")
    private String imageUrl;

    @Schema(description = "Cấp bậc danh mục (1: Gốc, 2: Cấp 2...)", example = "1")
    private Integer level;

    @Schema(description = "Thứ tự sắp xếp", example = "1")
    private Integer displayOrder;

    @Schema(description = "ID danh mục cha (null nếu là gốc)", example = "null")
    private Long parentId;
}
