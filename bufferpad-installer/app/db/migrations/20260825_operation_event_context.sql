-- Persist scanner and PLC identities on operation events so historical alerts
-- can be traced to the exact devices that participated in the operation.
-- Safe to execute repeatedly on MySQL 8.
SET @scanner_name_exists = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'operation_event' AND column_name = 'scanner_name'
);
SET @scanner_name_sql = IF(@scanner_name_exists = 0,
    'ALTER TABLE operation_event ADD COLUMN scanner_name VARCHAR(128) NULL AFTER scanner_seq', 'SELECT 1');
PREPARE operation_event_scanner_name_statement FROM @scanner_name_sql;
EXECUTE operation_event_scanner_name_statement;
DEALLOCATE PREPARE operation_event_scanner_name_statement;

SET @scanner_ip_exists = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'operation_event' AND column_name = 'scanner_ip'
);
SET @scanner_ip_sql = IF(@scanner_ip_exists = 0,
    'ALTER TABLE operation_event ADD COLUMN scanner_ip VARCHAR(64) NULL AFTER scanner_name', 'SELECT 1');
PREPARE operation_event_scanner_ip_statement FROM @scanner_ip_sql;
EXECUTE operation_event_scanner_ip_statement;
DEALLOCATE PREPARE operation_event_scanner_ip_statement;

SET @plc_id_exists = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'operation_event' AND column_name = 'plc_id'
);
SET @plc_id_sql = IF(@plc_id_exists = 0,
    'ALTER TABLE operation_event ADD COLUMN plc_id BIGINT NULL AFTER scanner_ip', 'SELECT 1');
PREPARE operation_event_plc_id_statement FROM @plc_id_sql;
EXECUTE operation_event_plc_id_statement;
DEALLOCATE PREPARE operation_event_plc_id_statement;

SET @plc_name_exists = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'operation_event' AND column_name = 'plc_name'
);
SET @plc_name_sql = IF(@plc_name_exists = 0,
    'ALTER TABLE operation_event ADD COLUMN plc_name VARCHAR(128) NULL AFTER plc_id', 'SELECT 1');
PREPARE operation_event_plc_name_statement FROM @plc_name_sql;
EXECUTE operation_event_plc_name_statement;
DEALLOCATE PREPARE operation_event_plc_name_statement;

SET @plc_ip_exists = (
    SELECT COUNT(1) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'operation_event' AND column_name = 'plc_ip'
);
SET @plc_ip_sql = IF(@plc_ip_exists = 0,
    'ALTER TABLE operation_event ADD COLUMN plc_ip VARCHAR(64) NULL AFTER plc_name', 'SELECT 1');
PREPARE operation_event_plc_ip_statement FROM @plc_ip_sql;
EXECUTE operation_event_plc_ip_statement;
DEALLOCATE PREPARE operation_event_plc_ip_statement;

UPDATE operation_event oe
LEFT JOIN device_info scanner ON scanner.id = oe.scanner_id
LEFT JOIN device_info plc ON plc.id = CASE WHEN oe.code LIKE 'PLC_%' THEN oe.device_id ELSE oe.plc_id END
SET oe.scanner_name = COALESCE(NULLIF(oe.scanner_name, ''), scanner.name,
                               CASE WHEN oe.code NOT LIKE 'PLC_%' THEN oe.device_name END),
    oe.scanner_ip = COALESCE(NULLIF(oe.scanner_ip, ''), scanner.ip),
    oe.plc_id = COALESCE(oe.plc_id, CASE WHEN oe.code LIKE 'PLC_%' THEN oe.device_id END),
    oe.plc_name = COALESCE(NULLIF(oe.plc_name, ''), plc.name,
                           CASE WHEN oe.code LIKE 'PLC_%' THEN oe.device_name END),
    oe.plc_ip = COALESCE(NULLIF(oe.plc_ip, ''), plc.ip);
