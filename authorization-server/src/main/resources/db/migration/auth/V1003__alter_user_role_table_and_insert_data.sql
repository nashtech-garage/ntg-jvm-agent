-- Drop TABLE authorities
DROP TABLE authorities;

-- Drop TABLE user_roles
DROP TABLE user_roles;

-- Drop TABLE users
DROP TABLE users;

-- Enable pgcrypto extension for gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ALTER TABLE users
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL,
    password VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL,
    name VARCHAR(255) NOT NULL DEFAULT 'Unknown',
    email VARCHAR(255) NOT NULL DEFAULT 'unknown@example.com',
    CONSTRAINT users_username_uk UNIQUE (username)
);

-- CREATE TABLE user_roles
CREATE TABLE IF NOT EXISTS user_roles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id             UUID NOT NULL,
    user_id             UUID NOT NULL,
    CONSTRAINT user_roles_unique UNIQUE (role_id, user_id),
    CONSTRAINT fk_user_roles_roles FOREIGN KEY (role_id) REFERENCES roles(id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id)
);


-- INSERT new data for User table
INSERT INTO users (id,username, password, enabled, name, email)
VALUES
('d0e07480-8060-4575-80f5-56bbaf6e65b5','testuser', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Test User', 'testuser@example.com'),
('d0e07480-8060-4575-80f5-56bbaf6e66c2','admin', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Admin User', 'admin@example.com'),
('d0e07480-8060-4575-80f5-56bbaf6e33b1','test01', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Tester', 'test@gmail.com'),
('d0e07480-8060-4575-80f5-56bbaf6e65c2','admin01', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Administrator', 'admin@gmail.com');

-- INSERT new data for roles table
INSERT INTO roles (id, name, description) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','ROLE_ADMIN', 'ADMIN');
INSERT INTO roles (id, name, description) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','ROLE_USER', 'USER');

-- INSERT new data for user_roles table
INSERT INTO user_roles (role_id, user_id) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','d0e07480-8060-4575-80f5-56bbaf6e66c2');
INSERT INTO user_roles (role_id, user_id) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','d0e07480-8060-4575-80f5-56bbaf6e65b5');
INSERT INTO user_roles (role_id, user_id) VALUES ('d0e07480-8060-4575-80f5-56bbaf6e42b4','d0e07480-8060-4575-80f5-56bbaf6e65c2');
INSERT INTO user_roles (role_id, user_id) VALUES ('d0e07433-8060-4547-80f5-56bbaf6e42c1','d0e07480-8060-4575-80f5-56bbaf6e33b1');
