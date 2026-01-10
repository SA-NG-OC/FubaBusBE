package com.example.Fuba_BE.security;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.example.Fuba_BE.domain.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Integer userId;
    private String email;
    private String phoneNumber;
    private String password;
    private String role;
    private String status;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        Collection<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName().toUpperCase())
        );

        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getPassword(),
                user.getRole().getRoleName(),
                user.getStatus(),
                authorities
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "Active".equalsIgnoreCase(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Active".equalsIgnoreCase(status);
    }
}
