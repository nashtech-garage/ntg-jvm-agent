-- ============================================================
-- USERS + AUTHORITIES
-- ============================================================
DROP TABLE IF EXISTS authorities;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    username VARCHAR(50) PRIMARY KEY,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL
);

CREATE TABLE authorities (
    username VARCHAR(50) NOT NULL,
    authority VARCHAR(50) NOT NULL,
    CONSTRAINT fk_authorities_users FOREIGN KEY(username) REFERENCES users(username)
);

CREATE UNIQUE INDEX ix_auth_username ON authorities (username, authority);

-- ============================================================
-- OAUTH2 REGISTERED CLIENTS
-- ============================================================
DROP TABLE IF EXISTS oauth2_registered_client;

CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP,
    client_name VARCHAR(200) NOT NULL,
    client_authentication_methods VARCHAR(1000) NOT NULL,
    authorization_grant_types VARCHAR(1000) NOT NULL,
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(1000) NOT NULL,
    client_settings VARCHAR(2000) NOT NULL,
    token_settings VARCHAR(2000) NOT NULL
);

CREATE UNIQUE INDEX ux_client_id ON oauth2_registered_client (client_id);

-- ============================================================
-- OAUTH2 AUTHORIZATIONS
-- ============================================================
DROP TABLE IF EXISTS oauth2_authorization;

CREATE TABLE oauth2_authorization (
    id VARCHAR(100) PRIMARY KEY,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100) NOT NULL,
    authorized_scopes VARCHAR(1000),
    attributes TEXT,
    state VARCHAR(500),
    authorization_code_value TEXT,
    authorization_code_issued_at TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata TEXT,
    access_token_value TEXT,
    access_token_issued_at TIMESTAMP,
    access_token_expires_at TIMESTAMP,
    access_token_metadata TEXT,
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(1000),
    oidc_id_token_value TEXT,
    oidc_id_token_issued_at TIMESTAMP,
    oidc_id_token_expires_at TIMESTAMP,
    oidc_id_token_metadata TEXT,
    refresh_token_value TEXT,
    refresh_token_issued_at TIMESTAMP,
    refresh_token_expires_at TIMESTAMP,
    refresh_token_metadata TEXT,
    user_code_value TEXT,
    user_code_issued_at TIMESTAMP,
    user_code_expires_at TIMESTAMP,
    user_code_metadata TEXT,
    device_code_value TEXT,
    device_code_issued_at TIMESTAMP,
    device_code_expires_at TIMESTAMP,
    device_code_metadata TEXT
);

CREATE INDEX idx_authorization_principal ON oauth2_authorization (principal_name);
CREATE INDEX idx_authorization_client ON oauth2_authorization (registered_client_id);

-- ============================================================
-- OAUTH2 AUTHORIZATION CONSENTS
-- ============================================================
DROP TABLE IF EXISTS oauth2_authorization_consent;

CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE INDEX idx_consent_client ON oauth2_authorization_consent (registered_client_id);


-- ============================================================
-- USERS (BCrypt encoded password: "password")
-- ADMIN (BCrypt encoded password: "password")
-- ============================================================
INSERT INTO users (username, password, enabled)
VALUES ('testuser', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true);
INSERT INTO users (username, password, enabled)
VALUES ('admin', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true);

INSERT INTO authorities (username, authority) VALUES ('testuser', 'ROLE_USER');
INSERT INTO authorities (username, authority) VALUES ('admin', 'ROLE_ADMIN');

-- ============================================================
-- DEMO CLIENT (client_id=demo-client / client_secret=demo-secret)
-- ============================================================
--INSERT INTO oauth2_registered_client (
--    id, client_id, client_id_issued_at, client_secret, client_name,
--    client_authentication_methods, authorization_grant_types, redirect_uris,
--    scopes, client_settings, token_settings
--) VALUES (
--    'demo-client-id',
--    'demo-client',
--    now(),
--    '{bcrypt}$2a$10$Obyh3d5Iqanm.lwKDW2cQekH6T4rR5NzVmURZO0ngwysYdbmsmdjm', -- "demo-secret"
--    'Demo Client',
--    'client_secret_basic',
--    'authorization_code,refresh_token',
--    'http://localhost:3000/auth/callback,http://127.0.0.1:8081/authorized',
--    'openid,profile,chatbot.read,chatbot.write',
--    '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
--    '{"settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":"RS256"}'
--);
