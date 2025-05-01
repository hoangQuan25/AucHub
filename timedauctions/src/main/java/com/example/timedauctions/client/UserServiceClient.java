package com.example.timedauctions.client;

import com.example.timedauctions.client.dto.UserBasicInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

// name should match the service discovery name
// path is *removed* because Gateway rewrites /api/users/** to /**
@FeignClient(name = "users") // Use registered service name
public interface UserServiceClient {

    // Path here matches the endpoint on the *target* UserController
    @GetMapping("/batch") // Path after Gateway rewrite
    Map<String, UserBasicInfoDto> getUsersBasicInfoByIds(@RequestParam("ids") List<String> userIds);

    // Add other methods if needed
}