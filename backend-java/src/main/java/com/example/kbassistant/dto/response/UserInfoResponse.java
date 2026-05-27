package com.example.kbassistant.dto.response;

import lombok.Data;

@Data
public class UserInfoResponse {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String role;
    private String status;
}
