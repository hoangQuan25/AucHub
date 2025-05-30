package com.example.timedauctions.client;

import com.example.timedauctions.client.dto.UserBanStatusDto;
import com.example.timedauctions.client.dto.UserBasicInfoDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "users")
public interface UserServiceClient {

    @GetMapping("/batch")
    Map<String, UserBasicInfoDto> getUsersBasicInfoByIds(@RequestParam("ids") List<String> userIds);

    @GetMapping("/{userId}/ban-status")
    UserBanStatusDto getUserBanStatus(@PathVariable("userId") String userId);
}