package com.example.Fuba_BE.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Fuba_BE.domain.entity.User;
import com.example.Fuba_BE.exception.NotFoundException;
import com.example.Fuba_BE.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String emailOrPhone) throws UsernameNotFoundException {
        User user = userRepository.findByEmailOrPhoneNumber(emailOrPhone, emailOrPhone)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email or phone: " + emailOrPhone));

        return UserPrincipal.create(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + userId));

        return UserPrincipal.create(user);
    }
}
