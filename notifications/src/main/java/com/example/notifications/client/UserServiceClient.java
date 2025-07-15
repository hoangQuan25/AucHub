package com.example.notifications.client;

import com.example.notifications.client.dto.EmailPreferenceRequest;
import com.example.notifications.client.dto.UserBasicInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "users")
public interface UserServiceClient {

    @GetMapping("/batch")
    Map<String, UserBasicInfoDto> getUsersBasicInfoByIds(@RequestParam("ids") List<String> userIds);

    @PutMapping("/preferences/email")
    void updateUserEmailPreference(@RequestHeader("X-User-ID") String userId, @RequestBody EmailPreferenceRequest request);
}