# Database initialization

[简体中文](README.md) · **English** · [All documentation](../../../docs/README.en.md)

The installer uses `app/db/wms_opc.sql` for a new MySQL database, after starting `BufferPadMySQL`. The full initialization structure synchronized on 2026-09-15 includes 12 tables:

- `opc_config`, `device_install_position`, `device_info`, `plc_addr`;
- `cushion_info`, `cushion_detail`, `scan_log`, `operation_event`;
- `sys_user`, `sys_user_session`, `sys_auth_audit`, `sys_builtin_session`.

`scan_log.msg` is TEXT; operation IDs/types/status, device snapshots, `detail_json`, the unique-operation index, and retention/pagination indexes are included. `operation_event` includes device context and correlation indexes.

Only the default lifetime of 500 and four base positions (`上`, `下`, `间层1`, `间层2`: upper, lower, middle 1/2) are seeded. Devices, mappings, cushions, logs, accounts, and sessions start empty. Backend startup creates the initial administrator (default username `admin`) using the installer's chosen `BUFFERPAD_ADMIN_PASSWORD`; no mandatory initial password change. `superadmin` maintenance login is disabled unless the deployer configures a BCrypt hash; SQL inserts no fixed maintenance account or password.

Use initialization only for an empty database. It contains no DROP TABLE or table-overwrite statements, but it is not an upgrade script. The SQL targets MySQL 8.0.20 and later 8.0 releases.

Device codes: scanner `0`, Mitsubishi `1`, Inovance `2`, Siemens S7-1200/S7-1500 `3`. Siemens uses S7comm over ISO-on-TCP on `102`, Rack/Slot `0/0`. `plc_addr.addr` needs at least 64 characters for addresses such as `DB1.DBW0`, `MW0`, `QW0`, and `IW0`. `IWoffset` is read-only and valid only for opening-count readback.

## Incremental migrations

For fresh installs and upgrades, the installer runs `app/db/migrations/*.sql` in filename order. Existing MySQL data causes whole-database initialization to be skipped, while incremental migrations continue. They preserve existing device/mapping/cushion/detail/log records according to their documented transformations; review the actual starting schema and backup first.

`20260821_siemens_s7_support.sql` expands register storage to `VARCHAR(64)`. `20260825_unify_siemens_s7_device_type.sql` normalizes old type `4` to `3`. `20260825_operation_event_context.sql` adds/backfills available scanner/PLC names and IPs. These migrations are repeatable and do not delete business records.

Structured operation events retain 30 days by default; that task does not remove cushion master/details or scan logs. Scan logs have their own [retention policy](../../../wms-opc/docs/log-retention-export.en.md). For old six-table databases, review the [full legacy upgrade guide](../../../wms-opc/docs/sql/20260915-legacy-upgrade.en.md).
