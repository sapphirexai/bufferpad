# Device communication verification: historical acceptance

[简体中文](设备连接与通信验证分离验收.md) · **English** · [All documentation](../../docs/README.en.md)

**Historical record — 2026-09-15 (UTC+8), before public migration.** Addresses are sanitized. These results were not rerun for this translation; referenced `tmp/` evidence and binaries are private historical artifacts, not included in the repository. Current credentials and maintenance settings follow [authentication](authentication.en.md). See the [design](device-communication-design.en.md).

## Delivered behavior

TCP handshakes show connectivity only; valid scanner messages or successful PLC protocol requests establish verified communication and its timestamp. Scanners default to passive receive without the old 90-second alarm or automatic application heartbeat. Optional per-device checks are disabled by default: scanner interval/wait `30/10` seconds, read-only PLC interval `15` seconds, default fault threshold three actual unanswered requests. Explicit disconnect faults immediately; re-handshaking does not clear timeout without valid communication.

Read-only probes share business I/O locking, do not overlap/accumulate, and change neither cushion data nor `scan_log`. Existing type-5 PLC heartbeat writes remain. The frontend separates TCP, verification, detection mode, latest request, and timeouts; unverified/connecting/unknown are not fault counts.

## Recorded validation

| Layer | Result | Historical evidence |
|---|---|---|
| Backend regression | 127 passed, no failures/errors/skips | `tmp/device-health-backend-test.log` |
| Additional concurrency/send failure | 2 passed | `tmp/device-health-concurrency-test.log` |
| Frontend | 16 suites, 65 passed | `tmp/device-health-frontend-test.log` |
| Builds | Backend package and frontend build passed | Backend log; `tmp/device-health-frontend-build.log` |
| Real Modbus TCP loopback | HSL reads, valid/exception responses, three timeouts, fault across reconnect, recovery; no writes | `PlcHealthLoopbackTest`, included in 127 |
| Full service / TCP simulations | 100 seconds, 21 samples; passive devices sent no application data, enabled checks verified, no added scan logs | `tmp/device-health-runtime/result.json` |
| Isolated MySQL authentication | Account/hash/session preservation, recovery, USER rights, CSRF, logout, restart persistence | Same runtime report |
| Local browser | Login, drawer, both device tabs, counts; no runtime JS errors | `tmp/device-health-local-browser/result.json` |
| Historical deployment browser | Login and both drawers; screenshots/layout/colors reviewed; no runtime JS errors | `tmp/device-health-online-browser/result.json` |

TCP simulations verify covered behavior, not every physical model. New `scanner-heartbeats` and `plc-probes` maps remained empty on the historical deployment pending device confirmation.

## Deployment observation

Release `bufferpad-device-health-20260915-101106` completed at 10:13:37 on 2026-09-15 using `/docker/docker-compose.yml`, service `bufferpad-wms-opc`, frontend `/docker/nginx/bufferpad`, sanitized URL `http://192.0.2.4:18088`.

Health was `UP`; 20 HTTP static resources and anonymous login interception passed. Backend/frontend hashes matched local tested and installer copies. Other containers, shared Compose/nginx configuration, and existing users were unchanged. Rollback image: `bufferpad/wms-opc:backup-bufferpad-device-health-20260915-101106`; backup directory: `/docker/.bufferpad-device-health-20260915-101106/backup`.

Over 120 seconds / 13 samples, none of 10 devices was falsely marked verified. Five scanners stayed `CONNECTED / UNVERIFIED / PASSIVE`, with no active request and zero consecutive timeouts. Three PLCs with existing heartbeat writes retained communication-timeout faults; two remained unverified. Scanner silence was not a fault, and reconnect did not erase PLC timeout.

Maintenance login, immutable built-in identity, and logout revocation passed under the historical configuration; all test sessions were logged out. Evidence: `tmp/bufferpad-device-health-20260915-101106/external-verification.json` (`PASS`) and `deployment-result.json`.

| Historical artifact | SHA-256 |
|---|---|
| Backend JAR | `31de3b1922a018eb4dbad2eaa128cd0b8f05c0ed66e2deb603ed39d9eabfce8a` |
| Frontend entry | `432e8d623104984f90d9df2f8c95f6173bad6a90c062e7fce8b534a2faae15b2` |
| Sorted database columns/indexes before and after | `1c19bfecc4e973f24011ef38d84e558cd994b8bdf53c31cb8709f7317d1f18da` |
| Then-current R4 upgrade SQL | `37e48069172c9bb7fed5b45c56f795dc09dd0800f5ef644ab451870b9b0d4178` |

## Database and limits

No table, column, index, or upgrade SQL was changed/applied for this feature. New fields live in runtime state/DTOs and configuration. Historical report field `sessionTableCreated` actually checked table existence; it did not prove new table creation. The schema hash stayed identical. The legacy database at sanitized `192.0.2.6` was untouched and deployment large logs were not scanned. Authentication checks only created/revoked test sessions.

Recorded completion comprises 129 backend tests (127 + 2), 65 frontend tests, isolated MySQL/TCP, local/deployment browsers, 120-second observation, and installer synchronization. It is evidence for that delivery only.
