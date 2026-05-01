USE auth_db;
INSERT IGNORE INTO users (email, role, active, email_verified, keycloak_id, password) VALUES
('seddik202209@gmail.com', 'ADMIN', 1, 1, 'b3a0dae9-735e-4fde-a80b-56dc91fe4a61', ''),
('4stable@gmail.com', 'ADMIN', 1, 1, '0185dd4b-695b-4487-82be-d86e2610adc3', ''),
('alian@gmail.com', 'ADMIN', 1, 1, 'f35ea6e1-81d0-4905-b4b3-d7ae9de9d7dd', '');

USE user_management_db;
INSERT IGNORE INTO user_profiles (email, role, active, blocked, created_at, updated_at) VALUES
('seddik202209@gmail.com', 'ADMIN', 1, 0, NOW(), NOW()),
('4stable@gmail.com', 'ADMIN', 1, 0, NOW(), NOW()),
('alian@gmail.com', 'ADMIN', 1, 0, NOW(), NOW());