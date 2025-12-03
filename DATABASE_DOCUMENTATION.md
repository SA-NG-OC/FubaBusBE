# TÀI LIỆU MÔ TẢ CẤU TRÚC DATABASE

## Hệ Thống Quản Lý Bán Vé Xe Khách

**Database:** PostgreSQL  
**Phiên bản:** 12+  
**Mã hóa:** UTF-8  
**Ngày tạo:** December 2, 2025

---

## 📋 TỔNG QUAN

Database được thiết kế cho hệ thống quản lý bán vé xe khách với đầy đủ các chức năng:

- ✅ Quản lý người dùng và phân quyền (4 vai trò)
- ✅ Quản lý xe, tài xế, lộ trình
- ✅ Đặt vé online và tại quầy
- ✅ Thanh toán và hoàn tiền tự động
- ✅ Check-in hành khách (QR Code, Manual)
- ✅ Theo dõi GPS chuyến xe (TripTracking)
- ✅ Quản lý nhật ký công việc tài xế (DriverWorklog)
- ✅ Đánh giá và phản hồi
- ✅ Theo dõi chi phí và báo cáo thống kê
- ✅ Audit logs đầy đủ

**Đặc điểm kỹ thuật:**
- Sử dụng SERIAL cho Primary Key (tự động tăng)
- Trigger tự động cập nhật UpdatedAt
- Computed columns cho tính toán tự động (TotalCost, NetRevenue, Profit, TotalSalary)
- Indexes đầy đủ cho hiệu suất tối ưu (35+ indexes)
- Constraints đảm bảo tính toàn vẹn dữ liệu
- Views để truy vấn dữ liệu phức tạp
- Stored Procedures/Functions cho nghiệp vụ
- Audit Logs theo dõi thay đổi

---

## 📊 SƠ ĐỒ QUAN HỆ BẢNG

```
[Roles] 1---* [Users] *---1 [Drivers] 1---* [DriverWorklog]
                |                |
                +---* [Bookings] |
                        |        |
                        *        |
                    [Tickets]    |
                        |        |
                        1        |
                    [Passengers] |
                                 |
[VehicleTypes] 1---* [Vehicles]  |
                         |       |
[Locations] *---* [Routes] *---* [RouteStops]
                    |
                    +---* [Trips] 1---* [TripSeats]
                              |
                              +---* [Reviews]
                              +---* [TripCosts]
                              +---* [TripTracking]
                              +---* [Refunds]
                              +---* [TicketChanges]
                              
[Payments] *---1 [Bookings]
[AuditLogs] *--- [Users]
```

---

## 📂 CẤU TRÚC DATABASE CHI TIẾT

### 1. NHÓM BẢNG NGƯỜI DÙNG VÀ TÀI KHOẢN

#### 1.1. Bảng `Roles` - Vai trò người dùng

**Mục đích:** Định nghĩa các vai trò trong hệ thống (Admin, Nhân viên, Tài xế, Khách hàng)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **RoleID** | SERIAL | PRIMARY KEY | ID vai trò (tự động tăng) |
| **RoleName** | VARCHAR(50) | NOT NULL, UNIQUE | Tên vai trò |
| Description | TEXT | | Mô tả chi tiết vai trò |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian cập nhật |

**Dữ liệu mẫu:**
```sql
1 - Admin              - Quản trị viên hệ thống
2 - Nhân viên bán vé   - Nhân viên bán vé tại quầy
3 - Tài xế             - Tài xế điều khiển xe
4 - Khách hàng         - Khách hàng sử dụng dịch vụ
```

**Quy tắc nghiệp vụ:**
- Mỗi người dùng chỉ có 1 vai trò duy nhất
- Vai trò không thể xóa nếu còn user đang sử dụng
- Chỉ Admin mới có quyền quản lý vai trò

---

#### 1.2. Bảng `Users` - Người dùng

**Mục đích:** Lưu trữ thông tin tài khoản của tất cả người dùng trong hệ thống

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **UserID** | SERIAL | PRIMARY KEY | ID người dùng (tự động tăng) |
| **FullName** | VARCHAR(100) | NOT NULL | Họ và tên đầy đủ |
| **Email** | VARCHAR(100) | UNIQUE, NOT NULL | Email (định danh tài khoản) |
| **PhoneNumber** | VARCHAR(20) | UNIQUE, NOT NULL | Số điện thoại |
| **Password** | VARCHAR(255) | NOT NULL | Mật khẩu (mã hóa Hash bcrypt/SHA-256) |
| **RoleID** | INTEGER | NOT NULL, FK → Roles | Vai trò của người dùng |
| Status | VARCHAR(20) | DEFAULT 'Hoạt động' | Trạng thái tài khoản |
| EmailVerified | BOOLEAN | DEFAULT FALSE | Email đã xác thực chưa |

**CHECK Constraints:**
```sql
Status IN ('Hoạt động', 'Khóa')
Email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'
PhoneNumber ~ '^[0-9]{10,20}$'
```

**Indexes:**
- `idx_users_email` ON (Email)
- `idx_users_phone` ON (PhoneNumber)
- `idx_users_role_status` ON (RoleID, Status)

**Quy tắc nghiệp vụ:**
- Email và SĐT không được trùng lặp
- Mật khẩu tối thiểu 6 ký tự, có ít nhất 1 chữ số
- Tài khoản mới mặc định là vai trò "Khách hàng"
- Tài khoản phải ở trạng thái "Hoạt động" mới đăng nhập được
- Email đã xác thực không thể thay đổi
- Xóa user sẽ cascade xóa các bản ghi liên quan (Drivers, Bookings)

**Dữ liệu mẫu:**
```sql
Admin mặc định:
- Email: admin@busticket.com
- Phone: 0900000000
- Password: $2a$10$XYZ... (Admin@123 - đã hash)
- RoleID: 1 (Admin)
```

---

### 2. NHÓM BẢNG QUẢN LÝ XE VÀ TÀI XẾ

#### 2.1. Bảng `VehicleTypes` - Loại xe

**Mục đích:** Phân loại các loại xe và định nghĩa số ghế, tầng

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **TypeID** | SERIAL | PRIMARY KEY | ID loại xe |
| **TypeName** | VARCHAR(50) | NOT NULL, UNIQUE | Tên loại xe |
| **TotalSeats** | INTEGER | NOT NULL | Tổng số ghế |
| **NumberOfFloors** | INTEGER | DEFAULT 1, CHECK >= 1 | Số tầng (1 hoặc 2) |
| Description | TEXT | | Mô tả chi tiết |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**Dữ liệu mẫu:**
```sql
Limousine    - 9 ghế  - 1 tầng - Xe limousine cao cấp
Giường nằm   - 40 ghế - 2 tầng - Xe giường nằm 40 chỗ
Ghế ngồi     - 45 ghế - 1 tầng - Xe ghế ngồi 45 chỗ
```

**Quy tắc nghiệp vụ:**
- Loại xe quyết định sơ đồ ghế tự động
- Số ghế và số tầng phải hợp lý (ghế chia đều cho tầng)
- Không xóa loại xe nếu còn xe đang sử dụng

---

#### 2.2. Bảng `Vehicles` - Xe

**Mục đích:** Quản lý thông tin các xe trong đội xe

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **VehicleID** | SERIAL | PRIMARY KEY | ID xe |
| **LicensePlate** | VARCHAR(20) | NOT NULL, UNIQUE | Biển kiểm soát (duy nhất) |
| **TypeID** | INTEGER | NOT NULL, FK → VehicleTypes | Loại xe |
| InsuranceNumber | VARCHAR(50) | | Số bảo hiểm |
| InsuranceExpiry | DATE | | Ngày hết hạn bảo hiểm |
| Status | VARCHAR(20) | DEFAULT 'Hoàn thiện' | Tình trạng xe |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
Status IN ('Hoàn thiện', 'Hư hại', 'Phế liệu')
```

**Triggers:**
- `update_vehicles_updated_at` - Tự động cập nhật UpdatedAt khi UPDATE

**Indexes:**
- `idx_vehicles_status` ON (Status)

**Quy tắc nghiệp vụ:**
- Biển kiểm soát là duy nhất (không trùng)
- Xe có 3 trạng thái: Hoàn thiện, Hư hại, Phế liệu
- Chỉ xe "Hoàn thiện" mới được xếp lịch chạy
- Loại xe quy định số ghế và sơ đồ ghế
- Cần cảnh báo khi bảo hiểm sắp hết hạn (< 30 ngày)

---

#### 2.3. Bảng `Drivers` - Tài xế

**Mục đích:** Quản lý thông tin tài xế và bằng lái

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **DriverID** | SERIAL | PRIMARY KEY | ID tài xế |
| **UserID** | INTEGER | NOT NULL, UNIQUE, FK → Users | Liên kết với tài khoản Users |
| **DriverLicense** | VARCHAR(50) | NOT NULL, UNIQUE | Số bằng lái (duy nhất) |
| **LicenseExpiry** | DATE | NOT NULL | Ngày hết hạn bằng lái |
| **DateOfBirth** | DATE | NOT NULL | Ngày sinh |
| Salary | DECIMAL(15,2) | DEFAULT 0 | Lương cơ bản |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**Triggers:**
- `update_drivers_updated_at` - Tự động cập nhật UpdatedAt

**Quy tắc nghiệp vụ:**
- Một user chỉ có thể là 1 tài xế (UNIQUE UserID)
- Số bằng lái phải là duy nhất
- Lịch chạy là danh sách chuyến xe tài xế được phân công
- Tài xế làm việc tối đa 10 giờ/ngày, tối đa 4 tiếng liên tục
- Tài xế không được trùng lịch (cùng thời điểm không được chạy 2 chuyến)
- Cần cảnh báo khi bằng lái sắp hết hạn (< 30 ngày)
- Xóa User sẽ cascade xóa Driver (ON DELETE CASCADE)

---

### 3. NHÓM BẢNG QUẢN LÝ TUYẾN ĐƯỜNG

#### 3.1. Bảng `Locations` - Địa điểm

**Mục đích:** Lưu trữ thông tin các địa điểm/bến xe/điểm dừng

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **LocationID** | SERIAL | PRIMARY KEY | ID địa điểm |
| **LocationName** | VARCHAR(100) | NOT NULL | Tên địa điểm |
| **Province** | VARCHAR(100) | NOT NULL | Tỉnh/Thành phố |
| Address | TEXT | | Địa chỉ chi tiết |
| Latitude | DECIMAL(10, 8) | | Vĩ độ (GPS) |
| Longitude | DECIMAL(11, 8) | | Kinh độ (GPS) |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**Quy tắc nghiệp vụ:**
- Hỗ trợ lưu tọa độ GPS cho tích hợp bản đồ
- Một địa điểm có thể là điểm đi/đến/dừng chân
- Không xóa địa điểm nếu đang được sử dụng trong Routes hoặc RouteStops

---

#### 3.2. Bảng `Routes` - Lộ trình

**Mục đích:** Định nghĩa các tuyến đường từ điểm A đến điểm B

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **RouteID** | SERIAL | PRIMARY KEY | ID lộ trình |
| **RouteName** | VARCHAR(200) | NOT NULL | Tên lộ trình (VD: Hà Nội - Hải Phòng) |
| **OriginID** | INTEGER | NOT NULL, FK → Locations | Điểm đi |
| **DestinationID** | INTEGER | NOT NULL, FK → Locations | Điểm đến |
| **Distance** | DECIMAL(10,2) | NOT NULL, CHECK > 0 | Khoảng cách (km) |
| **EstimatedDuration** | INTEGER | NOT NULL | Thời gian dự kiến (phút) |
| Status | VARCHAR(20) | DEFAULT 'Hoạt động' | Trạng thái lộ trình |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
Status IN ('Hoạt động', 'Bảo trì', 'Dừng')
Distance > 0
OriginID != DestinationID  -- Điểm đi và đến phải khác nhau
```

