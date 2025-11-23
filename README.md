# Tài liệu Cấu trúc Dự án Spring Boot - Fuba_BE

## Tổng quan kiến trúc
Dự án sử dụng kiến trúc phân lớp (Layered Architecture) theo mô hình MVC mở rộng:
- **Controller**: Nhận request từ client
- **Service**: Xử lý business logic
- **Repository**: Tương tác với database
- **Domain/Entity**: Đại diện cho bảng trong database
- **DTO**: Data Transfer Object - truyền dữ liệu giữa các lớp
- **Mapper**: Chuyển đổi giữa Entity và DTO
- **Exception**: Xử lý lỗi tập trung
- **Config**: Cấu hình ứng dụng
- **Util**: Các hàm tiện ích dùng chung

---

## 📁 **1. config/**
**Mục đích**: Chứa các file cấu hình cho ứng dụng

### Các file thường có:
- `SecurityConfig.java` - Cấu hình bảo mật (JWT, authentication)
- `DatabaseConfig.java` - Cấu hình kết nối database
- `CorsConfig.java` - Cấu hình CORS cho API
- `SwaggerConfig.java` - Cấu hình API documentation

**Ví dụ - SecurityConfig.java**:
```java
package com.example.Fuba_BE.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable()
            .authorizeHttpRequests()
            .requestMatchers("/api/auth/**").permitAll()
            .anyRequest().authenticated();
        return http.build();
    }
}
```

---

## 📁 **2. controller/**
**Mục đích**: Nhận HTTP requests từ client và trả về responses

### Đặc điểm:
- Sử dụng annotation `@RestController`
- Mapping URL với `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`
- Không chứa business logic
- Gọi Service layer để xử lý

**Ví dụ - UserController.java**:
```java
package com.example.Fuba_BE.controller;

import com.example.Fuba_BE.dto.UserDTO;
import com.example.Fuba_BE.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    // GET /api/users - Lấy danh sách users
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    // GET /api/users/{id} - Lấy user theo ID
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    // POST /api/users - Tạo user mới
    @PostMapping
    public ResponseEntity<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        UserDTO createdUser = userService.createUser(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
    
    // PUT /api/users/{id} - Cập nhật user
    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(
            @PathVariable Long id, 
            @RequestBody UserDTO userDTO) {
        UserDTO updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }
    
    // DELETE /api/users/{id} - Xóa user
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 📁 **3. domain/**
**Mục đích**: Chứa các Entity class đại diện cho bảng trong database

### Đặc điểm:
- Sử dụng JPA annotations: `@Entity`, `@Table`, `@Id`, `@Column`
- Ánh xạ 1-1 với bảng database
- Chứa các mối quan hệ: `@OneToMany`, `@ManyToOne`, `@ManyToMany`

**Ví dụ - User.java**:
```java
package com.example.Fuba_BE.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    
    @Column(nullable = false)
    private String password;
    
    @Column(length = 100)
    private String fullName;
    
    @Column(length = 15)
    private String phoneNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.USER;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    // Enum cho Role
    public enum UserRole {
        USER, ADMIN, MODERATOR
    }
}
```

---

## 📁 **4. dto/**
**Mục đích**: Data Transfer Objects - truyền dữ liệu giữa client và server

### Đặc điểm:
- Không chứa business logic
- Có thể có validation annotations: `@NotNull`, `@Email`, `@Size`
- Dùng để che giấu thông tin nhạy cảm (ví dụ: password)
- Có thể có nhiều DTO cho một Entity (CreateUserDTO, UpdateUserDTO, UserResponseDTO)

**Ví dụ - UserDTO.java**:
```java
package com.example.Fuba_BE.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    
    @NotBlank(message = "Username không được để trống")
    @Size(min = 3, max = 50, message = "Username phải từ 3-50 ký tự")
    private String username;
    
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
    
    // Không trả password về client
    // private String password;
    
    private String fullName;
    private String phoneNumber;
    private String role;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Ví dụ - CreateUserDTO.java**:
