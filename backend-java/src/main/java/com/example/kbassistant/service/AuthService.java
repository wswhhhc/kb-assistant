package com.example.kbassistant.service;

import com.example.kbassistant.dto.request.LoginRequest;
import com.example.kbassistant.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout();
}
