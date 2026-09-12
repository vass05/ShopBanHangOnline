package com.helishop.core.modules.user.service;

import com.helishop.core.common.exception.AppException;
import com.helishop.core.common.exception.ErrorCode;
import com.helishop.core.modules.user.dto.AddressRequest;
import com.helishop.core.modules.user.dto.ChangePasswordRequest;
import com.helishop.core.modules.user.dto.UpdateProfileRequest;
import com.helishop.core.modules.user.dto.UserAddressResponse;
import com.helishop.core.modules.user.dto.UserResponse;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.entity.UserAddress;
import com.helishop.core.modules.user.mapper.UserMapper;
import com.helishop.core.modules.user.repository.UserAddressRepository;
import com.helishop.core.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        User user = getById(userId);
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getById(userId);

        if (request.getPhone() != null && !request.getPhone().trim().isBlank()) {
            String newPhone = request.getPhone().trim();
            if (!newPhone.equals(user.getPhone()) && userRepository.existsByPhone(newPhone)) {
                throw new AppException(ErrorCode.USER_EXISTED, "Số điện thoại này đã được liên kết với một tài khoản khác");
            }
            user.setPhone(newPhone);
        }

        if (request.getFullName() != null && !request.getFullName().trim().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("Cập nhật thông tin tài khoản thành công cho userId: {}", userId);
        return userMapper.toResponse(updatedUser);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getById(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu hiện tại không chính xác");
        }

        if (request.getConfirmPassword() != null && !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu xác nhận không khớp với mật khẩu mới");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Mật khẩu mới không được trùng với mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword().trim()));
        userRepository.save(user);
        log.info("Đổi mật khẩu thành công cho userId: {}", userId);
    }

    @Transactional(readOnly = true)
    public List<UserAddressResponse> getUserAddresses(Long userId) {
        List<UserAddress> addresses = userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
        return addresses.stream()
                .map(userMapper::toAddressResponse)
                .toList();
    }

    @Transactional
    public UserAddressResponse addAddress(Long userId, AddressRequest request) {
        User user = getById(userId);
        List<UserAddress> existingAddresses = userAddressRepository.findByUserId(userId);

        boolean shouldBeDefault = existingAddresses.isEmpty() || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault && !existingAddresses.isEmpty()) {
            for (UserAddress addr : existingAddresses) {
                if (Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    userAddressRepository.save(addr);
                }
            }
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .provinceCity(request.getProvinceCity().trim())
                .district(request.getDistrict().trim())
                .ward(request.getWard().trim())
                .streetAddress(request.getStreetAddress().trim())
                .isDefault(shouldBeDefault)
                .build();

        UserAddress saved = userAddressRepository.save(address);
        log.info("Thêm mới địa chỉ nhận hàng id: {} cho userId: {}", saved.getId(), userId);
        return userMapper.toAddressResponse(saved);
    }

    @Transactional
    public UserAddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ này"));

        boolean isDefaultRequested = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefaultRequested && !Boolean.TRUE.equals(address.getIsDefault())) {
            List<UserAddress> existingAddresses = userAddressRepository.findByUserId(userId);
            for (UserAddress addr : existingAddresses) {
                if (!addr.getId().equals(addressId) && Boolean.TRUE.equals(addr.getIsDefault())) {
                    addr.setIsDefault(false);
                    userAddressRepository.save(addr);
                }
            }
            address.setIsDefault(true);
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setProvinceCity(request.getProvinceCity().trim());
        address.setDistrict(request.getDistrict().trim());
        address.setWard(request.getWard().trim());
        address.setStreetAddress(request.getStreetAddress().trim());

        UserAddress updated = userAddressRepository.save(address);
        log.info("Cập nhật địa chỉ nhận hàng id: {} cho userId: {}", addressId, userId);
        return userMapper.toAddressResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ này"));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);
        log.info("Đã xóa địa chỉ id: {} của userId: {}", addressId, userId);

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findByUserIdOrderByIsDefaultDescIdDesc(userId);
            if (!remaining.isEmpty()) {
                UserAddress newDefault = remaining.get(0);
                newDefault.setIsDefault(true);
                userAddressRepository.save(newDefault);
                log.info("Tự động chuyển địa chỉ id: {} thành mặc định mới cho userId: {}", newDefault.getId(), userId);
            }
        }
    }

    @Transactional
    public UserAddressResponse setDefaultAddress(Long userId, Long addressId) {
        UserAddress address = userAddressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy địa chỉ này"));

        List<UserAddress> allAddresses = userAddressRepository.findByUserId(userId);
        for (UserAddress addr : allAddresses) {
            boolean isTarget = addr.getId().equals(addressId);
            if (!addr.getIsDefault().equals(isTarget)) {
                addr.setIsDefault(isTarget);
                userAddressRepository.save(addr);
            }
        }

        address.setIsDefault(true);
        log.info("Đã đặt địa chỉ id: {} làm mặc định cho userId: {}", addressId, userId);
        return userMapper.toAddressResponse(address);
    }
}
