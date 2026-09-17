# Legacy database upgrade — delivery 20260916-r5

[简体中文](20260915_旧库升级说明.md) · **English** · [All documentation](../../../docs/README.en.md)

**Historical delivery documentation, adapted for the public source repository.** Addresses are sanitized. Private packages/evidence mentioned below are not bundled. Current initial credentials and maintenance login follow [authentication](../authentication.en.md). This English edition does not establish a new database acceptance result.

## Revision 5: Workbench error 1175

Delivery revision is **20260916-r5**, retaining older filenames for compatibility. With SQL_SAFE_UPDATES enabled, prior backfills using non-key position/scanner fields could fail with error `1175`; that did not mean legacy business data was invalid.

R5 saves the connection's SQL_SAFE_UPDATES, temporarily disables it for backfill, and restores it on both success and SQL exceptions. It changes no global variable or Workbench preference and preserves preflight, association checks, and data-preservation rules. Relative to R4 it adds no tables, columns, or indexes.

After a 1175 failure:

1. Keep old/new backend writers stopped and retain a verified recoverable backup. DDL may have committed partially; the whole upgrade was not necessarily rolled back.
2. Open the current `20260915_upgrade_legacy_to_current.sql`, select the intended database, clear any selected-text range, and run the **entire file from the beginning**. Do not merely rerun an old `CALL bp_upgrade_20260914()`. Disable “continue on error”.
3. Require final `UPGRADE_OK` and `20260916-r5`. `SCAN_LOG_PREFLIGHT` alone is not success.
4. Run `20260915_upgrade_legacy_verify.sql`; all `violations` must be zero before application acceptance/restarting writers. Check `SELECT @@SESSION.sql_safe_updates;` for restoration.

Historical testing recovered from a real R4 half-upgraded 1175 state by fully rerunning R5, without reimporting the old database. Investigate other failures while writes remain stopped.

## Included transformations

This full script supports the legacy six-table database, R2-upgraded databases, and reruns on the current structure. It includes later log summaries, cleanup indexes, and built-in sessions; do not first run R2 or unnecessarily layer those changes. It only acts on the currently selected database, does not connect elsewhere or merge two deployments. The original delivery did not upgrade the two live business databases (historically labeled 27 and 149).

| Item | Transformation |
|---|---|
| `scan_log.msg` | VARCHAR to TEXT; retain messages and empty-default behavior |
| Summary fields | `operation_id`, `operation_type`, `status`, `result_code`, `work_line`, `operator_name`, `scanner_id`, `plc_id`, `scanner_snapshot`, `plc_snapshot`, `detail_json`, `updated_date` |
| Unique operation index | `uk_scan_log_operation(operation_id)`; historical NULL IDs coexist |
| Cleanup/time index | `idx_scan_log_created_id(created_date,id)` |
| Status/device indexes | `idx_scan_log_status_time`, `idx_scan_log_scanner_time`, `idx_scan_log_plc_time` |
| Maintenance sessions | `sys_builtin_session` stores digests/timestamps, no fixed account/password |
| Conflict preflight | Stop on incompatible types/lengths, mismatched same-name indexes, duplicate nonempty operation IDs, or reserved `superadmin` database users |
| Completion marker | `UPGRADE_OK` / `20260916-r5` only after all steps |

Historical log IDs, barcodes, messages/types, and timestamps (including milliseconds) are preserved. Current summaries/snapshots/IDs/states/details are not overwritten. The script does no scan_log INSERT/UPDATE/DELETE or retention cleanup; new columns remain NULL for old rows.

Earlier position/scanner associations, PLC type normalization, operation-event fields, and auth transformations remain. Preserve actual endpoints/registers, counts, lifetime settings, historical positions/times. Missing `cushion_detail.scanner_id` is added with INSTANT and backfilled by primary-key windows, without changing old positions/open_count types or copying the entire detail table. Unresolvable historical scanner associations remain NULL.

