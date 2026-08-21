/*
 Navicat Premium Dump SQL

 Source Server         : projectm
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : 192.0.2.4:3306
 Source Schema         : wms_opc

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 03/08/2026 13:39:27
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cushion_detail
-- ----------------------------
DROP TABLE IF EXISTS `cushion_detail`;
CREATE TABLE `cushion_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_seq` int NULL DEFAULT NULL COMMENT '扫码器安装顺序',
  `scanner_position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '扫码器位置',
  `open_count` int NULL DEFAULT 0 COMMENT '开口数',
  `scanner_id` bigint NULL DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '缓冲垫明详情表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for cushion_info
-- ----------------------------
DROP TABLE IF EXISTS `cushion_info`;
CREATE TABLE `cushion_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `max_use_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `last_scan_date` datetime NULL DEFAULT NULL COMMENT '最近一次扫码时间',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '扫码器位置',
  `scanner_seq` int NULL DEFAULT NULL,
  `open_count` smallint NULL DEFAULT 0,
  `scanner_id` bigint NULL DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `qr_code`(`qr_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 123 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '缓冲垫信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for device_info
-- ----------------------------
DROP TABLE IF EXISTS `device_info`;
CREATE TABLE `device_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` int NOT NULL DEFAULT 0 COMMENT '设备类型，0: 扫码器；1: 三菱PLC；2: 汇川PLC；3: 西门子S7-1200；4: 西门子S7-1500',
  `port` int NOT NULL DEFAULT 0 COMMENT '设备端口号',
  `status` int NOT NULL DEFAULT 0 COMMENT '设备状态，0：未连接；1：活跃中',
  `ip` varchar(15) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '设备IP',
  `name` varchar(60) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '设备名字',
  `position` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT NULL COMMENT '设备位置',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `install_seq` int NOT NULL DEFAULT 1 COMMENT '安装顺序，从1开始，不同类型的设备分别计算',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci COMMENT = '设备基本信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for device_install_position
-- ----------------------------
DROP TABLE IF EXISTS `device_install_position`;
CREATE TABLE `device_install_position`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL,
  `sort_no` int NULL DEFAULT 0,
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for opc_config
-- ----------------------------
DROP TABLE IF EXISTS `opc_config`;
CREATE TABLE `opc_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cushion_max_use_count` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for operation_event
-- ----------------------------
DROP TABLE IF EXISTS `operation_event`;
CREATE TABLE `operation_event`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `operation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `severity` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `message` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `suggestion` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `work_line` int NOT NULL,
  `scanner_id` bigint NULL DEFAULT NULL,
  `scanner_seq` int NULL DEFAULT NULL,
  `device_id` bigint NULL DEFAULT NULL,
  `device_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `qr_code` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `address` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `error_code` int NULL DEFAULT NULL,
  `technical_detail` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_operation_event_event_id`(`event_id` ASC) USING BTREE,
  INDEX `idx_operation_event_operation_id`(`operation_id` ASC, `id` ASC) USING BTREE,
  INDEX `idx_operation_event_line_id`(`work_line` ASC, `id` ASC) USING BTREE,
  INDEX `idx_operation_event_qr_code`(`qr_code` ASC) USING BTREE,
  INDEX `idx_operation_event_created_date`(`created_date` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 32 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for plc_addr
-- ----------------------------
DROP TABLE IF EXISTS `plc_addr`;
CREATE TABLE `plc_addr`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plc_id` bigint NOT NULL DEFAULT 0 COMMENT 'PLC的ID',
  `addr` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT 'PLC寄存器地址；西门子S7示例：DB1.DBW0、MW0、IW0、QW0',
  `type` int NOT NULL DEFAULT 0 COMMENT '地址类型\r\n0：扫码失败\r\n1：超出最大使用次数\r\n2：扫码成功',
  `scanner_id` bigint NOT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plc_addr_scanner_type`(`scanner_id` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for scan_log
-- ----------------------------
DROP TABLE IF EXISTS `scan_log`;
CREATE TABLE `scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `msg` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL DEFAULT '' COMMENT '日志内容',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `msg_type` smallint NOT NULL DEFAULT 0 COMMENT '设备状态，0：普通；1：异常',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb3 COLLATE = utf8mb3_general_ci ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
