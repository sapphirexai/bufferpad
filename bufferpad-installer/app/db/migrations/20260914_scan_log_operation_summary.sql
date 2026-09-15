-- MySQL 8.0. Back up scan_log and stop this application's writers before running.
-- VARCHAR to TEXT rebuilds the table. COPY is deliberate, not an online migration.
SET @bp_summary_lock_wait=@@SESSION.lock_wait_timeout;
SET SESSION lock_wait_timeout=15;
DROP PROCEDURE IF EXISTS bp_scan_summary_20260914;
DELIMITER $$
CREATE PROCEDURE bp_scan_summary_20260914()
BEGIN
 DECLARE changes LONGTEXT DEFAULT '';
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN SET SESSION lock_wait_timeout=@bp_summary_lock_wait; RESIGNAL; END;
 IF DATABASE() IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select the database first'; END IF;
 IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='msg' AND data_type NOT IN ('text','mediumtext','longtext')) THEN
   SET changes=CONCAT('MODIFY COLUMN msg TEXT NOT NULL DEFAULT (',CHAR(39),CHAR(39),')');
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_id') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN operation_id varchar(64) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_type') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN operation_type varchar(24) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='status') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN status varchar(16) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='result_code') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN result_code varchar(48) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='work_line') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN work_line int NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operator_name') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN operator_name varchar(64) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_id') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN scanner_id bigint NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_id') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN plc_id bigint NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_snapshot') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN scanner_snapshot varchar(1024) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_snapshot') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN plc_snapshot varchar(1024) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='detail_json') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN detail_json text NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='updated_date') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD COLUMN updated_date datetime(3) NULL'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='uk_scan_log_operation') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD UNIQUE INDEX uk_scan_log_operation(operation_id)'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_status_time') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD INDEX idx_scan_log_status_time(status,updated_date,id)'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_scanner_time') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD INDEX idx_scan_log_scanner_time(scanner_id,created_date,id)'); END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_plc_time') THEN SET changes=CONCAT(changes,IF(changes='','',','),'ADD INDEX idx_scan_log_plc_time(plc_id,created_date,id)'); END IF;
 IF changes<>'' THEN
 SET @bp_summary_ddl=CONCAT('ALTER TABLE scan_log ',changes,', ALGORITHM=COPY, LOCK=SHARED');
 PREPARE bp_summary_stmt FROM @bp_summary_ddl; EXECUTE bp_summary_stmt; DEALLOCATE PREPARE bp_summary_stmt;
 END IF;
 SELECT 'SCAN_SUMMARY_SCHEMA_OK' AS result;
END$$
DELIMITER ;
CALL bp_scan_summary_20260914();
DROP PROCEDURE bp_scan_summary_20260914;
SET SESSION lock_wait_timeout=@bp_summary_lock_wait;