Existing users, passwords, roles, sessions, audits, and change requirements are retained. Empty `sys_user` initialization is delegated to backend startup with `BUFFERPAD_ADMIN_PASSWORD`; maintenance login stays disabled by default, with no fixed SQL password.

## Large tables and stopped writers

**Converting old scan_log to TEXT rebuilds that table using COPY.** The older R2 promise of no log-schema change does not apply to R3 onward. For old structures, TEXT, missing fields, and five indexes are combined in one ALTER to avoid repeated rebuilds. Complete current structures need no log DDL; index-only additions use INPLACE.

Stop all old/new backend, scanner ingestion, and cleanup writers. Back up fully and verify restoration. Check MySQL data/temp free space: the historical guidance suggests at least three times scan_log data+index size plus backups/runtime space, but actual requirements depend on storage/log configuration. `SCAN_LOG_PREFLIGHT` estimates table size, not available disk.

Metadata/row-lock waits are bounded at 15 seconds and fail visibly. DDL commits implicitly; one ROLLBACK cannot undo the entire upgrade. After failure, keep writers stopped, diagnose and rerun or restore; do not start the new application against a partial schema.

Detail batches default to 5000. Set `SET @bp_upgrade_detail_batch_size=1000;` in the same connection to override within `100..20000`. Committed backfills survive interruption; reruns revisit windows and only fill missing links. This does not split the single scan_log rebuild.

Ordinary six-table upgrades avoid a full log COUNT or business DML. A partial schema with operation IDs but no unique index needs duplicate-ID checking, which may scan the existing column. Mismatched same-name indexes fail for manual review, not automatic deletion.

## Execution

Historical metadata inspection on 2026-09-15 found MySQL 8.0.20 / six tables on the legacy host and 8.0.42 / 12 tables on the current host. Actual upgrade DDL was tested locally on 8.0.45, not executed on production 8.0.20. An estimated TABLE_ROWS of zero does not prove a table is empty.

1. Finish restoring any old backup, stop writers, and verify a complete backup. Structure-only `wms_opc_str.sql` is neither a data backup nor a production upgrade script.
2. Connect to the intended legacy database or full restored copy, select `wms_opc`, and run UTF-8 SQL without `--force`/continue-on-error.
3. Require the final revision/success marker and all read-only violations zero. Unknown historical scanner IDs are reported separately, not treated as lost data.
4. Test current login/users, paginated logs, device/PLC settings, and cushion history; ensure only one collection instance before resuming production.

```text
mysqldump --default-character-set=utf8mb4 --single-transaction --routines --triggers --events --hex-blob --set-gtid-purged=OFF --column-statistics=0 -h 192.0.2.6 -P 3306 -u root -p --result-file=wms_opc_before_upgrade.sql wms_opc
mysql --default-character-set=utf8mb4 -h 192.0.2.6 -P 3306 -u root -p wms_opc
```

Replace the fictional host and local SQL paths for your isolated/authorized environment:

```sql
SOURCE C:/path/to/20260915_upgrade_legacy_to_current.sql;
SOURCE C:/path/to/20260915_upgrade_legacy_verify.sql;
```

When moving an old database to another host, restore into a separate empty database, upgrade/validate it, then switch the application connection; do not overwrite the currently used database.

Upgrade SQL retains old logs, but the running backend defaults to daily 03:00 three-calendar-month retention. Set `SCAN_LOG_RETENTION_ENABLED=false` during isolated full-history verification if needed, then enable according to deployment policy. Earlier “unpaginated log API” limitations describe old code, not current behavior.

The `20260914_upgrade_legacy_to_current.sql`/verify aliases are synchronized source entry points. Old downloaded R2/R3/R4 archives do not update themselves; use current R5 source. R4 corrected reserved `superamin` to `superadmin` without changing structural strategy; R3 comments describe the structural baseline. See [historical acceptance](20260915-legacy-upgrade-acceptance.en.md); historical delivery SHA256SUMS apply to those packages, not automatically to today's source.
