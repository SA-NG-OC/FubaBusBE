-- V9: Fix admin password BCrypt hash
-- Description: Update admin user password with proper BCrypt hash for Admin@123
INSERT INTO users (fullname, email, phonenumber, password, roleid, status, emailverified)
SELECT
    'System Administrator',
    'admin@futabus.com',
    '0988000000',
    '$2a$10$d0PaTrIdB.Hg.TIdMOLSNulkKUc/NwzE6T34r.IGIpwG4mjKmTs0W', -- BCrypt: Admin@123
    roleid,
    'Active',
    true
FROM roles
WHERE rolename = 'ADMIN'
ON CONFLICT (email) DO NOTHING;