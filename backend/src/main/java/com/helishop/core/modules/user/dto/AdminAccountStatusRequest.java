package com.helishop.core.modules.user.dto;

import com.helishop.core.common.constants.UserStatus;
import jakarta.validation.constraints.NotNull;

public record AdminAccountStatusRequest(@NotNull UserStatus expectedStatus, @NotNull UserStatus status) {}
