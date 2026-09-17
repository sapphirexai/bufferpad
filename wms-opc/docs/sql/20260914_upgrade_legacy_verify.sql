-- 升级后只读核验（20260915修订3，大表不做全表COUNT）；先选择已升级数据库。
SET NAMES utf8mb4;
SELECT DATABASE() AS checked_database,VERSION() AS mysql_version;
SELECT 'cushion_info' AS table_name,COUNT(*) AS row_count FROM cushion_info
UNION ALL SELECT 'device_info',COUNT(*) FROM device_info
UNION ALL SELECT 'plc_addr',COUNT(*) FROM plc_addr;
-- TABLE_ROWS 是估计值，不能作为精确完整性校验；核验仅查询日志表元数据，不读取实际日志。
SELECT table_name,table_rows AS estimated_rows,data_length,index_length,auto_increment
FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name IN ('cushion_detail','scan_log');

-- 以下结果应全部为 0；历史 scanner_id 为空在后面单独报告。
SELECT 'invalid_plc_scanner_reference' AS check_name,COUNT(*) AS violations
FROM plc_addr a
LEFT JOIN device_info p ON p.id=a.plc_id AND p.type IN (1,2,3)
LEFT JOIN device_info s ON s.id=a.scanner_id AND s.type=0 AND s.work_line=p.work_line
WHERE p.id IS NULL OR s.id IS NULL
UNION ALL
SELECT 'unsupported_address_type',COUNT(*) FROM plc_addr WHERE type NOT BETWEEN 0 AND 7
UNION ALL
SELECT 'unsupported_device_type',COUNT(*) FROM device_info WHERE type NOT BETWEEN 0 AND 3
UNION ALL
SELECT 'duplicate_scanner_address_type',COUNT(*) FROM (
    SELECT scanner_id,type FROM plc_addr GROUP BY scanner_id,type HAVING COUNT(*)>1
) duplicated
UNION ALL
SELECT 'scanner_has_multiple_plcs',COUNT(*) FROM (
    SELECT scanner_id FROM plc_addr GROUP BY scanner_id HAVING COUNT(DISTINCT plc_id)>1
) multiple_plcs
UNION ALL
SELECT 'missing_device_position_dictionary',COUNT(*)
FROM device_info d LEFT JOIN device_install_position p ON p.id=d.install_seq WHERE p.id IS NULL;

SELECT table_name,column_name,column_type,is_nullable,column_default
FROM information_schema.columns
WHERE table_schema=DATABASE() AND (
    (table_name='device_info' AND column_name='position') OR
    (table_name='cushion_info' AND column_name IN ('scanner_id','scanner_position','last_scan_date','open_count')) OR
    (table_name='cushion_detail' AND column_name IN ('scanner_id','open_count')) OR
    (table_name='plc_addr' AND column_name IN ('scanner_id','scanner_seq','addr')) OR
    (table_name='sys_user' AND column_name='must_change_password') OR table_name IN ('scan_log','sys_builtin_session')
) ORDER BY table_name,ordinal_position;

SELECT table_name,index_name,non_unique,GROUP_CONCAT(column_name ORDER BY seq_in_index) AS index_columns
FROM information_schema.statistics WHERE table_schema=DATABASE()
AND table_name IN ('cushion_info','plc_addr','operation_event','sys_user','sys_user_session','scan_log','sys_builtin_session')
GROUP BY table_name,index_name,non_unique ORDER BY table_name,index_name;

SELECT id,cushion_max_use_count FROM opc_config ORDER BY id;
SELECT id,name,sort_no FROM device_install_position ORDER BY id;
SELECT 'cushion_info' AS table_name,COUNT(*) AS historical_unknown_scanner FROM cushion_info WHERE scanner_id IS NULL;
-- 明细精确处理数、未知扫码器数量见升级脚本的分批累计输出，不额外扫描大表。
-- 不查询密码哈希或会话令牌。
SELECT id,username,role,enabled,must_change_password FROM sys_user ORDER BY id;

-- 以下新增结构检查也应全部为0；不查询日志内容、密码哈希或会话令牌。
SELECT 'missing_scan_log_summary_columns' AS check_name,12-COUNT(*) AS violations FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name IN ('operation_id','operation_type','status','result_code','work_line','operator_name','scanner_id','plc_id','scanner_snapshot','plc_snapshot','detail_json','updated_date');
SELECT 'scan_log_msg_not_text' AS check_name,1-COUNT(*) AS violations FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='msg' AND data_type IN ('text','mediumtext','longtext') AND is_nullable='NO';
SELECT 'uk_scan_log_operation' AS check_name,1-COUNT(*) AS violations FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='uk_scan_log_operation' GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='operation_id' AND MIN(non_unique)=0 AND MAX(non_unique)=0 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES') valid_index;
SELECT 'idx_scan_log_created_id' AS check_name,1-COUNT(*) AS violations FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_created_id' GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES') valid_index;
SELECT 'idx_scan_log_status_time' AS check_name,1-COUNT(*) AS violations FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_status_time' GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='status,updated_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES') valid_index;
SELECT 'idx_scan_log_scanner_time' AS check_name,1-COUNT(*) AS violations FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_scanner_time' GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='scanner_id,created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES') valid_index;
SELECT 'idx_scan_log_plc_time' AS check_name,1-COUNT(*) AS violations FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_plc_time' GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='plc_id,created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES') valid_index;
SELECT 'missing_builtin_session_columns' AS check_name,2-COUNT(*) AS violations FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='sys_builtin_session' AND column_name IN ('token_hash','created_at');
SELECT 'reserved_superadmin_database_account' AS check_name,COUNT(*) AS violations FROM sys_user WHERE LOWER(username)='superadmin';
