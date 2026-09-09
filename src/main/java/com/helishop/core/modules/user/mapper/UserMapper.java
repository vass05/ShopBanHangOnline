package com.helishop.core.modules.user.mapper;

import com.helishop.core.modules.user.dto.UserRequest;
import com.helishop.core.modules.user.dto.UserResponse;
import com.helishop.core.modules.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);

    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    @Mapping(target = "shop", ignore = true)
    User toEntity(UserRequest request);
}
