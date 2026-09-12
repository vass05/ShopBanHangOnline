package com.helishop.core.modules.user.mapper;

import com.helishop.core.modules.user.dto.UserAddressResponse;
import com.helishop.core.modules.user.dto.UserRequest;
import com.helishop.core.modules.user.dto.UserResponse;
import com.helishop.core.modules.user.entity.User;
import com.helishop.core.modules.user.entity.UserAddress;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "shop", ignore = true)
    User toEntity(UserRequest request);

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "fullAddress", expression = "java(formatAddress(userAddress))")
    UserAddressResponse toAddressResponse(UserAddress userAddress);

    default String formatAddress(UserAddress address) {
        if (address == null) return "";
        return String.join(", ",
            address.getStreetAddress() != null ? address.getStreetAddress() : "",
            address.getWard() != null ? address.getWard() : "",
            address.getDistrict() != null ? address.getDistrict() : "",
            address.getProvinceCity() != null ? address.getProvinceCity() : ""
        );
    }
}
