-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE IF NOT EXISTS roles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(255) NOT NULL,
    description         VARCHAR(500) NULL,

    CONSTRAINT roles_name_key UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS user_roles (
    role_id             UUID NOT NULL,
    username            VARCHAR(50) NOT NULL,
    CONSTRAINT user_roles_pkey PRIMARY KEY (role_id, username),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (username) REFERENCES users(username)
);
