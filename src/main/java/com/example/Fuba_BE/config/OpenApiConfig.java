package com.example.Fuba_BE.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Fuba Bus Backend API",
                version = "1.0.0",
                description = """
                        REST API for Fuba Bus Ticket Management System.
                        
                        **Features:**
                        - Trip management (CRUD operations)
                        - Real-time seat booking with WebSocket support
                        - Route & vehicle management
                        - Driver assignment and tracking
                        - Payment processing
                        - Location management
                        
                        **Authentication:**
                        Currently in development mode - most endpoints are open.
                        JWT authentication will be required in production.
                        """,
                contact = @Contact(
                        name = "Fuba Development Team",
                        email = "dev@fubabus.com",
                        url = "https://github.com/fubabus"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        url = "http://localhost:5230",
                        description = "Local Development Server"
                ),
                @Server(
                        url = "https://api.fubabus.com",
                        description = "Production Server (Future)"
                )
        }
)
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT authentication token (format: Bearer <token>)"
)
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("Fuba Bus Backend API")
                        .version("1.0.0")
                        .description("Complete API documentation for bus ticket management system")
                );
    }
}
