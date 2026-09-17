# Scan-operation log maintenance

[简体中文](扫码操作日志维护说明.md) · **English** · [All documentation](../../docs/README.en.md)

## Behavior

`scan_log` aggregates one operation by `operation_id`. Each received automatic scan creates a new operation/log, including repeats inside the two-hour counting window; counting deduplication still applies. Reusing the same operation ID does not execute the business operation twice. Scan, count, PLC notification, and readback update the same row.

`msg` includes source, result, barcode, line, scan/count outcome, current/maximum uses, scanner/PLC name/ID/IP/port/position, register/result, and readback. Manual scans also include the signed-in username. Unresolved devices are explicitly unassociated/unconfigured, never guessed. Snapshots are captured at first association and retain distinct IDs/positions and original names after later device edits.

| Status | Meaning |
|---|---|
| Processing | A required stage has no result yet |
| Success | All required stages completed |
| Advisory | Duplicate, lifetime reached, or committed count with missing configuration/readback failure |
| Failed | Unreadable barcode, rolled-back count, failed/rejected PLC write; successful stages remain visible |
| Unknown result | Incomplete for over five minutes or business outcome cannot be confirmed |
| Historical log | Pre-upgrade row; no invented stage/device history |

Each minute, up to 1000 timed-out processing rows are checked. This only updates logs: it never resends PLC writes or adds counts. Late real results can update the row. Standalone readback is labeled a separate PLC operation. Diagnostic logs and structured events remain; heartbeats/reconnects do not flood the scan summary.

The page defaults to 20 rows and combines result/scanner/PLC filters. Numeric device input means exact ID; other text matches historical name/IP/position snapshots. Long messages show a summary and detail view. Older rows lack device fields and remain searchable by barcode/message/time.

## Database upgrade and installation

After an older base-schema upgrade, apply `docs/sql/20260914_scan_log_operation_summary.sql` unless the current full legacy upgrade already supplied these changes. It targets MySQL 8.0; the historical record reports isolated 8.0.45 and deployment 8.0.42 execution. It preserves rows, adds nullable fields/indexes, converts `msg` to TEXT while retaining empty-string default behavior, and can be rerun.

VARCHAR-to-TEXT requires a COPY table rebuild. Back up and verify recovery, check free space, stop backend writes, run SQL, and deploy matching backend/frontend. Replacing only the JAR is insufficient. Startup does not automatically run large-table DDL.

The Windows installer includes the migration and runs files in name order; `prepare-app.ps1` synchronizes artifacts. Docker deployments must run reviewed migrations explicitly. Added nullable columns/TEXT remain compatible with the previous writer; on application rollback retain the new schema instead of overwriting the production log table.

The retention task still batches cleanup at 03:00 for three-calendar-month-old rows. If the initial audit log cannot be written, scanning stops with an error. A later summary-update failure is recorded in application logs; unfinished rows become unknown after timeout.

## Verification

- `mvn test` / `mvn package` with usable Maven settings and local HSL.
- `ScanOperationLogServiceTest`: row-lock merging, stage order, duplicate events, snapshots, rollback, timeout.
- `ScanSummaryMysqlAcceptance`: dedicated loopback database, real MySQL/Druid concurrency, long messages, independent transactions, retention compatibility.
- Historical private runner `tmp/test-scan-summary.py`: isolated TCP scanner/Modbus simulation. The private scan-summary acceptance report holds original deployment/backup evidence; neither is a public bundled artifact.
