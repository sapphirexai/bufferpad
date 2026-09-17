-- BufferPad 旧库 -> 当前版本，2026-09-16 修订 5（兼容 Workbench 安全更新模式）；MySQL 8.0.20+。
-- 在已选中的旧库或其完整恢复副本执行；不是跨服务器合并/覆盖脚本。
-- 先停止旧、新后端写入并备份。DDL 会隐式提交，不能依靠 ROLLBACK 撤销整个升级。
-- mysql --default-character-set=utf8mb4 -h HOST -P 3306 -u root -p wms_opc < 本文件
-- 不使用 --force / 遇错继续。支持完整升级后重跑，也支持本脚本中断后的重跑。
-- 不删除业务表/记录；不覆盖现有密码、会话、位置名称、寿命值。
-- scanner_id 按“PLC 所属产线 + 旧 scanner_seq”转换，关联冲突时停止。
-- 保留旧时间列的小数秒精度；不为了与 149 的 datetime(0) 一致而丢失时间精度。
-- cushion_detail 仅瞬时追加 scanner_id，保留 SMALLINT/INT 原类型，不复制整表。
-- 历史关联按主键每批最多 5000 条提交；失败重跑会跳过已填好的关联，原记录和时间不变。
-- scan_log 保留全部记录；msg改TEXT、追加可空字段和5个索引，合并一次COPY表重建。
-- 已有新版字段/索引保留；不写入superadmin用户或密码。完整成功只在末尾输出UPGRADE_OK。
-- 日志表重建需要停写、可恢复备份和充足磁盘空间；不在SQL中触发三个月清理。
-- 可在执行前 SET @bp_upgrade_detail_batch_size=1000; 允许 100~20000，默认 5000。
-- 过程仅在数据回填期间临时关闭当前会话 SQL_SAFE_UPDATES，成功/异常均恢复；不改全局设置。
SET NAMES utf8mb4;
SET @bp_upgrade_original_sql_mode = @@SESSION.sql_mode;
SET @bp_upgrade_original_lock_wait = @@SESSION.lock_wait_timeout;
SET @bp_upgrade_original_row_lock_wait = @@SESSION.innodb_lock_wait_timeout;
SET @bp_upgrade_original_autocommit = @@SESSION.autocommit;
SET SESSION lock_wait_timeout = 15;
SET SESSION innodb_lock_wait_timeout = 15;
SET SESSION autocommit = 1;
SET SESSION sql_mode = 'ONLY_FULL_GROUP_BY,STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

DROP PROCEDURE IF EXISTS bp_upgrade_20260914;
DROP PROCEDURE IF EXISTS bp_upgrade_scan_log_20260915;
DROP PROCEDURE IF EXISTS bp_upgrade_add_column_20260914;
DROP PROCEDURE IF EXISTS bp_upgrade_add_index_20260914;
DELIMITER $$
CREATE PROCEDURE bp_upgrade_add_column_20260914(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=p_table AND column_name=p_column) THEN
        SET @bp_ddl = CONCAT('ALTER TABLE `',p_table,'` ADD COLUMN `',p_column,'` ',p_definition);
        PREPARE bp_stmt FROM @bp_ddl; EXECUTE bp_stmt; DEALLOCATE PREPARE bp_stmt;
    END IF;
END$$
CREATE PROCEDURE bp_upgrade_add_index_20260914(IN p_table VARCHAR(64), IN p_index VARCHAR(64), IN p_definition TEXT)
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name=p_table AND index_name=p_index) THEN
        SET @bp_ddl = CONCAT('ALTER TABLE `',p_table,'` ADD ',p_definition);
        PREPARE bp_stmt FROM @bp_ddl; EXECUTE bp_stmt; DEALLOCATE PREPARE bp_stmt;
    END IF;
