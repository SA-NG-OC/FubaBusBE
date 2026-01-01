-- ====================================
-- SQL SCRIPTS HỖ TRỢ TEST SEAT LOCKING
-- ====================================

-- 1. XEM TRẠNG THÁI HIỆN TẠI CỦA TẤT CẢ GHẾ
-- ========================================
SELECT 
    t.tripid,
    ts.seatid,
    ts.seatnumber,
    ts.floornumber,
    ts.status,
    ts.lockedby,
    ts.lockedbysessionid,
    ts.holdexpiry,
    CASE 
        WHEN ts.holdexpiry IS NULL THEN NULL
        WHEN ts.holdexpiry > NOW() THEN EXTRACT(EPOCH FROM (ts.holdexpiry - NOW()))::INTEGER
        ELSE 0
    END as seconds_remaining
FROM tripseats ts
JOIN trips t ON ts.tripid = t.tripid
ORDER BY t.tripid, ts.floornumber, ts.seatnumber;


-- 2. XEM CHỈ CÁC GHẾ ĐANG BỊ LOCK
-- ========================================
SELECT 
    ts.seatid,
    ts.seatnumber,
    ts.status,
    ts.lockedby,
    ts.lockedbysessionid,
    ts.holdexpiry,
    ts.holdexpiry - NOW() as time_remaining
FROM tripseats ts
WHERE ts.status = 'Đang giữ'
ORDER BY ts.holdexpiry;


-- 3. XEM GHẾ LOCK BỞI MỘT USER CỤ THỂ
-- ========================================
-- Thay 'user_test_1' bằng user ID cần tìm
SELECT 
    ts.seatid,
    ts.seatnumber,
    ts.status,
    ts.lockedby,
    ts.holdexpiry
FROM tripseats ts
WHERE ts.lockedby = 'user_test_1';


-- 4. XEM GHẾ LOCK BỞI MỘT SESSION CỤ THỂ
-- ========================================
-- Thay '<session_id>' bằng session ID cần tìm
SELECT 
    ts.seatid,
    ts.seatnumber,
    ts.status,
    ts.lockedby,
    ts.lockedbysessionid,
    ts.holdexpiry
FROM tripseats ts
WHERE ts.lockedbysessionid = '<session_id>';


-- 5. TÌM GHẾ LOCK SẮP HẾT HẠN (trong vòng 1 phút)
-- ========================================
SELECT 
    ts.seatid,
    ts.seatnumber,
    ts.lockedby,
    ts.holdexpiry,
    EXTRACT(EPOCH FROM (ts.holdexpiry - NOW()))::INTEGER as seconds_remaining
FROM tripseats ts
WHERE ts.status = 'Đang giữ'
  AND ts.holdexpiry BETWEEN NOW() AND NOW() + INTERVAL '1 minute'
ORDER BY ts.holdexpiry;


-- 6. TÌM GHẾ LOCK ĐÃ HẾT HẠN (expired locks)
-- ========================================
SELECT 
    ts.seatid,
    ts.seatnumber,
    ts.lockedby,
    ts.holdexpiry,
    NOW() - ts.holdexpiry as expired_duration
FROM tripseats ts
WHERE ts.status = 'Đang giữ'
  AND ts.holdexpiry < NOW()
ORDER BY ts.holdexpiry DESC;


-- 7. THỐNG KÊ TRẠNG THÁI GHẾ THEO TRIP
-- ========================================
SELECT 
    t.tripid,
    COUNT(*) as total_seats,
    COUNT(CASE WHEN ts.status = 'Trống' THEN 1 END) as available,
    COUNT(CASE WHEN ts.status = 'Đang giữ' THEN 1 END) as locked,
    COUNT(CASE WHEN ts.status = 'Đã đặt' THEN 1 END) as booked
FROM trips t
JOIN tripseats ts ON t.tripid = ts.tripid
GROUP BY t.tripid
ORDER BY t.tripid;


-- 8. XEM LỊCH SỬ LOCK/UNLOCK (nếu có audit log)
-- ========================================
-- Nếu bạn có bảng audit_logs
-- SELECT * FROM audit_logs 
-- WHERE entity_type = 'TripSeat' 
-- ORDER BY created_at DESC 
-- LIMIT 50;


-- ====================================
-- SCRIPTS ADMIN (CHẠY KHI TEST)
-- ====================================

-- 9. XÓA TẤT CẢ LOCKS (Reset về trạng thái ban đầu)
-- ========================================
-- CẢNH BÁO: Chỉ dùng khi test!
UPDATE tripseats 
SET status = 'Trống',
    lockedby = NULL,
    lockedbysessionid = NULL,
    holdexpiry = NULL
WHERE status = 'Đang giữ';

-- Kiểm tra kết quả
SELECT COUNT(*) as remaining_locks FROM tripseats WHERE status = 'Đang giữ';


-- 10. XÓA LOCK CỦA MỘT USER CỤ THỂ
-- ========================================
-- Thay 'user_test_1' bằng user cần xóa
UPDATE tripseats 
SET status = 'Trống',
    lockedby = NULL,
    lockedbysessionid = NULL,
    holdexpiry = NULL
WHERE lockedby = 'user_test_1'
  AND status = 'Đang giữ';


-- 11. XÓA LOCK CỦA MỘT SESSION CỤ THỂ
-- ========================================
-- Thay '<session_id>' bằng session cần xóa
UPDATE tripseats 
SET status = 'Trống',
    lockedby = NULL,
    lockedbysessionid = NULL,
    holdexpiry = NULL
WHERE lockedbysessionid = '<session_id>'
  AND status = 'Đang giữ';


