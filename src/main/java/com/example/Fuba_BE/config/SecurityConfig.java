package com.example.Fuba_BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/user/role/Buyer",
                                "/api/public/**",
                                "/api/trips/seats/**"
                        ).permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/api/tickets/**").authenticated()
                        .requestMatchers("/api/users/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest().permitAll()
//                        .anyRequest().authenticated()
                );

        return http.build();
    }
}