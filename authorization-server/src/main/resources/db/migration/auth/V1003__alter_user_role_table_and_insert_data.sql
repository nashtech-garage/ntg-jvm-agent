-- Drop TABLE authorities
DROP TABLE authorities;
-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

DROP TABLE user_roles;
CREATE TABLE IF NOT EXISTS user_roles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id             UUID NOT NULL,
    username            VARCHAR(50) NOT NULL,
    CONSTRAINT user_roles_unique UNIQUE (role_id, username),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (username) REFERENCES users(username)
);

INSERT INTO roles (id, name, description) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','ROLE_ADMIN', 'ADMIN');
INSERT INTO roles (id, name, description) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','ROLE_USER', 'USER');

INSERT INTO user_roles (role_id, username) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','admin');
INSERT INTO user_roles (role_id, username) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','testuser');
INSERT INTO user_roles (role_id, username) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','admin01');
INSERT INTO user_roles (role_id, username) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','test01');
