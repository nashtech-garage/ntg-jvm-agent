-- ====================================================================
-- Purpose: Create the chat_message_media table
-- Database: PostgreSQL
-- ====================================================================

CREATE TABLE IF NOT EXISTS chat_message_media (
	id uuid NOT NULL,
	chat_message_id uuid NOT NULL,
	file_name varchar(255) NULL,
	file_size int8 NOT NULL DEFAULT 0,
	content_type varchar(255) NULL,
	"data" bytea NULL,
	created_at timestamptz(6) NOT NULL,
	created_by UUID REFERENCES users(id),
	updated_at timestamptz(6) NULL,
  updated_by UUID REFERENCES users(id),
	CONSTRAINT chat_message_media_pkey PRIMARY KEY (id)
);


-- chat_message_media foreign keys
ALTER TABLE chat_message_media ADD CONSTRAINT fk_chat_message_media FOREIGN KEY (chat_message_id) REFERENCES chat_message(id);
