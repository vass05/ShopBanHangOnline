package com.helishop.core.modules.user.service;

import com.helishop.core.common.constants.*;
import com.helishop.core.common.exception.*;
import com.helishop.core.common.response.PageResponse;
import com.helishop.core.modules.auth.service.RefreshTokenService;
import com.helishop.core.modules.user.dto.*;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Slf4j @Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AdminAccountService {
    private final UserRepository users;
    private final RefreshTokenService refreshTokens;

    private void requireAdmin(Long actorId) {
        User actor = users.findById(actorId).orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
        if (actor.getRole() != UserRole.ROLE_ADMIN || actor.getStatus() != UserStatus.ACTIVE) throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    public PageResponse<AdminAccountResponse> list(Long actorId, int page, int size, String keyword, UserRole role, UserStatus status) {
        requireAdmin(actorId);
        if (page < 1 || size < 1 || size > 50 || (keyword != null && keyword.length() > 100) || role == UserRole.ROLE_ADMIN) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Bộ lọc tài khoản không hợp lệ");
        }
        Specification<User> spec = (root, query, cb) -> root.get("role").in(UserRole.ROLE_CUSTOMER, UserRole.ROLE_SELLER);
        if (role != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        if (status != null) spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        if (keyword != null && !keyword.isBlank()) {
            String term = "%" + keyword.trim().toLowerCase(java.util.Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            spec = spec.and((root, query, cb) -> cb.or(cb.like(cb.lower(root.get("email")), term, '!'), cb.like(cb.lower(root.get("fullName")), term, '!')));
        }
        return PageResponse.from(users.findAll(spec, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"))).map(this::response));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdminAccountResponse update(Long actorId, Long id, AdminAccountStatusRequest request) {
        requireAdmin(actorId);
        if (actorId.equals(id) || request.status() == UserStatus.INACTIVE) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Chỉ khóa hoặc mở khóa tài khoản người mua/người bán");
        }
        User user = users.findManagedAccountWithLock(id).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy tài khoản người mua/người bán"));
        if (user.getStatus() != request.status() && user.getStatus() != request.expectedStatus()) {
            throw new AppException(ErrorCode.OPTIMISTIC_LOCK_CONFLICT, "Tài khoản đã thay đổi. Hãy tải lại danh sách trước khi thao tác");
        }
        UserStatus previous = user.getStatus();
        if (previous == request.status()) return response(user);
        // Both block and unblock invalidate all earlier access and refresh sessions.
        refreshTokens.deleteByUserEmail(user.getEmail());
        if (previous != request.status()) user.setAuthVersion(Math.incrementExact(user.getAuthVersion()));
        user.setStatus(request.status());
        users.saveAndFlush(user);
        log.info("Admin account status: actorId={}, targetId={}, previous={}, current={}", actorId, id, previous, request.status());
        return response(user);
    }

    private AdminAccountResponse response(User user) {
        return new AdminAccountResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
