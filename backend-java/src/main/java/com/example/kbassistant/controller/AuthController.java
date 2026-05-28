package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.dto.request.LoginRequest;
import com.example.kbassistant.dto.response.LoginResponse;
import com.example.kbassistant.dto.response.UserInfoResponse;
import com.example.kbassistant.security.JwtUserDetails;
import com.example.kbassistant.service.AuthService;
import com.example.kbassistant.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserInfoResponse> me(@AuthenticationPrincipal JwtUserDetails userDetails) {
        return Result.success(userService.getUserInfo(userDetails.getUserId()));
    }
}
