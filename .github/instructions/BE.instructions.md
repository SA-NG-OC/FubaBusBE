---
applyTo: '**'
---
Provide project context and coding guidelines that AI should follow when generating code, answering questions, or reviewing changes.

### **Controller:**
- [ ] Dùng `@RestController` + `@RequestMapping`
- [ ] Sử dụng đường dẫn API không có phiên bản (ví dụ: `/trips` thay vì `/api/v1/trips`)
- [ ] Constructor injection với `private final` + `@RequiredArgsConstructor`
- [ ] Tất cả methods trả về `ResponseEntity<ApiResponse<T>>`
- [ ] Đối với GET requests, sử dụng `@GetMapping` + `@RequestParam` cho filters/pagination và luôn dùng pagination mặc định không truyền sẽ trả về 20 items 
- [ ] Dùng `@Valid` cho request body
- [ ] Không có business logic trong controller
- [ ] Không gọi Repository trực tiếp

### **Service:**
- [ ] Dùng `@Service` + implement interface
- [ ] Constructor injection
- [ ] Thêm `@Transactional` (hoặc `@Transactional(readOnly = true)`)
- [ ] Business logic validation
- [ ] Throw custom exceptions (`NotFoundException`, `BadRequestException`)
- [ ] Tên Interface bắt đầu với chữ I (ví dụ: `IUserService`)
- [ ] tên service theo dạng động từ (ví dụ: UserService, AuthService)
- [ ] Dùng Mapper để convert Entity ↔ DTO
- [ ] Không expose Entity ra ngoài

### **Repository:**
- [ ] Extend `JpaRepository<Entity, ID>`
- [ ] Dùng `@Query` với `JOIN FETCH` để tránh N+1
- [ ] Custom query methods theo naming convention
- [ ] Không có business logic

### **Entity:**
- [ ] `@Entity` + `@Table(name = "...")`
- [ ] `@Builder` + `@Builder.Default` cho fields có giá trị mặc định
- [ ] Lazy loading cho relationships (`fetch = FetchType.LAZY`)
- [ ] `@CreationTimestamp` / `@UpdateTimestamp` cho audit fields

### **DTO:**
- [ ] Validation annotations (`@NotNull`, `@Size`, `@Email`, etc.)
- [ ] Flat structure (không nested entities)
- [ ] Separate RequestDTO và ResponseDTO
- [ ] service trả về entity không trả về dto
- [ ] dto được và mapper thuộc tầng controller

### **Mapper:**
- [ ] `@Mapper(componentModel = "spring")`
- [ ] `@Mapping` cho complex conversions
- [ ] Helper methods cho custom logic
- [ ] Handle null-safe operations
- [ ] luôn sử dụng Mapper để convert Entity ↔ DTO
- [ ] mapper sẽ map khi map sang DTO cho những field trả ra ở controller và map ngược lại khi nhận request từ controller

### **Exception Handling:**
- [ ] Custom exceptions extend `AppException`
- [ ] `@ExceptionHandler` trong `GlobalExceptionHandler`
- [ ] Trả về `ApiResponse` với error code
- [ ] Log exception với appropriate level
- [ ] dùng lombok @Slf4j để log
