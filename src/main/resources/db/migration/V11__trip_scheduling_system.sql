-- V11: Trip Scheduling System - Driver/Vehicle Route Assignments & Trip Templates
-- Author: GitHub Copilot
-- Date: 2026-01-15
-- Description: Hệ thống tạo lịch trip tự động với assignment và template

-- =====================================================
-- 1. DRIVER ROUTE ASSIGNMENTS
-- =====================================================
CREATE TABLE IF NOT EXISTS DriverRouteAssignments (
    AssignmentID SERIAL PRIMARY KEY,
    DriverID INT NOT NULL,
    RouteID INT NOT NULL,
    PreferredRole VARCHAR(20) DEFAULT 'Main' CHECK (PreferredRole IN ('Main', 'SubDriver')),
    Priority INT DEFAULT 1 CHECK (Priority >= 1 AND Priority <= 10),
    IsActive BOOLEAN DEFAULT TRUE,
    StartDate DATE,
    EndDate DATE,
    Notes TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_driver_assignment FOREIGN KEY (DriverID) REFERENCES Drivers(DriverID) ON DELETE CASCADE,
    CONSTRAINT fk_route_assignment FOREIGN KEY (RouteID) REFERENCES Routes(RouteID) ON DELETE CASCADE,
    
    -- Unique constraint: 1 driver không thể assign vào 1 route với cùng preferred role 2 lần
    CONSTRAINT uq_driver_route_role UNIQUE (DriverID, RouteID, PreferredRole),
    
    -- Business rule: EndDate phải sau StartDate
    CONSTRAINT chk_date_range CHECK (EndDate IS NULL OR EndDate >= StartDate)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_driver_route_active ON DriverRouteAssignments(DriverID, IsActive) WHERE IsActive = TRUE;
CREATE INDEX IF NOT EXISTS idx_route_driver_active ON DriverRouteAssignments(RouteID, IsActive) WHERE IsActive = TRUE;
CREATE INDEX IF NOT EXISTS idx_assignment_date_range ON DriverRouteAssignments(StartDate, EndDate);
CREATE INDEX IF NOT EXISTS idx_assignment_priority ON DriverRouteAssignments(RouteID, Priority, IsActive);

-- Trigger: Auto update UpdatedAt
DROP TRIGGER IF EXISTS trigger_driver_assignment_updated ON DriverRouteAssignments;
CREATE TRIGGER trigger_driver_assignment_updated
BEFORE UPDATE ON DriverRouteAssignments
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE DriverRouteAssignments IS 'Phân công tài xế vào các tuyến đường cụ thể';
COMMENT ON COLUMN DriverRouteAssignments.PreferredRole IS 'Main = tài xế chính, SubDriver = phụ xe';
COMMENT ON COLUMN DriverRouteAssignments.Priority IS 'Độ ưu tiên (1 = cao nhất, 10 = thấp nhất)';
COMMENT ON COLUMN DriverRouteAssignments.IsActive IS 'Trạng thái kích hoạt (TRUE = đang hoạt động)';

-- =====================================================
-- 2. VEHICLE ROUTE ASSIGNMENTS
-- =====================================================
CREATE TABLE IF NOT EXISTS VehicleRouteAssignments (
    AssignmentID SERIAL PRIMARY KEY,
    VehicleID INT NOT NULL,
    RouteID INT NOT NULL,
    Priority INT DEFAULT 1 CHECK (Priority >= 1 AND Priority <= 10),
    IsActive BOOLEAN DEFAULT TRUE,
    StartDate DATE,
    EndDate DATE,
    MaintenanceSchedule VARCHAR(50) CHECK (MaintenanceSchedule IN ('Weekly', 'Bi-weekly', 'Monthly', 'Quarterly', NULL)),
    LastMaintenanceDate DATE,
    NextMaintenanceDate DATE,
    Notes TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_vehicle_assignment FOREIGN KEY (VehicleID) REFERENCES Vehicles(VehicleID) ON DELETE CASCADE,
    CONSTRAINT fk_route_vehicle_assignment FOREIGN KEY (RouteID) REFERENCES Routes(RouteID) ON DELETE CASCADE,
    
    -- Unique constraint: 1 vehicle chỉ assign vào 1 route 1 lần
    CONSTRAINT uq_vehicle_route UNIQUE (VehicleID, RouteID),
    
    -- Business rule: EndDate phải sau StartDate
    CONSTRAINT chk_vehicle_date_range CHECK (EndDate IS NULL OR EndDate >= StartDate)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_vehicle_route_active ON VehicleRouteAssignments(VehicleID, IsActive) WHERE IsActive = TRUE;
CREATE INDEX IF NOT EXISTS idx_route_vehicle_active ON VehicleRouteAssignments(RouteID, IsActive) WHERE IsActive = TRUE;
CREATE INDEX IF NOT EXISTS idx_vehicle_assignment_date_range ON VehicleRouteAssignments(StartDate, EndDate);
CREATE INDEX IF NOT EXISTS idx_vehicle_assignment_priority ON VehicleRouteAssignments(RouteID, Priority, IsActive);
CREATE INDEX IF NOT EXISTS idx_vehicle_maintenance ON VehicleRouteAssignments(NextMaintenanceDate) WHERE IsActive = TRUE;

-- Trigger: Auto update UpdatedAt
DROP TRIGGER IF EXISTS trigger_vehicle_assignment_updated ON VehicleRouteAssignments;
CREATE TRIGGER trigger_vehicle_assignment_updated
BEFORE UPDATE ON VehicleRouteAssignments
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE VehicleRouteAssignments IS 'Phân công xe vào các tuyến đường cụ thể';
COMMENT ON COLUMN VehicleRouteAssignments.MaintenanceSchedule IS 'Lịch bảo trì định kỳ';
COMMENT ON COLUMN VehicleRouteAssignments.Priority IS 'Độ ưu tiên sử dụng xe trên tuyến (1 = cao nhất)';

-- =====================================================
-- 3. TRIP TEMPLATES
-- =====================================================
CREATE TABLE IF NOT EXISTS TripTemplates (
    TemplateID SERIAL PRIMARY KEY,
    TemplateName VARCHAR(100) NOT NULL,
    RouteID INT NOT NULL,
    DepartureTime TIME NOT NULL,
    DaysOfWeek VARCHAR(50) NOT NULL, -- Format: 'Mon,Tue,Wed,Thu,Fri' hoặc 'Daily' hoặc 'Weekends'
    BasePrice DECIMAL(10, 2) NOT NULL CHECK (BasePrice > 0),
    OnlineBookingCutoff INT DEFAULT 60 CHECK (OnlineBookingCutoff >= 0),
    MinPassengers INT DEFAULT 1 CHECK (MinPassengers >= 0),
    MaxPassengers INT DEFAULT 40 CHECK (MaxPassengers >= MinPassengers),
    
    -- ========== ROUND-TRIP & INTERVAL CONFIGURATION ==========
    GenerateRoundTrip BOOLEAN DEFAULT FALSE NOT NULL, -- TRUE = Tạo cả chuyến đi và về
    IntervalMinutes INT DEFAULT 0 CHECK (IntervalMinutes >= 0), -- Khoảng cách giữa các chuyến (phút)
    TripsPerDay INT DEFAULT 1 CHECK (TripsPerDay >= 1 AND TripsPerDay <= 20), -- Số chuyến mỗi ngày
    MaxGenerationDays INT DEFAULT 31 CHECK (MaxGenerationDays >= 1 AND MaxGenerationDays <= 365), -- Max ngày có thể generate
    AutoAssignDriver BOOLEAN DEFAULT TRUE, -- Tự động gán driver khi generate
    AutoAssignVehicle BOOLEAN DEFAULT TRUE, -- Tự động gán vehicle khi generate
    
    AutoCancelIfNotEnough BOOLEAN DEFAULT FALSE,
    IsActive BOOLEAN DEFAULT TRUE,
    EffectiveFrom DATE NOT NULL,
    EffectiveTo DATE,
    Notes TEXT,
    CreatedBy INT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_template_route FOREIGN KEY (RouteID) REFERENCES Routes(RouteID) ON DELETE CASCADE,
    CONSTRAINT fk_template_creator FOREIGN KEY (CreatedBy) REFERENCES Users(UserID) ON DELETE SET NULL,
    
    -- Business rule: EffectiveTo phải sau EffectiveFrom
    CONSTRAINT chk_template_effective_range CHECK (EffectiveTo IS NULL OR EffectiveTo >= EffectiveFrom),
    
    -- Unique constraint: Không được có 2 template active cùng route + departure time + days
    CONSTRAINT uq_template_route_time UNIQUE (RouteID, DepartureTime, DaysOfWeek, EffectiveFrom)
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_template_route_active ON TripTemplates(RouteID, IsActive) WHERE IsActive = TRUE;
CREATE INDEX IF NOT EXISTS idx_template_effective ON TripTemplates(EffectiveFrom, EffectiveTo);
CREATE INDEX IF NOT EXISTS idx_template_departure ON TripTemplates(DepartureTime);
CREATE INDEX IF NOT EXISTS idx_template_days ON TripTemplates(DaysOfWeek);

-- Trigger: Auto update UpdatedAt
DROP TRIGGER IF EXISTS trigger_trip_template_updated ON TripTemplates;
CREATE TRIGGER trigger_trip_template_updated
BEFORE UPDATE ON TripTemplates
FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE TripTemplates IS 'Mẫu tạo chuyến xe tự động theo lịch định kỳ với hỗ trợ round-trip và interval scheduling';
COMMENT ON COLUMN TripTemplates.DaysOfWeek IS 'Các ngày trong tuần áp dụng: Mon,Tue,Wed,Thu,Fri hoặc Daily hoặc Mon,Wed,Fri';
COMMENT ON COLUMN TripTemplates.OnlineBookingCutoff IS 'Thời gian ngưng đặt vé online trước giờ khởi hành (phút)';
COMMENT ON COLUMN TripTemplates.GenerateRoundTrip IS 'TRUE = Tạo cả chuyến đi và về (xe quay trở lại điểm xuất phát)';
COMMENT ON COLUMN TripTemplates.IntervalMinutes IS 'Khoảng cách giữa các chuyến (0 = chỉ 1 chuyến/ngày)';
COMMENT ON COLUMN TripTemplates.TripsPerDay IS 'Số chuyến xuất phát mỗi ngày (không tính round-trip)';
COMMENT ON COLUMN TripTemplates.MaxGenerationDays IS 'Giới hạn số ngày có thể generate (default 31 = 1 tháng)';
COMMENT ON COLUMN TripTemplates.EffectiveFrom IS 'Ngày bắt đầu áp dụng template';
COMMENT ON COLUMN TripTemplates.EffectiveTo IS 'Ngày kết thúc (NULL = vô thời hạn)';


-- =====================================================
-- 4. TRIP GENERATION LOG (Optional - for tracking)
-- =====================================================
CREATE TABLE IF NOT EXISTS TripGenerationLogs (
    LogID SERIAL PRIMARY KEY,
    TemplateID INT NOT NULL,
    GeneratedBy INT,
    StartDate DATE NOT NULL,
    EndDate DATE NOT NULL,
    TotalTripsCreated INT DEFAULT 0,
    TotalTripsSkipped INT DEFAULT 0,
    SkipReasons TEXT, -- JSON format: [{"date": "2026-01-15", "reason": "No available driver"}]
    ExecutionTime INT, -- Milliseconds
    Status VARCHAR(20) DEFAULT 'Success' CHECK (Status IN ('Success', 'Partial', 'Failed')),
    ErrorMessage TEXT,
    CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Keys
    CONSTRAINT fk_generation_template FOREIGN KEY (TemplateID) REFERENCES TripTemplates(TemplateID) ON DELETE CASCADE,
    CONSTRAINT fk_generation_user FOREIGN KEY (GeneratedBy) REFERENCES Users(UserID) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_generation_template ON TripGenerationLogs(TemplateID);
CREATE INDEX IF NOT EXISTS idx_generation_date_range ON TripGenerationLogs(StartDate, EndDate);
CREATE INDEX IF NOT EXISTS idx_generation_status ON TripGenerationLogs(Status);

COMMENT ON TABLE TripGenerationLogs IS 'Nhật ký tạo chuyến tự động từ template (audit trail)';
COMMENT ON COLUMN TripGenerationLogs.SkipReasons IS 'JSON array chứa các ngày bị skip và lý do';

-- =====================================================
-- 5. HELPER VIEWS
-- =====================================================

-- View: Active Driver Assignments by Route
CREATE OR REPLACE VIEW vw_ActiveDriverAssignments AS
SELECT 
    dra.AssignmentID,
    dra.DriverID,
    d.DriverLicense,
    u.FullName AS DriverName,
    dra.RouteID,
    r.RouteName,
    dra.PreferredRole,
    dra.Priority,
    dra.StartDate,
    dra.EndDate,
    dra.IsActive
FROM DriverRouteAssignments dra
JOIN Drivers d ON dra.DriverID = d.DriverID
JOIN Users u ON d.UserID = u.UserID
JOIN Routes r ON dra.RouteID = r.RouteID
WHERE dra.IsActive = TRUE
    AND (dra.StartDate IS NULL OR dra.StartDate <= CURRENT_DATE)
    AND (dra.EndDate IS NULL OR dra.EndDate >= CURRENT_DATE);

COMMENT ON VIEW vw_ActiveDriverAssignments IS 'Danh sách assignment tài xế đang active';

-- View: Active Vehicle Assignments by Route
CREATE OR REPLACE VIEW vw_ActiveVehicleAssignments AS
SELECT 
    vra.AssignmentID,
    vra.VehicleID,
    v.LicensePlate,
    vt.TypeName AS VehicleType,
    vra.RouteID,
    r.RouteName,
    vra.Priority,
    vra.StartDate,
    vra.EndDate,
    vra.MaintenanceSchedule,
    vra.NextMaintenanceDate,
    vra.IsActive
FROM VehicleRouteAssignments vra
JOIN Vehicles v ON vra.VehicleID = v.VehicleID
JOIN VehicleTypes vt ON v.TypeID = vt.TypeID
JOIN Routes r ON vra.RouteID = r.RouteID
WHERE vra.IsActive = TRUE
    AND (vra.StartDate IS NULL OR vra.StartDate <= CURRENT_DATE)
    AND (vra.EndDate IS NULL OR vra.EndDate >= CURRENT_DATE);

COMMENT ON VIEW vw_ActiveVehicleAssignments IS 'Danh sách assignment xe đang active';

-- View: Active Templates by Route
CREATE OR REPLACE VIEW vw_ActiveTripTemplates AS
SELECT 
    tt.TemplateID,
    tt.TemplateName,
    tt.RouteID,
    r.RouteName,
    r.Distance,
    r.EstimatedDuration,
    tt.DepartureTime,
    tt.DaysOfWeek,
    tt.BasePrice,
    tt.EffectiveFrom,
    tt.EffectiveTo,
    tt.IsActive
FROM TripTemplates tt
JOIN Routes r ON tt.RouteID = r.RouteID
WHERE tt.IsActive = TRUE
    AND tt.EffectiveFrom <= CURRENT_DATE
    AND (tt.EffectiveTo IS NULL OR tt.EffectiveTo >= CURRENT_DATE);

COMMENT ON VIEW vw_ActiveTripTemplates IS 'Danh sách template đang active và trong hiệu lực';

-- =====================================================
-- 6. SAMPLE DATA (Optional - for testing)
-- =====================================================

-- Insert sample driver assignments (if routes and drivers exist)
-- COMMENTED OUT: Uncomment khi đã có dữ liệu Drivers và Routes
-- INSERT INTO DriverRouteAssignments (DriverID, RouteID, PreferredRole, Priority, StartDate, Notes)
-- VALUES 
--     (1, 1, 'Main', 1, '2026-01-01', 'Tài xế có kinh nghiệm 5 năm tuyến Hà Nội - Đà Nẵng'),
--     (2, 1, 'Main', 2, '2026-01-01', 'Tài xế backup cho tuyến Hà Nội - Đà Nẵng'),
--     (3, 1, 'SubDriver', 1, '2026-01-01', 'Phụ xe chính cho tuyến Hà Nội - Đà Nẵng');

-- Insert sample vehicle assignments
-- COMMENTED OUT: Uncomment khi đã có dữ liệu Vehicles và Routes
-- INSERT INTO VehicleRouteAssignments (VehicleID, RouteID, Priority, StartDate, MaintenanceSchedule, Notes)
-- VALUES 
--     (1, 1, 1, '2026-01-01', 'Weekly', 'Xe 45 chỗ phù hợp tuyến dài'),
--     (2, 1, 2, '2026-01-01', 'Weekly', 'Xe backup cho tuyến Hà Nội - Đà Nẵng');


-- Insert sample trip template
-- COMMENTED OUT: Uncomment khi đã có dữ liệu Routes và Users
-- INSERT INTO TripTemplates (TemplateName, RouteID, DepartureTime, DaysOfWeek, BasePrice, EffectiveFrom, CreatedBy)
-- VALUES 
--     ('HN-DN Sáng (T2-T6)', 1, '07:00:00', 'Mon,Tue,Wed,Thu,Fri', 350000, '2026-02-01', 1),
--     ('HN-DN Chiều (T2-T6)', 1, '14:00:00', 'Mon,Tue,Wed,Thu,Fri', 350000, '2026-02-01', 1),
--     ('HN-DN Cuối tuần', 1, '09:00:00', 'Sat,Sun', 380000, '2026-02-01', 1);


-- =====================================================
-- 7. GRANT PERMISSIONS (if needed)
-- =====================================================
-- GRANT SELECT, INSERT, UPDATE, DELETE ON DriverRouteAssignments TO fuba_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON VehicleRouteAssignments TO fuba_app_user;
-- GRANT SELECT, INSERT, UPDATE, DELETE ON TripTemplates TO fuba_app_user;
-- GRANT SELECT, INSERT ON TripGenerationLogs TO fuba_app_user;
-- GRANT SELECT ON vw_ActiveDriverAssignments TO fuba_app_user;
-- GRANT SELECT ON vw_ActiveVehicleAssignments TO fuba_app_user;
-- GRANT SELECT ON vw_ActiveTripTemplates TO fuba_app_user;