```java
package com.example.Fuba_BE.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserDTO {
    
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6, message = "Password phải có ít nhất 6 ký tự")
    private String password;
    
    private String fullName;
    private String phoneNumber;
}
```

---

## 📁 **5. exception/**
**Mục đích**: Xử lý lỗi tập trung cho toàn bộ ứng dụng

### Đặc điểm:
- Custom exceptions cho từng loại lỗi
- Global exception handler với `@ControllerAdvice`
- Trả về response lỗi thống nhất

**Ví dụ - UserNotFoundException.java**:
```java
package com.example.Fuba_BE.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long id) {
        super("Không tìm thấy user với ID: " + id);
    }
    
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

**Ví dụ - GlobalExceptionHandler.java**:
```java
package com.example.Fuba_BE.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(
            UserNotFoundException ex, WebRequest request) {
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", ex.getMessage());
        body.put("status", HttpStatus.NOT_FOUND.value());
        
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("message", "Đã có lỗi xảy ra");
        body.put("details", ex.getMessage());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
```

---

## 📁 **6. mapper/**
**Mục đích**: Chuyển đổi giữa Entity và DTO

### Đặc điểm:
- Tách biệt logic chuyển đổi
- Có thể dùng thư viện MapStruct hoặc viết manual
- Giúp code sạch hơn và dễ maintain

**Ví dụ - UserMapper.java**:
```java
package com.example.Fuba_BE.mapper;

import com.example.Fuba_BE.domain.User;
import com.example.Fuba_BE.dto.CreateUserDTO;
import com.example.Fuba_BE.dto.UserDTO;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    
    // Entity -> DTO
    public UserDTO toDTO(User user) {
        if (user == null) return null;
        
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFullName(user.getFullName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole().name());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        
        return dto;
    }
    
    // DTO -> Entity
    public User toEntity(UserDTO dto) {
        if (dto == null) return null;
        
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        user.setIsActive(dto.getIsActive());
        
        if (dto.getRole() != null) {
            user.setRole(User.UserRole.valueOf(dto.getRole()));
        }
        
        return user;
    }
    
    // CreateUserDTO -> Entity
    public User toEntity(CreateUserDTO dto) {
        if (dto == null) return null;
        
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // Sẽ được mã hóa ở Service
        user.setFullName(dto.getFullName());
        user.setPhoneNumber(dto.getPhoneNumber());
        
        return user;
    }
}
```

---

## 📁 **7. repository/**
**Mục đích**: Tương tác với database (CRUD operations)

### Đặc điểm:
- Extend `JpaRepository<Entity, IDType>`
- Spring tự động implement các method cơ bản
- Có thể thêm custom query với `@Query`

**Ví dụ - UserRepository.java**:
```java
package com.example.Fuba_BE.repository;

import com.example.Fuba_BE.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Spring tự động implement dựa trên tên method
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    
    // Tìm users theo role
    List<User> findByRole(User.UserRole role);
    
    // Tìm users đang active
    List<User> findByIsActiveTrue();
    
    // Custom query với JPQL
    @Query("SELECT u FROM User u WHERE u.fullName LIKE %:keyword% OR u.username LIKE %:keyword%")
    List<User> searchUsers(@Param("keyword") String keyword);
    
    // Native SQL query
    @Query(value = "SELECT * FROM users WHERE created_at > :date", nativeQuery = true)
    List<User> findUsersCreatedAfter(@Param("date") String date);
}
```

---

## 📁 **8. service/**
**Mục đích**: Chứa business logic của ứng dụng

### Đặc điểm:
- Annotation `@Service`
- Xử lý logic nghiệp vụ
- Gọi Repository để thao tác database
- Sử dụng Mapper để chuyển đổi Entity/DTO

**Ví dụ - UserService.java**:
```java
package com.example.Fuba_BE.service;

