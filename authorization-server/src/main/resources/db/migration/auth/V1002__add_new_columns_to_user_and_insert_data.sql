ALTER TABLE users
ADD COLUMN IF NOT EXISTS name VARCHAR(255) NOT NULL DEFAULT 'Unknown',
ADD COLUMN IF NOT EXISTS email VARCHAR(255) NOT NULL DEFAULT 'unknown@example.com';

-- Update existing users created by previous migrations to have meaningful name and email
UPDATE users SET name = 'Test User', email = 'testuser@example.com' WHERE username = 'testuser';
UPDATE users SET name = 'Admin User', email = 'admin@example.com' WHERE username = 'admin';

INSERT INTO users (username, password, enabled, name, email)
VALUES ('test01', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Tester', 'test@gmail.com'),
       ('admin01', '{bcrypt}$2a$10$qTfVZZmyoQoom30ApjtoBuX5ebMe1WT4WNBuYQ4pBT43SLUld7tvq', true, 'Administrator', 'admin@gmail.com');

INSERT INTO authorities(username, authority)
VALUES ('test01', 'ROLE_USER'),
       ('admin01', 'ROLE_ADMIN');
