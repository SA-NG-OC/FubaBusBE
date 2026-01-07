-- =============================================
-- CẬP NHẬT TRIPS STATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ (Postgres thường đặt tên mặc định là trips_status_check)
ALTER TABLE public.trips DROP CONSTRAINT IF EXISTS trips_status_check;

-- 2. Cập nhật dữ liệu cũ sang Tiếng Anh
UPDATE public.trips
SET status = CASE
    WHEN status = 'Chờ' THEN 'Waiting'
    WHEN status = 'Đang chạy' THEN 'Running'
    WHEN status = 'Hoãn' THEN 'Delayed'
    WHEN status = 'Hoàn thành' THEN 'Completed'
    WHEN status = 'Hủy' THEN 'Cancelled'
    ELSE 'Waiting' -- Giá trị mặc định nếu không khớp
END;

-- 3. Đặt lại giá trị mặc định và thêm ràng buộc mới
ALTER TABLE public.trips
    ALTER COLUMN status SET DEFAULT 'Waiting',
    ADD CONSTRAINT trips_status_check
    CHECK (status IN ('Waiting', 'Running', 'Delayed', 'Completed', 'Cancelled'));

-- =============================================
-- CẬP NHẬT TRIPSEATS SEATTYPE VÀ STATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa các ràng buộc cũ của TripSeats
ALTER TABLE public.tripseats DROP CONSTRAINT IF EXISTS tripseats_seattype_check;
ALTER TABLE public.tripseats DROP CONSTRAINT IF EXISTS tripseats_status_check;

-- 2. Cập nhật SeatType từ Tiếng Việt sang Tiếng Anh
UPDATE public.tripseats
SET seattype = CASE
    WHEN seattype = 'Thường' THEN 'Standard'
    WHEN seattype = 'VIP' THEN 'VIP'
    WHEN seattype = 'Giường' THEN 'Sleeper'
    ELSE 'Standard' -- Giá trị mặc định nếu không khớp
END;

-- 3. Cập nhật Status từ Tiếng Việt sang Tiếng Anh
UPDATE public.tripseats
SET status = CASE
    WHEN status = 'Trống' THEN 'Available'
    WHEN status = 'Đang giữ' THEN 'Held'
    WHEN status = 'Đã đặt' THEN 'Booked'
    ELSE 'Available' -- Giá trị mặc định nếu không khớp
END;

-- 4. Đặt lại giá trị mặc định và thêm ràng buộc mới cho SeatType
ALTER TABLE public.tripseats
    ALTER COLUMN seattype SET DEFAULT 'Standard',
    ADD CONSTRAINT tripseats_seattype_check
    CHECK (seattype IN ('Standard', 'VIP', 'Sleeper'));

-- 5. Đặt lại giá trị mặc định và thêm ràng buộc mới cho Status
ALTER TABLE public.tripseats
    ALTER COLUMN status SET DEFAULT 'Available',
    ADD CONSTRAINT tripseats_status_check
    CHECK (status IN ('Available', 'Held', 'Booked'));

-- =============================================
-- CẬP NHẬT USERS STATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ
ALTER TABLE public.users DROP CONSTRAINT IF EXISTS users_status_check;

-- 2. Cập nhật dữ liệu
UPDATE public.users
SET status = CASE
    WHEN status = 'Hoạt động' THEN 'Active'
    WHEN status = 'Khóa' THEN 'Locked'
    ELSE 'Active'
END;

-- 3. Thêm ràng buộc mới
ALTER TABLE public.users
    ALTER COLUMN status SET DEFAULT 'Active',
    ADD CONSTRAINT users_status_check
    CHECK (status IN ('Active', 'Locked'));

-- =============================================
-- CẬP NHẬT ROUTES STATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ
ALTER TABLE public.routes DROP CONSTRAINT IF EXISTS routes_status_check;

-- 2. Cập nhật dữ liệu
UPDATE public.routes
SET status = CASE
    WHEN status = 'Hoạt động' THEN 'Active'
    WHEN status = 'Bảo trì' THEN 'Maintenance'
    WHEN status = 'Dừng' THEN 'Stopped'
    ELSE 'Active'
END;

-- 3. Thêm ràng buộc mới
ALTER TABLE public.routes
    ALTER COLUMN status SET DEFAULT 'Active',
    ADD CONSTRAINT routes_status_check
    CHECK (status IN ('Active', 'Maintenance', 'Stopped'));

-- =============================================
-- CẬP NHẬT TRIPTRACKING TRAFFICSTATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ
ALTER TABLE public.triptracking DROP CONSTRAINT IF EXISTS triptracking_trafficstatus_check;

-- 2. Cập nhật dữ liệu
UPDATE public.triptracking
SET trafficstatus = CASE
    WHEN trafficstatus = 'Bình thường' THEN 'Normal'
    WHEN trafficstatus = 'Kẹt xe nhẹ' THEN 'Light Traffic'
    WHEN trafficstatus = 'Kẹt xe nặng' THEN 'Heavy Traffic'
    WHEN trafficstatus = 'Tai nạn' THEN 'Accident'
    WHEN trafficstatus = 'Sửa đường' THEN 'Road Work'
    WHEN trafficstatus = 'Khác' THEN 'Other'
    ELSE 'Normal'
END;

-- 3. Thêm ràng buộc mới
ALTER TABLE public.triptracking
    ALTER COLUMN trafficstatus SET DEFAULT 'Normal',
    ADD CONSTRAINT triptracking_trafficstatus_check
    CHECK (trafficstatus IN ('Normal', 'Light Traffic', 'Heavy Traffic', 'Accident', 'Road Work', 'Other'));

-- =============================================
-- CẬP NHẬT PAYMENTS PAYMENTSTATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ (nếu có)
ALTER TABLE public.payments DROP CONSTRAINT IF EXISTS payments_paymentstatus_check;

-- 2. Cập nhật dữ liệu
UPDATE public.payments
SET paymentstatus = CASE
    WHEN paymentstatus = 'Chờ xử lý' THEN 'Pending'
    WHEN paymentstatus = 'Thành công' THEN 'Success'
    WHEN paymentstatus = 'Thất bại' THEN 'Failed'
    WHEN paymentstatus = 'Hoàn tiền' THEN 'Refunded'
    ELSE 'Pending'
END;

-- 3. Thêm ràng buộc mới (nếu cần)
ALTER TABLE public.payments
    ALTER COLUMN paymentstatus SET DEFAULT 'Pending';

-- =============================================
-- CẬP NHẬT TICKETCHANGES CHANGESTATUS SANG TIẾNG ANH
-- =============================================

-- 1. Xóa ràng buộc cũ (nếu có)
ALTER TABLE public.ticketchanges DROP CONSTRAINT IF EXISTS ticketchanges_changestatus_check;

-- 2. Cập nhật dữ liệu
UPDATE public.ticketchanges
SET changestatus = CASE
    WHEN changestatus = 'Chờ xử lý' THEN 'Pending'
    WHEN changestatus = 'Đã duyệt' THEN 'Approved'
    WHEN changestatus = 'Từ chối' THEN 'Rejected'
    ELSE 'Pending'
END;

-- 3. Thêm ràng buộc mới (nếu cần)
ALTER TABLE public.ticketchanges
    ALTER COLUMN changestatus SET DEFAULT 'Pending';