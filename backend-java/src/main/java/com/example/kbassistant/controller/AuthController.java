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
        var user = userService.getById(userDetails.getUserId());
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setStatus(user.getStatus());
        return Result.success(resp);
    }
}
