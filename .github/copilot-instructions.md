# FubaBusBE - Spring Boot Backend Instructions

## Architecture Overview

FubaBusBE is a **Spring Boot 4.0 + Java 21** bus ticket booking system with:
- **PostgreSQL** database (Supabase hosted) with Flyway migrations
- **JWT authentication** (access + refresh tokens) with role-based auth (ADMIN, USER, DRIVER, STAFF)
- **WebSocket** (STOMP/SockJS) for real-time seat locking during booking
- **MapStruct** for Entity ↔ DTO conversion
- Layered architecture: Controller → Service → Repository → Entity

## Project Structure

```
src/main/java/com/example/Fuba_BE/
├── config/          # Security, WebSocket, CORS configs
├── controller/      # REST endpoints (no business logic)
├── service/         # Business logic + interfaces (I-prefixed)
├── repository/      # JpaRepository interfaces
├── domain/
│   ├── entity/      # JPA entities (@Entity)
│   └── enums/       # Status, PaymentType, etc.
├── dto/             # Request/Response DTOs
├── mapper/          # MapStruct mappers + manual mappers
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── payload/         # ApiResponse wrapper
└── security/        # JWT, UserDetails, AuthenticationFilter
```

## Critical Patterns

### 1. Controller Layer
- Use `@RestController` + `@RequestMapping("/resource-path")` (no `/api` prefix)
- Constructor injection: `@RequiredArgsConstructor` + `private final`
- All responses return `ResponseEntity<ApiResponse<T>>`
- GET with pagination: `@RequestParam` defaults to 20 items if not specified
- Validate inputs: `@Valid` for request bodies
- **Never** call repositories directly - only through services

```java
@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {
    private final IBookingService bookingService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<BookingResponse> bookings = bookingService.getBookings(page, size);
        return ResponseEntity.ok(ApiResponse.success("Success", bookings));
    }
}
```

### 2. Service Layer
- Interface naming: `IUserService`, implementation: `UserService`
- Service names use action verbs: `BookingService`, `AuthService`
- `@Service` + `@Transactional` (or `@Transactional(readOnly = true)` for queries)
- Throw custom exceptions: `NotFoundException`, `BadRequestException`, `UnauthorizedException`
- Use `@Slf4j` for logging
- Services return **entities**, not DTOs (conversion happens in controller/mapper)

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BookingService implements IBookingService {
    private final BookingRepository bookingRepository;
    
    @Override
    @Transactional
    public Booking createBooking(BookingRequest request) {
        // Business logic here
        return bookingRepository.save(booking);
    }
}
```

### 3. Repository Layer
- Extend `JpaRepository<Entity, ID>`
- Use `@Query` with `JOIN FETCH` to avoid N+1 queries
- Follow Spring Data naming conventions for query methods
- No business logic - pure data access

```java
@Repository
public interface TripSeatRepository extends JpaRepository<TripSeat, Integer> {
    @Query("SELECT ts FROM TripSeat ts JOIN FETCH ts.seat WHERE ts.trip.tripId = :tripId")
    List<TripSeat> findByTripIdWithSeats(@Param("tripId") Integer tripId);
}
```

### 4. Entity Layer
- `@Entity` + `@Table(name = "table_name")`
- Use `@Builder` + `@Builder.Default` for default values
- Relationships: `fetch = FetchType.LAZY` (always lazy load)
- Audit fields: `@CreationTimestamp`, `@UpdateTimestamp`
- Use `@Column(name = "db_column_name")` to match database schema

```java
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "userid")
    private Integer userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roleid")
    private Role role;
    
    @Builder.Default
    @Column(name = "status")
    private String status = "Active";
}
```

### 5. DTO & Mapper
- DTOs: Separate `RequestDTO` and `ResponseDTO` (flat structure)
- DTOs live in `dto/` package, grouped by feature
- Validation: `@NotNull`, `@Size`, `@Email`, etc.
- Mappers: Use `@Mapper(componentModel = "spring")` for MapStruct, or manual `@Component` mappers
- **Controllers** handle Entity → DTO conversion, **services** work with entities

```java
// DTO
public record BookingRequest(
    @NotNull Integer tripId,
    @NotNull List<Integer> seatIds,
    @NotNull String userId
) {}

// Mapper
@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingResponse toResponse(Booking booking);
}
```

### 6. Exception Handling
- Custom exceptions extend `AppException`
- `GlobalExceptionHandler` with `@RestControllerAdvice`
- Return `ApiResponse` with error code and HTTP status

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NotFoundException ex) {
        log.warn("Not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }
}
```

## Key Features

### WebSocket (Real-time Seat Locking)
- Endpoint: `/ws` (STOMP/SockJS)
- Topics: `/topic/seats/{tripId}` for seat status broadcasts
- Client subscribes → locks seat → broadcasts update → others see locked seats
- `SimpMessagingTemplate` for server-side messaging

### Authentication Flow
- Login: `POST /auth/login` → returns JWT access token (24h) + refresh token (7d)
- Requests: `Authorization: Bearer <token>` header
- Security: Currently **allow-all** in dev (see `SecurityConfig.java`)
- Roles: ADMIN, USER, DRIVER, STAFF (role-based checks ready but disabled)

### Database
- PostgreSQL with Flyway migrations (`src/main/resources/db/migration/`)
- 23 tables: users, roles, trips, tickets, bookings, vehicles, drivers, etc.
- See `DATABASE_DOCUMENTATION.md` for full schema
- Connection: Supabase pooler (port 6543)

## Development Workflow

### Running the App
```bash
# Using Maven wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Build without tests
.\mvnw.cmd -DskipTests package

# Run JAR
java -jar target/Fuba-BE-0.0.1-SNAPSHOT.jar
```

### Build Tasks
- VS Code task: `maven-package-skip-tests` for quick builds
- Logs: Written to `logs/app.log`
- Clear logs: Run `clear-logs` task

### Testing
- Unit tests in `src/test/java/com/example/Fuba_BE/`
- Integration tests use Spring Boot test annotations

## Common Pitfalls

1. **Don't call repositories from controllers** - always use services
2. **Services return entities** - DTOs are controller/mapper responsibility
3. **Use lazy loading** - `fetch = FetchType.LAZY` on all relationships
4. **JOIN FETCH for collections** - prevents N+1 queries
5. **@Transactional placement** - on service methods, not repositories
6. **Pagination defaults** - GET endpoints return 20 items max by default
7. **No /api prefix** - routes are `/bookings`, `/trips`, not `/api/v1/bookings`

## Reference Documentation

- `README.md` - Full architecture guide with examples
- `AUTH_SETUP.md` - JWT authentication details
- `DATABASE_DOCUMENTATION.md` - Complete schema with relationships
- `PATTERN_AUDIT_REPORT.md` - Design patterns used
- `.github/instructions/BE.instructions.md` - Detailed coding checklist
