package com.helishop.core.modules.user.dto;

import com.helishop.core.common.constants.ShopStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@Schema(description = "Gian hàng thuộc tài khoản người bán hiện tại")
public class ShopResponse {
    private Long id;
    private Long ownerId;
    private String shopName;
    private String description;
    private String logoUrl;
    private String bannerUrl;
    private ShopStatus status;
    private LocalDateTime createdAt;
}
