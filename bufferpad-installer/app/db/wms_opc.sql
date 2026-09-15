-- BufferPad current database initializer; MySQL 8.0.20+; updated 2026-09-15.
-- Run only in a new empty database. Existing installations use app/db/migrations.
-- Contains schema and base settings only; no site devices, business data, users or sessions.
-- The backend bootstraps admin/example-admin-password (or the configured initial credential) on first startup.
-- The built-in superadmin identity and password remain in the backend, never in SQL.
SET NAMES utf8mb4;

-- cushion_detail
CREATE TABLE `cushion_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `work_line` int NOT NULL DEFAULT '1' COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_seq` int DEFAULT NULL COMMENT '扫码器安装顺序',
  `scanner_position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '扫码器位置',
  `open_count` int DEFAULT '0' COMMENT '开口数',
  `scanner_id` bigint DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='缓冲垫明详情表';

-- cushion_info
CREATE TABLE `cushion_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `max_use_count` int unsigned NOT NULL DEFAULT '0' COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT '0' COMMENT '已使用次数',
  `last_scan_date` datetime DEFAULT NULL COMMENT '最近一次扫码时间',
  `work_line` int NOT NULL DEFAULT '1' COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '扫码器位置',
  `scanner_seq` int DEFAULT NULL,
  `open_count` smallint DEFAULT '0' COMMENT '开口数',
  `scanner_id` bigint DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `qr_code` (`qr_code`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='缓冲垫信息表';

-- device_info
CREATE TABLE `device_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` int NOT NULL DEFAULT '0' COMMENT '设备类型，0:扫码器；1:三菱PLC；2:汇川PLC；3:西门子S7 PLC',
  `port` int NOT NULL DEFAULT '0' COMMENT '设备端口号',
  `status` int NOT NULL DEFAULT '0' COMMENT '设备状态，0：未连接；1：活跃中',
  `ip` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '设备IP',
  `name` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '设备名字',
  `position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT NULL COMMENT '设备位置',
  `work_line` int NOT NULL DEFAULT '1' COMMENT '产线，例如：1，2，3...',
  `install_seq` int NOT NULL DEFAULT '1' COMMENT '安装顺序，从1开始，不同类型的设备分别计算',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC COMMENT='设备基本信息表';

-- device_install_position
CREATE TABLE `device_install_position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `sort_no` int DEFAULT '0',
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- opc_config
CREATE TABLE `opc_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cushion_max_use_count` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- operation_event
CREATE TABLE `operation_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `suggestion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `work_line` int NOT NULL,
  `scanner_id` bigint DEFAULT NULL,
  `scanner_seq` int DEFAULT NULL,
  `scanner_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `scanner_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `plc_id` bigint DEFAULT NULL,
  `plc_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `plc_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `device_id` bigint DEFAULT NULL,
  `device_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `qr_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `error_code` int DEFAULT NULL,
  `technical_detail` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_operation_event_event_id` (`event_id`) USING BTREE,
  KEY `idx_operation_event_operation_id` (`operation_id`,`id`) USING BTREE,
  KEY `idx_operation_event_line_id` (`work_line`,`id`) USING BTREE,
  KEY `idx_operation_event_qr_code` (`qr_code`) USING BTREE,
  KEY `idx_operation_event_created_date` (`created_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;

-- plc_addr
CREATE TABLE `plc_addr` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plc_id` bigint NOT NULL DEFAULT '0' COMMENT 'PLC的ID',
  `addr` varchar(64) NOT NULL DEFAULT '' COMMENT 'PLC寄存器地址；西门子S7示例：DB1.DBW0、MW0、IW0、QW0',
  `type` int NOT NULL DEFAULT '0' COMMENT '地址类型\r\n0：扫码失败\r\n1：超出最大使用次数\r\n2：扫码成功',
  `scanner_id` bigint NOT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_plc_addr_scanner_type` (`scanner_id`,`type`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- scan_log
CREATE TABLE `scan_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci DEFAULT '' COMMENT '缓冲垫二维码',
  `msg` text NOT NULL DEFAULT (_utf8mb4''),
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `msg_type` smallint NOT NULL DEFAULT '0' COMMENT '设备状态，0：普通；1：异常',
  `operation_id` varchar(64) DEFAULT NULL,
  `operation_type` varchar(24) DEFAULT NULL,
  `status` varchar(16) DEFAULT NULL,
  `result_code` varchar(48) DEFAULT NULL,
  `work_line` int DEFAULT NULL,
  `operator_name` varchar(64) DEFAULT NULL,
  `scanner_id` bigint DEFAULT NULL,
  `plc_id` bigint DEFAULT NULL,
  `scanner_snapshot` varchar(1024) DEFAULT NULL,
  `plc_snapshot` varchar(1024) DEFAULT NULL,
  `detail_json` text,
  `updated_date` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_scan_log_operation` (`operation_id`),
  KEY `idx_scan_log_status_time` (`status`,`updated_date`,`id`),
  KEY `idx_scan_log_scanner_time` (`scanner_id`,`created_date`,`id`),
  KEY `idx_scan_log_plc_time` (`plc_id`,`created_date`,`id`),
  KEY `idx_scan_log_created_id` (`created_date`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 ROW_FORMAT=DYNAMIC;

-- sys_auth_audit
CREATE TABLE `sys_auth_audit` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `actor` varchar(64) NOT NULL,
  `action` varchar(40) NOT NULL,
  `target_username` varchar(64) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- sys_builtin_session
CREATE TABLE `sys_builtin_session` (
  `token_hash` varchar(64) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_hash`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- sys_user
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `password_hash` varchar(100) NOT NULL,
  `role` varchar(16) NOT NULL,
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `must_change_password` tinyint(1) NOT NULL DEFAULT '0',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- sys_user_session
CREATE TABLE `sys_user_session` (
  `token_hash` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`token_hash`),
  KEY `fk_auth_session_user` (`user_id`),
  CONSTRAINT `fk_auth_session_user` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO opc_config(id,cushion_max_use_count) VALUES(1,500);
INSERT INTO device_install_position(id,name,sort_no) VALUES(1,'上',1),(2,'下',2),(3,'间层1',3),(4,'间层2',4);
