package com.helishop.core.modules.user.service;

import com.helishop.core.common.constants.ShopStatus;
import com.helishop.core.common.constants.UserRole;
import com.helishop.core.common.constants.UserStatus;
import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.user.dto.OpenShopRequest;
import com.helishop.core.modules.user.dto.ShopResponse;
import com.helishop.core.modules.user.entity.Shop;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.repository.ShopRepository;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Slf4j
public class ShopService {
    private final ShopRepository shops;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public ShopResponse mine(Long ownerId) {
        requireSeller(users.findById(ownerId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
        return shops.findByOwnerId(ownerId).map(this::response).orElse(null);
    }

    @Transactional
    public ShopResponse open(Long ownerId, OpenShopRequest request) {
        // Serialize creation for the same account; the unique owner constraint is the final safeguard.
        User owner = users.findByIdWithLock(ownerId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
        requireSeller(owner);
        if (shops.findByOwnerId(ownerId).isPresent()) throw new AppException(ErrorCode.SHOP_ALREADY_EXISTS);
        String name = request.getShopName() == null ? "" : request.getShopName().strip();
        String description = request.getDescription() == null ? null : request.getDescription().strip();
        if (name.length() < 3 || name.length() > 150 || (description != null && description.length() > 2000)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Tên gian hàng cần từ 3 đến 150 ký tự; mô tả tối đa 2000 ký tự");
        }
        if (shops.existsByShopNameIgnoreCase(name)) throw new AppException(ErrorCode.SHOP_NAME_TAKEN);
        Shop shop = Shop.builder().owner(owner).shopName(name).description(description)
                .status(ShopStatus.ACTIVE).build();
        try {
            // A concurrent account can claim the same name after the lookup; let the database arbitrate.
            return response(shops.saveAndFlush(shop));
        } catch (DataIntegrityViolationException exception) {
            log.info("Shop name constraint prevented a conflicting creation for owner {}", ownerId);
            throw new AppException(ErrorCode.SHOP_NAME_TAKEN);
        }
    }

    private void requireSeller(User user) {
        if (user.getRole() != UserRole.ROLE_SELLER || user.getStatus() != UserStatus.ACTIVE) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Chỉ tài khoản người bán đang hoạt động được mở gian hàng");
        }
    }

    private ShopResponse response(Shop shop) {
        return ShopResponse.builder().id(shop.getId()).ownerId(shop.getOwner().getId())
                .shopName(shop.getShopName()).description(shop.getDescription()).logoUrl(shop.getLogoUrl())
                .bannerUrl(shop.getBannerUrl()).status(shop.getStatus()).createdAt(shop.getCreatedAt()).build();
    }
}
