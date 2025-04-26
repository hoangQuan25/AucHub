// src/main/java/com/example/users/mapper/UserMapper.java
package com.example.products.mapper;

import com.example.products.dto.UserDto;
import com.example.products.dto.UpdateUserDto;
import com.example.products.entity.User;

// NO @Mapper annotation here anymore
public interface UserMapper {

    UserDto toUserDto(User user);

    // Changed return type to void - we update the passed-in entity directly
    void updateUserFromDto(UpdateUserDto dto, User user);
}