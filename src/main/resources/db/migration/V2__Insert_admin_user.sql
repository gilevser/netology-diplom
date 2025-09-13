INSERT INTO users (login, password)
VALUES ('admin', '$2a$12$QUYVvsNdFsUCyjnM555CguPwfxvQgXOp7IvnBn3n38ekpsuZXD9Nu')
ON CONFLICT (login) DO NOTHING;
