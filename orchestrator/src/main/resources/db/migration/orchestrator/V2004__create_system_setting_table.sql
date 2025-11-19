-- ====================================================================
-- Purpose: Create the system_setting table
-- Database: PostgreSQL
-- ====================================================================
-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS system_setting (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_name           VARCHAR(255) NOT NULL,
    maximum_user        INTEGER NOT NULL,
    session_timeout     INTEGER NOT NULL,
    maximum_size_file_upload     INTEGER NOT NULL,
    allowed_file_types  VARCHAR(255) NOT NULL,
    maintenance_mode    BOOLEAN NOT NULL,
    user_registration   BOOLEAN NOT NULL,
    email_verification  BOOLEAN NOT NULL,
    created_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);


-- Init data for system_setting table
INSERT INTO system_setting (site_name, maximum_user, session_timeout, maximum_size_file_upload, allowed_file_types, maintenance_mode, user_registration, email_verification)
VALUES ('NTG JVM Agent Chat UI', 1000, 3600, 10, 'jpg, jpeg, png, pdf, doc, docx', false, true, true)
