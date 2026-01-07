-- V6: Seed initial roles for authorization
-- Description: Insert default roles (ADMIN, USER, DRIVER, STAFF) into roles table

-- Insert roles with descriptions
INSERT INTO roles (rolename, description, createdat, updatedat)
VALUES
    ('ADMIN', 'System administrator with full access to all resources', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('USER', 'Regular customer who can book tickets and manage bookings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('DRIVER', 'Bus driver who can view and update assigned trips', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('STAFF', 'Employee/staff member who can manage routes, trips, and vehicles', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (rolename) DO NOTHING;

-- Create an admin user (optional - for testing)
-- Password: Admin@123 (BCrypt hash)
-- Uncomment if you want a default admin account