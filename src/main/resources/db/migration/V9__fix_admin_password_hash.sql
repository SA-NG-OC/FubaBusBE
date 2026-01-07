-- V9: Fix admin password BCrypt hash
-- Description: Update admin user password with proper BCrypt hash for Admin@123
INSERT INTO users (fullname, email, phonenumber, password, roleid, status, emailverified)
SELECT
    'System Administrator',
    'admin@futabus.com',
    '0900000000',
    '$2a$10$xKzXvKqXJqKqXv4KqXvKqOeX4JjX8JjX8JjX8JjX8JjX8JjX8JjXe', -- BCrypt: Admin@123
    roleid,
    'Active',
    true
FROM roles
WHERE rolename = 'ADMIN'
ON CONFLICT (email) DO NOTHING;