**Triggers:**
- `update_routes_updated_at` - Tự động cập nhật UpdatedAt

**Indexes:**
- `idx_routes_locations` ON (OriginID, DestinationID)
- `idx_routes_status` ON (Status)

**Quy tắc nghiệp vụ:**
- Chỉ Admin được tạo, sửa, xóa lộ trình
- Lộ trình có 3 trạng thái: Hoạt động, Bảo trì, Dừng
- Chỉ lộ trình "Hoạt động" mới được dùng để lập lịch chuyến
- Điểm đi và điểm đến không được trùng nhau
- Khoảng cách và thời gian phải hợp lý

---

#### 3.3. Bảng `RouteStops` - Điểm dừng trên lộ trình

**Mục đích:** Định nghĩa các điểm dừng chân giữa hành trình

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **StopID** | SERIAL | PRIMARY KEY | ID điểm dừng |
| **RouteID** | INTEGER | NOT NULL, FK → Routes | Lộ trình |
| **LocationID** | INTEGER | NOT NULL, FK → Locations | Địa điểm |
| **StopOrder** | INTEGER | NOT NULL | Thứ tự dừng (1, 2, 3...) |
| **StopType** | VARCHAR(30) | NOT NULL | Loại điểm dừng |
| StopName | VARCHAR(200) | | Tên điểm dừng cụ thể |
| IsPickupPoint | BOOLEAN | DEFAULT TRUE | Cho phép đón khách |
| IsDropoffPoint | BOOLEAN | DEFAULT TRUE | Cho phép trả khách |
| StopAddress | TEXT | | Địa chỉ chi tiết |
| Latitude | DECIMAL(10, 8) | | Vĩ độ GPS |
| Longitude | DECIMAL(11, 8) | | Kinh độ GPS |
| DistanceFromOrigin | DECIMAL(10,2) | | Khoảng cách từ điểm xuất phát (km) |
| EstimatedTime | INTEGER | | Thời gian dự kiến đến điểm này (phút) |
| StopNote | TEXT | | Ghi chú |

**CHECK Constraints:**
```sql
StopType IN ('Điểm khởi hành', 'Điểm dừng chân', 'Điểm đến')
```

**UNIQUE Constraints:**
```sql
UNIQUE (RouteID, StopOrder)  -- Không trùng thứ tự trong cùng tuyến
```

**Indexes:**
- `idx_routestops_routeid` ON (RouteID)
- `idx_routestops_locationid` ON (LocationID)

**Quy tắc nghiệp vụ:**
- Danh sách điểm phải theo đúng thứ tự thực tế
- StopOrder phải liên tục (1, 2, 3...)
- Điểm khởi hành (StopOrder = 1) phải trùng với OriginID của Route
- Điểm đến (StopOrder cuối) phải trùng với DestinationID của Route
- Hành khách có thể chọn lên/xuống tại các điểm được phép
- Xóa Route sẽ cascade xóa tất cả RouteStops (ON DELETE CASCADE)

---

### 4. NHÓM BẢNG QUẢN LÝ CHUYẾN XE

#### 4.1. Bảng `Trips` - Chuyến xe

**Mục đích:** Quản lý lịch trình các chuyến xe cụ thể

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **TripID** | SERIAL | PRIMARY KEY | ID chuyến xe |
| **RouteID** | INTEGER | NOT NULL, FK → Routes | Lộ trình |
| **VehicleID** | INTEGER | NOT NULL, FK → Vehicles | Xe chạy chuyến này |
| **DriverID** | INTEGER | NOT NULL, FK → Drivers | Tài xế phụ trách |
| **DepartureTime** | TIMESTAMP | NOT NULL | Giờ khởi hành |
| **ArrivalTime** | TIMESTAMP | NOT NULL | Giờ đến dự kiến |
| **BasePrice** | DECIMAL(15,2) | NOT NULL | Giá vé cơ bản |
| Status | VARCHAR(20) | DEFAULT 'Chờ' | Trạng thái chuyến xe |
| StatusNote | TEXT | | Ghi chú trạng thái |
| OnlineBookingCutoff | INTEGER | DEFAULT 60, CHECK > 0 | Thời gian ngưng đặt online (phút) |
| IsFullyBooked | BOOLEAN | DEFAULT FALSE | Đã hết chỗ? |
| MinPassengers | INTEGER | DEFAULT 1 | Số hành khách tối thiểu để chạy |
| AutoCancelIfNotEnough | BOOLEAN | DEFAULT FALSE | Tự động hủy nếu không đủ khách |
| CreatedBy | INTEGER | FK → Users | Admin tạo chuyến |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
Status IN ('Chờ', 'Đang chạy', 'Hoàn thành', 'Hủy', 'Trễ')
DepartureTime < ArrivalTime  -- Giờ đi phải trước giờ đến
OnlineBookingCutoff > 0
```

**Triggers:**
- `update_trips_updated_at` - Tự động cập nhật UpdatedAt

**Indexes:**
- `idx_trips_search` ON (RouteID, DepartureTime, Status)
- `idx_trips_departure` ON (DepartureTime, Status)
- `idx_trips_status` ON (Status)

**Quy tắc nghiệp vụ:**
- Chỉ Admin được tạo, sửa, xóa chuyến xe
- Xe và tài xế không được trùng lịch (cùng thời gian)
- Trạng thái hợp lệ: Chờ, Đang chạy, Hoàn thành, Hủy, Trễ
- Mọi thay đổi trạng thái phải ghi log
- OnlineBookingCutoff: thời gian trước giờ đi mà không cho đặt online nữa
- IsFullyBooked tự động cập nhật khi tất cả ghế đã đặt
- Nếu MinPassengers không đủ và AutoCancelIfNotEnough = TRUE → tự hủy chuyến

---

#### 4.2. Bảng `TripSeats` - Sơ đồ ghế chuyến xe

**Mục đích:** Quản lý sơ đồ ghế cụ thể cho từng chuyến xe

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **SeatID** | SERIAL | PRIMARY KEY | ID ghế |
| **TripID** | INTEGER | NOT NULL, FK → Trips | Chuyến xe |
| **SeatNumber** | VARCHAR(10) | NOT NULL | Số ghế (A01, A02, B01...) |
| FloorNumber | INTEGER | DEFAULT 1, CHECK >= 1 | Số tầng (1 hoặc 2) |
| SeatType | VARCHAR(20) | DEFAULT 'Thường' | Loại ghế |
| Status | VARCHAR(20) | DEFAULT 'Trống' | Trạng thái ghế |
| HoldExpiry | TIMESTAMP | | Thời gian hết hạn giữ chỗ |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**CHECK Constraints:**
```sql
SeatType IN ('Thường', 'VIP', 'Giường')
Status IN ('Trống', 'Đang giữ', 'Đã đặt')
FloorNumber >= 1
```

**UNIQUE Constraints:**
```sql
UNIQUE (TripID, SeatNumber)  -- Không trùng số ghế trong cùng chuyến
```

**Indexes:**
- `idx_tripseats_status` ON (TripID, Status)
- `idx_tripseats_tripid` ON (TripID)

**Quy tắc nghiệp vụ:**
- Danh sách ghế thuộc về xe của chuyến đó (dựa vào VehicleType)
- Chỉ cho phép chọn ghế "Trống"
- Ghế được giữ trong 10 phút (HoldExpiry = Current + 10 phút)
- Hết thời gian chưa thanh toán → tự động giải phóng (Status = 'Trống')
- Xóa Trip sẽ cascade xóa tất cả TripSeats (ON DELETE CASCADE)
- Số ghế được tạo tự động bởi function `sp_GenerateSeatsForTrip()`

**Format số ghế mới (sau khi sửa):**
- Tầng 1: A01, A02, A03... (chữ A + số 2 chữ số)
- Tầng 2: B01, B02, B03... (chữ B + số 2 chữ số)

---

### 5. NHÓM BẢNG ĐẶT VÉ VÀ THANH TOÁN

#### 5.1. Bảng `Bookings` - Đặt vé

**Mục đích:** Quản lý các đơn đặt vé (có thể 1 hoặc nhiều vé)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **BookingID** | SERIAL | PRIMARY KEY | ID đặt vé |
| **BookingCode** | VARCHAR(20) | UNIQUE, NOT NULL | Mã đặt vé (tự động BK20231201XXXXX) |
| CustomerID | INTEGER | FK → Users | ID khách hàng (NULL nếu vãng lai) |
| **CustomerName** | VARCHAR(100) | NOT NULL | Tên khách hàng |
| **CustomerPhone** | VARCHAR(20) | NOT NULL | SĐT khách hàng |
| CustomerEmail | VARCHAR(100) | | Email khách hàng |
| **TripID** | INTEGER | NOT NULL, FK → Trips | Chuyến xe |
| **TotalAmount** | DECIMAL(15,2) | NOT NULL, CHECK > 0 | Tổng tiền |
| BookingStatus | VARCHAR(30) | DEFAULT 'Đang giữ' | Trạng thái đơn |
| BookingType | VARCHAR(20) | DEFAULT 'Online' | Loại đặt |
| IsGuestBooking | BOOLEAN | DEFAULT FALSE | Đặt vãng lai? |
| GuestSessionID | VARCHAR(100) | | Session ID cho khách vãng lai |
| InvitationSentAt | TIMESTAMP | | Thời gian gửi lời mời tạo tài khoản |
| CreatedBy | INTEGER | FK → Users | Nhân viên tạo (nếu tại quầy) |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
BookingStatus IN ('Đang giữ', 'Đã thanh toán', 'Đã hủy', 'Đã hoàn thành')
BookingType IN ('Online', 'Tại quầy')
TotalAmount > 0
```

**Triggers:**
- `update_bookings_updated_at` - Tự động cập nhật UpdatedAt
- `trg_generate_booking_code` - Tự động tạo BookingCode khi INSERT

**Indexes:**
- `idx_bookings_customer` ON (CustomerID, BookingStatus)
- `idx_bookings_guest` ON (IsGuestBooking, CreatedAt)
- `idx_bookings_code` ON (BookingCode)
- `idx_bookings_created` ON (CreatedAt)
- `idx_bookings_trip_status` ON (TripID, BookingStatus)