END$$
CREATE PROCEDURE bp_upgrade_scan_log_20260915(IN p_apply BOOLEAN)
BEGIN
    DECLARE v_changes LONGTEXT DEFAULT '';
    DECLARE v_rebuild BOOLEAN DEFAULT FALSE;
    IF NOT EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='scan_log' AND engine='InnoDB') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='scan_log must be InnoDB; review custom storage before upgrade';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name IN ('id','qr_code','msg','msg_type','created_date'))<>5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='scan_log legacy columns are missing';
    END IF;
    IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='msg' AND data_type IN ('varchar','text','mediumtext','longtext') AND is_nullable='NO') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected scan_log.msg type/nullability; review before conversion';
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='msg' AND data_type='varchar') THEN
        SET v_changes=CONCAT('MODIFY COLUMN msg TEXT NOT NULL DEFAULT (',CHAR(39),CHAR(39),')');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_id') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_id' AND data_type='varchar' AND character_maximum_length>=64 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.operation_id; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN operation_id varchar(64) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_type') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_type' AND data_type='varchar' AND character_maximum_length>=24 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.operation_type; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN operation_type varchar(24) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='status') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='status' AND data_type='varchar' AND character_maximum_length>=16 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.status; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN status varchar(16) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='result_code') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='result_code' AND data_type='varchar' AND character_maximum_length>=48 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.result_code; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN result_code varchar(48) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='work_line') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='work_line' AND data_type='int' AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.work_line; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN work_line int NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operator_name') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operator_name' AND data_type='varchar' AND character_maximum_length>=64 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.operator_name; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN operator_name varchar(64) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_id') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_id' AND data_type='bigint' AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.scanner_id; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN scanner_id bigint NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_id') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_id' AND data_type='bigint' AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.plc_id; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN plc_id bigint NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_snapshot') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='scanner_snapshot' AND data_type='varchar' AND character_maximum_length>=1024 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.scanner_snapshot; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN scanner_snapshot varchar(1024) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_snapshot') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='plc_snapshot' AND data_type='varchar' AND character_maximum_length>=1024 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.plc_snapshot; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN plc_snapshot varchar(1024) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='detail_json') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='detail_json' AND data_type IN ('text','mediumtext','longtext') AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.detail_json; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN detail_json text NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='updated_date') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='updated_date' AND data_type='datetime' AND datetime_precision>=3 AND is_nullable='YES' AND extra NOT LIKE '%GENERATED%') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Incompatible existing scan_log.updated_date; review without overwriting data';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD COLUMN updated_date datetime(3) NULL');
        SET v_rebuild=TRUE;
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='uk_scan_log_operation') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='uk_scan_log_operation' GROUP BY index_name
                HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='operation_id' AND MIN(non_unique)=0 AND MAX(non_unique)=0 AND SUM(sub_part IS NOT NULL)=0 AND MIN(index_type)='BTREE' AND MIN(is_visible)='YES') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting scan_log index uk_scan_log_operation; review before upgrade';
        END IF;
    ELSE
        IF EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='scan_log' AND column_name='operation_id') THEN
            IF EXISTS(SELECT operation_id FROM scan_log WHERE operation_id IS NOT NULL GROUP BY operation_id HAVING COUNT(*)>1 LIMIT 1) THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate scan_log operation_id; rows were not merged or deleted';
            END IF;
        END IF;
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD UNIQUE INDEX uk_scan_log_operation (operation_id)');
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_created_id') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_created_id' GROUP BY index_name
                HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(index_type)='BTREE' AND MIN(is_visible)='YES') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting scan_log index idx_scan_log_created_id; review before upgrade';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD INDEX idx_scan_log_created_id (created_date,id)');
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_status_time') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_status_time' GROUP BY index_name
                HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='status,updated_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(index_type)='BTREE' AND MIN(is_visible)='YES') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting scan_log index idx_scan_log_status_time; review before upgrade';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD INDEX idx_scan_log_status_time (status,updated_date,id)');
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_scanner_time') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_scanner_time' GROUP BY index_name
                HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='scanner_id,created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(index_type)='BTREE' AND MIN(is_visible)='YES') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting scan_log index idx_scan_log_scanner_time; review before upgrade';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD INDEX idx_scan_log_scanner_time (scanner_id,created_date,id)');
    END IF;
    IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_plc_time') THEN
        IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='scan_log' AND index_name='idx_scan_log_plc_time' GROUP BY index_name
                HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='plc_id,created_date,id' AND MIN(non_unique)=1 AND MAX(non_unique)=1 AND SUM(sub_part IS NOT NULL)=0 AND MIN(index_type)='BTREE' AND MIN(is_visible)='YES') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Conflicting scan_log index idx_scan_log_plc_time; review before upgrade';
        END IF;
    ELSE
        SET v_changes=CONCAT(v_changes,IF(v_changes='','',','),'ADD INDEX idx_scan_log_plc_time (plc_id,created_date,id)');
    END IF;
    IF NOT p_apply THEN
        SELECT 'SCAN_LOG_PREFLIGHT' AS progress,IF(v_changes='','NONE',IF(v_rebuild,'COPY','INPLACE')) AS required_algorithm,
            data_length+index_length AS estimated_table_bytes,'Stop writers and verify backup/free disk space before applying' AS note
        FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='scan_log';
    ELSEIF v_changes<>'' THEN
        SET @bp_log_ddl=CONCAT('ALTER TABLE scan_log ',v_changes,IF(v_rebuild,', ALGORITHM=COPY, LOCK=SHARED',', ALGORITHM=INPLACE, LOCK=NONE'));
        PREPARE bp_log_stmt FROM @bp_log_ddl; EXECUTE bp_log_stmt; DEALLOCATE PREPARE bp_log_stmt;
    END IF;
