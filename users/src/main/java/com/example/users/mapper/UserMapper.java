// src/main/java/com/example/users/mapper/UserMapper.java
package com.example.users.mapper;

import com.example.users.dto.UserBasicInfoDto;
import com.example.users.dto.UserDto;
import com.example.users.dto.UpdateUserDto;
import com.example.users.entity.User;

// NO @Mapper annotation here anymore
public interface UserMapper {

    UserDto toUserDto(User user);

    // Changed return type to void - we update the passed-in entity directly
    void updateUserFromDto(UpdateUserDto dto, User user);

    UserBasicInfoDto toUserBasicInfoDto(User user);
}