**Quy tắc nghiệp vụ:**
- Một booking có thể chứa nhiều vé (nhiều ghế)
- CustomerID có thể NULL nếu là khách vãng lai (IsGuestBooking = TRUE)
- BookingCode tự động tạo format: BK + YYYYMMDD + 5 số random
- BookingStatus flow: Đang giữ → Đã thanh toán → Đã hoàn thành (hoặc Đã hủy)
- Nếu IsGuestBooking = TRUE, có thể gửi email mời tạo tài khoản (InvitationSentAt)
- Xóa User sẽ SET NULL cho CustomerID/CreatedBy (ON DELETE SET NULL)

---

#### 5.2. Bảng `Tickets` - Vé

**Mục đích:** Quản lý từng vé riêng lẻ (1 vé = 1 ghế)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **TicketID** | SERIAL | PRIMARY KEY | ID vé |
| **TicketCode** | VARCHAR(20) | UNIQUE, NOT NULL | Mã vé (tự động TK20231201XXXXX) |
| **BookingID** | INTEGER | NOT NULL, FK → Bookings | Đơn đặt vé |
| **SeatID** | INTEGER | NOT NULL, FK → TripSeats | Ghế |
| **Price** | DECIMAL(15,2) | NOT NULL, CHECK > 0 | Giá vé |
| TicketStatus | VARCHAR(30) | DEFAULT 'Chưa xác nhận' | Trạng thái vé |
| RequiresPassengerInfo | BOOLEAN | DEFAULT TRUE | Yêu cầu thông tin hành khách |
| PrintedBy | INTEGER | FK → Users | Nhân viên in vé |
| PrintedAt | TIMESTAMP | | Thời gian in vé |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**CHECK Constraints:**
```sql
TicketStatus IN ('Chưa xác nhận', 'Đã xác nhận', 'Đã sử dụng', 'Đã hủy', 'Hoàn tiền')
Price > 0
```

**Triggers:**
- `trg_generate_ticket_code` - Tự động tạo TicketCode khi INSERT
- `trg_update_seat_status` - Tự động cập nhật TripSeats.Status = 'Đã đặt'
- `trg_create_passenger_on_ticket` - Tự động tạo Passenger khi tạo Ticket

**Indexes:**
- `idx_tickets_booking` ON (BookingID, TicketStatus)
- `idx_tickets_code` ON (TicketCode)
- `idx_tickets_status` ON (TicketStatus)
- `idx_tickets_seatid` ON (SeatID)

**Quy tắc nghiệp vụ:**
- 1 Ticket = 1 Seat (quan hệ 1-1)
- TicketCode tự động tạo format: TK + YYYYMMDD + 5 số random
- Vé chỉ được in khi đã thanh toán thành công (BookingStatus = 'Đã thanh toán')
- Sau khi in, cập nhật TicketStatus = 'Đã xác nhận' và lưu PrintedBy
- Khi tạo Ticket, tự động tạo Passenger tương ứng
- Xóa Booking sẽ cascade xóa tất cả Tickets (ON DELETE CASCADE)

---

#### 5.3. Bảng `Passengers` - Hành khách

**Mục đích:** Lưu thông tin chi tiết hành khách và check-in

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **PassengerID** | SERIAL | PRIMARY KEY | ID hành khách |
| **TicketID** | INTEGER | NOT NULL, UNIQUE, FK → Tickets | Vé (1-1 relationship) |
| FullName | VARCHAR(100) | NOT NULL, DEFAULT 'Khách' | Họ tên hành khách |
| PhoneNumber | VARCHAR(20) | | Số điện thoại |
| Email | VARCHAR(100) | | Email |
| DateOfBirth | DATE | | Ngày sinh |
| PickupLocationID | INTEGER | FK → RouteStops | Điểm lên xe |
| PickupAddress | VARCHAR(200) | | Địa chỉ đón cụ thể |
| DropoffLocationID | INTEGER | FK → RouteStops | Điểm xuống xe |
| DropoffAddress | VARCHAR(200) | | Địa chỉ trả cụ thể |
| SpecialNote | TEXT | | Ghi chú đặc biệt |
| CheckInStatus | VARCHAR(20) | DEFAULT 'Chưa lên xe' | Trạng thái check-in |
| CheckInTime | TIMESTAMP | | Thời gian check-in |
| CheckInMethod | VARCHAR(30) | NOT NULL, DEFAULT 'QR' | Phương thức check-in |
| CheckOutTime | TIMESTAMP | | Thời gian check-out |
| CheckedInBy | INTEGER | FK → Users | Nhân viên check-in |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
CheckInStatus IN ('Chưa lên xe', 'Đã lên xe', 'Đã xuống xe')
```

**Triggers:**
- `update_passengers_updated_at` - Tự động cập nhật UpdatedAt

**Indexes:**
- `idx_passengers_checkin` ON (CheckInStatus, CheckInTime)
- `idx_passengers_ticketid` ON (TicketID)

**Quy tắc nghiệp vụ:**
- 1 Passenger = 1 Ticket (quan hệ 1-1, UNIQUE TicketID)
- Tự động tạo khi tạo Ticket (trigger `trg_create_passenger_on_ticket`)
- Thông tin mặc định: FullName từ Booking.CustomerName
- CheckInMethod: QR Code, Manual, RFID
- PickupLocationID/DropoffLocationID phải thuộc RouteStops của tuyến
- Check-in flow: Chưa lên xe → Đã lên xe → Đã xuống xe
- Xóa Ticket sẽ cascade xóa Passenger (ON DELETE CASCADE)

---

#### 5.4. Bảng `Payments` - Thanh toán

**Mục đích:** Ghi nhận các giao dịch thanh toán

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **PaymentID** | SERIAL | PRIMARY KEY | ID thanh toán |
| **BookingID** | INTEGER | NOT NULL, FK → Bookings | Đơn đặt vé |
| **Amount** | DECIMAL(15,2) | NOT NULL, CHECK > 0 | Số tiền |
| **PaymentMethod** | VARCHAR(30) | NOT NULL | Phương thức thanh toán |
| PaymentStatus | VARCHAR(30) | DEFAULT 'Chờ xử lý' | Trạng thái thanh toán |
| TransactionID | VARCHAR(100) | | Mã giao dịch từ cổng thanh toán |
| PaymentGateway | VARCHAR(50) | | Tên cổng thanh toán |
| PaymentNote | TEXT | | Ghi chú |
| PaidAt | TIMESTAMP | | Thời gian thanh toán thành công |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**CHECK Constraints:**
```sql
PaymentMethod IN ('Tiền mặt', 'Chuyển khoản', 'Thẻ tín dụng', 'Ví điện tử')
PaymentStatus IN ('Chờ xử lý', 'Thành công', 'Thất bại', 'Đã hoàn tiền')
Amount > 0
```

**Indexes:**
- `idx_payments_booking` ON (BookingID, PaymentStatus)
- `idx_payments_status_created` ON (PaymentStatus, CreatedAt)
- `idx_payments_method` ON (PaymentMethod, PaymentStatus)

**Quy tắc nghiệp vụ:**
- Một Booking có thể có nhiều Payment (ví dụ: thanh toán 1 phần, hoàn tiền)
- Chỉ xác nhận vé thành công khi nhận mã thành công từ cổng thanh toán
- TransactionID từ payment gateway (VNPay, MoMo, ZaloPay...)
- Khi PaymentStatus = 'Thành công', cập nhật BookingStatus = 'Đã thanh toán'
- Xóa Booking sẽ cascade xóa Payment (ON DELETE CASCADE)

---

#### 5.5. Bảng `Refunds` - Hoàn tiền

**Mục đích:** Quản lý yêu cầu hoàn tiền khi hủy vé

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **RefundID** | SERIAL | PRIMARY KEY | ID hoàn tiền |
| **BookingID** | INTEGER | NOT NULL, FK → Bookings | Đơn đặt vé |
| **RefundAmount** | DECIMAL(15,2) | NOT NULL, CHECK > 0 | Số tiền hoàn |
| RefundReason | TEXT | | Lý do hoàn tiền |
| RefundType | VARCHAR(30) | DEFAULT 'Hủy toàn bộ' | Loại hoàn tiền |
| AffectedTicketIDs | TEXT | | Danh sách TicketID bị ảnh hưởng |
| NewTripID | INTEGER | FK → Trips | Chuyến mới (nếu đổi chuyến) |
| PriceDifference | DECIMAL(15,2) | DEFAULT 0 | Chênh lệch giá (nếu đổi) |
| RefundStatus | VARCHAR(30) | DEFAULT 'Đang xử lý' | Trạng thái hoàn tiền |
| RefundMethod | VARCHAR(30) | NOT NULL | Phương thức hoàn |
| BankAccount | VARCHAR(100) | | Tài khoản ngân hàng nhận hoàn tiền |
| ProcessedBy | INTEGER | FK → Users | Nhân viên xử lý |
| ProcessedAt | TIMESTAMP | | Thời gian xử lý |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**CHECK Constraints:**
```sql
RefundType IN ('Hủy toàn bộ', 'Hủy 1 vé', 'Đổi chuyến')
RefundStatus IN ('Đang xử lý', 'Đã hoàn tiền', 'Từ chối')
RefundMethod IN ('Chuyển khoản', 'Tiền mặt')
RefundAmount > 0
```

**Indexes:**
- `idx_refunds_booking` ON (BookingID, RefundStatus)
- `idx_refunds_status` ON (RefundStatus)

**Quy tắc nghiệp vụ:**
- Vé chỉ được hủy trước giờ khởi hành tối thiểu 2 giờ
- Hoàn 90% nếu hủy trước 4 giờ, 50% nếu 2-4 giờ
- RefundType: Hủy toàn bộ, Hủy 1 vé (trong booking nhiều vé), Đổi chuyến
- Nếu đổi chuyến (NewTripID != NULL), tính PriceDifference
- Xóa Booking sẽ cascade xóa Refund (ON DELETE CASCADE)

---

#### 5.6. Bảng `TicketChanges` - Lịch sử đổi vé

**Mục đích:** Ghi nhận lịch sử thay đổi vé (đổi chuyến, đổi ghế)

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **ChangeID** | SERIAL | PRIMARY KEY | ID thay đổi |
| **TicketID** | INTEGER | NOT NULL, FK → Tickets | Vé |
| **OldTripID** | INTEGER | NOT NULL, FK → Trips | Chuyến cũ |
| **NewTripID** | INTEGER | NOT NULL, FK → Trips | Chuyến mới |
| **OldSeatID** | INTEGER | NOT NULL | Ghế cũ |
| **NewSeatID** | INTEGER | NOT NULL | Ghế mới |
| OldPrice | DECIMAL(15,2) | | Giá cũ |
| NewPrice | DECIMAL(15,2) | | Giá mới |
| PriceDifference | DECIMAL(15,2) | | Chênh lệch giá |
| ChangeReason | TEXT | | Lý do đổi |
| ChangeFee | DECIMAL(15,2) | DEFAULT 0 | Phí đổi vé |
| ChangedBy | INTEGER | FK → Users | Người yêu cầu đổi |
| ApprovedBy | INTEGER | FK → Users | Người duyệt |
| ChangeStatus | VARCHAR(30) | DEFAULT 'Chờ xử lý' | Trạng thái |
| ChangeDate | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian đổi |

**CHECK Constraints:**
```sql
ChangeStatus IN ('Chờ xử lý', 'Đã duyệt', 'Từ chối')
```

**Quy tắc nghiệp vụ:**
- Ghi nhận mọi thay đổi về chuyến xe hoặc ghế ngồi
- Phải được duyệt bởi Admin/Nhân viên (ApprovedBy)
- Có thể tính phí đổi vé (ChangeFee)
- Nếu giá mới > giá cũ → khách phải trả thêm (PriceDifference > 0)
- Nếu giá mới < giá cũ → hoàn tiền cho khách (PriceDifference < 0)
- Xóa Ticket sẽ cascade xóa TicketChanges (ON DELETE CASCADE)

---

### 6. NHÓM BẢNG ĐÁNH GIÁ VÀ PHẢN HỒI

#### 6.1. Bảng `Reviews` - Đánh giá

**Mục đích:** Quản lý đánh giá của khách hàng sau chuyến đi

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **ReviewID** | SERIAL | PRIMARY KEY | ID đánh giá |
| **TripID** | INTEGER | NOT NULL, FK → Trips | Chuyến xe |
| **CustomerID** | INTEGER | NOT NULL, FK → Users | Khách hàng |
| **TicketID** | INTEGER | NOT NULL, FK → Tickets | Vé |
| **Rating** | INTEGER | NOT NULL, CHECK 1-5 | Số sao tổng thể (1-5) |
| Comment | TEXT | | Bình luận |
| DriverRating | INTEGER | CHECK 1-5 | Đánh giá tài xế (1-5) |
| VehicleRating | INTEGER | CHECK 1-5 | Đánh giá xe (1-5) |
| ServiceRating | INTEGER | CHECK 1-5 | Đánh giá dịch vụ (1-5) |
| PunctualityRating | INTEGER | CHECK 1-5 | Đánh giá đúng giờ (1-5) |
| Feedback | TEXT | | Phản hồi chi tiết |
| ReviewStatus | VARCHAR(30) | DEFAULT 'Đã duyệt' | Trạng thái đánh giá |
| AdminResponse | TEXT | | Phản hồi từ Admin |
| RespondedBy | INTEGER | FK → Users | Admin phản hồi |
| RespondedAt | TIMESTAMP | | Thời gian phản hồi |
| ReviewDate | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian đánh giá |

**CHECK Constraints:**
```sql
Rating BETWEEN 1 AND 5
DriverRating BETWEEN 1 AND 5
VehicleRating BETWEEN 1 AND 5
ServiceRating BETWEEN 1 AND 5
PunctualityRating BETWEEN 1 AND 5
ReviewStatus IN ('Chờ duyệt', 'Đã duyệt', 'Bị ẩn')
```

**UNIQUE Constraints:**
```sql
UNIQUE (TicketID, CustomerID)  -- Mỗi vé chỉ đánh giá 1 lần
```

**Indexes:**
- `idx_reviews_trip` ON (TripID, Rating)

**Quy tắc nghiệp vụ:**
- Mỗi khách hàng chỉ đánh giá 1 lần cho mỗi vé
- Chỉ đánh giá được chuyến đã hoàn thành (Trip.Status = 'Hoàn thành')
- Rating tổng thể (1-5 sao) là bắt buộc
- Các rating chi tiết (Driver, Vehicle, Service, Punctuality) là tùy chọn
- Admin có thể phản hồi đánh giá (AdminResponse)
- ReviewStatus: Chờ duyệt (kiểm duyệt), Đã duyệt (hiển thị), Bị ẩn (vi phạm)
- Xóa Trip/Customer/Ticket sẽ cascade xóa Review (ON DELETE CASCADE)

---

### 7. NHÓM BẢNG QUẢN LÝ CHI PHÍ VÀ BÁO CÁO

#### 7.1. Bảng `TripCosts` - Chi phí chuyến xe

**Mục đích:** Theo dõi chi phí và lợi nhuận của từng chuyến xe

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **CostID** | SERIAL | PRIMARY KEY | ID chi phí |
| **TripID** | INTEGER | NOT NULL, UNIQUE, FK → Trips | Chuyến xe (1-1) |
| FuelCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí xăng dầu |
| TollFeeCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí phí đường |
| DriverSalary | DECIMAL(15,2) | DEFAULT 0 | Lương tài xế |
| MaintenanceCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí bảo trì |
| InsuranceCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí bảo hiểm |
| ParkingCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí đỗ xe |
| ServiceCost | DECIMAL(15,2) | DEFAULT 0 | Chi phí dịch vụ |
| OtherCosts | DECIMAL(15,2) | DEFAULT 0 | Chi phí khác |
| Revenue | DECIMAL(15,2) | DEFAULT 0 | Doanh thu |
| CancelledRevenue | DECIMAL(15,2) | DEFAULT 0 | Doanh thu bị hủy |
| ProfitMargin | DECIMAL(5,2) | | Tỷ suất lợi nhuận (%) |
| **TotalCost** | DECIMAL(15,2) | GENERATED COLUMN | Tổng chi phí (computed) |
| **NetRevenue** | DECIMAL(15,2) | GENERATED COLUMN | Doanh thu thuần (computed) |
| **Profit** | DECIMAL(15,2) | GENERATED COLUMN | Lợi nhuận (computed) |
| CostNote | TEXT | | Ghi chú |
| CalculatedBy | INTEGER | FK → Users | Người tính toán |
| CalculatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tính |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**Computed Columns (GENERATED):**
```sql
TotalCost = FuelCost + TollFeeCost + DriverSalary + MaintenanceCost + 
            InsuranceCost + ParkingCost + ServiceCost + OtherCosts

