package com.example.Fuba_BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
// import các class filter token sau này bạn sẽ viết
// import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // 1. KLV: Những API MỞ CỬA (Ai cũng vào được)
                        .requestMatchers(
                                "/api/auth/**",      // Đăng nhập, Đăng ký
                                "/user/role/Buyer",        // Xem danh sách role (Ví dụ test)
                                "/api/public/**"     // Các API công khai khác
                        ).permitAll()

                        // 2. KLV: Những API cần TOKEN (Phải đăng nhập mới vào được)
                        .requestMatchers("/api/tickets/**").authenticated() // Mua vé
                        .requestMatchers("/api/users/**").authenticated()   // Xem user info

                        // 3. KLV: Những API dành riêng cho ADMIN (Phân quyền sâu hơn)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 4. CHỐT: Tất cả các request còn lại bắt buộc phải xác thực
                        .anyRequest().authenticated()
                );

        // SAU NÀY: Bạn sẽ chèn thêm một cái "Bộ lọc Token" (Filter) vào đây
        // http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}