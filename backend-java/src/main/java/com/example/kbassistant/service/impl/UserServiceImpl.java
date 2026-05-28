package com.example.kbassistant.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.kbassistant.common.BusinessException;
import com.example.kbassistant.constants.UserRoles;
import com.example.kbassistant.dto.request.UserCreateRequest;
import com.example.kbassistant.dto.response.UserInfoResponse;
import com.example.kbassistant.entity.SysUser;
import com.example.kbassistant.mapper.SysUserMapper;
import com.example.kbassistant.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public SysUser getByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username));
    }

    @Override
    public IPage<SysUser> page(int pageNum, int pageSize) {
        return userMapper.selectPage(new Page<>(pageNum, pageSize), null);
    }

    @Override
    public void create(UserCreateRequest request) {
        SysUser existing = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, request.getUsername()));
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(request.getEmail());
        user.setRole(request.getRole() != null ? request.getRole() : UserRoles.USER);
        user.setStatus("ACTIVE");
        userMapper.insert(user);
    }

    @Override
    public void updateStatus(Long id, String status) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public UserInfoResponse getUserInfo(Long userId) {
        SysUser user = userMapper.selectById(userId);
        UserInfoResponse resp = new UserInfoResponse();
        resp.setId(user.getId());
        resp.setUsername(user.getUsername());
        resp.setRealName(user.getRealName());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        resp.setStatus(user.getStatus());
        return resp;
    }

    @Override
    public void resetPassword(Long id, String newPassword) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }
}