-- 12. FORCE EXPIRE TẤT CẢ LOCKS (để test scheduler)
-- ========================================
-- Set holdExpiry về quá khứ để scheduler tự động xóa
UPDATE tripseats 
SET holdexpiry = NOW() - INTERVAL '1 minute'
WHERE status = 'Đang giữ';

-- Đợi scheduler chạy (mỗi 30 giây)
-- Hoặc restart server để trigger ngay


-- 13. TẠO TEST DATA - LOCK GHẾ THỦ CÔNG
-- ========================================
-- Giả lập user đã lock ghế (để test unlock, expire, etc)
UPDATE tripseats 
SET status = 'Đang giữ',
    lockedby = 'test_user_manual',
    lockedbysessionid = 'manual_session_123',
    holdexpiry = NOW() + INTERVAL '5 minutes'
WHERE seatid = 1;


-- 14. RESET TẤT CẢ GHẾ VỀ TRẠNG THÁI BAN ĐẦU
-- ========================================
-- CẢNH BÁO: Xóa toàn bộ bookings và locks!
UPDATE tripseats 
SET status = 'Trống',
    lockedby = NULL,
    lockedbysessionid = NULL,
    holdexpiry = NULL;

SELECT 
    COUNT(CASE WHEN status = 'Trống' THEN 1 END) as available,
    COUNT(CASE WHEN status = 'Đang giữ' THEN 1 END) as locked,
    COUNT(CASE WHEN status = 'Đã đặt' THEN 1 END) as booked
FROM tripseats;


-- 15. KIỂM TRA INDEXES
-- ========================================
-- Xem các index đã được tạo chưa
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE tablename = 'tripseats'
ORDER BY indexname;


-- 16. PHÂN TÍCH PERFORMANCE
-- ========================================
-- Xem query plan cho findExpiredLocks
EXPLAIN ANALYZE
SELECT ts.* 
FROM tripseats ts 
WHERE ts.status = 'Đang giữ' 
  AND ts.holdexpiry < NOW();

-- Xem query plan cho findBySessionId
EXPLAIN ANALYZE
SELECT ts.* 
FROM tripseats ts 
WHERE ts.lockedbysessionid = 'test_session';


-- 17. TEST CONCURRENCY - Simulate race condition
-- ========================================
-- Mở 2 sessions PostgreSQL và chạy đồng thời:

-- Session 1:
BEGIN;
SELECT * FROM tripseats WHERE seatid = 1 FOR UPDATE;
-- Đợi 10 giây
UPDATE tripseats SET status = 'Đang giữ', lockedby = 'user_A' WHERE seatid = 1;
COMMIT;

-- Session 2 (chạy trong khi Session 1 đang đợi):
BEGIN;
SELECT * FROM tripseats WHERE seatid = 1 FOR UPDATE;  -- Sẽ bị block đến khi Session 1 commit
UPDATE tripseats SET status = 'Đang giữ', lockedby = 'user_B' WHERE seatid = 1;
COMMIT;


-- 18. XEM ACTIVE CONNECTIONS
-- ========================================
SELECT 
    pid,
    usename,
    application_name,
    client_addr,
    state,
    query_start,
    state_change
FROM pg_stat_activity
WHERE datname = current_database()
  AND state = 'active'
ORDER BY query_start DESC;


-- 19. XEM LOCKS ĐANG ACTIVE TRONG DB
-- ========================================
SELECT 
    locktype,
    relation::regclass,
    mode,
    granted,
    pid,
    pg_blocking_pids(pid) as blocked_by
FROM pg_locks
WHERE relation = 'tripseats'::regclass;


-- 20. CLEANUP - XÓA TOÀN BỘ SEATS (để test lại từ đầu)
-- ========================================
-- CẢNH BÁO: Xóa toàn bộ dữ liệu!
DELETE FROM tripseats WHERE tripid = 1;

-- Sau đó call API để tạo lại:
-- POST /api/trips/1/seat-map/migrate


-- ====================================
-- MONITORING QUERIES
-- ====================================

-- 21. REAL-TIME MONITORING
-- ========================================
-- Query này để chạy liên tục trong một terminal
-- Cập nhật mỗi 2 giây trong psql: \watch 2

SELECT 
    NOW() as check_time,
    COUNT(*) FILTER (WHERE status = 'Trống') as available,
    COUNT(*) FILTER (WHERE status = 'Đang giữ') as locked,
    COUNT(*) FILTER (WHERE status = 'Đã đặt') as booked,
    COUNT(*) FILTER (WHERE status = 'Đang giữ' AND holdexpiry < NOW()) as expired
FROM tripseats
WHERE tripid = 1;


-- 22. USER ACTIVITY SUMMARY
-- ========================================
SELECT 
    lockedby,
    COUNT(*) as seats_locked,
    MIN(holdexpiry) as earliest_expiry,
    MAX(holdexpiry) as latest_expiry
FROM tripseats
WHERE status = 'Đang giữ'
  AND lockedby IS NOT NULL
GROUP BY lockedby
ORDER BY seats_locked DESC;


-- ====================================
-- NOTES
-- ====================================
/*
Để test hiệu quả:
1. Mở pgAdmin hoặc psql
2. Chạy query #1 để xem tổng quan
3. Chạy query #2 để xem locks hiện tại
4. Khi test, dùng query #21 với \watch để monitor real-time
5. Dùng query #9 để reset về trạng thái ban đầu

Performance tips:
- Index trên lockedbysessionid đã được tạo
- Index trên holdexpiry đã được tạo
- Query với FOR UPDATE sẽ lock row-level

Debugging:
- Nếu scheduler không chạy, check query #6
- Nếu có race condition, check query #19
- Nếu performance chậm, check query #16
*/