END$$
CREATE PROCEDURE bp_upgrade_20260914()
BEGIN
    DECLARE v_original_safe_updates BOOLEAN DEFAULT @@SESSION.sql_safe_updates;
    DECLARE v_lock INT DEFAULT 0;
    DECLARE v_lock_name VARCHAR(64);
    DECLARE v_has_seq INT DEFAULT 0;
    DECLARE v_has_id INT DEFAULT 0;
    DECLARE v_info BIGINT;
    DECLARE v_detail BIGINT DEFAULT 0;
    DECLARE v_devices BIGINT;
    DECLARE v_plc BIGINT;
    DECLARE v_detail_last BIGINT DEFAULT NULL;
    DECLARE v_detail_high BIGINT DEFAULT NULL;
    DECLARE v_detail_end BIGINT DEFAULT NULL;
    DECLARE v_detail_batch_rows INT DEFAULT 0;
    DECLARE v_detail_batch_size INT DEFAULT 5000;
    DECLARE v_detail_batches INT DEFAULT 0;
    DECLARE v_detail_updated BIGINT DEFAULT 0;
    DECLARE v_detail_unknown BIGINT DEFAULT 0;
    DECLARE v_detail_batch_unknown INT DEFAULT 0;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        SET SESSION sql_safe_updates = v_original_safe_updates;
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS bp_upgrade_scanner_map;
        IF v_lock = 1 THEN DO RELEASE_LOCK(v_lock_name); END IF;
        SET SESSION sql_mode = @bp_upgrade_original_sql_mode;
        SET SESSION lock_wait_timeout = @bp_upgrade_original_lock_wait;
        SET SESSION innodb_lock_wait_timeout = @bp_upgrade_original_row_lock_wait;
        SET SESSION autocommit = @bp_upgrade_original_autocommit;
        RESIGNAL;
    END;
    IF DATABASE() IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Select the restored legacy database first'; END IF;
    IF VERSION() NOT REGEXP '^8\\.' THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='This upgrade requires MySQL 8.0.20 or later in the MySQL 8 family'; END IF;
    SET v_lock_name = CONCAT('bufferpad-upgrade:', LEFT(DATABASE(),45));
    SELECT GET_LOCK(v_lock_name,0) INTO v_lock;
    IF COALESCE(v_lock,0) <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Another BufferPad upgrade is running'; END IF;
    IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_type='BASE TABLE' AND table_name IN ('cushion_info','cushion_detail','device_info','plc_addr','opc_config','scan_log')) <> 6 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected all six legacy business tables; restore the complete legacy backup first';
    END IF;
    IF @bp_upgrade_detail_batch_size IS NOT NULL AND
       (@bp_upgrade_detail_batch_size < 100 OR @bp_upgrade_detail_batch_size > 20000 OR @bp_upgrade_detail_batch_size <> FLOOR(@bp_upgrade_detail_batch_size)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='detail_batch_size must be an integer between 100 and 20000';
    END IF;
    SET v_detail_batch_size=COALESCE(@bp_upgrade_detail_batch_size,5000);
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='cushion_detail' AND (engine<>'InnoDB' OR row_format='Compressed')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='cushion_detail must support INSTANT ADD COLUMN; no automatic table-copy fallback';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='cushion_detail' AND index_type='FULLTEXT') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='cushion_detail FULLTEXT prevents INSTANT ADD COLUMN; schedule a separate migration';
    END IF;
    IF EXISTS (SELECT 1 FROM information_schema.triggers WHERE trigger_schema=DATABASE() AND event_object_table='cushion_detail' AND event_manipulation='UPDATE') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Review cushion_detail UPDATE triggers before batch backfill; cannot guarantee historical values';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cushion_detail' AND column_name='open_count' AND data_type IN ('smallint','int')) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unexpected cushion_detail.open_count type; no automatic table rebuild';
    END IF;
    -- All mapping/range validations precede persistent table or business data changes.
    IF EXISTS (SELECT 1 FROM device_info WHERE type NOT IN (0,1,2,3,4) OR install_seq < 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unsupported device type or non-positive install_seq';
    END IF;
    IF EXISTS (SELECT 1 FROM device_info WHERE type=0 GROUP BY work_line,install_seq HAVING COUNT(*) > 1) THEN
        SELECT work_line,install_seq,GROUP_CONCAT(id ORDER BY id) AS conflicting_scanner_ids FROM device_info WHERE type=0 GROUP BY work_line,install_seq HAVING COUNT(*) > 1;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Ambiguous scanners: duplicate work_line + install_seq; fix before upgrade';
    END IF;
    IF EXISTS (SELECT 1 FROM cushion_info WHERE open_count NOT BETWEEN -32768 AND 32767) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='cushion_info.open_count exceeds current SMALLINT range; no data was truncated';
    END IF;
    IF EXISTS (SELECT 1 FROM cushion_info GROUP BY qr_code HAVING COUNT(*) > 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate cushion QR codes require manual reconciliation';
    END IF;
    IF EXISTS (SELECT 1 FROM opc_config) AND NOT EXISTS (SELECT 1 FROM opc_config WHERE id=1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='opc_config has custom IDs but no id=1; select the effective lifetime configuration first';
    END IF;
    IF EXISTS (SELECT 1 FROM plc_addr WHERE type NOT BETWEEN 0 AND 13) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Unknown legacy PLC address type; cannot translate safely';
    END IF;
    SELECT COUNT(*) INTO v_has_seq FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='plc_addr' AND column_name='scanner_seq';
    SELECT COUNT(*) INTO v_has_id FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='plc_addr' AND column_name='scanner_id';
    IF v_has_seq=0 AND v_has_id=0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='plc_addr has neither scanner_seq nor scanner_id'; END IF;
    DROP TEMPORARY TABLE IF EXISTS bp_upgrade_scanner_map;
    CREATE TEMPORARY TABLE bp_upgrade_scanner_map(id BIGINT PRIMARY KEY, scanner_id BIGINT NULL, address_type INT NOT NULL);
    IF v_has_seq > 0 THEN
        SET @bp_map_sql = CONCAT('INSERT INTO bp_upgrade_scanner_map SELECT a.id, ',
            IF(v_has_id > 0,'COALESCE(a.scanner_id,s.id)','s.id'),
            ', CASE WHEN a.type IN (8,10,12) THEN 6 WHEN a.type IN (9,11,13) THEN 7 ELSE a.type END FROM plc_addr a LEFT JOIN device_info p ON p.id=a.plc_id AND p.type IN (1,2,3,4)',
            ' LEFT JOIN device_info s ON s.type=0 AND s.work_line=p.work_line AND s.install_seq=a.scanner_seq');
    ELSE
        SET @bp_map_sql = 'INSERT INTO bp_upgrade_scanner_map SELECT id,scanner_id,CASE WHEN type IN (8,10,12) THEN 6 WHEN type IN (9,11,13) THEN 7 ELSE type END FROM plc_addr';
    END IF;
    PREPARE bp_stmt FROM @bp_map_sql; EXECUTE bp_stmt; DEALLOCATE PREPARE bp_stmt;
    IF v_has_seq>0 THEN
        IF EXISTS (SELECT 1 FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id
            JOIN device_info s ON s.id=m.scanner_id WHERE a.scanner_seq IS NOT NULL AND a.scanner_seq<>s.install_seq) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing scanner_id conflicts with legacy scanner_seq';
        END IF;
    END IF;
    IF EXISTS (SELECT 1 FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id
        LEFT JOIN device_info p ON p.id=a.plc_id AND p.type IN (1,2,3,4)
        LEFT JOIN device_info s ON s.id=m.scanner_id AND s.type=0 AND s.work_line=p.work_line
        WHERE p.id IS NULL OR s.id IS NULL) THEN
        SELECT a.id,a.plc_id,m.scanner_id AS proposed_scanner_id FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id
        LEFT JOIN device_info p ON p.id=a.plc_id AND p.type IN (1,2,3,4)
        LEFT JOIN device_info s ON s.id=m.scanner_id AND s.type=0 AND s.work_line=p.work_line
        WHERE p.id IS NULL OR s.id IS NULL;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='PLC/scanner missing or on different lines; fix the listed addresses before upgrade';
    END IF;
    IF EXISTS (SELECT 1 FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id GROUP BY m.scanner_id,m.address_type HAVING COUNT(*)>1) THEN
        SELECT m.scanner_id,m.address_type,GROUP_CONCAT(a.id ORDER BY a.id) AS conflicting_address_ids FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id GROUP BY m.scanner_id,m.address_type HAVING COUNT(*)>1;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Duplicate scanner_id + address type; automatic deletion is not permitted';
    END IF;
    IF EXISTS (SELECT 1 FROM plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id GROUP BY m.scanner_id HAVING COUNT(DISTINCT a.plc_id)>1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='One scanner is mapped to multiple PLCs; reconcile before upgrade';
    END IF;
    -- Reject incompatible partial upgrades before changing persistent business tables.
    CALL bp_upgrade_scan_log_20260915(FALSE);
    IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='sys_user') THEN
        IF EXISTS(SELECT 1 FROM sys_user WHERE LOWER(username)='superadmin') THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reserved superadmin name exists in sys_user; reconcile it before upgrade';
        END IF;
    END IF;
    SELECT COUNT(*) INTO v_info FROM cushion_info;
    SELECT COUNT(*) INTO v_devices FROM device_info;
    SELECT COUNT(*) INTO v_plc FROM plc_addr;

    CREATE TABLE IF NOT EXISTS `device_install_position` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
      `sort_no` int DEFAULT '0',
      `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
      `modified_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`) USING BTREE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8 ROW_FORMAT=DYNAMIC;

    CREATE TABLE IF NOT EXISTS `operation_event` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `event_id` varchar(64) NOT NULL,
      `operation_id` varchar(64) DEFAULT NULL,
      `code` varchar(64) NOT NULL,
      `severity` varchar(16) NOT NULL,
      `title` varchar(128) NOT NULL,
      `message` varchar(512) NOT NULL,
      `suggestion` varchar(512) DEFAULT NULL,
      `work_line` int NOT NULL,
      `scanner_id` bigint DEFAULT NULL,
      `scanner_seq` int DEFAULT NULL,
      `scanner_name` varchar(128) DEFAULT NULL,
      `scanner_ip` varchar(64) DEFAULT NULL,
      `plc_id` bigint DEFAULT NULL,
      `plc_name` varchar(128) DEFAULT NULL,
      `plc_ip` varchar(64) DEFAULT NULL,
      `device_id` bigint DEFAULT NULL,
      `device_name` varchar(128) DEFAULT NULL,
      `qr_code` varchar(128) DEFAULT NULL,
      `address` varchar(64) DEFAULT NULL,
      `error_code` int DEFAULT NULL,
      `technical_detail` varchar(1000) DEFAULT NULL,
      `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_operation_event_event_id` (`event_id`),
      KEY `idx_operation_event_line_id` (`work_line`,`id`),
      KEY `idx_operation_event_qr_code` (`qr_code`),
      KEY `idx_operation_event_created_date` (`created_date`),
      KEY `idx_operation_event_operation_id` (`operation_id`,`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

    CREATE TABLE IF NOT EXISTS `sys_user` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `username` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
      `password_hash` varchar(100) COLLATE utf8mb4_general_ci NOT NULL,
      `role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
      `enabled` tinyint(1) NOT NULL DEFAULT '1',
      `must_change_password` tinyint(1) NOT NULL DEFAULT '0',
      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      UNIQUE KEY `username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

    CREATE TABLE IF NOT EXISTS `sys_user_session` (
      `token_hash` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
      `user_id` bigint NOT NULL,
      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`token_hash`),
      KEY `fk_auth_session_user` (`user_id`),
      CONSTRAINT `fk_auth_session_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

    CREATE TABLE IF NOT EXISTS `sys_auth_audit` (
      `id` bigint NOT NULL AUTO_INCREMENT,
      `actor` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
      `action` varchar(40) COLLATE utf8mb4_general_ci NOT NULL,
      `target_username` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL,
      `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

    CREATE TABLE IF NOT EXISTS sys_builtin_session (
        token_hash VARCHAR(64) NOT NULL PRIMARY KEY,
        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

    -- Add fields missing from the six-table legacy schema.
    CALL bp_upgrade_add_column_20260914('device_info','position','VARCHAR(255) NULL DEFAULT NULL COMMENT ''设备位置'' AFTER name');
    CALL bp_upgrade_add_column_20260914('cushion_info','scanner_position','VARCHAR(255) NULL DEFAULT '''' COMMENT ''扫码器位置'' AFTER modified_date');
    CALL bp_upgrade_add_column_20260914('cushion_info','scanner_id','BIGINT NULL COMMENT ''scanner device_info.id''');
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='cushion_detail' AND column_name='scanner_id') THEN
        -- Append only: MySQL 8.0.20 cannot INSTANT-add at an arbitrary position.
        ALTER TABLE cushion_detail ADD COLUMN scanner_id BIGINT NULL COMMENT 'scanner device_info.id', ALGORITHM=INSTANT;
    END IF;
    CALL bp_upgrade_add_column_20260914('plc_addr','scanner_id','BIGINT NULL COMMENT ''scanner device_info.id''');
    CALL bp_upgrade_add_column_20260914('operation_event','operation_id','VARCHAR(64) NULL AFTER event_id');
    CALL bp_upgrade_add_column_20260914('operation_event','scanner_name','VARCHAR(128) NULL AFTER scanner_seq');
    CALL bp_upgrade_add_column_20260914('operation_event','scanner_ip','VARCHAR(64) NULL AFTER scanner_name');
    CALL bp_upgrade_add_column_20260914('operation_event','plc_id','BIGINT NULL AFTER scanner_ip');
    CALL bp_upgrade_add_column_20260914('operation_event','plc_name','VARCHAR(128) NULL AFTER plc_id');
    CALL bp_upgrade_add_column_20260914('operation_event','plc_ip','VARCHAR(64) NULL AFTER plc_name');

    -- Default labels only for missing IDs; never copy the test site's custom position 5.
    INSERT INTO device_install_position(id,name,sort_no)
    SELECT d.id,d.name,d.id FROM (
        SELECT 1 AS id,'上' AS name UNION ALL SELECT 2,'下' UNION ALL SELECT 3,'间层1' UNION ALL SELECT 4,'间层2'
    ) d LEFT JOIN device_install_position p ON p.id=d.id WHERE p.id IS NULL;
    INSERT INTO device_install_position(id,name,sort_no)
    SELECT DISTINCT d.install_seq,CONCAT('位置',d.install_seq),d.install_seq
    FROM device_info d LEFT JOIN device_install_position p ON p.id=d.install_seq WHERE p.id IS NULL;
    INSERT INTO opc_config(id,cushion_max_use_count)
    SELECT 1,500 WHERE NOT EXISTS (SELECT 1 FROM opc_config);

    -- Suppress ON UPDATE timestamps while backfilling derived fields.
    -- Workbench enables safe updates; derived-field backfills legitimately use non-key filters.
    -- Keep this change inside the procedure so both success and handled failure restore it.
    SET SESSION sql_safe_updates = 0;
    UPDATE device_info d JOIN device_install_position p ON p.id=d.install_seq
    SET d.position=p.name,d.modified_date=d.modified_date WHERE d.position IS NULL OR d.position='';
    UPDATE cushion_info c JOIN device_info s ON s.type=0 AND s.work_line=c.work_line AND s.install_seq=c.scanner_seq
    SET c.scanner_id=s.id,c.modified_date=c.modified_date WHERE c.scanner_id IS NULL;
    UPDATE cushion_info c JOIN device_info s ON s.id=c.scanner_id AND s.type=0
    SET c.scanner_position=s.position,c.modified_date=c.modified_date
    WHERE c.scanner_position IS NULL OR c.scanner_position='';
    -- Read PRIMARY-key windows, not OFFSET or one table-wide UPDATE transaction.
    -- Bound the run to existing rows; source writers must remain stopped.
    SELECT MAX(id) INTO v_detail_end FROM cushion_detail;
    detail_batches: LOOP
        SELECT MAX(b.id),COUNT(*) INTO v_detail_high,v_detail_batch_rows FROM (
            SELECT id FROM cushion_detail FORCE INDEX(PRIMARY)
            WHERE (v_detail_last IS NULL OR id>v_detail_last) AND id<=v_detail_end
            ORDER BY id LIMIT v_detail_batch_size
        ) b;
        IF v_detail_batch_rows=0 THEN LEAVE detail_batches; END IF;
        START TRANSACTION;
        UPDATE cushion_detail c FORCE INDEX(PRIMARY)
        STRAIGHT_JOIN device_info s ON s.type=0 AND s.work_line=c.work_line AND s.install_seq=c.scanner_seq
        SET c.scanner_id=s.id,c.modified_date=c.modified_date
        WHERE (v_detail_last IS NULL OR c.id>v_detail_last) AND c.id<=v_detail_high AND c.scanner_id IS NULL;
        SET v_detail_updated=v_detail_updated+ROW_COUNT();
        SELECT COUNT(*) INTO v_detail_batch_unknown FROM cushion_detail FORCE INDEX(PRIMARY)
        WHERE (v_detail_last IS NULL OR id>v_detail_last) AND id<=v_detail_high AND scanner_id IS NULL;
        COMMIT;
        SET v_detail_unknown=v_detail_unknown+v_detail_batch_unknown;
        SET v_detail=v_detail+v_detail_batch_rows;
        SET v_detail_last=v_detail_high;
        SET v_detail_batches=v_detail_batches+1;
        IF MOD(v_detail_batches,10)=0 THEN
            SELECT 'DETAIL_PROGRESS' AS progress,v_detail_last AS last_committed_id,v_detail AS rows_checked,v_detail_updated AS rows_backfilled;
        END IF;
    END LOOP;
    -- History position/open_count/timestamps stay intact. Unknown scanners remain NULL.
    UPDATE plc_addr a JOIN bp_upgrade_scanner_map m ON m.id=a.id SET a.scanner_id=m.scanner_id,a.type=m.address_type WHERE a.scanner_id IS NULL OR a.type<>m.address_type;
    ALTER TABLE plc_addr MODIFY COLUMN scanner_id BIGINT NOT NULL COMMENT 'scanner device_info.id';
    ALTER TABLE plc_addr MODIFY COLUMN addr VARCHAR(64) NOT NULL DEFAULT '' COMMENT 'PLC寄存器地址；西门子S7示例：DB1.DBW0、MW0、IW0、QW0';
    -- Keep the original cushion_detail.open_count type: widening it would COPY the entire table.
    ALTER TABLE cushion_info MODIFY COLUMN open_count SMALLINT NULL DEFAULT 0 COMMENT '开口数';
    UPDATE device_info SET type=3,modified_date=modified_date WHERE type=4;
    ALTER TABLE device_info MODIFY COLUMN type INT NOT NULL DEFAULT 0 COMMENT '设备类型，0:扫码器；1:三菱PLC；2:汇川PLC；3:西门子S7 PLC';
    CALL bp_upgrade_add_index_20260914('plc_addr','uk_plc_addr_scanner_type','UNIQUE INDEX uk_plc_addr_scanner_type (scanner_id,type)');
    IF NOT EXISTS (
        SELECT 1 FROM (SELECT index_name FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='cushion_info' AND non_unique=0 GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='qr_code') q
    ) THEN CREATE UNIQUE INDEX uk_cushion_info_qr_code ON cushion_info(qr_code); END IF;
    CALL bp_upgrade_add_index_20260914('operation_event','idx_operation_event_operation_id','INDEX idx_operation_event_operation_id (operation_id,id)');
    CALL bp_upgrade_add_index_20260914('operation_event','idx_operation_event_created_date','INDEX idx_operation_event_created_date (created_date)');
    UPDATE operation_event SET operation_id=event_id WHERE operation_id IS NULL OR operation_id='';
    UPDATE operation_event e
    LEFT JOIN device_info s ON s.id=e.scanner_id
    LEFT JOIN device_info p ON p.id=COALESCE(e.plc_id,CASE WHEN e.code LIKE 'PLC_%' THEN e.device_id END)
    SET e.scanner_name=COALESCE(NULLIF(e.scanner_name,''),s.name,CASE WHEN e.code NOT LIKE 'PLC_%' THEN e.device_name END),
        e.scanner_ip=COALESCE(NULLIF(e.scanner_ip,''),s.ip),
        e.plc_id=COALESCE(e.plc_id,CASE WHEN e.code LIKE 'PLC_%' THEN e.device_id END),
        e.plc_name=COALESCE(NULLIF(e.plc_name,''),p.name,CASE WHEN e.code LIKE 'PLC_%' THEN e.device_name END),
        e.plc_ip=COALESCE(NULLIF(e.plc_ip,''),p.ip);

    -- No scan_log INSERT/UPDATE/DELETE: the ALTER preserves legacy content and existing summaries.
    CALL bp_upgrade_scan_log_20260915(TRUE);

    -- A genuinely empty auth database gets the agreed bootstrap admin.
    -- BCrypt cost 12, admin / example-admin-password, no forced initial password change.
    -- Existing users, password hashes, roles, flags, sessions and audit rows are untouched.
    ALTER TABLE sys_user ALTER COLUMN must_change_password SET DEFAULT FALSE;
    IF NOT EXISTS (SELECT 1 FROM sys_user) THEN
        INSERT INTO sys_user(username,password_hash,role,enabled,must_change_password) VALUES('admin','REMOVED_SHARED_PASSWORD_HASH','ADMIN',TRUE,FALSE);
        INSERT INTO sys_auth_audit(actor,action,target_username) VALUES('system','INITIALIZE_ADMIN','admin');
    END IF;
    IF v_has_seq > 0 THEN ALTER TABLE plc_addr DROP COLUMN scanner_seq; END IF;
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='plc_addr' AND column_name='install_position_id') THEN
        ALTER TABLE plc_addr DROP COLUMN install_position_id;
    END IF;
    IF v_info<>(SELECT COUNT(*) FROM cushion_info)
       OR v_devices<>(SELECT COUNT(*) FROM device_info) OR v_plc<>(SELECT COUNT(*) FROM plc_addr)
       OR NOT (v_detail_end <=> (SELECT MAX(id) FROM cushion_detail)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Business row counts changed; keep services stopped and inspect concurrent writers';
    END IF;
    COMMIT;
    SET SESSION sql_safe_updates = v_original_safe_updates;
    DROP TEMPORARY TABLE bp_upgrade_scanner_map;
    DO RELEASE_LOCK(v_lock_name);
    SET v_lock=0;
    SELECT 'UPGRADE_OK' AS result,'20260916-r5' AS revision,DATABASE() AS upgraded_database,VERSION() AS mysql_version;
    SELECT 'cushion_info' AS table_name,COUNT(*) AS row_count FROM cushion_info
    UNION ALL SELECT 'cushion_detail (rows checked in batches)',v_detail
    UNION ALL SELECT 'device_info',COUNT(*) FROM device_info
    UNION ALL SELECT 'plc_addr',COUNT(*) FROM plc_addr;
    SELECT table_name,table_rows AS estimated_rows,'metadata estimate only; scan_log rows preserved by schema migration' AS note
    FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='scan_log';
    SELECT v_detail_batches AS detail_batches,v_detail_updated AS detail_rows_backfilled,v_detail_last AS last_committed_detail_id;
    SELECT 'Historical rows with unknown scanner are preserved, not guessed' AS note,
        (SELECT COUNT(*) FROM cushion_info WHERE scanner_id IS NULL) AS cushion_info_unknown_scanner,
        v_detail_unknown AS cushion_detail_unknown_scanner;
END$$
DELIMITER ;
CALL bp_upgrade_20260914();
DROP PROCEDURE bp_upgrade_20260914;
DROP PROCEDURE bp_upgrade_scan_log_20260915;
DROP PROCEDURE bp_upgrade_add_column_20260914;
DROP PROCEDURE bp_upgrade_add_index_20260914;
SET SESSION sql_mode = @bp_upgrade_original_sql_mode;
SET SESSION lock_wait_timeout = @bp_upgrade_original_lock_wait;
SET SESSION innodb_lock_wait_timeout = @bp_upgrade_original_row_lock_wait;
SET SESSION autocommit = @bp_upgrade_original_autocommit;
