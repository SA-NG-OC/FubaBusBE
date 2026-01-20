-- V16: Add MANAGER role
-- Description: Insert MANAGER role for managers who have similar permissions to ADMIN

-- Insert MANAGER role
INSERT INTO roles (rolename, description, createdat, updatedat)
VALUES
    ('MANAGER', 'Manager with administrative access to manage operations, routes, trips, and staff', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (rolename) DO NOTHING;
