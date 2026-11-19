package com.helishop.core.modules.auth.dto;

import jakarta.validation.constraints.*;
public record GoogleLoginRequest(@NotBlank @Size(max = 10000) String credential) {}