NetRevenue = Revenue - CancelledRevenue

Profit = NetRevenue - TotalCost
```

**Triggers:**
- `update_tripcosts_updated_at` - Tự động cập nhật UpdatedAt

**Indexes:**
- `idx_trip_costs_profit` ON (Profit, CalculatedAt)

**Quy tắc nghiệp vụ:**
- Mỗi chuyến xe có 1 bản ghi TripCosts (UNIQUE TripID)
- TotalCost, NetRevenue, Profit tự động tính (GENERATED ALWAYS AS)
- ProfitMargin = (Profit / NetRevenue) * 100
- Revenue tính từ tổng Payments thành công
- CancelledRevenue tính từ tổng Refunds
- Xóa Trip sẽ cascade xóa TripCosts (ON DELETE CASCADE)

---

#### 7.2. Bảng `TripTracking` - Theo dõi GPS chuyến xe

**Mục đích:** Theo dõi vị trí GPS thời gian thực của xe đang chạy

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **TrackingID** | SERIAL | PRIMARY KEY | ID tracking |
| **TripID** | INTEGER | NOT NULL, FK → Trips | Chuyến xe |
| CurrentLatitude | DECIMAL(10, 8) | | Vĩ độ hiện tại |
| CurrentLongitude | DECIMAL(11, 8) | | Kinh độ hiện tại |
| CurrentAddress | VARCHAR(255) | | Địa chỉ hiện tại |
| Speed | DECIMAL(5,2) | | Tốc độ (km/h) |
| Direction | VARCHAR(20) | | Hướng di chuyển |
| EstimatedArrival | TIMESTAMP | | Giờ đến dự kiến (cập nhật) |
| DelayMinutes | INTEGER | DEFAULT 0 | Số phút trễ |
| DelayReason | TEXT | | Lý do trễ |
| TrafficStatus | VARCHAR(30) | DEFAULT 'Bình thường' | Tình trạng giao thông |
| RecordedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian ghi nhận |
| RecordedBy | INTEGER | FK → Users | Người/thiết bị ghi nhận |
| DeviceInfo | VARCHAR(100) | | Thông tin thiết bị GPS |

**CHECK Constraints:**
```sql
TrafficStatus IN ('Bình thường', 'Kẹt xe nhẹ', 'Kẹt xe nặng', 'Tai nạn', 'Sửa đường', 'Khác')
```

**Indexes:**
- `idx_trip_tracking` ON (TripID, RecordedAt)

**Quy tắc nghiệp vụ:**
- Ghi nhận vị trí GPS theo thời gian thực (mỗi 1-5 phút)
- Tính toán DelayMinutes dựa trên EstimatedArrival vs thời gian thực
- TrafficStatus cập nhật theo tình trạng giao thông
- Dùng để hiển thị vị trí xe trên bản đồ cho khách hàng
- Tự động xóa tracking cũ sau khi chuyến hoàn thành (có thể dùng job)
- Xóa Trip sẽ cascade xóa TripTracking (ON DELETE CASCADE)

---

#### 7.3. Bảng `DriverWorklog` - Nhật ký công việc tài xế

**Mục đích:** Theo dõi giờ làm việc, lương, vi phạm của tài xế

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **LogID** | SERIAL | PRIMARY KEY | ID nhật ký |
| **DriverID** | INTEGER | NOT NULL, FK → Drivers | Tài xế |
| TripID | INTEGER | FK → Trips | Chuyến xe (có thể NULL) |
| **WorkDate** | DATE | NOT NULL | Ngày làm việc |
| **StartTime** | TIMESTAMP | NOT NULL | Giờ bắt đầu ca |
| EndTime | TIMESTAMP | | Giờ kết thúc ca |
| TotalHours | DECIMAL(4,2) | | Tổng giờ làm việc |
| BreakTime | DECIMAL(4,2) | DEFAULT 0 | Thời gian nghỉ (giờ) |
| TripCount | INTEGER | DEFAULT 0 | Số chuyến chạy trong ngày |
| TotalDistance | DECIMAL(10,2) | DEFAULT 0 | Tổng km chạy trong ngày |
| SalaryType | VARCHAR(30) | DEFAULT 'Theo chuyến' | Loại hình lương |
| SalaryAmount | DECIMAL(15,2) | | Tiền lương cơ bản |
| BonusAmount | DECIMAL(15,2) | DEFAULT 0 | Tiền thưởng |
| PenaltyAmount | DECIMAL(15,2) | DEFAULT 0 | Tiền phạt |
| **TotalSalary** | DECIMAL(15,2) | GENERATED COLUMN | Tổng lương (computed) |
| Status | VARCHAR(30) | DEFAULT 'Đang làm việc' | Trạng thái ca làm |
| HasViolation | BOOLEAN | DEFAULT FALSE | Có vi phạm không? |
| ViolationType | VARCHAR(50) | | Loại vi phạm |
| ViolationNote | TEXT | | Ghi chú vi phạm |
| PerformanceRating | INTEGER | CHECK 1-5 | Đánh giá hiệu suất (1-5) |
| PerformanceNote | TEXT | | Ghi chú đánh giá |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |
| UpdatedAt | TIMESTAMP | AUTO UPDATE | Thời gian cập nhật |

**CHECK Constraints:**
```sql
SalaryType IN ('Theo giờ', 'Theo chuyến', 'Cố định tháng')
Status IN ('Đang làm việc', 'Hoàn thành', 'Nghỉ giữa ca', 'Vắng mặt')
ViolationType IN ('Vượt 10h/ngày', 'Vượt 4h liên tục', 'Không nghỉ đủ', 'Khác')
PerformanceRating BETWEEN 1 AND 5
```

**Computed Column (GENERATED):**
```sql
TotalSalary = COALESCE(SalaryAmount, 0) + COALESCE(BonusAmount, 0) - COALESCE(PenaltyAmount, 0)
```

**Triggers:**
- `update_driverworklog_updated_at` - Tự động cập nhật UpdatedAt

**Indexes:**
- `idx_driver_date` ON (DriverID, WorkDate)
- `idx_driver_status` ON (DriverID, Status)

**Quy tắc nghiệp vụ:**
- Ghi nhận giờ làm việc của tài xế theo từng ca/ngày
- Tự động kiểm tra vi phạm:
  - Vượt 10h/ngày → HasViolation = TRUE, ViolationType = 'Vượt 10h/ngày'
  - Chạy liên tục quá 4h không nghỉ → ViolationType = 'Vượt 4h liên tục'
- TotalHours = (EndTime - StartTime) - BreakTime
- SalaryAmount tính theo SalaryType:
  - Theo giờ: TotalHours × đơn giá
  - Theo chuyến: TripCount × đơn giá
  - Cố định tháng: Lương cố định
- TotalSalary tự động tính (GENERATED): SalaryAmount + BonusAmount - PenaltyAmount
- Xóa Driver sẽ cascade xóa DriverWorklog (ON DELETE CASCADE)
- Xóa Trip sẽ SET NULL cho TripID (ON DELETE SET NULL)

---

### 8. NHÓM BẢNG AUDIT LOG

#### 8.1. Bảng `AuditLogs` - Nhật ký hệ thống

**Mục đích:** Ghi lại mọi thay đổi quan trọng trong hệ thống

| Tên cột | Kiểu dữ liệu | Ràng buộc | Mô tả |
|---------|-------------|-----------|-------|
| **LogID** | SERIAL | PRIMARY KEY | ID log |
| UserID | INTEGER | FK → Users | Người thực hiện (NULL = hệ thống) |
| **Action** | VARCHAR(100) | NOT NULL | Hành động (CREATE, UPDATE, DELETE, LOGIN, LOGOUT) |
| TableName | VARCHAR(50) | | Tên bảng bị thay đổi |
| RecordID | INTEGER | | ID bản ghi bị thay đổi |
| OldValue | TEXT | | Giá trị cũ (JSON format) |
| NewValue | TEXT | | Giá trị mới (JSON format) |
| IPAddress | VARCHAR(50) | | Địa chỉ IP |
| CreatedAt | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | Thời gian tạo |

**Quy tắc nghiệp vụ:**
- Ghi log cho các thao tác quan trọng:
  - CREATE/UPDATE/DELETE trên các bảng: Users, Trips, Bookings, Payments, Refunds
  - LOGIN/LOGOUT của user
  - Thay đổi trạng thái Trip
  - Thanh toán, Hoàn tiền
- OldValue và NewValue lưu dạng JSON để dễ so sánh
- Không được xóa AuditLogs (chỉ archive)
- Xóa User sẽ SET NULL cho UserID (ON DELETE SET NULL)
- Dùng để audit trail, troubleshooting, security

---

## 🔍 VIEWS (Các truy vấn có sẵn)

### View 1: `vw_TripDetails` - Chi tiết chuyến xe

**Mục đích:** Hiển thị thông tin đầy đủ về chuyến xe để tìm kiếm/đặt vé

**Các cột trả về:**
- TripID, DepartureTime, ArrivalTime, BasePrice
- TripStatus (Chờ, Đang chạy, Hoàn thành, Hủy, Trễ)
- RouteName, Origin, Destination, Distance
- LicensePlate, VehicleType, VehicleCapacity, NumberOfFloors
- DriverName
- TotalSeatsCreated, AvailableSeats

**Sử dụng:**
```sql
-- Tìm chuyến xe có ghế trống
SELECT * FROM vw_TripDetails
WHERE Origin LIKE '%Hà Nội%'
  AND Destination LIKE '%Hải Phòng%'
  AND DATE(DepartureTime) = '2025-12-05'
  AND TripStatus = 'Chờ'
  AND AvailableSeats > 0;
