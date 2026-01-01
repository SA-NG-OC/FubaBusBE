-- V3: Add columns for seat locking functionality
-- Adds lockedby and lockedbysessionid columns to tripseats table

ALTER TABLE tripseats 
ADD COLUMN IF NOT EXISTS lockedby VARCHAR(255),
ADD COLUMN IF NOT EXISTS lockedbysessionid VARCHAR(255);

-- Create index for efficient lookup by session ID (used during disconnect)
CREATE INDEX IF NOT EXISTS idx_tripseats_lockedbysessionid 
ON tripseats(lockedbysessionid) 
WHERE lockedbysessionid IS NOT NULL;

-- Create index for efficient lookup of expired locks
CREATE INDEX IF NOT EXISTS idx_tripseats_holdexpiry 
ON tripseats(holdexpiry) 
WHERE holdexpiry IS NOT NULL AND status = 'Đang giữ';

-- Create index for status to speed up lock-related queries
CREATE INDEX IF NOT EXISTS idx_tripseats_status 
ON tripseats(status);

COMMENT ON COLUMN tripseats.lockedby IS 'User ID who has locked this seat';
COMMENT ON COLUMN tripseats.lockedbysessionid IS 'WebSocket session ID for tracking locks on disconnect';
