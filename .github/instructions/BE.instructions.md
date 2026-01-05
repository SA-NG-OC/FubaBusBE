---
applyTo: '**'
---
Provide project context and coding guidelines that AI should follow when generating code, answering questions, or reviewing changes.

### **Controller:**
- [ ] Dùng `@RestController` + `@RequestMapping`
- [ ] Sử dụng đường dẫn API không có phiên bản (ví dụ: `/trips` thay vì `/api/v1/trips`)
- [ ] Constructor injection với `private final` + `@RequiredArgsConstructor`
- [ ] Tất cả methods trả về `ResponseEntity<ApiResponse<T>>`
- [ ] Dùng `@Valid` cho request body
- [ ] Thêm Swagger annotations (`@Tag`, `@Operation`, `@ApiResponse`)
- [ ] Không có business logic trong controller
- [ ] Không gọi Repository trực tiếp

### **Service:**
- [ ] Dùng `@Service` + implement interface
- [ ] Constructor injection
- [ ] Thêm `@Transactional` (hoặc `@Transactional(readOnly = true)`)
- [ ] Business logic validation
- [ ] Throw custom exceptions (`NotFoundException`, `BadRequestException`)
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
- [ ] Swagger schema annotations (`@Schema`)
- [ ] Flat structure (không nested entities)
- [ ] Separate RequestDTO và ResponseDTO

### **Mapper:**
- [ ] `@Mapper(componentModel = "spring")`
- [ ] `@Mapping` cho complex conversions
- [ ] Helper methods cho custom logic
- [ ] Handle null-safe operations

### **Exception Handling:**
- [ ] Custom exceptions extend `AppException`
- [ ] `@ExceptionHandler` trong `GlobalExceptionHandler`
- [ ] Trả về `ApiResponse` với error code
- [ ] Log exception với appropriate level
