package com.helishop.core.modules.user.dto;

import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import java.time.LocalDateTime;

public record AdminAccountResponse(Long id, String email, String fullName, UserRole role, UserStatus status, LocalDateTime createdAt) {}
