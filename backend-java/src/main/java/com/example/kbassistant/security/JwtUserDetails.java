package com.example.kbassistant.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.kbassistant.constants.UserRoles;

import java.util.Collection;

@Data
@AllArgsConstructor
public class JwtUserDetails implements UserDetails {
    private Long userId;
    private String username;
    private String role;
    private Collection<? extends GrantedAuthority> authorities;

    public boolean isAdmin() {
        return UserRoles.ADMIN.equals(this.role);
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
