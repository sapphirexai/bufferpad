-- Structured operation events used by the running monitor page.
-- Safe to execute repeatedly on MySQL 8.
CREATE TABLE IF NOT EXISTS operation_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL,
    operation_id VARCHAR(64) NULL,
    code VARCHAR(64) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    title VARCHAR(128) NOT NULL,
    message VARCHAR(512) NOT NULL,
    suggestion VARCHAR(512) NULL,
    work_line INT NOT NULL,
    scanner_id BIGINT NULL,
    scanner_seq INT NULL,
    device_id BIGINT NULL,
    device_name VARCHAR(128) NULL,
    qr_code VARCHAR(128) NULL,
    address VARCHAR(64) NULL,
    error_code INT NULL,
    technical_detail VARCHAR(1000) NULL,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_operation_event_event_id (event_id),
    KEY idx_operation_event_operation_id (operation_id, id),
    KEY idx_operation_event_line_id (work_line, id),
    KEY idx_operation_event_qr_code (qr_code),
    KEY idx_operation_event_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