import com.example.Fuba_BE.domain.User;
import com.example.Fuba_BE.dto.CreateUserDTO;
import com.example.Fuba_BE.dto.UserDTO;
import com.example.Fuba_BE.exception.UserNotFoundException;
import com.example.Fuba_BE.mapper.UserMapper;
import com.example.Fuba_BE.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    
    // Lấy tất cả users
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    // Lấy user theo ID
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return userMapper.toDTO(user);
    }
    
    // Tạo user mới
    public UserDTO createUser(CreateUserDTO createUserDTO) {
        // Kiểm tra username đã tồn tại
        if (userRepository.existsByUsername(createUserDTO.getUsername())) {
            throw new RuntimeException("Username đã tồn tại");
        }
        
        // Kiểm tra email đã tồn tại
        if (userRepository.existsByEmail(createUserDTO.getEmail())) {
            throw new RuntimeException("Email đã tồn tại");
        }
        
        // Chuyển DTO -> Entity
        User user = userMapper.toEntity(createUserDTO);
        
        // Mã hóa password
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        // Lưu vào database
        User savedUser = userRepository.save(user);
        
        // Trả về DTO
        return userMapper.toDTO(savedUser);
    }
    
    // Cập nhật user
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        
        // Cập nhật thông tin
        existingUser.setFullName(userDTO.getFullName());
        existingUser.setPhoneNumber(userDTO.getPhoneNumber());
        existingUser.setIsActive(userDTO.getIsActive());
        
        User updatedUser = userRepository.save(existingUser);
        return userMapper.toDTO(updatedUser);
    }
    
    // Xóa user
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
    }
    
    // Tìm kiếm users
    public List<UserDTO> searchUsers(String keyword) {
        return userRepository.searchUsers(keyword)
                .stream()
                .map(userMapper::toDTO)
                .collect(Collectors.toList());
    }
}
```

---

## 📁 **9. util/**
**Mục đích**: Chứa các hàm tiện ích dùng chung

### Các file thường có:
- `JwtUtil.java` - Xử lý JWT token
- `DateUtil.java` - Xử lý ngày tháng
- `StringUtil.java` - Xử lý chuỗi
- `ValidationUtil.java` - Validation logic

**Ví dụ - JwtUtil.java**:
```java
package com.example.Fuba_BE.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;
    
    // Tạo token
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }
    
    // Validate token
    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    // Lấy username từ token
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
```

---

## 📄 **FubaBeApplication.java**
**Mục đích**: Main class để chạy ứng dụng Spring Boot

```java
package com.example.Fuba_BE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FubaBeApplication {
    public static void main(String[] args) {
        SpringApplication.run(FubaBeApplication.class, args);
    }
}
```

---

## 🔄 Luồng xử lý một HTTP Request

```
1. Client gửi request → /api/users/1
2. Controller nhận request (UserController)
3. Controller gọi Service (UserService.getUserById(1))
4. Service gọi Repository (UserRepository.findById(1))
5. Repository truy vấn Database
6. Database trả về Entity (User)
7. Service dùng Mapper chuyển Entity → DTO (UserDTO)
8. Service trả DTO về Controller
9. Controller trả response về Client
```

---

## 📋 Dependencies cần thiết (pom.xml)

```xml
<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<!-- Spring Data JPA -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<!-- Database Driver (MySQL/PostgreSQL) -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>

<!-- Lombok -->
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt</artifactId>
    <version>0.9.1</version>
</dependency>
```

---

## ⚙️ application.properties

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/fuba_db
spring.datasource.username=root
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# JWT
jwt.secret=your_secret_key_here
jwt.expiration=86400000
```

---

## 🎯 Best Practices

1. **Không để business logic trong Controller** - Chỉ nhận request và trả response
2. **Luôn sử dụng DTO** - Không expose Entity trực tiếp ra ngoài
3. **Xử lý exception tập trung** - Dùng @ControllerAdvice
4. **Validate input** - Dùng @Valid và validation annotations
5. **Sử dụng @Transactional** - Cho các operations quan trọng
6. **Repository chỉ tương tác Database** - Không có business logic
7. **Tách interface và implementation** - Service nên có interface riêng

---

## 📚 Tài liệu tham khảo

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Lombok: https://projectlombok.org/
