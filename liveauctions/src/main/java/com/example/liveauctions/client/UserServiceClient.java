package com.example.liveauctions.client;

import com.example.liveauctions.client.dto.UserBanStatusDto;
import com.example.liveauctions.client.dto.UserBasicInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "users") // Use registered service name
public interface UserServiceClient {

    // Path here matches the endpoint on the *target* UserController
    @GetMapping("/batch") // Path after Gateway rewrite
    Map<String, UserBasicInfoDto> getUsersBasicInfoByIds(@RequestParam("ids") List<String> userIds);

    @GetMapping("/{userId}/ban-status")
    UserBanStatusDto getUserBanStatus(@PathVariable("userId") String userId);
}