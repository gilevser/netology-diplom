CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT true
);

CREATE TABLE files (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    size BIGINT NOT NULL,
    content BYTEA,
    user_id BIGINT REFERENCES users(id),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);