```

---

### View 2: `vw_DailyRevenue` - Thống kê doanh thu theo ngày

**Mục đích:** Báo cáo doanh thu hàng ngày (Online vs Tại quầy)

**Các cột trả về:**
- RevenueDate (ngày)
- TotalBookings (tổng số booking)
- TotalTickets (tổng số vé)
- TotalRevenue (tổng doanh thu)
- OnlineRevenue (doanh thu online)
- OfflineRevenue (doanh thu tại quầy)

**Sử dụng:**
```sql
-- Doanh thu tháng 12/2025
SELECT 
    TO_CHAR(RevenueDate, 'YYYY-MM') AS Month,
    SUM(TotalRevenue) AS MonthlyRevenue,
    SUM(TotalTickets) AS TotalTickets
FROM vw_DailyRevenue
WHERE TO_CHAR(RevenueDate, 'YYYY-MM') = '2025-12'
GROUP BY TO_CHAR(RevenueDate, 'YYYY-MM');
```

---

### View 3: `vw_PassengerManifest` - Danh sách hành khách theo chuyến

**Mục đích:** Hiển thị danh sách đầy đủ hành khách để check-in/quản lý

**Các cột trả về:**
- TripID, DepartureTime, RouteName
- BookingCode, CustomerName, CustomerPhone
- TicketCode, SeatNumber, FloorNumber
- TicketStatus
- PassengerID, PassengerName, PassengerPhone
- PickupStop, PickupAddress
- DropoffStop, DropoffAddress
- CheckInStatus, CheckInTime
- SpecialNote

**Sử dụng:**
```sql
-- Danh sách hành khách chuyến 123
SELECT * FROM vw_PassengerManifest
WHERE TripID = 123
ORDER BY SeatNumber;
```

---

### View 4: `vw_PriceSuggestion` - Đề xuất giá vé

**Mục đích:** Tính toán giá vé tối thiểu và đề xuất giá dựa trên chi phí

**Các cột trả về:**
- TripID, DepartureTime, RouteName
- LicensePlate, VehicleType
- TotalCost (tổng chi phí)
- TotalSeats, OccupiedSeats, AvailableSeats
- CurrentPrice (giá hiện tại)
- MinPriceToBreakEven (giá hòa vốn)
- SuggestedPrice20Percent (giá đề xuất lãi 20%)
- OccupancyRate (tỷ lệ lấp đầy %)
- CurrentRevenue (doanh thu hiện tại)

**Sử dụng:**
```sql
-- Xem giá đề xuất cho chuyến sắp chạy
SELECT * FROM vw_PriceSuggestion
WHERE DepartureTime > CURRENT_TIMESTAMP
  AND OccupancyRate < 50
ORDER BY DepartureTime;
```

---

### View 5: `vw_DriverPerformance` - Hiệu suất tài xế

**Mục đích:** Đánh giá hiệu suất làm việc của tài xế

**Các cột trả về:**
- DriverID, DriverName, PhoneNumber
- TotalTrips (tổng số chuyến)
- CompletedTrips (chuyến hoàn thành)
- CancelledTrips (chuyến bị hủy)
- TotalDelays (số lần trễ)
- AvgDelayMinutes (trung bình phút trễ)
- AvgDriverRating (đánh giá trung bình)
- TotalReviews (số lượt đánh giá)
- TotalWorkHours (tổng giờ làm)
- TotalWorkDays (tổng ngày làm)
- TotalViolations (số lần vi phạm)
- TotalSalary (tổng lương)

**Sử dụng:**
```sql
-- Top 10 tài xế xuất sắc nhất
SELECT * FROM vw_DriverPerformance
WHERE CompletedTrips > 10
ORDER BY AvgDriverRating DESC, TotalViolations ASC
LIMIT 10;
```

---

## ⚙️ STORED PROCEDURES & FUNCTIONS

### 1. `sp_GenerateSeatsForTrip(p_TripID INTEGER)`

**Mục đích:** Tự động tạo sơ đồ ghế cho chuyến xe dựa trên loại xe

**Tham số:**
- `p_TripID`: ID chuyến xe cần tạo ghế

**Logic:**
1. Lấy VehicleID từ Trip
2. Lấy TypeID từ Vehicle
3. Lấy TotalSeats, NumberOfFloors từ VehicleType
4. Chia ghế đều cho các tầng
5. Tạo số ghế format: A01, A02... (tầng 1), B01, B02... (tầng 2)
6. Insert vào TripSeats với Status = 'Trống'

**Cách sử dụng:**
```sql
-- Tạo ghế cho chuyến xe ID = 1
SELECT sp_GenerateSeatsForTrip(1);
```

**Lưu ý:**
- Phải tạo Trip trước khi gọi function này
- Chỉ gọi 1 lần cho mỗi Trip
- Nếu gọi lại sẽ bị lỗi UNIQUE constraint

---

### 2. `sp_CheckBookingEligibility(p_TripID, p_BookingType)`

**Mục đích:** Kiểm tra xem có thể đặt vé cho chuyến xe không

**Tham số:**
- `p_TripID`: ID chuyến xe
- `p_BookingType`: 'Online' hoặc 'Tại quầy'

**Output:**
- `p_CanBook` (BOOLEAN): TRUE = có thể đặt, FALSE = không thể
- `p_Message` (VARCHAR): Thông báo chi tiết

**Logic kiểm tra:**
1. Nếu IsFullyBooked = TRUE → không thể đặt
2. Nếu BookingType = 'Online' và còn < OnlineBookingCutoff phút → không thể đặt online
3. Nếu DepartureTime đã qua → không thể đặt
4. Còn lại → có thể đặt

**Cách sử dụng:**
```sql
DO $$
DECLARE
    can_book BOOLEAN;
    msg VARCHAR(255);
BEGIN
    SELECT * FROM sp_CheckBookingEligibility(123, 'Online') INTO can_book, msg;
    RAISE NOTICE 'CanBook: %, Message: %', can_book, msg;
END $$;
```

---

### 3. `sp_CancelTicket(p_BookingID INTEGER, p_RefundReason TEXT)`

**Mục đích:** Hủy vé và xử lý hoàn tiền theo quy định

**Tham số:**
- `p_BookingID`: ID đơn đặt vé cần hủy
- `p_RefundReason`: Lý do hủy

**Output:**
- `Success` (BOOLEAN): TRUE = hủy thành công, FALSE = thất bại
- `Message` (VARCHAR): Thông báo kết quả

**Logic:**
1. Kiểm tra thời gian còn lại đến giờ khởi hành
2. Nếu < 2 giờ → không cho hủy
3. Nếu >= 4 giờ → hoàn 90%
4. Nếu 2-4 giờ → hoàn 50%
5. Cập nhật BookingStatus = 'Đã hủy'
6. Cập nhật TicketStatus = 'Đã hủy'
7. Giải phóng ghế (Status = 'Trống', HoldExpiry = NULL)
8. Cập nhật PaymentStatus = 'Đã hoàn tiền'
9. Tạo Refund record

**Cách sử dụng:**
```sql
SELECT * FROM sp_CancelTicket(123, 'Khách hàng có việc đột xuất');
```

**Kết quả mẫu:**
```
Success | Message
--------|----------------------------------------------------------
TRUE    | Hủy vé thành công. Số tiền hoàn: 450000 VNĐ
```

---

### 4. `sp_ReleaseExpiredSeats()`

**Mục đích:** Tự động giải phóng các ghế đã hết thời gian giữ chỗ

**Logic:**
- Tìm tất cả TripSeats có:
  - Status = 'Đang giữ'
  - HoldExpiry < CURRENT_TIMESTAMP (đã hết hạn)
- Cập nhật Status = 'Trống', HoldExpiry = NULL

**Cách sử dụng:**
```sql
-- Chạy thủ công
SELECT sp_ReleaseExpiredSeats();

