package com.helishop.core.modules.auth.dto;

import com.helishop.core.common.constants.UserRole;
import jakarta.validation.constraints.*;
public record GoogleCompleteRequest(@NotBlank @Pattern(regexp = "[a-f0-9]{64}") String ticket, UserRole role, @Size(max = 72) String password) {}
