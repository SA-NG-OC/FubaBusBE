package com.example.Fuba_BE.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.dto.User.UserResponseDTO;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "role.roleId", target = "roleId")
    @Mapping(source = "role.roleName", target = "roleName")
    @Mapping(source = "role.description", target = "roleDescription")
    UserResponseDTO toResponseDTO(User user);
}
