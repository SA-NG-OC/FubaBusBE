-- =============================================
-- HỆ THỐNG QUẢN LÝ BÁN VÉ XE KHÁCH
-- Bus Ticket Management System - PostgreSQL
-- =============================================

-- Tạo database
--CREATE DATABASE BusTicketManagement;

-- -- Kết nối vào database
-- \c BusTicketManagement;

-- Tạo extension UUID (nếu cần)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- PHẢI THÊM TRƯỚC KHI TẠO CÁC TRIGGER
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.UpdatedAt = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 1. BẢNG QUẢN LÝ NGƯỜI DÙNG VÀ TÀI KHOẢN
-- =============================================

-- Bảng vai trò (Roles)
CREATE TABLE Roles (
    RoleID SERIAL PRIMARY KEY,
    RoleName VARCHAR(50) NOT NULL UNIQUE,
    Description TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng người dùng (Users)
CREATE TABLE Users (
    UserID SERIAL PRIMARY KEY,
    FullName VARCHAR(100) NOT NULL,
    Email VARCHAR(100) UNIQUE NOT NULL,
    PhoneNumber VARCHAR(20) UNIQUE NOT NULL,
    Password VARCHAR(255) NOT NULL, -- Mã hóa Hash
    RoleID INTEGER NOT NULL,
    Status VARCHAR(20) DEFAULT 'Hoạt động' CHECK (Status IN ('Hoạt động', 'Khóa')),
    EmailVerified BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (RoleID) REFERENCES Roles(RoleID)
);

-- =============================================
-- 2. BẢNG QUẢN LÝ XE VÀ TÀI XẾ
-- =============================================

-- Bảng loại xe (VehicleTypes)
CREATE TABLE VehicleTypes (
    TypeID SERIAL PRIMARY KEY,
    TypeName VARCHAR(50) NOT NULL UNIQUE,
    TotalSeats INTEGER NOT NULL,
    NumberOfFloors INTEGER DEFAULT 1 CHECK (NumberOfFloors >= 1),
    Description TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng xe (Vehicles)
CREATE TABLE Vehicles (
    VehicleID SERIAL PRIMARY KEY,
    LicensePlate VARCHAR(20) NOT NULL UNIQUE,
    TypeID INTEGER NOT NULL,
    InsuranceNumber VARCHAR(50),
    InsuranceExpiry DATE,
    Status VARCHAR(20) DEFAULT 'Hoàn thiện' CHECK (Status IN ('Hoàn thiện', 'Hư hại', 'Phế liệu')),
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TypeID) REFERENCES VehicleTypes(TypeID)
);

CREATE TRIGGER update_vehicles_updated_at
    BEFORE UPDATE ON Vehicles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng tài xế (Drivers)
CREATE TABLE Drivers (
    DriverID SERIAL PRIMARY KEY,
    UserID INTEGER NOT NULL UNIQUE,
    DriverLicense VARCHAR(50) NOT NULL UNIQUE,
    LicenseExpiry DATE NOT NULL,
    DateOfBirth DATE NOT NULL,
    Salary DECIMAL(15,2) DEFAULT 0,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE
);

CREATE TRIGGER update_drivers_updated_at
    BEFORE UPDATE ON Drivers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 3. BẢNG QUẢN LÝ TUYẾN ĐƯỜNG VÀ ĐIỂM DỪNG
-- =============================================

-- Bảng địa điểm (Locations)
CREATE TABLE Locations (
    LocationID SERIAL PRIMARY KEY,
    LocationName VARCHAR(100) NOT NULL,
    Province VARCHAR(100) NOT NULL,
    Address TEXT,
    Latitude DECIMAL(10, 8),
    Longitude DECIMAL(11, 8),
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng lộ trình (Routes)
CREATE TABLE Routes (
    RouteID SERIAL PRIMARY KEY,
    RouteName VARCHAR(200) NOT NULL,
    OriginID INTEGER NOT NULL,
    DestinationID INTEGER NOT NULL,
    Distance DECIMAL(10,2) NOT NULL,
    EstimatedDuration INTEGER NOT NULL,
    Status VARCHAR(20) DEFAULT 'Hoạt động' CHECK (Status IN ('Hoạt động', 'Bảo trì', 'Dừng')),
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (OriginID) REFERENCES Locations(LocationID),
    FOREIGN KEY (DestinationID) REFERENCES Locations(LocationID)
);

CREATE TRIGGER update_routes_updated_at
    BEFORE UPDATE ON Routes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng điểm dừng trên lộ trình (RouteStops)
CREATE TABLE RouteStops (
    StopID SERIAL PRIMARY KEY,
    RouteID INTEGER NOT NULL,
    LocationID INTEGER NOT NULL,
    StopOrder INTEGER NOT NULL,
    StopType VARCHAR(30) NOT NULL CHECK (StopType IN ('Điểm khởi hành', 'Điểm dừng chân', 'Điểm đến')),
    StopName VARCHAR(200),
    IsPickupPoint BOOLEAN DEFAULT TRUE,
    IsDropoffPoint BOOLEAN DEFAULT TRUE,
    StopAddress TEXT,
    Latitude DECIMAL(10, 8),
    Longitude DECIMAL(11, 8),
    DistanceFromOrigin DECIMAL(10,2),
    EstimatedTime INTEGER,
    StopNote TEXT,
    FOREIGN KEY (RouteID) REFERENCES Routes(RouteID) ON DELETE CASCADE,
    FOREIGN KEY (LocationID) REFERENCES Locations(LocationID),
    UNIQUE (RouteID, StopOrder)
);

-- =============================================
-- 4. BẢNG QUẢN LÝ CHUYẾN XE
-- =============================================

-- Bảng chuyến xe (Trips)
CREATE TABLE Trips (
    TripID SERIAL PRIMARY KEY,
    RouteID INTEGER NOT NULL,
    VehicleID INTEGER NOT NULL,
    DriverID INTEGER NOT NULL,
    DepartureTime TIMESTAMP NOT NULL,
    ArrivalTime TIMESTAMP NOT NULL,
    BasePrice DECIMAL(15,2) NOT NULL,
    Status VARCHAR(20) DEFAULT 'Chờ' CHECK (Status IN ('Chờ', 'Đang chạy', 'Hoàn thành', 'Hủy', 'Trễ')),
    StatusNote TEXT,
    OnlineBookingCutoff INTEGER DEFAULT 60,
    IsFullyBooked BOOLEAN DEFAULT FALSE,
    MinPassengers INTEGER DEFAULT 1,
    AutoCancelIfNotEnough BOOLEAN DEFAULT FALSE,
    CreatedBy INTEGER,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (RouteID) REFERENCES Routes(RouteID),
    FOREIGN KEY (VehicleID) REFERENCES Vehicles(VehicleID),
    FOREIGN KEY (DriverID) REFERENCES Drivers(DriverID),
    FOREIGN KEY (CreatedBy) REFERENCES Users(UserID)
);

CREATE TRIGGER update_trips_updated_at
    BEFORE UPDATE ON Trips
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng sơ đồ ghế của chuyến xe (TripSeats)
CREATE TABLE TripSeats (
    SeatID SERIAL PRIMARY KEY,
    TripID INTEGER NOT NULL,
    SeatNumber VARCHAR(10) NOT NULL,
    FloorNumber INTEGER DEFAULT 1 CHECK (FloorNumber >= 1),
    SeatType VARCHAR(20) DEFAULT 'Thường' CHECK (SeatType IN ('Thường', 'VIP', 'Giường')),
    Status VARCHAR(20) DEFAULT 'Trống' CHECK (Status IN ('Trống', 'Đang giữ', 'Đã đặt')),
    HoldExpiry TIMESTAMP,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TripID) REFERENCES Trips(TripID) ON DELETE CASCADE,
    UNIQUE (TripID, SeatNumber)
);

-- =============================================
-- 5. BẢNG QUẢN LÝ ĐẶT VÉ VÀ THANH TOÁN
-- =============================================

-- Bảng đặt vé (Bookings)
CREATE TABLE Bookings (
    BookingID SERIAL PRIMARY KEY,
    BookingCode VARCHAR(20) UNIQUE NOT NULL,
    CustomerID INTEGER,
    CustomerName VARCHAR(100) NOT NULL,
    CustomerPhone VARCHAR(20) NOT NULL,
    CustomerEmail VARCHAR(100),
    TripID INTEGER NOT NULL,
    TotalAmount DECIMAL(15,2) NOT NULL,
    BookingStatus VARCHAR(30) DEFAULT 'Đang giữ' CHECK (BookingStatus IN ('Đang giữ', 'Đã thanh toán', 'Đã hủy', 'Đã hoàn thành')),
    BookingType VARCHAR(20) DEFAULT 'Online' CHECK (BookingType IN ('Online', 'Tại quầy')),
    IsGuestBooking BOOLEAN DEFAULT FALSE,
    GuestSessionID VARCHAR(100),
    InvitationSentAt TIMESTAMP,
    CreatedBy INTEGER,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID) ON DELETE SET NULL,
    FOREIGN KEY (TripID) REFERENCES Trips(TripID),
    FOREIGN KEY (CreatedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

CREATE TRIGGER update_bookings_updated_at
    BEFORE UPDATE ON Bookings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng vé (Tickets)
CREATE TABLE Tickets (
    TicketID SERIAL PRIMARY KEY,
    TicketCode VARCHAR(20) UNIQUE NOT NULL,
    BookingID INTEGER NOT NULL,
    SeatID INTEGER NOT NULL,
    Price DECIMAL(15,2) NOT NULL,
    TicketStatus VARCHAR(30) DEFAULT 'Chưa xác nhận' CHECK (TicketStatus IN ('Chưa xác nhận', 'Đã xác nhận', 'Đã sử dụng', 'Đã hủy', 'Hoàn tiền')),
    RequiresPassengerInfo BOOLEAN DEFAULT TRUE,
    PrintedBy INTEGER,
    PrintedAt TIMESTAMP,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID) ON DELETE CASCADE,
    FOREIGN KEY (SeatID) REFERENCES TripSeats(SeatID),
    FOREIGN KEY (PrintedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- Bảng hành khách (Passengers)
CREATE TABLE Passengers (
    PassengerID SERIAL PRIMARY KEY,
    TicketID INTEGER NOT NULL UNIQUE,
    FullName VARCHAR(100) NOT NULL DEFAULT 'Khách',
    PhoneNumber VARCHAR(20),
    Email VARCHAR(100),
    DateOfBirth DATE,
    PickupLocationID INTEGER,
    PickupAddress VARCHAR(200),
    DropoffLocationID INTEGER,
    DropoffAddress VARCHAR(200),
    SpecialNote TEXT,
    CheckInStatus VARCHAR(20) DEFAULT 'Chưa lên xe' CHECK (CheckInStatus IN ('Chưa lên xe', 'Đã lên xe', 'Đã xuống xe')),
    CheckInTime TIMESTAMP,
    CheckInMethod VARCHAR(30) NOT NULL DEFAULT 'QR',
    CheckOutTime TIMESTAMP,
    CheckedInBy INTEGER,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TicketID) REFERENCES Tickets(TicketID) ON DELETE CASCADE,
    FOREIGN KEY (PickupLocationID) REFERENCES RouteStops(StopID) ON DELETE SET NULL,
    FOREIGN KEY (DropoffLocationID) REFERENCES RouteStops(StopID) ON DELETE SET NULL,
    FOREIGN KEY (CheckedInBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

CREATE TRIGGER update_passengers_updated_at
    BEFORE UPDATE ON Passengers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng thanh toán (Payments)
CREATE TABLE Payments (
    PaymentID SERIAL PRIMARY KEY,
    BookingID INTEGER NOT NULL,
    Amount DECIMAL(15,2) NOT NULL,
    PaymentMethod VARCHAR(30) NOT NULL CHECK (PaymentMethod IN ('Tiền mặt', 'Chuyển khoản', 'Thẻ tín dụng', 'Ví điện tử')),
    PaymentStatus VARCHAR(30) DEFAULT 'Chờ xử lý' CHECK (PaymentStatus IN ('Chờ xử lý', 'Thành công', 'Thất bại', 'Đã hoàn tiền')),
    TransactionID VARCHAR(100),
    PaymentGateway VARCHAR(50),
    PaymentNote TEXT,
    PaidAt TIMESTAMP,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID) ON DELETE CASCADE
);

-- Bảng hoàn tiền (Refunds)
CREATE TABLE Refunds (
    RefundID SERIAL PRIMARY KEY,
    BookingID INTEGER NOT NULL,
    RefundAmount DECIMAL(15,2) NOT NULL,
    RefundReason TEXT,
    RefundType VARCHAR(30) DEFAULT 'Hủy toàn bộ' CHECK (RefundType IN ('Hủy toàn bộ', 'Hủy 1 vé', 'Đổi chuyến')),
    AffectedTicketIDs TEXT,
    NewTripID INTEGER,
    PriceDifference DECIMAL(15,2) DEFAULT 0,
    RefundStatus VARCHAR(30) DEFAULT 'Đang xử lý' CHECK (RefundStatus IN ('Đang xử lý', 'Đã hoàn tiền', 'Từ chối')),
    RefundMethod VARCHAR(30) NOT NULL CHECK (RefundMethod IN ('Chuyển khoản', 'Tiền mặt')),
    BankAccount VARCHAR(100),
    ProcessedBy INTEGER,
    ProcessedAt TIMESTAMP,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (BookingID) REFERENCES Bookings(BookingID) ON DELETE CASCADE,
    FOREIGN KEY (NewTripID) REFERENCES Trips(TripID) ON DELETE SET NULL,
    FOREIGN KEY (ProcessedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- Bảng lịch sử đổi vé (TicketChanges)
CREATE TABLE TicketChanges (
    ChangeID SERIAL PRIMARY KEY,
    TicketID INTEGER NOT NULL,
    OldTripID INTEGER NOT NULL,
    NewTripID INTEGER NOT NULL,
    OldSeatID INTEGER NOT NULL,
    NewSeatID INTEGER NOT NULL,
    OldPrice DECIMAL(15,2),
    NewPrice DECIMAL(15,2),
    PriceDifference DECIMAL(15,2),
    ChangeReason TEXT,
    ChangeFee DECIMAL(15,2) DEFAULT 0,
    ChangedBy INTEGER,
    ApprovedBy INTEGER,
    ChangeStatus VARCHAR(30) DEFAULT 'Chờ xử lý' CHECK (ChangeStatus IN ('Chờ xử lý', 'Đã duyệt', 'Từ chối')),
    ChangeDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TicketID) REFERENCES Tickets(TicketID) ON DELETE CASCADE,
    FOREIGN KEY (OldTripID) REFERENCES Trips(TripID),
    FOREIGN KEY (NewTripID) REFERENCES Trips(TripID),
    FOREIGN KEY (ChangedBy) REFERENCES Users(UserID) ON DELETE SET NULL,
    FOREIGN KEY (ApprovedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- =============================================
-- 6. BẢNG ĐÁNH GIÁ VÀ PHẢN HỒI
-- =============================================

-- Bảng đánh giá (Reviews)
CREATE TABLE Reviews (
    ReviewID SERIAL PRIMARY KEY,
    TripID INTEGER NOT NULL,
    CustomerID INTEGER NOT NULL,
    TicketID INTEGER NOT NULL,
    Rating INTEGER NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    Comment TEXT,
    DriverRating INTEGER CHECK (DriverRating BETWEEN 1 AND 5),
    VehicleRating INTEGER CHECK (VehicleRating BETWEEN 1 AND 5),
    ServiceRating INTEGER CHECK (ServiceRating BETWEEN 1 AND 5),
    PunctualityRating INTEGER CHECK (PunctualityRating BETWEEN 1 AND 5),
    Feedback TEXT,
    ReviewStatus VARCHAR(30) DEFAULT 'Đã duyệt' CHECK (ReviewStatus IN ('Chờ duyệt', 'Đã duyệt', 'Bị ẩn')),
    AdminResponse TEXT,
    RespondedBy INTEGER,
    RespondedAt TIMESTAMP,
    ReviewDate TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TripID) REFERENCES Trips(TripID) ON DELETE CASCADE,
    FOREIGN KEY (CustomerID) REFERENCES Users(UserID) ON DELETE CASCADE,
    FOREIGN KEY (TicketID) REFERENCES Tickets(TicketID) ON DELETE CASCADE,
    FOREIGN KEY (RespondedBy) REFERENCES Users(UserID) ON DELETE SET NULL,
    UNIQUE (TicketID, CustomerID)
);

-- =============================================
-- 8. BẢNG QUẢN LÝ CHI PHÍ VÀ BÁO CÁO
-- =============================================

-- Bảng chi phí chuyến (TripCosts)
CREATE TABLE TripCosts (
    CostID SERIAL PRIMARY KEY,
    TripID INTEGER NOT NULL UNIQUE,
    FuelCost DECIMAL(15,2) DEFAULT 0,
    TollFeeCost DECIMAL(15,2) DEFAULT 0,
    DriverSalary DECIMAL(15,2) DEFAULT 0,
    MaintenanceCost DECIMAL(15,2) DEFAULT 0,
    InsuranceCost DECIMAL(15,2) DEFAULT 0,
    ParkingCost DECIMAL(15,2) DEFAULT 0,
    ServiceCost DECIMAL(15,2) DEFAULT 0,
    OtherCosts DECIMAL(15,2) DEFAULT 0,
    Revenue DECIMAL(15,2) DEFAULT 0,
    CancelledRevenue DECIMAL(15,2) DEFAULT 0,
    ProfitMargin DECIMAL(5,2),
    CostNote TEXT,
    CalculatedBy INTEGER,
    CalculatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (TripID) REFERENCES Trips(TripID) ON DELETE CASCADE,
    FOREIGN KEY (CalculatedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- Ràng buộc CHECK sau khi tạo bảng
ALTER TABLE Bookings ADD CONSTRAINT chk_total_amount_positive CHECK (TotalAmount > 0);
ALTER TABLE Trips ADD CONSTRAINT chk_departure_before_arrival CHECK (DepartureTime < ArrivalTime);
ALTER TABLE Trips ADD CONSTRAINT chk_online_booking_cutoff CHECK (OnlineBookingCutoff > 0);
ALTER TABLE Routes ADD CONSTRAINT chk_distance_positive CHECK (Distance > 0);
ALTER TABLE Routes ADD CONSTRAINT chk_origin_destination_different CHECK (OriginID != DestinationID);
ALTER TABLE Tickets ADD CONSTRAINT chk_price_positive CHECK (Price > 0);
ALTER TABLE Payments ADD CONSTRAINT chk_payment_amount_positive CHECK (Amount > 0);
ALTER TABLE Refunds ADD CONSTRAINT chk_refund_amount_positive CHECK (RefundAmount > 0);
ALTER TABLE Users ADD CONSTRAINT chk_email_format CHECK (Email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$');
ALTER TABLE Users ADD CONSTRAINT chk_phone_format CHECK (PhoneNumber ~ '^[0-9]{10,20}$');

-- Thêm computed columns cho TripCosts
ALTER TABLE TripCosts ADD COLUMN TotalCost DECIMAL(15,2)
    GENERATED ALWAYS AS (
        FuelCost + TollFeeCost + DriverSalary + MaintenanceCost +
        InsuranceCost + ParkingCost + ServiceCost + OtherCosts
    ) STORED;

ALTER TABLE TripCosts ADD COLUMN NetRevenue DECIMAL(15,2)
    GENERATED ALWAYS AS (Revenue - CancelledRevenue) STORED;

ALTER TABLE TripCosts ADD COLUMN Profit DECIMAL(15,2)
    GENERATED ALWAYS AS (
        Revenue - CancelledRevenue -
        (FuelCost + TollFeeCost + DriverSalary + MaintenanceCost +
         InsuranceCost + ParkingCost + ServiceCost + OtherCosts)
    ) STORED;

CREATE TRIGGER update_tripcosts_updated_at
    BEFORE UPDATE ON TripCosts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Bảng theo dõi GPS (TripTracking)
CREATE TABLE TripTracking (
    TrackingID SERIAL PRIMARY KEY,
    TripID INTEGER NOT NULL,
    CurrentLatitude DECIMAL(10, 8),
    CurrentLongitude DECIMAL(11, 8),
    CurrentAddress VARCHAR(255),
    Speed DECIMAL(5,2),
    Direction VARCHAR(20),
    EstimatedArrival TIMESTAMP,
    DelayMinutes INTEGER DEFAULT 0,
    DelayReason TEXT,
    TrafficStatus VARCHAR(30) DEFAULT 'Bình thường'
        CHECK (TrafficStatus IN ('Bình thường', 'Kẹt xe nhẹ', 'Kẹt xe nặng', 'Tai nạn', 'Sửa đường', 'Khác')),
    RecordedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    RecordedBy INTEGER,
    DeviceInfo VARCHAR(100),
    FOREIGN KEY (TripID) REFERENCES Trips(TripID) ON DELETE CASCADE,
    FOREIGN KEY (RecordedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- Bảng nhật ký tài xế (DriverWorklog)
CREATE TABLE DriverWorklog (
    LogID SERIAL PRIMARY KEY,
    DriverID INTEGER NOT NULL,
    TripID INTEGER,
    WorkDate DATE NOT NULL,
    StartTime TIMESTAMP NOT NULL,
    EndTime TIMESTAMP,
    TotalHours DECIMAL(4,2),
    BreakTime DECIMAL(4,2) DEFAULT 0,
    TripCount INTEGER DEFAULT 0,
    TotalDistance DECIMAL(10,2) DEFAULT 0,
    SalaryType VARCHAR(30) DEFAULT 'Theo chuyến'
        CHECK (SalaryType IN ('Theo giờ', 'Theo chuyến', 'Cố định tháng')),
    SalaryAmount DECIMAL(15,2),
    BonusAmount DECIMAL(15,2) DEFAULT 0,
    PenaltyAmount DECIMAL(15,2) DEFAULT 0,
    Status VARCHAR(30) DEFAULT 'Đang làm việc'
        CHECK (Status IN ('Đang làm việc', 'Hoàn thành', 'Nghỉ giữa ca', 'Vắng mặt')),
    HasViolation BOOLEAN DEFAULT FALSE,
    ViolationType VARCHAR(50)
        CHECK (ViolationType IN ('Vượt 10h/ngày', 'Vượt 4h liên tục', 'Không nghỉ đủ', 'Khác')),
    ViolationNote TEXT,
    PerformanceRating INTEGER CHECK (PerformanceRating BETWEEN 1 AND 5),
    PerformanceNote TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (DriverID) REFERENCES Drivers(DriverID) ON DELETE CASCADE,
    FOREIGN KEY (TripID) REFERENCES Trips(TripID) ON DELETE SET NULL
);

-- Thêm computed column TotalSalary
ALTER TABLE DriverWorklog ADD COLUMN TotalSalary DECIMAL(15,2)
    GENERATED ALWAYS AS (
        COALESCE(SalaryAmount, 0) + COALESCE(BonusAmount, 0) - COALESCE(PenaltyAmount, 0)
    ) STORED;

CREATE TRIGGER update_driverworklog_updated_at
    BEFORE UPDATE ON DriverWorklog
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =============================================
-- 9. BẢNG AUDIT LOG
-- =============================================

-- Bảng nhật ký hoạt động (AuditLogs)
CREATE TABLE AuditLogs (
    LogID SERIAL PRIMARY KEY,
    UserID INTEGER,
    Action VARCHAR(100) NOT NULL,
    TableName VARCHAR(50),
    RecordID INTEGER,
    OldValue TEXT,
    NewValue TEXT,
    IPAddress VARCHAR(50),
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE SET NULL
);

-- =============================================
-- 10. THÊM DỮ LIỆU MẪU
-- =============================================

-- Thêm vai trò
INSERT INTO Roles (RoleName, Description) VALUES
('Admin', 'Quản trị viên hệ thống'),
('Nhân viên bán vé', 'Nhân viên bán vé tại quầy'),
('Tài xế', 'Tài xế điều khiển xe'),
('Khách hàng', 'Khách hàng sử dụng dịch vụ');

-- Thêm loại xe
INSERT INTO VehicleTypes (TypeName, TotalSeats, NumberOfFloors, Description) VALUES
('Limousine', 9, 1, 'Xe limousine cao cấp 9 chỗ'),
('Giường nằm', 40, 2, 'Xe giường nằm 40 chỗ 2 tầng'),
('Ghế ngồi', 45, 1, 'Xe ghế ngồi 45 chỗ');

-- Thêm admin mặc định (mật khẩu: Admin@123 - cần hash trong production)
INSERT INTO Users (FullName, Email, PhoneNumber, Password, RoleID, EmailVerified) VALUES
('Administrator', 'admin@busticket.com', '0900000000', '$2a$10$XYZ...', 1, TRUE);

-- =============================================
-- 11. TẠO INDEX ĐỂ TỐI ƯU HIỆU SUẤT
-- =============================================

CREATE INDEX idx_trips_search ON Trips(RouteID, DepartureTime, Status);
CREATE INDEX idx_trips_departure ON Trips(DepartureTime, Status);
CREATE INDEX idx_routes_locations ON Routes(OriginID, DestinationID);
CREATE INDEX idx_bookings_customer ON Bookings(CustomerID, BookingStatus);
CREATE INDEX idx_bookings_guest ON Bookings(IsGuestBooking, CreatedAt);
CREATE INDEX idx_tickets_booking ON Tickets(BookingID, TicketStatus);
CREATE INDEX idx_payments_booking ON Payments(BookingID, PaymentStatus);
CREATE INDEX idx_reviews_trip ON Reviews(TripID, Rating);
CREATE INDEX idx_passengers_checkin ON Passengers(CheckInStatus, CheckInTime);
CREATE INDEX idx_trip_costs_profit ON TripCosts(Profit, CalculatedAt);
CREATE INDEX idx_trip_tracking ON TripTracking(TripID, RecordedAt);
CREATE INDEX idx_driver_date ON DriverWorklog(DriverID, WorkDate);
CREATE INDEX idx_driver_status ON DriverWorklog(DriverID, Status);
CREATE INDEX idx_users_email ON Users(Email);
CREATE INDEX idx_users_phone ON Users(PhoneNumber);
CREATE INDEX idx_tripseats_status ON TripSeats(TripID, Status);
CREATE INDEX idx_bookings_code ON Bookings(BookingCode);
CREATE INDEX idx_tickets_code ON Tickets(TicketCode);
CREATE INDEX idx_refunds_booking ON Refunds(BookingID, RefundStatus);
CREATE INDEX idx_tickets_status ON Tickets(TicketStatus);
CREATE INDEX idx_trips_status ON Trips(Status);
CREATE INDEX idx_vehicles_status ON Vehicles(Status);
CREATE INDEX idx_users_role_status ON Users(RoleID, Status);
CREATE INDEX idx_routes_status ON Routes(Status);
CREATE INDEX idx_refunds_status ON Refunds(RefundStatus);
CREATE INDEX idx_payments_status_created ON Payments(PaymentStatus, CreatedAt);
CREATE INDEX idx_bookings_created ON Bookings(CreatedAt);
CREATE INDEX idx_bookings_trip_status ON Bookings(TripID, BookingStatus);
CREATE INDEX idx_tripseats_tripid ON TripSeats(TripID);
CREATE INDEX idx_tickets_seatid ON Tickets(SeatID);
CREATE INDEX idx_routestops_routeid ON RouteStops(RouteID);
CREATE INDEX idx_routestops_locationid ON RouteStops(LocationID);
CREATE INDEX idx_passengers_ticketid ON Passengers(TicketID);
CREATE INDEX idx_payments_method ON Payments(PaymentMethod, PaymentStatus);

-- =============================================
-- 12. TẠO VIEWS
-- =============================================

-- View: Thông tin chuyến xe đầy đủ
CREATE VIEW vw_TripDetails AS
SELECT
    t.TripID,
    t.DepartureTime,
    t.ArrivalTime,
    t.BasePrice,
    t.Status AS TripStatus,
    r.RouteName,
    ol.LocationName AS Origin,
    dl.LocationName AS Destination,
    r.Distance,
    v.LicensePlate,
    vt.TypeName AS VehicleType,
    vt.TotalSeats AS VehicleCapacity,
    vt.NumberOfFloors,
    u.FullName AS DriverName,
    COUNT(DISTINCT ts.SeatID) AS TotalSeatsCreated,
    COUNT(DISTINCT CASE WHEN ts.Status = 'Trống' THEN ts.SeatID END) AS AvailableSeats
FROM Trips t
JOIN Routes r ON t.RouteID = r.RouteID
JOIN Locations ol ON r.OriginID = ol.LocationID
JOIN Locations dl ON r.DestinationID = dl.LocationID
JOIN Vehicles v ON t.VehicleID = v.VehicleID
JOIN VehicleTypes vt ON v.TypeID = vt.TypeID
JOIN Drivers d ON t.DriverID = d.DriverID
JOIN Users u ON d.UserID = u.UserID
LEFT JOIN TripSeats ts ON t.TripID = ts.TripID
GROUP BY t.TripID, r.RouteName, ol.LocationName, dl.LocationName, r.Distance,
         v.LicensePlate, vt.TypeName, vt.TotalSeats, u.FullName, t.DepartureTime,
         t.ArrivalTime, t.BasePrice, t.Status, vt.NumberOfFloors;

-- View: Thống kê doanh thu theo ngày (đã COALESCE để tránh NULL)
CREATE VIEW vw_DailyRevenue AS
SELECT
    DATE(b.CreatedAt) AS RevenueDate,
    COUNT(DISTINCT b.BookingID) AS TotalBookings,
    COUNT(DISTINCT t.TicketID) AS TotalTickets,
    COALESCE(SUM(p.Amount), 0) AS TotalRevenue,
    COALESCE(SUM(CASE WHEN b.BookingType = 'Online' THEN p.Amount ELSE 0 END), 0) AS OnlineRevenue,
    COALESCE(SUM(CASE WHEN b.BookingType = 'Tại quầy' THEN p.Amount ELSE 0 END), 0) AS OfflineRevenue
FROM Bookings b
LEFT JOIN Payments p
    ON b.BookingID = p.BookingID AND p.PaymentStatus = 'Thành công'
LEFT JOIN Tickets t
    ON b.BookingID = t.BookingID
GROUP BY DATE(b.CreatedAt);

-- View: Danh sách hành khách theo chuyến (đã bỏ JOIN Trips thừa)
CREATE VIEW vw_PassengerManifest AS
SELECT
    tr.TripID,
    tr.DepartureTime,
    r.RouteName,
    b.BookingCode,
    b.CustomerName,
    b.CustomerPhone,
    tk.TicketCode,
    ts.SeatNumber,
    ts.FloorNumber,
    tk.TicketStatus,
    p.PassengerID,
    p.FullName AS PassengerName,
    p.PhoneNumber AS PassengerPhone,
    pickup.StopName AS PickupStop,
    p.PickupAddress,
    dropoff.StopName AS DropoffStop,
    p.DropoffAddress,
    p.CheckInStatus,
    p.CheckInTime,
    p.SpecialNote
FROM Tickets tk
JOIN Bookings b ON tk.BookingID = b.BookingID
JOIN TripSeats ts ON tk.SeatID = ts.SeatID
JOIN Trips tr ON ts.TripID = tr.TripID
JOIN Routes r ON tr.RouteID = r.RouteID
LEFT JOIN Passengers p ON tk.TicketID = p.TicketID
LEFT JOIN RouteStops pickup ON p.PickupLocationID = pickup.StopID
LEFT JOIN RouteStops dropoff ON p.DropoffLocationID = dropoff.StopID
WHERE tk.TicketStatus != 'Đã hủy'
ORDER BY tr.TripID, ts.SeatNumber;

-- View: Đề xuất giá vé (đã tránh chia cho 0)
CREATE VIEW vw_PriceSuggestion AS
SELECT
    t.TripID,
    t.DepartureTime,
    r.RouteName,
    v.LicensePlate,
    vt.TypeName AS VehicleType,
    COALESCE(tc.TotalCost, 0) AS TotalCost,
    COUNT(ts.SeatID) AS TotalSeats,
    COUNT(CASE WHEN ts.Status = 'Đã đặt' THEN 1 END) AS OccupiedSeats,
    COUNT(CASE WHEN ts.Status = 'Trống' THEN 1 END) AS AvailableSeats,
    t.BasePrice AS CurrentPrice,
    CASE
        WHEN tc.TotalCost > 0 AND COUNT(ts.SeatID) > 0
            THEN CEIL(tc.TotalCost / COUNT(ts.SeatID))
        ELSE 0
    END AS MinPriceToBreakEven,
    CASE
        WHEN tc.TotalCost > 0 AND COUNT(ts.SeatID) > 0
            THEN CEIL(tc.TotalCost * 1.2 / COUNT(ts.SeatID))
        ELSE t.BasePrice
    END AS SuggestedPrice20Percent,
    ROUND(
        COUNT(CASE WHEN ts.Status = 'Đã đặt' THEN 1 END) * 100.0
        / NULLIF(COUNT(ts.SeatID), 0), 2
    ) AS OccupancyRate,
    COALESCE(
        SUM(CASE WHEN tk.TicketStatus IN ('Đã xác nhận', 'Đã sử dụng') THEN tk.Price ELSE 0 END),
        0
    ) AS CurrentRevenue
FROM Trips t
JOIN Routes r ON t.RouteID = r.RouteID
JOIN Vehicles v ON t.VehicleID = v.VehicleID
JOIN VehicleTypes vt ON v.TypeID = vt.TypeID
LEFT JOIN TripCosts tc ON t.TripID = tc.TripID
LEFT JOIN TripSeats ts ON t.TripID = ts.TripID
LEFT JOIN Tickets tk ON ts.SeatID = tk.SeatID
GROUP BY t.TripID, r.RouteName, v.LicensePlate, vt.TypeName, tc.TotalCost, t.BasePrice, t.DepartureTime;

-- View: Hiệu suất tài xế
CREATE VIEW vw_DriverPerformance AS
SELECT
    d.DriverID,
    u.FullName AS DriverName,
    u.PhoneNumber,
    COUNT(DISTINCT t.TripID) AS TotalTrips,
    COUNT(DISTINCT CASE WHEN t.Status = 'Hoàn thành' THEN t.TripID END) AS CompletedTrips,
    COUNT(DISTINCT CASE WHEN t.Status = 'Hủy' THEN t.TripID END) AS CancelledTrips,
    COUNT(DISTINCT CASE WHEN tt.DelayMinutes > 0 THEN tt.TripID END) AS TotalDelays,
    ROUND(AVG(CASE WHEN tt.DelayMinutes > 0 THEN tt.DelayMinutes END)) AS AvgDelayMinutes,
    AVG(r.DriverRating) AS AvgDriverRating,
    COUNT(r.ReviewID) AS TotalReviews,
    SUM(dw.TotalHours) AS TotalWorkHours,
    COUNT(DISTINCT dw.WorkDate) AS TotalWorkDays,
    SUM(CASE WHEN dw.HasViolation = TRUE THEN 1 ELSE 0 END) AS TotalViolations,
    SUM(dw.TotalSalary) AS TotalSalary
FROM Drivers d
JOIN Users u ON d.UserID = u.UserID
LEFT JOIN Trips t ON d.DriverID = t.DriverID
LEFT JOIN Reviews r ON t.TripID = r.TripID
LEFT JOIN DriverWorklog dw ON d.DriverID = dw.DriverID
LEFT JOIN TripTracking tt ON t.TripID = tt.TripID
GROUP BY d.DriverID, u.FullName, u.PhoneNumber;

-- =============================================
-- 13. TẠO FUNCTIONS & PROCEDURES
-- =============================================

-- Function: Tạo ghế tự động cho chuyến xe
CREATE OR REPLACE FUNCTION sp_GenerateSeatsForTrip(p_TripID INTEGER)
RETURNS VOID AS $$
DECLARE
    v_VehicleID INTEGER;
    v_TypeID INTEGER;
    v_TotalSeats INTEGER;
    v_Floors INTEGER;
    v_SeatsPerFloor INTEGER;
    v_Remainder INTEGER;
    v_Floor INTEGER := 1;
    v_SeatCounter INTEGER;
    v_SeatLimit INTEGER;
    v_RowLetter CHAR(1);
BEGIN
    -- Lấy thông tin xe
    SELECT VehicleID INTO v_VehicleID
    FROM Trips WHERE TripID = p_TripID;

    SELECT TypeID INTO v_TypeID
    FROM Vehicles WHERE VehicleID = v_VehicleID;

    SELECT TotalSeats, NumberOfFloors
    INTO v_TotalSeats, v_Floors
    FROM VehicleTypes WHERE TypeID = v_TypeID;

    v_SeatsPerFloor := v_TotalSeats / v_Floors;
    v_Remainder := v_TotalSeats % v_Floors;

    WHILE v_Floor <= v_Floors LOOP
        IF v_Floor = v_Floors THEN
            v_SeatLimit := v_SeatsPerFloor + v_Remainder;
        ELSE
            v_SeatLimit := v_SeatsPerFloor;
        END IF;

        v_SeatCounter := 1;
        v_RowLetter := CHR(64 + v_Floor); -- 1->A, 2->B, ...

        WHILE v_SeatCounter <= v_SeatLimit LOOP
            INSERT INTO TripSeats (TripID, SeatNumber, FloorNumber, Status)
            VALUES (
                p_TripID,
                v_RowLetter || LPAD(v_SeatCounter::TEXT, 2, '0'),
                v_Floor,
                'Trống'
            );
            v_SeatCounter := v_SeatCounter + 1;
        END LOOP;

        v_Floor := v_Floor + 1;
    END LOOP;
END;
$$ LANGUAGE plpgsql;

-- Function: Kiểm tra thời gian đặt vé
CREATE OR REPLACE FUNCTION sp_CheckBookingEligibility(
    p_TripID INTEGER,
    p_BookingType VARCHAR(20),
    OUT p_CanBook BOOLEAN,
    OUT p_Message VARCHAR(255)
)
AS $$
DECLARE
    v_DepartureTime TIMESTAMP;
    v_OnlineCutoff INTEGER;
    v_TimeDiff INTEGER;
    v_IsFullyBooked BOOLEAN;
BEGIN
    SELECT DepartureTime, OnlineBookingCutoff, IsFullyBooked
    INTO v_DepartureTime, v_OnlineCutoff, v_IsFullyBooked
    FROM Trips WHERE TripID = p_TripID;

    IF v_IsFullyBooked THEN
        p_CanBook := FALSE;
        p_Message := 'Chuyến xe đã hết chỗ';
    ELSE
        v_TimeDiff := EXTRACT(EPOCH FROM (v_DepartureTime - CURRENT_TIMESTAMP)) / 60;

        IF p_BookingType = 'Online' AND v_TimeDiff < v_OnlineCutoff THEN
            p_CanBook := FALSE;
            p_Message := 'Không thể đặt vé online. Còn ' || v_TimeDiff || ' phút tới giờ khởi hành. Vui lòng đặt tại quầy.';
        ELSIF v_TimeDiff < 0 THEN
            p_CanBook := FALSE;
            p_Message := 'Chuyến xe đã khởi hành';
        ELSE
            p_CanBook := TRUE;
            p_Message := 'Có thể đặt vé';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function: Hủy vé và hoàn tiền (đã fix lỗi concat numeric)
CREATE OR REPLACE FUNCTION sp_CancelTicket(
    p_BookingID INTEGER,
    p_RefundReason TEXT
)
RETURNS TABLE(Success BOOLEAN, Message VARCHAR(255)) AS $$
DECLARE
    v_DepartureTime TIMESTAMP;
    v_TimeDiff INTEGER;
    v_RefundAmount DECIMAL(15,2);
    v_TotalAmount DECIMAL(15,2);
BEGIN
    -- Lấy thời gian khởi hành
    SELECT t.DepartureTime INTO v_DepartureTime
    FROM Bookings b
    JOIN Trips t ON b.TripID = t.TripID
    WHERE b.BookingID = p_BookingID;

    -- Kiểm tra thời gian hủy (giờ)
    v_TimeDiff := EXTRACT(EPOCH FROM (v_DepartureTime - CURRENT_TIMESTAMP)) / 3600;

    IF v_TimeDiff < 2 THEN
        RETURN QUERY
        SELECT FALSE, 'Không thể hủy vé. Phải hủy trước giờ khởi hành tối thiểu 2 giờ.'::VARCHAR(255);
    ELSE
        SELECT TotalAmount INTO v_TotalAmount
        FROM Bookings WHERE BookingID = p_BookingID;

        IF v_TimeDiff >= 4 THEN
            v_RefundAmount := v_TotalAmount * 0.9;
        ELSE
            v_RefundAmount := v_TotalAmount * 0.5;
        END IF;

        -- Cập nhật trạng thái
        UPDATE Bookings
        SET BookingStatus = 'Đã hủy', UpdatedAt = CURRENT_TIMESTAMP
        WHERE BookingID = p_BookingID;

        UPDATE Tickets
        SET TicketStatus = 'Đã hủy'
        WHERE BookingID = p_BookingID;

        -- Giải phóng ghế
        UPDATE TripSeats ts
        SET Status = 'Trống', HoldExpiry = NULL
        FROM Tickets tk
        WHERE ts.SeatID = tk.SeatID
          AND tk.BookingID = p_BookingID;

        -- Cập nhật payment status nếu đã thanh toán
        UPDATE Payments
        SET PaymentStatus = 'Đã hoàn tiền'
        WHERE BookingID = p_BookingID AND PaymentStatus = 'Thành công';

        -- Tạo yêu cầu hoàn tiền
        INSERT INTO Refunds (BookingID, RefundAmount, RefundReason, RefundMethod, RefundStatus)
        VALUES (p_BookingID, v_RefundAmount, p_RefundReason, 'Chuyển khoản', 'Đang xử lý');

        RETURN QUERY
        SELECT TRUE,
               ('Hủy vé thành công. Số tiền hoàn: ' || v_RefundAmount::TEXT || ' VNĐ')::VARCHAR(255);
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Function: Giải phóng ghế hết hạn
CREATE OR REPLACE FUNCTION sp_ReleaseExpiredSeats()
RETURNS VOID AS $$
BEGIN
    UPDATE TripSeats
    SET Status = 'Trống', HoldExpiry = NULL
    WHERE Status = 'Đang giữ'
      AND HoldExpiry IS NOT NULL
      AND HoldExpiry < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

-- Function: Check-in hành khách
CREATE OR REPLACE FUNCTION sp_CheckInPassenger(
    p_TicketCode VARCHAR(20),
    p_CheckInMethod VARCHAR(30),
    p_CheckedInBy INTEGER,
    OUT p_Success BOOLEAN,
    OUT p_Message VARCHAR(255)
)
AS $$
DECLARE
    v_TicketID INTEGER;
    v_PassengerID INTEGER;
    v_CurrentStatus VARCHAR(20);
    v_TripStatus VARCHAR(20);
BEGIN
    SELECT TicketID INTO v_TicketID
    FROM Tickets WHERE TicketCode = p_TicketCode;

    IF v_TicketID IS NULL THEN
        p_Success := FALSE;
        p_Message := 'Không tìm thấy vé';
    ELSE
        SELECT p.PassengerID, p.CheckInStatus, tr.Status
        INTO v_PassengerID, v_CurrentStatus, v_TripStatus
        FROM Passengers p
        JOIN Tickets tk ON p.TicketID = tk.TicketID
        JOIN TripSeats ts ON tk.SeatID = ts.SeatID
        JOIN Trips tr ON ts.TripID = tr.TripID
        WHERE p.TicketID = v_TicketID;

        IF v_PassengerID IS NULL THEN
            p_Success := FALSE;
            p_Message := 'Không tìm thấy hành khách';
        ELSIF v_CurrentStatus = 'Đã lên xe' THEN
            p_Success := FALSE;
            p_Message := 'Hành khách đã check-in rồi';
        ELSIF v_TripStatus NOT IN ('Chờ', 'Đang chạy') THEN
            p_Success := FALSE;
            p_Message := 'Không thể check-in. Trạng thái chuyến: ' || v_TripStatus;
        ELSE
            UPDATE Passengers
            SET
                CheckInStatus = 'Đã lên xe',
                CheckInTime = CURRENT_TIMESTAMP,
                CheckInMethod = p_CheckInMethod,
                CheckedInBy = p_CheckedInBy
            WHERE PassengerID = v_PassengerID;

            p_Success := TRUE;
            p_Message := 'Check-in thành công';
        END IF;
    END IF;
END;
$$ LANGUAGE plpgsql;

-- =============================================
-- 14. TẠO TRIGGERS
-- =============================================

-- Trigger: Tự động tạo mã booking
CREATE OR REPLACE FUNCTION trg_GenerateBookingCode()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.BookingCode IS NULL OR NEW.BookingCode = '' THEN
        NEW.BookingCode :=
            'BK' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') ||
            LPAD(FLOOR(RANDOM() * 99999)::TEXT, 5, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_booking_code
    BEFORE INSERT ON Bookings
    FOR EACH ROW
    EXECUTE FUNCTION trg_GenerateBookingCode();

-- Trigger: Tự động tạo mã vé
CREATE OR REPLACE FUNCTION trg_GenerateTicketCode()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.TicketCode IS NULL OR NEW.TicketCode = '' THEN
        NEW.TicketCode :=
            'TK' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') ||
            LPAD(FLOOR(RANDOM() * 99999)::TEXT, 5, '0');
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_generate_ticket_code
    BEFORE INSERT ON Tickets
    FOR EACH ROW
    EXECUTE FUNCTION trg_GenerateTicketCode();

-- Trigger: Cập nhật trạng thái ghế khi tạo vé
CREATE OR REPLACE FUNCTION trg_UpdateSeatStatus()
RETURNS TRIGGER AS $$
BEGIN
    UPDATE TripSeats
    SET Status = 'Đã đặt'
    WHERE SeatID = NEW.SeatID;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_seat_status
    AFTER INSERT ON Tickets
    FOR EACH ROW
    EXECUTE FUNCTION trg_UpdateSeatStatus();

-- Trigger: Tự động tạo Passenger khi tạo Ticket
CREATE OR REPLACE FUNCTION trg_CreatePassengerOnTicket()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO Passengers (TicketID, FullName, PhoneNumber, Email)
    SELECT
        NEW.TicketID,
        b.CustomerName,
        b.CustomerPhone,
        b.CustomerEmail
    FROM Bookings b
    WHERE b.BookingID = NEW.BookingID;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_create_passenger_on_ticket
    AFTER INSERT ON Tickets
    FOR EACH ROW
    EXECUTE FUNCTION trg_CreatePassengerOnTicket();

-- Trigger: Cập nhật IsFullyBooked theo trạng thái ghế
CREATE OR REPLACE FUNCTION trg_UpdateTripFullStatus()
RETURNS TRIGGER AS $$
DECLARE
    v_TotalSeats INTEGER;
    v_BookedSeats INTEGER;
BEGIN
    IF NEW.Status IS DISTINCT FROM OLD.Status THEN
        SELECT COUNT(*) INTO v_TotalSeats
        FROM TripSeats WHERE TripID = NEW.TripID;

        SELECT COUNT(*) INTO v_BookedSeats
        FROM TripSeats WHERE TripID = NEW.TripID AND Status = 'Đã đặt';

        IF v_BookedSeats >= v_TotalSeats AND v_TotalSeats > 0 THEN
            UPDATE Trips SET IsFullyBooked = TRUE WHERE TripID = NEW.TripID;
        ELSE
            UPDATE Trips SET IsFullyBooked = FALSE WHERE TripID = NEW.TripID;
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_update_trip_full_status
    AFTER UPDATE ON TripSeats
    FOR EACH ROW
    EXECUTE FUNCTION trg_UpdateTripFullStatus();

-- =============================================
-- KẾT THÚC SCRIPT
-- =============================================
