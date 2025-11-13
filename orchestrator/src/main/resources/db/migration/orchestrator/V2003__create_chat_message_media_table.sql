-- ====================================================================
-- Purpose: Create the chat_message_media table
-- Database: PostgreSQL
-- ====================================================================

CREATE TABLE IF NOT EXISTS chat_message_media (
	created_at timestamptz(6) NOT NULL,
	file_size int8 NOT NULL DEFAULT 0,
	updated_at timestamptz(6) NULL,
	chat_message_id uuid NOT NULL,
	id uuid NOT NULL,
	content_type varchar(255) NULL,
	file_name varchar(255) NULL,
	"data" bytea NULL,
	CONSTRAINT chat_message_media_pkey PRIMARY KEY (id)
);


-- chat_message_media foreign keys
ALTER TABLE chat_message_media ADD CONSTRAINT fk_chat_message_media FOREIGN KEY (chat_message_id) REFERENCES chat_message(id);
