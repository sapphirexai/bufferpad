/*
 Navicat Premium Dump SQL

 Source Server         : wms_opc
 Source Server Type    : MySQL
 Source Server Version : 80020 (8.0.20)
 Source Host           : 192.0.2.6:3306
 Source Schema         : wms_opc

 Target Server Type    : MySQL
 Target Server Version : 80020 (8.0.20)
 File Encoding         : 65001

 Date: 17/06/2026 09:36:13
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for cushion_detail
-- ----------------------------
DROP TABLE IF EXISTS `cushion_detail`;
CREATE TABLE `cushion_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_seq` int NULL DEFAULT NULL COMMENT '扫码器安装顺序',
  `scanner_position` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '扫码器位置',
  `open_count` int NULL DEFAULT 0 COMMENT '开口数',
  `scanner_id` bigint NULL DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '缓冲垫明详情表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of cushion_detail
-- ----------------------------

-- ----------------------------
-- Table structure for cushion_info
-- ----------------------------
DROP TABLE IF EXISTS `cushion_info`;
CREATE TABLE `cushion_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `max_use_count` int UNSIGNED NOT NULL DEFAULT 0 COMMENT '最大使用次数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `last_scan_date` datetime NULL DEFAULT NULL COMMENT '最近一次扫码时间',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `scanner_position` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '扫码器位置',
  `scanner_seq` int NULL DEFAULT NULL,
  `open_count` smallint NULL DEFAULT 0,
  `scanner_id` bigint NULL DEFAULT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `qr_code`(`qr_code` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 114 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '缓冲垫信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of cushion_info
-- ----------------------------

-- ----------------------------
-- Table structure for device_info
-- ----------------------------
DROP TABLE IF EXISTS `device_info`;
CREATE TABLE `device_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `type` int NOT NULL DEFAULT 0 COMMENT '设备类型，0: 扫码器；1: PLC ',
  `port` int NOT NULL DEFAULT 0 COMMENT '设备端口号',
  `status` int NOT NULL DEFAULT 0 COMMENT '设备状态，0：未连接；1：活跃中',
  `ip` varchar(15) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '设备IP',
  `name` varchar(60) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '设备名字',
  `position` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '设备位置',
  `work_line` int NOT NULL DEFAULT 1 COMMENT '产线，例如：1，2，3...',
  `install_seq` int NOT NULL DEFAULT 1 COMMENT '安装顺序，从1开始，不同类型的设备分别计算',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `modified_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '设备基本信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of device_info
-- ----------------------------

-- ----------------------------
-- Table structure for device_install_position
-- ----------------------------
DROP TABLE IF EXISTS `device_install_position`;
CREATE TABLE `device_install_position`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sort_no` int NULL DEFAULT 0,
  `created_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_date` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of device_install_position
-- ----------------------------

-- ----------------------------
-- Table structure for opc_config
-- ----------------------------
DROP TABLE IF EXISTS `opc_config`;
CREATE TABLE `opc_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `cushion_max_use_count` int NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of opc_config
-- ----------------------------
INSERT INTO `opc_config` VALUES (1, 500);

-- ----------------------------
-- Table structure for plc_addr
-- ----------------------------
DROP TABLE IF EXISTS `plc_addr`;
CREATE TABLE `plc_addr`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `plc_id` bigint NOT NULL DEFAULT 0 COMMENT 'PLC的ID',
  `addr` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT 'PLC寄存器地址',
  `type` int NOT NULL DEFAULT 0 COMMENT '地址类型\r\n0：扫码失败\r\n1：超出最大使用次数\r\n2：扫码成功',
  `scanner_id` bigint NOT NULL COMMENT 'scanner device_info.id',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plc_addr_scanner_type`(`scanner_id` ASC, `type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of plc_addr
-- ----------------------------

-- ----------------------------
-- Table structure for scan_log
-- ----------------------------
DROP TABLE IF EXISTS `scan_log`;
CREATE TABLE `scan_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `qr_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '' COMMENT '缓冲垫二维码',
  `msg` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL DEFAULT '' COMMENT '日志内容',
  `created_date` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `msg_type` smallint NOT NULL DEFAULT 0 COMMENT '设备状态，0：普通；1：异常',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of scan_log
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
