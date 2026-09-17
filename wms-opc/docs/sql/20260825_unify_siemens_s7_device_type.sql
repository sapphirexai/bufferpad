-- Consolidate the former S7-1200 (3) and S7-1500 (4) device types.
-- Both models use the same S7comm over ISO-on-TCP configuration in BufferPad.
-- Safe to execute repeatedly on MySQL 8.
UPDATE device_info
SET type = 3
WHERE type = 4;

ALTER TABLE device_info
    MODIFY COLUMN type INT NOT NULL DEFAULT 0
    COMMENT '设备类型，0:扫码器；1:三菱PLC；2:汇川PLC；3:西门子S7 PLC';
