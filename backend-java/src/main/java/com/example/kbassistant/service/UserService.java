package com.example.kbassistant.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.kbassistant.dto.request.UserCreateRequest;
import com.example.kbassistant.entity.SysUser;

public interface UserService {
    SysUser getById(Long id);
    SysUser getByUsername(String username);
    IPage<SysUser> page(int pageNum, int pageSize);
    void create(UserCreateRequest request);
    void updateStatus(Long id, String status);
    void resetPassword(Long id, String newPassword);
}
