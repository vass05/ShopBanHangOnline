package com.helishop.core.modules.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Thông tin mở gian hàng; chủ sở hữu và trạng thái được máy chủ xác định")
public class OpenShopRequest {
    @NotBlank
    @Size(min = 3, max = 150)
    @Schema(description = "Tên gian hàng duy nhất, từ 3 đến 150 ký tự", example = "Heli Books")
    private String shopName;

    @Size(max = 2000)
    @Schema(description = "Giới thiệu gian hàng, tối đa 2000 ký tự")
    private String description;
}