-- Hoặc thiết lập scheduled job (pg_cron)
SELECT cron.schedule('release-expired-seats', '* * * * *',
    'SELECT sp_ReleaseExpiredSeats();');
```

**Lưu ý:**
- Nên chạy định kỳ mỗi 1-5 phút
- Đảm bảo ghế được giải phóng kịp thời cho khách khác đặt

---

### 5. `sp_CheckInPassenger(p_TicketCode, p_CheckInMethod, p_CheckedInBy)`

**Mục đích:** Check-in hành khách lên xe

**Tham số:**
- `p_TicketCode`: Mã vé (TK...)
- `p_CheckInMethod`: Phương thức ('QR', 'Manual', 'RFID')
- `p_CheckedInBy`: UserID của người check-in

**Output:**
- `p_Success` (BOOLEAN): TRUE = check-in thành công
- `p_Message` (VARCHAR): Thông báo kết quả

**Logic:**
1. Tìm Ticket theo TicketCode
2. Tìm Passenger theo TicketID
3. Kiểm tra:
   - Vé có tồn tại không?
   - Hành khách có tồn tại không?
   - Đã check-in chưa?
   - Trạng thái chuyến xe (phải Chờ hoặc Đang chạy)
4. Nếu hợp lệ → cập nhật:
   - CheckInStatus = 'Đã lên xe'
   - CheckInTime = CURRENT_TIMESTAMP
   - CheckInMethod
   - CheckedInBy

**Cách sử dụng:**
```sql
DO $$
DECLARE
    success BOOLEAN;
    msg VARCHAR(255);
BEGIN
    SELECT * FROM sp_CheckInPassenger('TK20251202XXXXX', 'QR', 2) INTO success, msg;
    RAISE NOTICE 'Success: %, Message: %', success, msg;
END $$;
```

---

## 🔔 TRIGGERS

### 1. `trg_GenerateBookingCode`

**Bảng:** Bookings  
**Sự kiện:** BEFORE INSERT  
**Chức năng:** Tự động tạo BookingCode nếu NULL

**Format:** BK + YYYYMMDD + 5 số random  
**Ví dụ:** BK2025120212345

---

### 2. `trg_GenerateTicketCode`

**Bảng:** Tickets  
**Sự kiện:** BEFORE INSERT  
**Chức năng:** Tự động tạo TicketCode nếu NULL

**Format:** TK + YYYYMMDD + 5 số random  
**Ví dụ:** TK2025120267890

---

### 3. `trg_UpdateSeatStatus`

**Bảng:** Tickets  
**Sự kiện:** AFTER INSERT  
**Chức năng:** Tự động cập nhật TripSeats.Status = 'Đã đặt' khi tạo vé

---

### 4. `trg_CreatePassengerOnTicket`

**Bảng:** Tickets  
**Sự kiện:** AFTER INSERT  
**Chức năng:** Tự động tạo Passenger với thông tin từ Booking

**Dữ liệu mặc định:**
- FullName ← Booking.CustomerName
- PhoneNumber ← Booking.CustomerPhone
- Email ← Booking.CustomerEmail

---

### 5. `trg_UpdateTripFullStatus`

**Bảng:** TripSeats  
**Sự kiện:** AFTER UPDATE  
**Chức năng:** Tự động cập nhật Trips.IsFullyBooked khi tất cả ghế đã đặt

**Logic:**
- Đếm TotalSeats
- Đếm BookedSeats (Status = 'Đã đặt')
- Nếu BookedSeats >= TotalSeats → Trips.IsFullyBooked = TRUE
- Ngược lại → Trips.IsFullyBooked = FALSE

---

### 6. `update_*_updated_at` Triggers

**Các bảng:** Vehicles, Drivers, Routes, Trips, Bookings, Passengers, TripCosts, DriverWorklog

**Sự kiện:** BEFORE UPDATE  
**Chức năng:** Tự động cập nhật cột UpdatedAt = CURRENT_TIMESTAMP

---

## 📈 INDEXES (Tối ưu hiệu suất)

Hệ thống có **35+ indexes** được tạo sẵn để tối ưu hiệu suất:

### Indexes cho tìm kiếm chuyến xe:
```sql
idx_trips_search          ON Trips(RouteID, DepartureTime, Status)
idx_trips_departure       ON Trips(DepartureTime, Status)
idx_trips_status          ON Trips(Status)
idx_routes_locations      ON Routes(OriginID, DestinationID)
idx_routes_status         ON Routes(Status)
```

### Indexes cho quản lý vé:
```sql
idx_bookings_customer     ON Bookings(CustomerID, BookingStatus)
idx_bookings_guest        ON Bookings(IsGuestBooking, CreatedAt)
idx_bookings_code         ON Bookings(BookingCode)
idx_bookings_created      ON Bookings(CreatedAt)
idx_bookings_trip_status  ON Bookings(TripID, BookingStatus)
idx_tickets_booking       ON Tickets(BookingID, TicketStatus)
idx_tickets_code          ON Tickets(TicketCode)
idx_tickets_status        ON Tickets(TicketStatus)
idx_tickets_seatid        ON Tickets(SeatID)
idx_tripseats_status      ON TripSeats(TripID, Status)
idx_tripseats_tripid      ON TripSeats(TripID)
```

### Indexes cho thanh toán:
```sql
idx_payments_booking           ON Payments(BookingID, PaymentStatus)
idx_payments_status_created    ON Payments(PaymentStatus, CreatedAt)
idx_payments_method            ON Payments(PaymentMethod, PaymentStatus)
idx_refunds_booking            ON Refunds(BookingID, RefundStatus)
idx_refunds_status             ON Refunds(RefundStatus)
```

### Indexes cho hành khách:
```sql
idx_passengers_checkin    ON Passengers(CheckInStatus, CheckInTime)
idx_passengers_ticketid   ON Passengers(TicketID)
```

### Indexes cho người dùng:
```sql
idx_users_email           ON Users(Email)
idx_users_phone           ON Users(PhoneNumber)
idx_users_role_status     ON Users(RoleID, Status)
```

### Indexes cho báo cáo:
```sql
idx_reviews_trip          ON Reviews(TripID, Rating)
idx_trip_costs_profit     ON TripCosts(Profit, CalculatedAt)
idx_trip_tracking         ON TripTracking(TripID, RecordedAt)
idx_driver_date           ON DriverWorklog(DriverID, WorkDate)
idx_driver_status         ON DriverWorklog(DriverID, Status)
```

### Indexes cho routes:
```sql
idx_routestops_routeid    ON RouteStops(RouteID)
idx_routestops_locationid ON RouteStops(LocationID)
idx_vehicles_status       ON Vehicles(Status)
```

---

## 🔐 YÊU CẦU BẢO MẬT

### 1. Mã hóa dữ liệu
- **Mật khẩu:** Sử dụng bcrypt hoặc SHA-256 hash (không lưu plain text)
- **Dữ liệu nhạy cảm:** SSL/TLS cho kết nối database
- **Thông tin thanh toán:** Tuân thủ PCI DSS (không lưu số thẻ đầy đủ)

### 2. Phân quyền truy cập
```sql
-- Admin: Full access
GRANT ALL PRIVILEGES ON ALL TABLES TO admin_role;

-- Nhân viên: Read/Write (không DELETE)
GRANT SELECT, INSERT, UPDATE ON ALL TABLES TO staff_role;
REVOKE DELETE ON ALL TABLES FROM staff_role;

-- Tài xế: Chỉ đọc và cập nhật Trip, DriverWorklog
GRANT SELECT ON ALL TABLES TO driver_role;
GRANT UPDATE ON Trips, DriverWorklog, TripTracking TO driver_role;

-- Khách hàng: Chỉ đọc Trips, Routes, đọc/ghi Bookings, Reviews của họ
GRANT SELECT ON Trips, Routes, Locations, VehicleTypes TO customer_role;
```

### 3. Audit Trail
- Tất cả thao tác quan trọng được ghi vào `AuditLogs`
- Bao gồm: User, Action, OldValue, NewValue, IPAddress, Timestamp
- Không được xóa logs (chỉ archive sau 2-3 năm)

### 4. Xác thực 2 lớp (2FA)
- Hỗ trợ OTP qua Email/SMS
- Thêm cột `TwoFactorEnabled`, `TwoFactorSecret` vào bảng Users (nếu cần)

### 5. Row Level Security (RLS)
```sql
-- Khách hàng chỉ thấy booking của mình
ALTER TABLE Bookings ENABLE ROW LEVEL SECURITY;
CREATE POLICY customer_bookings ON Bookings
    FOR SELECT USING (CustomerID = current_user_id());
```

---

## 📊 CÁC TRUY VẤN MẪU

### 1. Tìm chuyến xe
```sql
-- Tìm chuyến xe Hà Nội → Hải Phòng ngày 05/12/2025
SELECT * FROM vw_TripDetails
WHERE Origin LIKE '%Hà Nội%'
  AND Destination LIKE '%Hải Phòng%'
  AND DATE(DepartureTime) = '2025-12-05'
  AND TripStatus = 'Chờ'
  AND AvailableSeats > 0
ORDER BY DepartureTime;
```

### 2. Thống kê doanh thu tháng
```sql
-- Doanh thu tháng 12/2025
SELECT 
    TO_CHAR(RevenueDate, 'YYYY-MM') AS Month,
    SUM(TotalRevenue) AS MonthlyRevenue,
    SUM(TotalTickets) AS TotalTickets,
    SUM(TotalBookings) AS TotalBookings,
    ROUND(SUM(TotalRevenue) / NULLIF(SUM(TotalBookings), 0), 2) AS AvgRevenuePerBooking
FROM vw_DailyRevenue
WHERE TO_CHAR(RevenueDate, 'YYYY-MM') = '2025-12'
GROUP BY TO_CHAR(RevenueDate, 'YYYY-MM');
```

### 3. Danh sách vé của khách hàng
```sql
-- Xem tất cả vé của khách hàng ID = 123
SELECT
    b.BookingCode,
    b.CustomerName,
    t.TicketCode,
    tr.DepartureTime,
    r.RouteName,
    ts.SeatNumber,
    ts.FloorNumber,
    t.Price,
    t.TicketStatus,
    p.CheckInStatus
