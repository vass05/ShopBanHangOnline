package com.helishop.core.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Kết quả tải lên hình ảnh media")
public class MediaUploadResponse {

    @Schema(description = "Đường dẫn ảnh trực tuyến bảo mật CDN (HTTPS)", example = "https://res.cloudinary.com/helishop/image/upload/v123456/product.webp")
    private String url;

    @Schema(description = "Mã định danh công khai trên Cloudinary", example = "helishop/products/sample_product")
    private String publicId;

    @Schema(description = "Định dạng hình ảnh (jpg, png, webp)", example = "webp")
    private String format;

    @Schema(description = "Chiều rộng ảnh (pixel)", example = "800")
    private Integer width;

    @Schema(description = "Chiều cao ảnh (pixel)", example = "800")
    private Integer height;

    @Schema(description = "Kích thước tệp (bytes)", example = "104857")
    private Long size;
}
