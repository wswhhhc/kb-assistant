package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.UserCreateRequest;
import com.example.kbassistant.dto.response.UserInfoResponse;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Result<UserInfoResponse> me(@AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(userService.getUserInfo(userDetails.getUserId()));
    }

    @GetMapping
    public Result<?> list(@RequestParam(defaultValue = "1") int pageNum,
                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(userService.page(pageNum, pageSize));
    }

    @PostMapping
    public Result<Void> create(@Valid @RequestBody UserCreateRequest request) {
        userService.create(request);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status) {
        userService.updateStatus(id, status);
        return Result.success();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestParam String password) {
        userService.resetPassword(id, password);
        return Result.success();
    }
}
