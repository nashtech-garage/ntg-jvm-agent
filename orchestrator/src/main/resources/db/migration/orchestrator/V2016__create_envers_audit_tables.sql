-- Hibernate Envers Audit Configuration
-- Creates revision tracking infrastructure for entity auditing

-- Create sequence for revision numbers
CREATE SEQUENCE IF NOT EXISTS revinfo_seq START WITH 1 INCREMENT BY 1;

-- Create revision info table
-- This table stores information about each revision (who made changes and when)
CREATE TABLE IF NOT EXISTS revinfo (
    rev INTEGER NOT NULL PRIMARY KEY DEFAULT nextval('revinfo_seq'),
    revtstmp BIGINT NOT NULL,
    username VARCHAR(255),
    user_id UUID
);

CREATE INDEX idx_revinfo_revtstmp ON revinfo(revtstmp);
CREATE INDEX idx_revinfo_user_id ON revinfo(user_id);

-- Create audit table for agent
CREATE TABLE IF NOT EXISTS agent_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    name VARCHAR(100),
    description TEXT,
    avatar TEXT,
    provider VARCHAR(50),
    base_url VARCHAR(150),
    api_key VARCHAR(200),
    chat_completions_path VARCHAR(50),
    embeddings_path VARCHAR(50),
    embedding_model VARCHAR(50),
    dimension INTEGER,
    model VARCHAR(100),
    temperature DECIMAL(3,2),
    max_tokens INTEGER,
    top_p DECIMAL(3,2),
    frequency_penalty DECIMAL(3,2),
    presence_penalty DECIMAL(3,2),
    settings JSONB,
    version INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    active BOOLEAN,
    created_by UUID,
    updated_by UUID,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_agent_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE INDEX idx_agent_aud_rev ON agent_aud(rev);
CREATE INDEX idx_agent_aud_revtype ON agent_aud(revtype);

-- Create audit table for tool
CREATE TABLE IF NOT EXISTS tool_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    name VARCHAR(100),
    type VARCHAR(50),
    description TEXT,
    definition JSONB,
    connection_config JSONB,
    base_url VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP,
    active BOOLEAN,
    created_by UUID,
    updated_by UUID,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_tool_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE INDEX idx_tool_aud_rev ON tool_aud(rev);
CREATE INDEX idx_tool_aud_revtype ON tool_aud(revtype);

-- Create audit table for conversation
CREATE TABLE IF NOT EXISTS conversation_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    title TEXT,
    status VARCHAR(50),
    is_active BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_conversation_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE INDEX idx_conversation_aud_rev ON conversation_aud(rev);
CREATE INDEX idx_conversation_aud_revtype ON conversation_aud(revtype);

-- Create audit table for system_setting
CREATE TABLE IF NOT EXISTS system_setting_aud (
    id UUID NOT NULL,
    rev INTEGER NOT NULL,
    revtype SMALLINT,
    site_name VARCHAR(255),
    maximum_user INTEGER,
    session_timeout INTEGER,
    maximum_size_file_upload INTEGER,
    allowed_file_types VARCHAR(255),
    maintenance_mode BOOLEAN,
    user_registration BOOLEAN,
    email_verification BOOLEAN,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_system_setting_aud_rev FOREIGN KEY (rev) REFERENCES revinfo(rev)
);

CREATE INDEX idx_system_setting_aud_rev ON system_setting_aud(rev);
CREATE INDEX idx_system_setting_aud_revtype ON system_setting_aud(revtype);

-- Comment on tables
COMMENT ON TABLE revinfo IS 'Stores revision information for Hibernate Envers auditing';
COMMENT ON TABLE agent_aud IS 'Audit history for agent table';
COMMENT ON TABLE tool_aud IS 'Audit history for tool table';
COMMENT ON TABLE conversation_aud IS 'Audit history for conversation table';
COMMENT ON TABLE system_setting_aud IS 'Audit history for system_setting table';

-- Comment on revtype column
-- revtype values: 0 = ADD, 1 = MOD, 2 = DEL
COMMENT ON COLUMN agent_aud.revtype IS 'Revision type: 0=ADD, 1=MOD, 2=DEL';
COMMENT ON COLUMN tool_aud.revtype IS 'Revision type: 0=ADD, 1=MOD, 2=DEL';
COMMENT ON COLUMN conversation_aud.revtype IS 'Revision type: 0=ADD, 1=MOD, 2=DEL';
COMMENT ON COLUMN system_setting_aud.revtype IS 'Revision type: 0=ADD, 1=MOD, 2=DEL';

