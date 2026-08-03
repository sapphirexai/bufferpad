-- BufferPad test-environment data. Safe to run repeatedly.
-- No mock device is inserted because device rows trigger real Netty connections.

SET NAMES utf8mb4;
START TRANSACTION;

INSERT INTO cushion_info
    (qr_code, max_use_count, used_count, last_scan_date, work_line,
     created_date, modified_date, scanner_position, scanner_seq, open_count, scanner_id)
VALUES
    ('TEST-BUFFERPAD-NORMAL', 500, 25, DATE_SUB(NOW(), INTERVAL 3 HOUR), 1,
     DATE_SUB(NOW(), INTERVAL 3 DAY), NOW(), '测试上料位', 1, 2, 1),
    ('TEST-BUFFERPAD-WARNING', 500, 475, DATE_SUB(NOW(), INTERVAL 1 HOUR), 1,
     DATE_SUB(NOW(), INTERVAL 30 DAY), NOW(), '测试上料位', 1, 5, 1),
    ('TEST-BUFFERPAD-EXPIRED', 500, 500, NOW(), 1,
     DATE_SUB(NOW(), INTERVAL 60 DAY), NOW(), '测试上料位', 1, 8, 1)
ON DUPLICATE KEY UPDATE
    max_use_count = VALUES(max_use_count),
    used_count = VALUES(used_count),
    last_scan_date = VALUES(last_scan_date),
    work_line = VALUES(work_line),
    modified_date = NOW(),
    scanner_position = VALUES(scanner_position),
    scanner_seq = VALUES(scanner_seq),
    open_count = VALUES(open_count),
    scanner_id = VALUES(scanner_id);

DELETE FROM cushion_detail
WHERE qr_code IN ('TEST-BUFFERPAD-NORMAL', 'TEST-BUFFERPAD-WARNING', 'TEST-BUFFERPAD-EXPIRED');

INSERT INTO cushion_detail
    (qr_code, work_line, created_date, modified_date, scanner_seq,
     scanner_position, open_count, scanner_id)
VALUES
    ('TEST-BUFFERPAD-NORMAL', 1, DATE_SUB(NOW(), INTERVAL 3 HOUR), NOW(), 1, '测试上料位', 2, 1),
    ('TEST-BUFFERPAD-WARNING', 1, DATE_SUB(NOW(), INTERVAL 1 HOUR), NOW(), 1, '测试上料位', 5, 1),
    ('TEST-BUFFERPAD-EXPIRED', 1, NOW(), NOW(), 1, '测试上料位', 8, 1);

DELETE FROM scan_log
WHERE qr_code IN ('TEST-BUFFERPAD-NORMAL', 'TEST-BUFFERPAD-WARNING', 'TEST-BUFFERPAD-EXPIRED');

INSERT INTO scan_log (qr_code, msg, created_date, msg_type)
VALUES
    ('TEST-BUFFERPAD-NORMAL', '测试数据：缓冲垫状态正常，当前使用25次', DATE_SUB(NOW(), INTERVAL 2 MINUTE), 0),
    ('TEST-BUFFERPAD-WARNING', '测试数据：缓冲垫已达到95%寿命，请关注', DATE_SUB(NOW(), INTERVAL 1 MINUTE), 1),
    ('TEST-BUFFERPAD-EXPIRED', '测试数据：缓冲垫已达到最大使用次数，应撤离产线', NOW(), 1);

DELETE FROM operation_event
WHERE event_id IN ('test-data-20260803-normal', 'test-data-20260803-warning', 'test-data-20260803-expired');

INSERT INTO operation_event
    (event_id, code, severity, title, message, suggestion, work_line,
     scanner_id, scanner_seq, device_id, device_name, qr_code,
     address, error_code, technical_detail, created_date)
VALUES
    ('test-data-20260803-normal', 'SCAN_COUNTED', 'INFO',
     '测试：扫码计数完成', '缓冲垫已完成计数，当前使用25次', '', 1,
     1, 1, 1, '测试上料位', 'TEST-BUFFERPAD-NORMAL',
     NULL, NULL, '测试环境预置数据', DATE_SUB(NOW(), INTERVAL 2 MINUTE)),
    ('test-data-20260803-warning', 'PLC_ADDRESS_NOT_CONFIGURED', 'WARNING',
     '测试：PLC地址未配置', '缓冲垫已计数，但没有找到对应的PLC地址',
     '请检查PLC地址配置；这条记录是测试数据，不代表当前设备真实故障', 1,
     1, 1, 1, '测试上料位', 'TEST-BUFFERPAD-WARNING',
     NULL, NULL, '测试环境预置数据', DATE_SUB(NOW(), INTERVAL 1 MINUTE)),
    ('test-data-20260803-expired', 'CUSHION_MAX_REACHED', 'ERROR',
     '测试：缓冲垫达到寿命', '缓冲垫使用次数已达到500次上限',
     '请从回流线上撤走该缓冲垫；这条记录是测试数据', 1,
     1, 1, 1, '测试上料位', 'TEST-BUFFERPAD-EXPIRED',
     NULL, NULL, '测试环境预置数据', NOW());

COMMIT;
