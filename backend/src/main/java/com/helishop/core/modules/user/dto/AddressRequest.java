package com.helishop.core.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class AddressRequest {

    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String recipientName;

    @NotBlank(message = "Số điện thoại nhận hàng không được để trống")
    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String phone;

    @NotBlank(message = "Tỉnh / Thành phố không được để trống")
    @Size(max = 100, message = "Tỉnh / Thành phố tối đa 100 ký tự")
    private String provinceCity;

    @NotBlank(message = "Quận / Huyện không được để trống")
    @Size(max = 100, message = "Quận / Huyện tối đa 100 ký tự")
    private String district;

    @NotBlank(message = "Phường / Xã không được để trống")
    @Size(max = 100, message = "Phường / Xã tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự")
    private String streetAddress;

    private Boolean isDefault;
}
