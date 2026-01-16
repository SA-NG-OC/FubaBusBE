package com.example.Fuba_BE.security;

import java.io.IOException;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.example.Fuba_BE.payload.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point for authentication errors (401 Unauthorized)
 * This is invoked when a user tries to access a secured endpoint without proper
 * authentication
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {

        log.warn("🚫 Unauthorized access attempt to: {} - {}",
                request.getRequestURI(),
                authException.getMessage());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> apiResponse = ApiResponse.error(
                "Authentication required. Please provide a valid JWT token in the Authorization header.",
                "UNAUTHORIZED");

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
