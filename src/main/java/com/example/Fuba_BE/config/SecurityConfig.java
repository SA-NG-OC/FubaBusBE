package com.example.Fuba_BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.example.Fuba_BE.security.CustomUserDetailsService;
import com.example.Fuba_BE.security.JwtAccessDeniedHandler;
import com.example.Fuba_BE.security.JwtAuthenticationEntryPoint;
import com.example.Fuba_BE.security.JwtAuthenticationFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // === PUBLIC ENDPOINTS (No authentication required) ===
                        .requestMatchers(
                                "/auth/**", // Authentication endpoints
                                "/api/public/**", // Public API
                                "/ws/**", "/ws", // WebSocket
                                "/actuator/**" // Health/metrics
                        ).permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // === TEMPORARILY ALLOW ALL FOR DEVELOPMENT ===
                        // Comment this line and uncomment role-based policies below when ready for
                        // production
                        .anyRequest().permitAll()

                // === ROLE-BASED AUTHORIZATION POLICIES (COMMENTED FOR DEV) ===
                // Uncomment the policies below when ready to enforce
                // authentication/authorization

                // --- ADMIN & MANAGER ---
                // .requestMatchers("/admin/**").hasAnyRole("ADMIN", "MANAGER")
                // .requestMatchers("/manager/**").hasAnyRole("ADMIN", "MANAGER")
                // .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "MANAGER")
                // .requestMatchers("/users/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                // .requestMatchers("/dashboard/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")

                // --- STAFF/EMPLOYEE ---
                // .requestMatchers("/employees/**").hasAnyRole("ADMIN", "MANAGER", "STAFF")
                // .requestMatchers(HttpMethod.POST, "/routes/**").hasAnyRole("ADMIN",
                // "MANAGER", "STAFF")
                // .requestMatchers(HttpMethod.PUT, "/routes/**").hasAnyRole("ADMIN", "MANAGER",
                // "STAFF")
                // .requestMatchers(HttpMethod.DELETE, "/routes/**").hasAnyRole("ADMIN",
                // "MANAGER")
                // .requestMatchers(HttpMethod.POST, "/trips/**").hasAnyRole("ADMIN", "MANAGER",
                // "STAFF")
                // .requestMatchers(HttpMethod.PUT, "/trips/**").hasAnyRole("ADMIN", "MANAGER",
                // "STAFF")
                // .requestMatchers(HttpMethod.DELETE, "/trips/**").hasAnyRole("ADMIN",
                // "MANAGER")
                // .requestMatchers(HttpMethod.POST, "/vehicles/**").hasAnyRole("ADMIN",
                // "MANAGER",
                // "STAFF")
                // .requestMatchers(HttpMethod.PUT, "/vehicles/**").hasAnyRole("ADMIN",
                // "MANAGER", "STAFF")
                // .requestMatchers(HttpMethod.DELETE, "/vehicles/**").hasAnyRole("ADMIN",
                // "MANAGER")

                // --- DRIVER ---
                // .requestMatchers("/drivers/me/**").hasRole("DRIVER")
                // .requestMatchers("/drivers/{driverId}/trips").hasAnyRole("ADMIN", "MANAGER",
                // "STAFF",
                // "DRIVER")

                // --- USER/CUSTOMER ---
                // .requestMatchers(HttpMethod.GET, "/routes/**").permitAll() // Anyone can view
                // routes
                // .requestMatchers(HttpMethod.GET, "/trips/**").permitAll() // Anyone can view
                // trips
                // .requestMatchers(HttpMethod.GET, "/locations/**").permitAll() // Anyone can
                // view locations
                // .requestMatchers("/bookings/**").authenticated() // Bookings require
                // authentication
                // .requestMatchers("/tickets/**").authenticated() // Tickets require
                // authentication
                // .requestMatchers("/api/seats/**").authenticated() // Seat locking requires
                // authentication

                // --- DEFAULT: All other requests require authentication ---
                // .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOriginPattern("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return new CorsFilter(source);
    }
}
