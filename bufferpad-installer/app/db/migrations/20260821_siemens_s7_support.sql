-- Siemens SIMATIC S7-1200/S7-1500 support.
-- Safe to execute repeatedly on MySQL 8.
-- Device type codes: 0 scanner, 1 Mitsubishi PLC, 2 Inovance PLC,
--                    3 Siemens S7-1200 PLC, 4 Siemens S7-1500 PLC.

ALTER TABLE device_info
    MODIFY COLUMN type INT NOT NULL DEFAULT 0
        COMMENT '设备类型，0:扫码器；1:三菱PLC；2:汇川PLC；3:西门子S7-1200；4:西门子S7-1500';

ALTER TABLE plc_addr
    MODIFY COLUMN addr VARCHAR(64) NOT NULL DEFAULT ''
        COMMENT 'PLC寄存器地址；西门子S7示例：DB1.DBW0、MW0、IW0、QW0';
