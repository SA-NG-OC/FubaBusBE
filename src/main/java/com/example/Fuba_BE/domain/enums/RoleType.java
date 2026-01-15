package com.example.Fuba_BE.domain.enums;

/**
 * Role types used in the system for authorization
 * These values must match the 'rolename' column in the 'roles' table
 * 
 * Usage in @PreAuthorize:
 * - For single role: @PreAuthorize("hasRole('ADMIN')")
 * - For multiple roles: @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
 * 
 * Note: Spring Security automatically adds "ROLE_" prefix
 * So hasRole('ADMIN') checks for authority "ROLE_ADMIN"
 */
public enum RoleType {
    /**
     * System administrator with full access to all resources
     * Can perform all CRUD operations on any entity
     */
    ADMIN,
    
    /**
     * Regular customer who can book tickets and manage their own bookings
     * Limited to their own data
     */
    USER,
    
    /**
     * Bus driver who can view and update assigned trips
     * Can update GPS location and trip status
     */
    DRIVER,
    
    /**
     * Employee/staff member who can manage routes, trips, and vehicles
     * Can perform most operations except system-wide administration
     */
    STAFF;
    
    /**
     * Get role name as stored in database
     */
    public String getRoleName() {
        return this.name();
    }
    
    /**
     * Get authority name as used by Spring Security (with ROLE_ prefix)
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
