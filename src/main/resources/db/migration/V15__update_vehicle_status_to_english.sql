-- Update vehicle status from Vietnamese to English
-- Migration V15: Standardize vehicle status to English

-- Update existing vehicle statuses
UPDATE vehicles 
SET status = 'Operational' 
WHERE status IN ('Hoàn thiện', 'hoàn thiện', 'operational', 'OPERATIONAL');

UPDATE vehicles 
SET status = 'Maintenance' 
WHERE status IN ('Hư hại', 'hư hại', 'maintenance', 'MAINTENANCE');

UPDATE vehicles 
SET status = 'Inactive' 
WHERE status IN ('Phế liệu', 'phế liệu', 'inactive', 'INACTIVE');

-- Update the constraint to use English values
ALTER TABLE vehicles DROP CONSTRAINT IF EXISTS vehicles_status_check;

ALTER TABLE vehicles ADD CONSTRAINT vehicles_status_check 
CHECK (status IN ('Operational', 'Maintenance', 'Inactive'));

-- Add comment for documentation
COMMENT ON COLUMN vehicles.status IS 'Vehicle status: Operational (ready), Maintenance (under repair), Inactive (decommissioned)';
