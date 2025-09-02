INSERT INTO users (login, password)
VALUES ('testuser', '$2a$10$ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456')
ON CONFLICT (login) DO NOTHING;