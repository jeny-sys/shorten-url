CREATE TABLE url_map (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(16),
    long_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_short_code UNIQUE (short_code)
);
