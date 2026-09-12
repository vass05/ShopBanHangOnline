package com.helishop.core.modules.user.dto;

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
public class UserAddressResponse {

    private Long id;
    private Long userId;
    private String recipientName;
    private String phone;
    private String provinceCity;
    private String district;
    private String ward;
    private String streetAddress;
    private Boolean isDefault;
    private String fullAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
