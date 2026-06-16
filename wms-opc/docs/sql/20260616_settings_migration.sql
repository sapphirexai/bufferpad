-- Settings module and PLC address model migration.
-- Compatible with MySQL 8.0.20.

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS assert_plc_addr_scanner_integrity;
DELIMITER //
CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN alter_sql_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @alter_sql = alter_sql_value;
        PREPARE alter_stmt FROM @alter_sql;
        EXECUTE alter_stmt;
        DEALLOCATE PREPARE alter_stmt;
    END IF;
END//
CREATE PROCEDURE drop_column_if_exists(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN alter_sql_value TEXT
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = table_name_value
          AND COLUMN_NAME = column_name_value
    ) THEN
        SET @alter_sql = alter_sql_value;
        PREPARE alter_stmt FROM @alter_sql;
        EXECUTE alter_stmt;
        DEALLOCATE PREPARE alter_stmt;
    END IF;
END//
CREATE PROCEDURE assert_plc_addr_scanner_integrity()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM plc_addr
        WHERE scanner_id IS NULL
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'plc_addr.scanner_id has null values after backfill';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT scanner_id, type
            FROM plc_addr
            GROUP BY scanner_id, type
            HAVING COUNT(1) > 1
        ) duplicated
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'plc_addr has duplicate scanner_id and type values';
    END IF;
END//
DELIMITER ;

CREATE TABLE IF NOT EXISTS device_install_position (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    sort_no INT DEFAULT 0,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    modified_date TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP
);

INSERT INTO device_install_position (id, name, sort_no)
VALUES
    (1, CONVERT(0xE4B88A USING utf8mb4), 1),
    (2, CONVERT(0xE4B88B USING utf8mb4), 2),
    (3, CONVERT(0xE997B4E5B18231 USING utf8mb4), 3),
    (4, CONVERT(0xE997B4E5B18232 USING utf8mb4), 4)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_no = VALUES(sort_no);

INSERT INTO opc_config (id, cushion_max_use_count)
VALUES (1, 500)
ON DUPLICATE KEY UPDATE
    cushion_max_use_count = IFNULL(cushion_max_use_count, 500);

CALL add_column_if_missing(
    'device_info',
    'install_seq',
    'ALTER TABLE device_info ADD COLUMN install_seq INT NULL COMMENT ''device_install_position.id'''
);

UPDATE device_info di
LEFT JOIN device_install_position dip ON dip.id = di.install_seq
SET di.position = dip.name
WHERE dip.name IS NOT NULL;

CALL add_column_if_missing(
    'plc_addr',
    'scanner_id',
    'ALTER TABLE plc_addr ADD COLUMN scanner_id BIGINT NULL COMMENT ''scanner device_info.id'''
);

SET @plc_scanner_seq_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plc_addr'
      AND COLUMN_NAME = 'scanner_seq'
);

SET @backfill_plc_scanner_sql = IF(
    @plc_scanner_seq_exists > 0,
    'UPDATE plc_addr pa LEFT JOIN device_info scanner ON scanner.type = 0 AND scanner.install_seq = pa.scanner_seq SET pa.scanner_id = scanner.id WHERE pa.scanner_id IS NULL',
    'SELECT 1'
);

PREPARE backfill_plc_scanner_stmt FROM @backfill_plc_scanner_sql;
EXECUTE backfill_plc_scanner_stmt;
DEALLOCATE PREPARE backfill_plc_scanner_stmt;

CALL assert_plc_addr_scanner_integrity();

ALTER TABLE plc_addr
    MODIFY COLUMN scanner_id BIGINT NOT NULL COMMENT 'scanner device_info.id';

CALL drop_column_if_exists(
    'plc_addr',
    'install_position_id',
    'ALTER TABLE plc_addr DROP COLUMN install_position_id'
);

CALL drop_column_if_exists(
    'plc_addr',
    'scanner_seq',
    'ALTER TABLE plc_addr DROP COLUMN scanner_seq'
);

CALL add_column_if_missing(
    'cushion_info',
    'scanner_id',
    'ALTER TABLE cushion_info ADD COLUMN scanner_id BIGINT NULL COMMENT ''scanner device_info.id'''
);

UPDATE cushion_info ci
LEFT JOIN device_info scanner
    ON scanner.type = 0 AND scanner.install_seq = ci.scanner_seq
SET ci.scanner_id = scanner.id
WHERE ci.scanner_id IS NULL;

CALL add_column_if_missing(
    'cushion_detail',
    'scanner_id',
    'ALTER TABLE cushion_detail ADD COLUMN scanner_id BIGINT NULL COMMENT ''scanner device_info.id'''
);

UPDATE cushion_detail cd
LEFT JOIN device_info scanner
    ON scanner.type = 0 AND scanner.install_seq = cd.scanner_seq
SET cd.scanner_id = scanner.id
WHERE cd.scanner_id IS NULL;

SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'plc_addr'
      AND INDEX_NAME = 'uk_plc_addr_scanner_type'
);

SET @create_index_sql = IF(
    @index_exists = 0,
    'CREATE UNIQUE INDEX uk_plc_addr_scanner_type ON plc_addr (scanner_id, type)',
    'SELECT 1'
);

PREPARE create_index_stmt FROM @create_index_sql;
EXECUTE create_index_stmt;
DEALLOCATE PREPARE create_index_stmt;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DROP PROCEDURE IF EXISTS drop_column_if_exists;
DROP PROCEDURE IF EXISTS assert_plc_addr_scanner_integrity;
