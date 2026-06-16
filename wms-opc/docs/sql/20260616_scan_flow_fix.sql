-- Scan flow integrity hardening.
-- Compatible with MySQL 8.0.20.

DROP PROCEDURE IF EXISTS assert_cushion_info_qr_code_integrity;
DELIMITER //
CREATE PROCEDURE assert_cushion_info_qr_code_integrity()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM (
            SELECT qr_code
            FROM cushion_info
            WHERE qr_code IS NOT NULL
            GROUP BY qr_code
            HAVING COUNT(1) > 1
        ) duplicated
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'cushion_info has duplicate qr_code values';
    END IF;
END//
DELIMITER ;

CALL assert_cushion_info_qr_code_integrity();

SET @qr_code_unique_index_exists = (
    SELECT COUNT(1)
    FROM (
        SELECT INDEX_NAME
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'cushion_info'
          AND NON_UNIQUE = 0
        GROUP BY INDEX_NAME
        HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'qr_code'
    ) indexes_on_qr_code
);

SET @create_qr_code_unique_index_sql = IF(
    @qr_code_unique_index_exists = 0,
    'CREATE UNIQUE INDEX uk_cushion_info_qr_code ON cushion_info (qr_code)',
    'SELECT 1'
);

PREPARE create_qr_code_unique_index_stmt FROM @create_qr_code_unique_index_sql;
EXECUTE create_qr_code_unique_index_stmt;
DEALLOCATE PREPARE create_qr_code_unique_index_stmt;

DROP PROCEDURE IF EXISTS assert_cushion_info_qr_code_integrity;