FROM Tickets t
JOIN Bookings b ON t.BookingID = b.BookingID
JOIN TripSeats ts ON t.SeatID = ts.SeatID
JOIN Trips tr ON ts.TripID = tr.TripID
JOIN Routes r ON tr.RouteID = r.RouteID
LEFT JOIN Passengers p ON t.TicketID = p.TicketID
WHERE b.CustomerID = 123
ORDER BY tr.DepartureTime DESC;
```

### 4. Top 5 tuyến đường doanh thu cao nhất
```sql
SELECT
    r.RouteName,
    COUNT(DISTINCT t.TripID) AS TotalTrips,
    COUNT(DISTINCT tk.TicketID) AS TotalTickets,
    SUM(p.Amount) AS TotalRevenue,
    ROUND(SUM(p.Amount) / COUNT(DISTINCT t.TripID), 2) AS AvgRevenuePerTrip
FROM Routes r
JOIN Trips t ON r.RouteID = t.RouteID
JOIN Bookings b ON t.TripID = b.TripID
JOIN Tickets tk ON b.BookingID = tk.BookingID
JOIN Payments p ON b.BookingID = p.BookingID
WHERE p.PaymentStatus = 'Thành công'
  AND t.DepartureTime >= CURRENT_DATE - INTERVAL '3 months'
GROUP BY r.RouteID, r.RouteName
ORDER BY TotalRevenue DESC
LIMIT 5;
```

### 5. Kiểm tra ghế trống của chuyến xe
```sql
-- Xem sơ đồ ghế chuyến xe ID = 123
SELECT 
    SeatNumber,
    FloorNumber,
    SeatType,
    Status,
    CASE 
        WHEN Status = 'Trống' THEN 'Có thể đặt'
        WHEN Status = 'Đang giữ' THEN 'Đang giữ - Hết hạn: ' || TO_CHAR(HoldExpiry, 'HH24:MI:SS')
        WHEN Status = 'Đã đặt' THEN 'Đã có người đặt'
    END AS StatusNote
FROM TripSeats
WHERE TripID = 123
ORDER BY FloorNumber, SeatNumber;
```

### 6. Tìm tài xế rảnh trong khung giờ
```sql
-- Tìm tài xế không có lịch chạy trong ngày 05/12/2025 từ 8h-12h
SELECT 
    d.DriverID,
    u.FullName,
    u.PhoneNumber,
    d.DriverLicense
FROM Drivers d
JOIN Users u ON d.UserID = u.UserID
WHERE d.DriverID NOT IN (
    SELECT DISTINCT DriverID
    FROM Trips
    WHERE DATE(DepartureTime) = '2025-12-05'
      AND (
          (DepartureTime BETWEEN '2025-12-05 08:00:00' AND '2025-12-05 12:00:00')
          OR (ArrivalTime BETWEEN '2025-12-05 08:00:00' AND '2025-12-05 12:00:00')
          OR (DepartureTime <= '2025-12-05 08:00:00' AND ArrivalTime >= '2025-12-05 12:00:00')
      )
)
ORDER BY u.FullName;
```

### 7. Báo cáo chi phí và lợi nhuận chuyến xe
```sql
-- Báo cáo chi tiết chi phí và lợi nhuận tháng 12/2025
SELECT 
    t.TripID,
    t.DepartureTime,
    r.RouteName,
    v.LicensePlate,
    tc.TotalCost,
    tc.Revenue,
    tc.CancelledRevenue,
    tc.NetRevenue,
    tc.Profit,
    ROUND((tc.Profit / NULLIF(tc.NetRevenue, 0)) * 100, 2) AS ProfitMargin
FROM TripCosts tc
JOIN Trips t ON tc.TripID = t.TripID
JOIN Routes r ON t.RouteID = r.RouteID
JOIN Vehicles v ON t.VehicleID = v.VehicleID
WHERE t.DepartureTime >= '2025-12-01' 
  AND t.DepartureTime < '2026-01-01'
  AND t.Status = 'Hoàn thành'
ORDER BY tc.Profit DESC;
```

### 8. Danh sách booking cần xử lý hoàn tiền
```sql
-- Xem các yêu cầu hoàn tiền đang chờ xử lý
SELECT 
    r.RefundID,
    b.BookingCode,
    b.CustomerName,
    b.CustomerPhone,
    r.RefundAmount,
    r.RefundReason,
    r.RefundType,
    r.RefundMethod,
    r.CreatedAt,
    EXTRACT(DAY FROM (CURRENT_TIMESTAMP - r.CreatedAt)) AS DaysWaiting
FROM Refunds r
JOIN Bookings b ON r.BookingID = b.BookingID
WHERE r.RefundStatus = 'Đang xử lý'
ORDER BY r.CreatedAt ASC;
```

### 9. Thống kê đánh giá theo tài xế
```sql
-- Xem đánh giá chi tiết của tài xế
SELECT 
    d.DriverID,
    u.FullName AS DriverName,
    COUNT(r.ReviewID) AS TotalReviews,
    ROUND(AVG(r.DriverRating), 2) AS AvgDriverRating,
    ROUND(AVG(r.PunctualityRating), 2) AS AvgPunctualityRating,
    COUNT(CASE WHEN r.DriverRating >= 4 THEN 1 END) AS PositiveReviews,
    COUNT(CASE WHEN r.DriverRating <= 2 THEN 1 END) AS NegativeReviews
FROM Drivers d
JOIN Users u ON d.UserID = u.UserID
LEFT JOIN Trips t ON d.DriverID = t.DriverID
LEFT JOIN Reviews r ON t.TripID = r.TripID
WHERE r.ReviewID IS NOT NULL
GROUP BY d.DriverID, u.FullName
HAVING COUNT(r.ReviewID) > 0
ORDER BY AvgDriverRating DESC;
```

### 10. Theo dõi vị trí xe đang chạy
```sql
-- Xem vị trí hiện tại của các xe đang chạy
SELECT 
    t.TripID,
    r.RouteName,
    v.LicensePlate,
    u.FullName AS DriverName,
    t.DepartureTime,
    t.ArrivalTime,
    tt.CurrentAddress,
    tt.Speed,
    tt.TrafficStatus,
    tt.DelayMinutes,
    tt.EstimatedArrival,
    tt.RecordedAt
FROM Trips t
JOIN Routes r ON t.RouteID = r.RouteID
JOIN Vehicles v ON t.VehicleID = v.VehicleID
JOIN Drivers d ON t.DriverID = d.DriverID
JOIN Users u ON d.UserID = u.UserID
LEFT JOIN LATERAL (
    SELECT * FROM TripTracking
    WHERE TripID = t.TripID
    ORDER BY RecordedAt DESC
    LIMIT 1
) tt ON TRUE
WHERE t.Status = 'Đang chạy'
ORDER BY t.DepartureTime;
```

### 11. Lịch làm việc tài xế trong tuần
```sql
-- Xem lịch làm việc của tài xế trong tuần
SELECT 
    d.DriverID,
    u.FullName AS DriverName,
    dw.WorkDate,
    dw.StartTime,
    dw.EndTime,
    dw.TotalHours,
    dw.TripCount,
    dw.TotalSalary,
    dw.Status,
    CASE 
        WHEN dw.HasViolation THEN '⚠️ ' || dw.ViolationType
        ELSE '✓ Không vi phạm'
    END AS ViolationStatus
FROM DriverWorklog dw
JOIN Drivers d ON dw.DriverID = d.DriverID
JOIN Users u ON d.UserID = u.UserID
WHERE dw.WorkDate >= CURRENT_DATE - INTERVAL '7 days'
  AND dw.WorkDate <= CURRENT_DATE
ORDER BY d.DriverID, dw.WorkDate DESC;
```

### 12. Thống kê tỷ lệ hủy vé theo tuyến
```sql
-- Phân tích tỷ lệ hủy vé theo tuyến đường
SELECT 
    r.RouteName,
    COUNT(DISTINCT b.BookingID) AS TotalBookings,
    COUNT(DISTINCT CASE WHEN b.BookingStatus = 'Đã hủy' THEN b.BookingID END) AS CancelledBookings,
    ROUND(
        COUNT(DISTINCT CASE WHEN b.BookingStatus = 'Đã hủy' THEN b.BookingID END) * 100.0 
        / NULLIF(COUNT(DISTINCT b.BookingID), 0), 2
    ) AS CancellationRate,
    SUM(CASE WHEN b.BookingStatus = 'Đã hủy' THEN b.TotalAmount ELSE 0 END) AS LostRevenue
FROM Routes r
JOIN Trips t ON r.RouteID = t.RouteID
JOIN Bookings b ON t.TripID = b.TripID
WHERE t.DepartureTime >= CURRENT_DATE - INTERVAL '3 months'
GROUP BY r.RouteID, r.RouteName
HAVING COUNT(DISTINCT b.BookingID) > 10
ORDER BY CancellationRate DESC;
```

---

## 🚀 HƯỚNG DẪN CÀI ĐẶT VÀ SỬ DỤNG

### Bước 1: Cài đặt PostgreSQL

#### Trên Windows:
1. Tải PostgreSQL từ: https://www.postgresql.org/download/windows/
2. Chạy file cài đặt và làm theo hướng dẫn
3. Ghi nhớ mật khẩu cho user `postgres`
4. Thêm PostgreSQL vào PATH: `C:\Program Files\PostgreSQL\<version>\bin`

#### Trên Linux (Ubuntu/Debian):
```bash
sudo apt-get update
sudo apt-get install postgresql postgresql-contrib
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

#### Trên macOS (sử dụng Homebrew):
```bash
brew install postgresql
brew services start postgresql
```

---

### Bước 2: Tạo Database

#### Cách 1: Sử dụng psql (Command Line)
```bash
# Đăng nhập vào PostgreSQL
psql -U postgres

# Tạo database
CREATE DATABASE BusTicketManagement;

# Kết nối vào database
\c BusTicketManagement

# Chạy script từ file
\i 'D:/OOAD/DTB/SQLQuery1.sql'

# Hoặc nếu ở cùng thư mục
\i SQLQuery1.sql
```

#### Cách 2: Chạy trực tiếp từ terminal
```bash
# Chạy toàn bộ script
psql -U postgres -f SQLQuery1.sql

# Hoặc với database cụ thể
psql -U postgres -d BusTicketManagement -f SQLQuery1.sql
```

#### Cách 3: Sử dụng pgAdmin (GUI)
1. Mở pgAdmin
2. Kết nối đến PostgreSQL server
3. Right-click → Create → Database
4. Đặt tên: `BusTicketManagement`
5. Right-click database → Query Tool
6. Mở file `SQLQuery1.sql` (File → Open)
7. Click Execute (F5)

---

### Bước 3: Kiểm tra cài đặt

```sql
-- Kết nối vào database
psql -U postgres -d BusTicketManagement

-- Xem danh sách bảng
\dt

-- Xem tất cả schema objects
\d

-- Xem chi tiết cấu trúc bảng
\d+ Users
\d+ Trips
\d+ Bookings

-- Xem các views
\dv

-- Xem các functions
\df

-- Xem các triggers
SELECT 
    trigger_name, 
    event_object_table, 
    action_timing, 
    event_manipulation
FROM information_schema.triggers
WHERE trigger_schema = 'public'
ORDER BY event_object_table, trigger_name;

-- Kiểm tra dữ liệu mẫu
SELECT 'Roles' AS TableName, COUNT(*) AS RecordCount FROM Roles
UNION ALL
SELECT 'VehicleTypes', COUNT(*) FROM VehicleTypes
UNION ALL
SELECT 'Users', COUNT(*) FROM Users;
```

