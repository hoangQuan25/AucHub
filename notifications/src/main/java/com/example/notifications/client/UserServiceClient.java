package com.example.notifications.client;

// Assuming UserBasicInfoDto exists in a shared module or copied here
import com.example.notifications.client.dto.UserBasicInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "users") // Name of the User service registered in Eureka
public interface UserServiceClient {

    // Assuming endpoint exists on User service to get basic info (including email)
    @GetMapping("/batch") // Path on User service after gateway rewrite
    Map<String, UserBasicInfoDto> getUsersBasicInfoByIds(@RequestParam("ids") List<String> userIds);

    // Add specific method if needed just for email?
    // @GetMapping("/users/emails")
    // Map<String, String> getEmailsByUserIds(@RequestParam("ids") List<String> userIds);
}