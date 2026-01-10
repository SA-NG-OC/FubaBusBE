-- Fix logId column type in auth_audit_logs table
ALTER TABLE auth_audit_logs ALTER COLUMN logId TYPE BIGINT;

-- Fix tokenId column type in refresh_tokens table
ALTER TABLE refresh_tokens ALTER COLUMN tokenId TYPE BIGINT;
