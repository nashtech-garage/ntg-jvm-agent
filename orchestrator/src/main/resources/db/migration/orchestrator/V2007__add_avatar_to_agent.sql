-- Add avatar column to agent table
ALTER TABLE agent ADD COLUMN avatar LONGTEXT NULL;

-- Add comment to avatar column
COMMENT ON COLUMN agent.avatar IS 'Base64 encoded avatar image or URL for storing agent profile pictures';

