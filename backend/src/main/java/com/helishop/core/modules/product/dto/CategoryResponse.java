package com.helishop.core.modules.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Thông tin danh mục sản phẩm (hỗ trợ phân cấp cây đa tầng)")
public class CategoryResponse {

    @Schema(description = "ID danh mục", example = "10")
    private Long id;

    @Schema(description = "Tên danh mục", example = "Điện thoại & Phụ kiện")
    private String name;

    @Schema(description = "Đường dẫn SEO (Slug)", example = "dien-thoai-phu-kien")
    private String slug;

    @Schema(description = "Ảnh đại diện danh mục", example = "https://cdn.helishop.com/cat-phone.png")
    private String imageUrl;

    @Schema(description = "Cấp bậc phân tầng (1: Gốc, 2: Con, 3: Cháu...)", example = "1")
    private Integer level;

    @Schema(description = "Thứ tự sắp xếp hiển thị", example = "1")
    private Integer displayOrder;

    @Schema(description = "ID danh mục cha (null nếu là gốc)", example = "null")
    private Long parentId;

    @Schema(description = "Tên danh mục cha", example = "null")
    private String parentName;

    @Builder.Default
    @Schema(description = "Danh sách danh mục con cháu phân cấp")
    private java.util.List<CategoryResponse> children = new java.util.ArrayList<>();

    @Schema(description = "Thời gian tạo")
    private LocalDateTime createdAt;

    @Schema(description = "Thời gian cập nhật")
    private LocalDateTime updatedAt;
}
