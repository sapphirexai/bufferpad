-- Add the cleanup index to an existing operation_event table.
-- Safe to execute repeatedly on MySQL 8.
SET @index_exists = (
    SELECT COUNT(1)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'operation_event'
      AND index_name = 'idx_operation_event_created_date'
);
SET @index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE operation_event ADD INDEX idx_operation_event_created_date (created_date)',
    'SELECT 1'
);
PREPARE operation_event_index_statement FROM @index_sql;
EXECUTE operation_event_index_statement;
DEALLOCATE PREPARE operation_event_index_statement;
