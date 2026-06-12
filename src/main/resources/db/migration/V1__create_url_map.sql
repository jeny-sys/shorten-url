CREATE TABLE url_map (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code VARCHAR(16) NULL,
    long_url VARCHAR(2048) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL,
    total_clicks BIGINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_short_code (short_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
