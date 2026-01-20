Class specifications (OOAD-style)

1. User

- Thuộc tính:
  - id: Integer
  - fullName: String
  - email: String
  - phone: String (nullable)
  - status: String (e.g., ACTIVE/INACTIVE)
- Quan hệ:
  - User 1 - 0..\* Booking (User đặt Booking)
- Ghi chú: đại diện người dùng hệ thống (khách hàng, nhân viên, admin)

2. Route

- Thuộc tính:
  - id: Integer
  - origin: String
  - destination: String
  - distanceKm: Decimal
- Quan hệ:
  - Route 1 - 0..\* Trip (Route định nghĩa các Trip)
- Ghi chú: mô tả lộ trình giữa hai địa điểm

3. Vehicle

- Thuộc tính:
  - id: Integer
  - plateNumber: String
  - model: String
  - capacity: Integer
- Quan hệ:
  - Vehicle 1 - 0..\* Trip (Vehicle phục vụ nhiều Trip theo thời gian)
- Ghi chú: phương tiện chở khách, có số ghế cố định (capacity)

4. Trip

- Thuộc tính:
  - id: Integer
  - departureAt: DateTime
  - arrivalAt: DateTime
  - seatPrice: Decimal
  - status: String (e.g., SCHEDULED/CANCELLED)
- Quan hệ:
  - Trip 1 - 0..\* Booking (một Trip có thể có nhiều Booking)
  - Trip n - 1 Vehicle
  - Trip n - 1 Route
- Ghi chú: một chuyến chạy cố định trên một Route với Vehicle cụ thể

5. Booking

- Thuộc tính:
  - id: Integer
  - createdAt: DateTime
  - totalAmount: Decimal
  - status: String (e.g., PENDING/CONFIRMED/CANCELLED)
- Quan hệ:
  - Booking n - 1 User (Booking được tạo bởi một User)
  - Booking 1 - 1..\* Ticket (Booking có thể sinh ra một hoặc nhiều Ticket)
  - Booking 1 - 0..\* Payment (Booking có thể liên quan tới nhiều giao dịch)
  - Booking n - 1 Trip (Booking dành cho một Trip)
- Ghi chú: đại diện hành động đặt ghế/vé bởi người dùng

6. Ticket

- Thuộc tính:
  - id: Integer
  - ticketNumber: String
  - issuedAt: DateTime
  - pdfUrl: String (nếu có)
  - status: String (e.g., ACTIVE/USED/CANCELLED)
- Quan hệ:
  - Ticket n - 1 Booking (Ticket thuộc về một Booking)
- Ghi chú: vé phát hành sau khi Booking được xác nhận/thanh toán

7. Payment

- Thuộc tính:
  - id: Integer
  - amount: Decimal
  - method: String (e.g., MoMo, CARD)
  - providerRef: String (tham chiếu bên cung cấp)
  - status: String (e.g., INIT/PAID/FAILED)
- Quan hệ:
  - Payment n - 1 Booking (một Payment thanh toán cho một Booking)
- Ghi chú: giao dịch thanh toán, cần xử lý idempotency cho callback

Phần chú:

- Mức chi tiết: Thuộc tính ở mức khái niệm (OOAD), không đi sâu vào các cột DB hay annotation.
- Không mô tả phương thức hay thuật toán; chỉ mô tả dữ liệu và quan hệ giữa các lớp chính.
- Sơ đồ PlantUML file: diagram/class-diagram.puml (sử dụng cho báo cáo).
