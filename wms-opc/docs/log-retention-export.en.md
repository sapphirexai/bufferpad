# Log retention and cushion time-range export

[简体中文](日志清理与时间段导出.md) · **English** · [All documentation](../../docs/README.en.md)

The monitor offers selected-row export and time-range export. Range export chooses first-use or last-use time (default last use), includes both boundary seconds, and exports one XLSX row per matching cushion with current data. It ignores current pagination, selection, and search filters. No matches or more than 100000 rows prompts adjustment. USER and ADMIN can query/export.

## Scan-log cleanup

Daily at 03:00 Beijing time, remove `scan_log` rows earlier than midnight three calendar months before that day. The cutoff itself is retained: on 2026-09-15, delete before `2026-06-15 00:00:00`. Day-based retention can retain less than one extra day beyond exactly three months. It does not affect `cushion_info` or `cushion_detail`.

Defaults: 1000-row independently committed batches, 100 ms pauses, and a 300-second budget for starting batches. A started query may additionally wait up to its 10-second timeout. Remaining expired rows wait until the next run; large backlogs may take multiple nights. Database named locks exclude concurrent instances; failures retry next time and log cutoff, deleted count, batches, and status.

Back up `scan_log`, then apply `docs/sql/20260914_scan_log_retention_index.sql`. It only creates indexes, is repeatable, and uses MySQL 8.0 online indexing without falling back to table copying. Cleanup skips with an error if no index starts with `created_date`. Earlier upgrade revisions needed this migration separately; the current full r5 legacy upgrade already includes the retention index. The Windows installer also supplies the migration.

| Variable | Default | Meaning |
|---|---|---|
| `SCAN_LOG_RETENTION_ENABLED` | `true` | Enable cleanup; restart after changing |
| `SCAN_LOG_RETENTION_MONTHS` | `3` | Calendar months |
| `SCAN_LOG_RETENTION_ZONE` | `Asia/Shanghai` | Cutoff/schedule timezone |
| `SCAN_LOG_CLEANUP_CRON` | `0 0 3 * * ?` | Daily 03:00 |
| `SCAN_LOG_CLEANUP_BATCH_SIZE` | `1000` | Rows per batch, maximum 10000 |
| `SCAN_LOG_CLEANUP_MAX_SECONDS` | `300` | Batch-start budget |
| `SCAN_LOG_CLEANUP_PAUSE_MILLIS` | `100` | Pause between batches |

Startup does not immediately clean logs. The historical deployment retained defaults and waited for the next 03:00. Look for `scan_log cleanup completed` or `scan_log cleanup task failed`. Resolve missing indexes, lock ownership, and database errors rather than substituting unconditional DELETE/TRUNCATE.

## Export API

`POST /cushion/cushions/excel/time-range` requires login and CSRF:

```json
{
  "timeType": "LAST_USE",
  "startTime": "2026-09-01 00:00:00",
  "endTime": "2026-09-14 23:59:59"
}
```

`FIRST_USE` maps to `created_date`; `LAST_USE` to `last_scan_date`. Database times are interpreted/displayed in Beijing time. Success returns a complete XLSX; invalid parameters, empty results, and excessive matches return HTTP 400 JSON. Reads use 1000-row batches in one read-only transaction for a consistent snapshot. Temporary files are removed after generation/download; failures do not return a partial workbook.

## Verification entry points

- Backend `mvn test`: calendar months, timezones, concurrency, retry, and time validation.
- Frontend `npm run unit`: time modes, empty ranges, duplicate submissions, errors, authenticated downloads.
- `src/test/java/cn/tpl/opc/infrastructure/maintenance/RetentionExportMysqlAcceptance.java`: explicit acceptance entry, restricted to loopback database `retention_export_acceptance`. The historical fixture runner `tmp/test-retention-export.py` is private workspace evidence, not a bundled script. Never retarget it to production.
