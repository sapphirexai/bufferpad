# BufferPad backend (`wms-opc`)

[简体中文](README.md) · **English** · [All documentation](../docs/README.en.md)

Read the [overview](../README.en.md) and [current build guide](../docs/build-and-package.en.md) first. Historical deployment addresses are sanitized examples; historical acceptance does not validate the current revision. There is no shared initial password, and maintenance login is disabled by default. The directory name is historical; this project is not a general OPC UA server.

The backend receives industrial scanner or manual barcodes, creates cushion records, counts valid uses, checks lifetime, records operations, and notifies a configured PLC. For six-table legacy databases, use the [revision-3 upgrade guide](docs/sql/20260915-legacy-upgrade.en.md); its recorded isolated-copy checks are historical evidence.

## Business rules

- A first valid scan creates `cushion_info` with `used_count=1` and one `cushion_detail`.
- A repeat within two hours of the last valid scan adds neither usage nor a detail row.
- Valid rescans use an atomic conditional database increment to avoid concurrent double counting.
- At `used_count >= max_use_count`, send only the lifetime-exceeded PLC command, not an additional success command.
- Counting and PLC notification are decoupled. Missing/offline/rejecting PLCs do not roll back a committed count.
- If a manual scan has no uniquely resolvable scanner target, retain the count and emit a target-unresolved event.

## Stack and architecture

Java 17, Spring Boot 2.7.10 / MVC, MyBatis-Plus, MySQL 8, Druid, Netty, GreenRobot EventBus, HSL communication, SSE, JUnit 4, Mockito, and H2.

| Package under `cn.tpl.opc` | Responsibility |
|---|---|
| `controller` | HTTP and SSE endpoints |
| `application.scan` | Scan orchestration and EventBus listener |
| `application.plc` | Target resolution, dispatch, and results |
| `domain.scan` | Two-hour interval and lifetime rules |
| `service` | Configuration, devices, logs, and events |
| `infrastructure.scanner` / `event` / `plc` / `maintenance` | Message parsing, event adaptation, I/O classification, scheduled maintenance |
| `netty` | Connections, heartbeats, incoming messages |
| `mapper` / `entity` | Persistence and entities |
| `commons` | DTOs, requests, enums, constants |

Scanner TCP → `MsgHandler.channelRead` → `ScannerMessageParser` → `EventBusMsgCushionQrCode` → `ScanEventListener` → `ScanApplicationService.handleScan/handleScanCodeFailed`. The parser handles `HeartBeat`, `NoRead`, actual STX/ETX, and `[TPL_STX]/[TPL_ETX]` framing.

Manual `POST /cushion/manualCushionInfo` → `CushionController` → `ScanCommand` → the same application service, sharing counting, deduplication, lifetime, detail, and event rules.

`ScanApplicationService` owns transactional business changes. `PlcNotifyService` resolves scanner/PLC/register mappings and optional readback. `EventBusMsgPlcCmd` reaches the sole asynchronous consumer, `PlcCommandDispatcher`, which performs `Connection.write`, logs/results, device-state updates, and optional readback. Each scanner maps to one PLC and at most one address per operation type; both devices share a line.

Protocol rejection is distinct from transport failure; for example error `85` can mean writing is prohibited during RUN. Current timeout thresholds and TCP/verification separation are described in the [communication design](docs/device-communication-design.en.md). Successful heartbeat traffic does not erase an unresolved business rejection; successful business I/O restores it. Success commands can read Int16 opening counts into the cushion and latest detail.

## Siemens S7 and supported addressing

Type `3` covers S7-1200 and S7-1500 using classic S7comm over ISO-on-TCP, TCP `102`, Rack `0`, Slot `0`. This is not PROFINET real-time I/O, OPC UA, or symbolic access. Upgrade normalizes historical type `4` to `3`.

| Area | Syntax | Use |
|---|---|---|
| DB word | `DBn.DBWoffset`, e.g. `DB1.DBW0` | Write/readback |
| M word | `MW0` | Write/readback |
| Output word | `QW0` | Write/readback |
| Input word | `IW0` | Readback only |

Numbers after DBW/MW/IW/QW are byte offsets. A word occupies two bytes; allocate even offsets and avoid overlapping intervals such as DBW0/DBW1. Saving normalizes case and validates syntax. DB numbers are `1..65535`; word-start offsets are `0..2097150`, subject to actual CPU/DB limits. TIA `%` prefixes and bit addresses such as DBX/MX are rejected. Declare opening counts as `INT`, `0..32767`; negative values are rejected.

For each scanner, the full example maps types `0..7` to `DB100.DBW0,2,4,6,8,10,12,14`. Types `0..4` write `1`; type `5` writes `0` every two seconds; types `6/7` read after successful type `2/4` notifications. PLC logic clears consumed notifications and keeps readback current. Enable PUT/GET, external access, and nonoptimized absolute-address DBs; verify TCP reachability. S7-1500 secure authentication, custom TSAPs for special CPs, and symbolic variables are unsupported here. See [all model-specific tables](../bufferpad-installer/plc-recommended-configuration.en.md).

## Events, retention, and APIs

Structured `operation_event` records and SSE `operationEvent` correlate counting, address checks, PLC writes, and readback through an `operation_id` of up to 64 characters. Manual scans may supply `X-Operation-Id`; otherwise the backend generates it. Events snapshot scanner/PLC names and IPs plus original barcode, so the frontend can identify context even for PLC/address errors. Transactional events persist and publish after commit.

