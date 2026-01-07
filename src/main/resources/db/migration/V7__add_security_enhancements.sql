-- V7: Add security enhancements - refresh tokens, audit logs, account lockout
-- Description: Add RefreshToken, AuthAuditLog tables and update User table with security fields

-- Add new columns to users table for account lockout and password reset
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failedloginattempts INTEGER DEFAULT 0,
    ADD COLUMN IF NOT EXISTS accountlockeduntil TIMESTAMP,
    ADD COLUMN IF NOT EXISTS lastloginat TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resettoken VARCHAR(255),
    ADD COLUMN IF NOT EXISTS resettokenexpiry TIMESTAMP;

-- Create refresh_tokens table
CREATE TABLE IF NOT EXISTS refresh_tokens (
    tokenid SERIAL PRIMARY KEY,
    token VARCHAR(500) NOT NULL UNIQUE,
    userid INTEGER NOT NULL REFERENCES users(userid) ON DELETE CASCADE,
    expirydate TIMESTAMP NOT NULL,
    createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked BOOLEAN DEFAULT FALSE,
    revokedat TIMESTAMP,
    CONSTRAINT fk_refresh_token_user FOREIGN KEY (userid) REFERENCES users(userid)
);

-- Create indexes for refresh_tokens
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(userid);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expiry ON refresh_tokens(expirydate);

-- Create auth_audit_logs table
CREATE TABLE IF NOT EXISTS auth_audit_logs (
    logid SERIAL PRIMARY KEY,
    userid INTEGER REFERENCES users(userid) ON DELETE SET NULL,
    email VARCHAR(255),
    phonenumber VARCHAR(20),
    action VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    ipaddress VARCHAR(50),
    useragent TEXT,
    details TEXT,
    createdat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for auth_audit_logs
CREATE INDEX IF NOT EXISTS idx_audit_logs_userid ON auth_audit_logs(userid);
CREATE INDEX IF NOT EXISTS idx_audit_logs_email ON auth_audit_logs(email);
CREATE INDEX IF NOT EXISTS idx_audit_logs_phonenumber ON auth_audit_logs(phonenumber);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON auth_audit_logs(action);
CREATE INDEX IF NOT EXISTS idx_audit_logs_createdat ON auth_audit_logs(createdat);
CREATE INDEX IF NOT EXISTS idx_audit_logs_status ON auth_audit_logs(status);

-- Add index to users table for account lockout queries
CREATE INDEX IF NOT EXISTS idx_users_accountlockeduntil ON users(accountlockeduntil);
CREATE INDEX IF NOT EXISTS idx_users_resettoken ON users(resettoken);
