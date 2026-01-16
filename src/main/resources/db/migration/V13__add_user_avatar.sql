-- Add avatar (avt) column to users table
ALTER TABLE users
ADD COLUMN IF NOT EXISTS avt VARCHAR(500);

-- Add comment
COMMENT ON COLUMN users.avt IS 'Avatar URL from Cloudinary';

-- Update existing users to have default avatar URL if null
UPDATE users
SET avt = 'https://i.pinimg.com/736x/61/85/c3/6185c30215db7423445ee74c02e729b6.jpg'
WHERE avt IS NULL OR avt = '';
