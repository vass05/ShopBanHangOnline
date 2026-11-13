package com.helishop.core.security;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    public Long getCurrentUserId(Long requestedCustomerId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED, "Vui lòng đăng nhập để thực hiện thao tác");
        }

        Long currentId;
        if (authentication.getPrincipal() instanceof AuthenticatedUser user && user.id() != null) {
            currentId = user.id();
        } else {
            currentId = userRepository.findByEmail(authentication.getName())
                .map(user -> user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED, "Không tìm thấy thông tin người dùng"));
        }
        if (requestedCustomerId != null && !requestedCustomerId.equals(currentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Không được sử dụng danh tính của tài khoản khác");
        }
        return currentId;
    }

    public boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }
}