Events include count completed/failed, unreadable code, duplicate scan, lifetime reached, missing address/readback, unresolved manual target, PLC offline/success/rejection/communication failure, and failed readback. `GET /operationEvents/recent?workLine=1&limit=20` defaults to 20, bounded `1..100`.

```yaml
operation-event:
  retention:
    days: 30
    cleanup-cron: "0 15 2 * * ?"
```

Event cleanup runs daily at 02:15 in batches of up to 1000. This is separate from the [three-calendar-month scan-log retention](docs/log-retention-export.en.md).

| Table | Purpose |
|---|---|
| `cushion_info` / `cushion_detail` | Current cushion state and valid-use history |
| `scan_log` / `operation_event` | Operation summaries and structured monitoring events |
| `opc_config` | Single global lifetime row `id=1`, default 500 |
| `device_install_position` / `device_info` | Positions, device types, and connections |
| `plc_addr` | Scanner/PLC/operation/register mappings |
| `sys_user`, `sys_user_session`, `sys_auth_audit`, `sys_builtin_session` | Accounts, sessions, and authentication audit |

| API prefix | Purpose |
|---|---|
| `/cushion` | Pages, details, manual scans, lifetime changes, Excel |
| `/device` | CRUD, connections, and state |
| `/deviceInstallPositions` / `/plcAddr` / `/opcConfig` | Positions, mappings, global configuration |
| `/options` | Device/operation/position/scanner/PLC options |
| `/scanLogs` | Paginated scan/PLC logs |
| `/operationEvents/recent` | Up to 100 recent events |
| `/sse/devicesStatus/{clientId}` | Devices, scan results, events |

See [authentication and authorization](docs/authentication.en.md), [log pagination](docs/log-pagination.en.md), and [operation-log maintenance](docs/scan-operation-logs.en.md).

## Configuration and build

`src/main/resources/application.yml` defines common ports, MyBatis, and retention; `application-dev.yml`, `application-test.yml`, and `application-prod.yml` supply profile defaults overridden by environment/external configuration. Prepare HSL and database credentials as in [Getting started](../docs/getting-started.en.md).

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn -Pprod clean package
java -jar target/opc-0.0.1.-SNAPSHOT.jar --spring.profiles.active=prod
```

The runtime profile is independent of Maven's `-Pprod`. Health: `GET /actuator/health`. Prefer the root build helper when needing the explicit missing-HSL check or independent public Maven settings.

## SQL files

| File under `docs/sql/` | Purpose |
|---|---|
| `20260803_operation_event.sql` | Create the event table |
| `20260803_operation_event_retention.sql` | Add retention index |
| `20260804_operation_event_correlation.sql` | Add operation ID and indexes |
| `20260821_siemens_s7_support.sql` | S7 support and 64-character register addresses |
| `20260825_unify_siemens_s7_device_type.sql` | Normalize historical type 4 to 3 |
| `20260825_operation_event_context.sql` | Add/backfill available scanner/PLC context |
| `20260803_test_data.sql` | Repeatable frontend test data, without simulated device records |

These migrations preserve business records. Review [legacy upgrade instructions](docs/sql/20260915-legacy-upgrade.en.md) and separate log/auth migrations for your starting schema. Test data deliberately avoids connecting the backend to fake devices.

## Historical Docker deployment

Sanitized host `192.0.2.4`, Compose `/docker/docker-compose.yml`, service/container `bufferpad-wms-opc`, image `bufferpad/wms-opc:latest`, `prod`, host networking on `19001`, logs `/docker/bufferpad/wms-opc/logs`. Database values came from private `/docker/.env`, never Git.

Recorded releases: `bufferpad-first-login-20260914-113416` removed mandatory initial password changes while retaining accounts/sessions (backup `/docker/.bufferpad-first-login-20260914-113416/backup`). Earlier `bufferpad-auth-20260914-104318-r2` tested login/change/logout under the then-current policy, with its matching backup image and directory. `bufferpad-s7-unified-20260825-091500` recorded health `UP`, no type-4 devices, five operation-event context columns, and one Siemens option. These are past results, not current deployment proof.

The historical Compose service references an image without `build`: build a new image first, tag the previous image for rollback, and recreate only this service. After tests, package `Dockerfile` and `target/opc-0.0.1.-SNAPSHOT.jar`, upload to a private release directory, back up the database, and run reviewed SQL for the actual starting schema before switching the application. Supply database credentials through the controlled server environment, not command-line literals or Git.

```bash
release=bufferpad-YYYYMMDD-HHMMSS
docker tag bufferpad/wms-opc:latest "bufferpad/wms-opc:backup-$release"
docker build -t "bufferpad/wms-opc:$release" -t bufferpad/wms-opc:latest /docker/.$release/backend
cd /docker
docker compose -f docker-compose.yml up -d --no-deps bufferpad-wms-opc
curl --fail http://127.0.0.1:19001/actuator/health
docker logs --tail 200 bufferpad-wms-opc
tail -n 200 /docker/bufferpad/wms-opc/logs/wms-opc-prod.log
```

Wait for startup and require health `UP`. If application acceptance fails, retag `bufferpad/wms-opc:backup-$release` as `latest` and recreate only `bufferpad-wms-opc`; evaluate database compatibility separately rather than blindly restoring tables. The browser uses nginx `/api/` proxying, not a hardcoded backend IP.

For historical device-state and operation-chain evidence, see [communication acceptance](docs/device-communication-acceptance.en.md) and [eight PLC operation types](docs/plc-operation-acceptance.en.md).
