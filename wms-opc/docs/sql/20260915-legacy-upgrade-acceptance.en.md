# Legacy SQL upgrade acceptance — 20260916-r5

[简体中文](20260915_旧库升级验收.md) · **English** · [All documentation](../../../docs/README.en.md)

**Historical evidence before public migration.** Sanitized host labels 27/149 refer to old deployment environments. Private `tmp/` logs, result JSON, and packages are not distributed. This translation reruns none of these tests. Current credentials follow [authentication](../authentication.en.md).

## R5 safe-update compatibility

Nine targeted cases passed in isolated local MySQL 8.0.45 with SQL_SAFE_UPDATES enabled. No live/27/149 database was connected or changed. Earlier million-row/application acceptance was **not** rerun in this R5 pass.

| Scenario | Recorded result |
|---|---|
| R4 with safe updates | Reproduced error 1175 |
| Same connection and half-upgraded database, full R5 rerun | Success; safe updates restored to 1; legacy values retained |
| R5 rerun after success | Identical 12-table data; safe updates stayed 1 |
| Read-only verifier | All violations zero |
| Complete old database, original flag 0 and 1 | Both succeeded/restored original values |
| Deliberate preflight failure | Original error propagated, flag 1 retained, lock released |
| Trigger-induced backfill failure, original flag 0 and 1 | Both restored flag/released lock; rerun succeeded after removing fault |

Fixtures used cached old schema/small tables plus 100 synthetic old-format scan logs. Checks covered old business values/times/log IDs/content and rowwise 12-table equality after rerun. No new schema objects or retention-policy changes were introduced. Historical runner: `tmp/test-db-upgrade-r5.py`; result-location record: `tmp/db-upgrade-r5-test-current.txt`; delivery included `r5-test-result.json` and SHA256SUMS.

## Earlier R3 structural acceptance (retained historical results)

Twenty-seven isolated upgrade/application groups passed; three upgraded databases each passed 15 read-only association/schema checks. Neither live business database received upgrade DDL or business writes.

Read-only metadata inspection on 2026-09-15 used information_schema/SHOW CREATE TABLE, not scans of remote large tables. Legacy: MySQL 8.0.20, six tables, five-column scan_log with VARCHAR(255) message and DATETIME(3). Current: 8.0.42, 12 tables, 12 log summary fields, TEXT, five indexes, built-in sessions. Estimated zero rows was not proof of emptiness.

Actual DDL ran locally on MySQL 8.0.45, ROW binlog, 128 MiB buffer pool. Fixtures included cached 1197 cushions, 10 devices, 34 mappings, one lifetime row, supplied schema SQL, 121 sampled historical details, and synthetic large data. Full remote history was not downloaded or individually validated.

| Coverage | Recorded result |
|---|---|
| Six-table direct upgrade / exact supplied structure | Old fields, mappings/types, lifetime 450, positions and then-current administrator initialization correct |
| R2 → R3 / rerun current structure | Added log/session differences; existing values/accounts/passwords/change flags/settings retained |
| Current summaries | Long messages, operation IDs, states, snapshots, JSON and update times retained |
| Regular and built-in sessions | Both retained; no fixed built-in user inserted |
| 1000000 scan logs | Streaming SHA-256 of original fields identical before/after/rerun |
| 150121 details | Original digest unchanged; only missing scanner associations filled |
| Interrupted batch | 9902 committed associations retained after second-batch fault; rerun completed |
| Interrupted after log DDL | No premature UPGRADE_OK; rerun without duplicate columns/indexes |
| Long/default messages | 2000 Chinese characters and old empty-message default accepted |
| Unique operation IDs | Duplicate non-NULL rejected; NULL historical rows coexist |
| Conflict guards | Mismatched index, short snapshot field, duplicate operation ID, reserved-name conflict rejected before business transformations |
| Original safety guards | Duplicate scanners, missing devices, mapping/type conflicts, count overflow, detail UPDATE triggers/compressed rows rejected |
| Historical precision | Cross-line associations, micro/milliseconds, unknown scanners, positions/raw addresses preserved |
| Three verification databases | 15 zero-violation checks each |
| Application startup/login | Then-current administrator and built-in identity logged in |
| Million-row pagination | 20 rows returned under 256 MiB Java heap; total 1000002 including two test rows |
| Main APIs | Users, lifetime, devices/positions/mappings, cushions, logs/events, types passed |

Historical built-in spelling `superamin` in these earlier results predates the correction to `superadmin` and later deployer-configured credentials. See [the correction record](../device-state-admin-acceptance.en.md).

Local interrupted-upgrade completion took 14.219 seconds; rerun/preservation checking took 13.187 seconds including test-side digest reads. These are not deployment timing guarantees. Table rebuilding still needs stopped writers, verified backups, and space. Isolated device addresses were changed to `127.0.0.1:1` and retention disabled; production PLCs/cleanup were untouched. Local processes/databases were stopped afterward.

## Historical evidence identifiers

- Metadata: `tmp/db-upgrade-r3-inspection/legacy.json`, `current.json`.
- 27 groups: `tmp/db-upgrade-r3-test-20260915-083433/result.json`.
- Verifiers: same directory `verifier-result.json` and `*-verify.log`.
- Console: `tmp/db-upgrade-r3-tests.log`.
- Private runners: `tmp/test-db-upgrade-r3.py`, `tmp/verify-upgrade-r3-checks.py`.
- Original SQL byte SHA-256: `bfb838e503a97dcc4774e9b2833c46205e14dc7719dd825e56d80bfcefe39e91`.
- Newline-normalized test-input SHA-256: `cbe16846f8dc5961b02b785c2b62335d39056bd21d9c1d7b848d1c77c7437c4b`.

Use the [current R5 execution guide](20260915-legacy-upgrade.en.md), not old delivery archives. The hashes above identify historical evidence and are not checksums of today's adapted source.
