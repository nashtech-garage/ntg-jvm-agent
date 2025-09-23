-- ============================================================
-- USERS (BCrypt encoded password: "password")
-- ============================================================
INSERT INTO users (username, password, enabled)
VALUES ('testuser', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true);

INSERT INTO authorities (username, authority) VALUES ('testuser', 'ROLE_USER');

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
--    'http://127.0.0.1:8081/login/oauth2/code/demo-client-oidc,http://127.0.0.1:8081/authorized',
--    'openid,profile,chatbot.read,chatbot.write',
--    '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
--    '{"settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":"RS256"}'
--);