**Kết quả mong đợi:**
```
TableName      | RecordCount
---------------|------------
Roles          | 4
VehicleTypes   | 3
Users          | 1
```

---

### Bước 4: Tạo dữ liệu test

```sql
-- 1. Tạo thêm Users
INSERT INTO Users (FullName, Email, PhoneNumber, Password, RoleID, EmailVerified)
VALUES 
('Nguyễn Văn Tài Xế', 'driver1@example.com', '0901234567', '$2a$10$XYZ...', 3, TRUE),
('Trần Thị Nhân Viên', 'staff1@example.com', '0912345678', '$2a$10$XYZ...', 2, TRUE),
('Lê Văn Khách', 'customer1@example.com', '0923456789', '$2a$10$XYZ...', 4, TRUE);

-- 2. Tạo Locations
INSERT INTO Locations (LocationName, Province, Address, Latitude, Longitude)
VALUES 
('Bến xe Mỹ Đình', 'Hà Nội', 'Phạm Hùng, Nam Từ Liêm, Hà Nội', 21.0285, 105.7787),
('Bến xe Ninh Bình', 'Ninh Bình', 'QL1A, Ninh Bình', 20.2506, 105.9745),
('Bến xe Tam Điệp', 'Ninh Bình', 'Tam Điệp, Ninh Bình', 20.1667, 105.9000);

-- 3. Tạo Routes
INSERT INTO Routes (RouteName, OriginID, DestinationID, Distance, EstimatedDuration, Status)
VALUES 
('Hà Nội - Ninh Bình', 1, 2, 95, 120, 'Hoạt động');

-- 4. Tạo RouteStops
INSERT INTO RouteStops (RouteID, LocationID, StopOrder, StopType, DistanceFromOrigin, EstimatedTime)
VALUES 
(1, 1, 1, 'Điểm khởi hành', 0, 0),
(1, 3, 2, 'Điểm dừng chân', 50, 60),
(1, 2, 3, 'Điểm đến', 95, 120);

-- 5. Tạo Vehicles
INSERT INTO Vehicles (LicensePlate, TypeID, InsuranceNumber, InsuranceExpiry, Status)
VALUES 
('29A-12345', 1, 'BH123456', '2026-12-31', 'Hoàn thiện'),
('30B-67890', 2, 'BH789012', '2026-12-31', 'Hoàn thiện');

-- 6. Tạo Drivers
INSERT INTO Drivers (UserID, DriverLicense, LicenseExpiry, DateOfBirth, Salary)
VALUES 
(2, 'B2-123456789', '2028-12-31', '1985-05-15', 8000000);

-- 7. Tạo Trips
INSERT INTO Trips (RouteID, VehicleID, DriverID, DepartureTime, ArrivalTime, BasePrice, Status, CreatedBy)
VALUES 
(1, 1, 1, '2025-12-10 08:00:00', '2025-12-10 10:00:00', 150000, 'Chờ', 1);

-- 8. Tạo ghế cho chuyến xe
SELECT sp_GenerateSeatsForTrip(1);

-- 9. Kiểm tra kết quả
SELECT * FROM vw_TripDetails WHERE TripID = 1;
```

---

### Bước 5: Test các Functions

```sql
-- 1. Test kiểm tra đủ điều kiện đặt vé
DO $$
DECLARE
    can_book BOOLEAN;
    msg VARCHAR(255);
BEGIN
    SELECT * FROM sp_CheckBookingEligibility(1, 'Online') INTO can_book, msg;
    RAISE NOTICE 'CanBook: %, Message: %', can_book, msg;
END $$;

-- 2. Test đặt vé và tạo booking
INSERT INTO Bookings (BookingCode, CustomerID, CustomerName, CustomerPhone, CustomerEmail, TripID, TotalAmount, BookingStatus, BookingType)
VALUES ('', 4, 'Lê Văn Khách', '0923456789', 'customer1@example.com', 1, 150000, 'Đang giữ', 'Online');

-- Lấy BookingID vừa tạo
SELECT BookingID, BookingCode FROM Bookings ORDER BY BookingID DESC LIMIT 1;

-- 3. Tạo vé (giả sử BookingID = 1, SeatID = 1)
INSERT INTO Tickets (TicketCode, BookingID, SeatID, Price, TicketStatus)
VALUES ('', 1, 1, 150000, 'Chưa xác nhận');

-- 4. Test thanh toán
INSERT INTO Payments (BookingID, Amount, PaymentMethod, PaymentStatus, TransactionID, PaidAt)
VALUES (1, 150000, 'Chuyển khoản', 'Thành công', 'TXN123456', CURRENT_TIMESTAMP);

-- Cập nhật BookingStatus
UPDATE Bookings SET BookingStatus = 'Đã thanh toán' WHERE BookingID = 1;

-- 5. Test check-in
DO $$
DECLARE
    success BOOLEAN;
    msg VARCHAR(255);
    ticket_code VARCHAR(20);
BEGIN
    SELECT TicketCode INTO ticket_code FROM Tickets WHERE BookingID = 1 LIMIT 1;
    SELECT * FROM sp_CheckInPassenger(ticket_code, 'QR', 2) INTO success, msg;
    RAISE NOTICE 'Success: %, Message: %', success, msg;
END $$;

-- 6. Test hủy vé (tạo booking mới để test)
SELECT * FROM sp_CancelTicket(1, 'Test hủy vé');
```

---

### Bước 6: Thiết lập Scheduled Jobs (Tùy chọn)

#### Sử dụng pg_cron extension:

```sql
-- 1. Cài đặt pg_cron (chỉ chạy 1 lần)
CREATE EXTENSION IF NOT EXISTS pg_cron;

-- 2. Thiết lập job giải phóng ghế hết hạn (chạy mỗi phút)
SELECT cron.schedule(
    'release-expired-seats',
    '* * * * *',
    'SELECT sp_ReleaseExpiredSeats();'
);

-- 3. Xem danh sách jobs
SELECT * FROM cron.job;

-- 4. Xem lịch sử chạy jobs
SELECT * FROM cron.job_run_details ORDER BY start_time DESC LIMIT 10;

-- 5. Xóa job (nếu cần)
SELECT cron.unschedule('release-expired-seats');
```

#### Hoặc sử dụng Cron job của hệ thống (Linux):

```bash
# Mở crontab
crontab -e

# Thêm dòng sau (chạy mỗi phút)
* * * * * psql -U postgres -d BusTicketManagement -c "SELECT sp_ReleaseExpiredSeats();"

# Lưu và thoát
```

---

### Bước 7: Backup và Restore

#### Backup toàn bộ database:
```bash
pg_dump -U postgres -d BusTicketManagement -F c -b -v -f backup_$(date +%Y%m%d).dump

# Hoặc backup dạng SQL
pg_dump -U postgres -d BusTicketManagement > backup_$(date +%Y%m%d).sql
```

#### Restore từ backup:
```bash
# Restore từ .dump
pg_restore -U postgres -d BusTicketManagement -v backup_20251202.dump

# Restore từ .sql
psql -U postgres -d BusTicketManagement < backup_20251202.sql
```

#### Backup chỉ schema (không data):
```bash
pg_dump -U postgres -d BusTicketManagement --schema-only > schema_only.sql
```

#### Backup chỉ data (không schema):
```bash
pg_dump -U postgres -d BusTicketManagement --data-only > data_only.sql
```

---

## 📞 HỖ TRỢ VÀ BẢO TRÌ

### Monitoring và Performance

```sql
-- 1. Kiểm tra kích thước database
SELECT 
    pg_size_pretty(pg_database_size('BusTicketManagement')) AS DatabaseSize;

-- 2. Kiểm tra kích thước từng bảng
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS TotalSize
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- 3. Kiểm tra indexes không được sử dụng
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan,
    pg_size_pretty(pg_relation_size(indexrelid)) AS IndexSize
FROM pg_stat_user_indexes
WHERE idx_scan = 0
  AND schemaname = 'public'
ORDER BY pg_relation_size(indexrelid) DESC;

-- 4. Kiểm tra queries chậm (cần enable pg_stat_statements)
SELECT 
    query,
    calls,
    total_exec_time / 1000 AS total_time_seconds,
    mean_exec_time / 1000 AS avg_time_seconds
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 10;

-- 5. Vacuum và Analyze (bảo trì)
VACUUM ANALYZE;

-- Hoặc cho bảng cụ thể
VACUUM ANALYZE Bookings;
VACUUM ANALYZE Tickets;
```

### Troubleshooting

```sql
-- 1. Kiểm tra connections
SELECT 
    datname,
    usename,
    application_name,
    client_addr,
    state,
    query_start
FROM pg_stat_activity
WHERE datname = 'BusTicketManagement';

-- 2. Kill connection chậm
SELECT pg_terminate_backend(pid)
FROM pg_stat_activity
WHERE datname = 'BusTicketManagement'
  AND state = 'idle'
  AND query_start < CURRENT_TIMESTAMP - INTERVAL '1 hour';

-- 3. Kiểm tra locks
SELECT 
    locktype,
    database,
    relation::regclass,
    page,
    tuple,
    virtualxid,
    transactionid,
    mode,
    granted
FROM pg_locks
WHERE database = (SELECT oid FROM pg_database WHERE datname = 'BusTicketManagement');
```

---

## 📝 CHANGELOG

### Version 1.0.0 (December 2, 2025)
- ✅ Khởi tạo database với 15 bảng chính
- ✅ Tạo 5 Views để truy vấn
- ✅ Tạo 5 Functions/Procedures
- ✅ Tạo 6 Triggers tự động
- ✅ Tạo 35+ Indexes tối ưu
- ✅ Thêm Computed Columns (TotalCost, NetRevenue, Profit, TotalSalary)
- ✅ Thêm Constraints đầy đủ (CHECK, UNIQUE, FK)
- ✅ Thêm dữ liệu mẫu (Roles, VehicleTypes, Admin)
- ✅ Hỗ trợ tracking GPS (TripTracking)
- ✅ Hỗ trợ quản lý worklog tài xế (DriverWorklog)
- ✅ Audit logs đầy đủ (AuditLogs)

---

## 📚 TÀI LIỆU THAM KHẢO

- **PostgreSQL Documentation:** https://www.postgresql.org/docs/
- **pgAdmin Documentation:** https://www.pgadmin.org/docs/
- **SQL Style Guide:** https://www.sqlstyle.guide/
- **Database Design Best Practices:** https://en.wikipedia.org/wiki/Database_design

---

## 👥 ĐÓNG GÓP

Nếu phát hiện lỗi hoặc có đề xuất cải tiến, vui lòng:
1. Tạo issue mô tả chi tiết
2. Gửi pull request với thay đổi
3. Liên hệ team phát triển

---

## 📄 LICENSE

Copyright © 2025 Bus Ticket Management System. All rights reserved.

---

**Tài liệu này được cập nhật lần cuối: December 2, 2025**

**Database Version:** 1.0.0  
**PostgreSQL Version:** 12+  
**Tác giả:** Development Team

