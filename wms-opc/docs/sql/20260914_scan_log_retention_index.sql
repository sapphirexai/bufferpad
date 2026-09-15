-- 扫描日志查询/清理时间索引；只增加索引，不删除日志。
-- 先备份；MySQL 8.0；不要使用 --force。DDL 隐式提交，不能整体回滚。
SET @bp_scan_log_old_lock_wait=@@SESSION.lock_wait_timeout;
SET SESSION lock_wait_timeout=15;
DROP PROCEDURE IF EXISTS bp_scan_log_time_index_20260914;
DELIMITER $$
CREATE PROCEDURE bp_scan_log_time_index_20260914()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET SESSION lock_wait_timeout=@bp_scan_log_old_lock_wait;
        RESIGNAL;
    END;
    IF DATABASE() IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select the target database first'; END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
            AND table_name='scan_log' AND seq_in_index=1 AND column_name='created_date') THEN
        ALTER TABLE scan_log ADD INDEX idx_scan_log_created_id(created_date,id), ALGORITHM=INPLACE, LOCK=NONE;
    END IF;
    SELECT 'SCAN_LOG_INDEX_OK' AS result;
END$$
DELIMITER ;
CALL bp_scan_log_time_index_20260914();
DROP PROCEDURE bp_scan_log_time_index_20260914;
SET SESSION lock_wait_timeout=@bp_scan_log_old_lock_wait;
