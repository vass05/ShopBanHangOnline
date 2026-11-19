package com.helishop.core.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegistrationOtpRequest {
    @NotBlank
    @Size(max = 150)
    @Pattern(regexp = "(?i)[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@gmail\\.com", message = "Vui lòng nhập địa chỉ @gmail.com")
    private String email;
}
