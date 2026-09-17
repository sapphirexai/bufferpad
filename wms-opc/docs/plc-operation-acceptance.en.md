# Eight PLC operation types: historical chain acceptance

[简体中文](八类PLC操作链路测试验收.md) · **English** · [All documentation](../../docs/README.en.md)

**Historical record — 2026-09-15, before public migration.** Addresses are sanitized; current account policy follows [authentication](authentication.en.md). Historical logs/binaries under `tmp/` are private evidence, not included artifacts. These tests were not rerun for the English documentation.

The objective was to test the real backend with scanner/PLC simulations from framing through transactional counting, command dispatch, readback, logs, and SSE. “Rescan” means the manual-input entry point. A repeat inside two hours does not increment, but must still choose success versus lifetime-exceeded from the current lifetime state.

## Eight operation rules — all recorded as passed

| Type | Expected behavior |
|---:|---|
| `0` | `NoRead`: no cushion/detail creation; write `1` to failure register; one failure summary |
| `1` | At/above lifetime: write `1` to exceeded register, no success readback; quick repeats remain exceeded |
| `2` | First/eligible scan counts once; after commit write success `1`, then type `6` readback |
| `3` | Manual lifetime-exceeded preserves associated scanner, writes exceeded `1`, no success readback |
| `4` | Manual success writes `1`, then type `7`; ambiguous scanner targets never broadcast |
| `5` | Existing heartbeat periodically writes `0` without counts/details/business summary logs |
| `6` | Only after type `2` write success; `0`/`32767` accepted, negative/error values not saved |
| `7` | Only after type `4` success; master and latest detail save consistently |

The Inovance simulator used real Modbus TCP function `06` writes and `03` reads. A second PLC received no nontarget commands. Existing Siemens S7 simulation and TCP loopback cases remained in regression.

## Bugs found and fixed

1. Lifetime-exceeded quick repeats previously took the duplicate-success path. They now send type `1/3`, without type `2/4` or `6/7` readback.
2. TCP fragmentation/coalescing previously treated each network read as a message. A bounded stream decoder handles STX/ETX and TPL framing, split UTF-8, consecutive/incomplete frames, and noise recovery.
3. `contains` misclassified barcodes containing `NoRead`/`HeartBeat`. Only whole control messages match; empty/unclosed/overlong/invalid-encoding frames do not count.
4. Concurrent first scans could fail on duplicate keys. After insert conflict, read the committed record and apply duplicate rules. READ_COMMITTED transactions avoid stale lifetime decisions; uniqueness and conditional increments still guarantee one creation/count.
5. Imported readback mappings could reference another PLC. Runtime now validates same-PLC association and nonempty address before reading.
6. Readback persistence exceptions now produce correlated failure results. Missing detail/update failure rolls back the master opening count, without replaying PLC writes or misreporting a database error as TCP disconnect.
7. Latest-detail ordering adds `id DESC` for equal creation times.

## Recorded tests

| Stage | Result | Historical evidence |
|---|---|---|
| Baseline regression | 129 passed, revealing coverage gaps | `tmp/eight-operations-baseline.log` |
| New reproductions | Four failing assertions for exceeded repeats/parsing | `tmp/eight-operations-red.log` |
| Readback save reproduction | Uncaught exception | `tmp/eight-operations-focused.log` |
| Baseline full service | Reproduced framing/control-word/concurrency/mapping bugs | `tmp/eight-operations-baseline/result.json` |
| Fixed regression | 145 passed, no failures/errors/skips | `tmp/eight-operations-regression.log` |
| Fixed full service | 40 scenarios passed | `tmp/eight-operations-fixed/result.json` |
| Fresh isolated MySQL | Same 40 passed using installer schema/migrations | `tmp/eight-operations-clean-schema/result.json` |

Scenarios covered all eight types, lifetime boundaries, quick repeats, both framing styles, split/coalesced traffic, keyword barcodes, `0/32767/negative` readbacks, write/read rejection, missing/inconsistent configuration, offline targets, count/readback rollback, missing/equal-time details, manual ambiguity, six-way concurrent first/existing/limit-crossing scans, operation-ID idempotency, persisted events, real SSE, real timeouts/recovery, and disconnects.

Each operation checked count, actual target function/register/value, write-before-read order, and final correlated log context. Unanswered PLC writes were not replayed; rolled-back counts dispatched no command. Isolated database triggers injected failures.

## Reproduce in isolation

The tracked runner is `wms-opc/tools/acceptance/eight_operations.py`. It needs Java 17, MySQL 8 server/client, Python 3, and PyMySQL, plus a backend built with authorized local HSL. It defaults to locally extracted installer runtimes; `--java`, `--mysql-bin`, and `--jar` allow explicit paths. From the monorepo root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-backend.ps1 -PublicRepositories
python wms-opc/tools/acceptance/eight_operations.py acceptance
```

It uses a dedicated MySQL instance under `tmp/eight-operations-<label>/mysql-data`, binding random loopback ports and recreating only `eight_operations_acceptance` inside that instance. Simulators listen locally. It stops test services and retains JSON, protocol, SSE, and application logs. Never retarget it to a business database. Simulations do not certify physical firmware, ladder logic, networks, or deployment registers.

## Historical build and deployment

Test JAR SHA-256: `4d37c8be14472dea682ee5875041b8ca9a67fbb11fa6ab2625ce85add4f90379`; the installer copy matched.

Release `bufferpad-eight-operations-20260915-104641` deployed at 10:49:30 on 2026-09-15 through `/docker/docker-compose.yml`. Service `bufferpad-wms-opc` was `UP`, with matching container JAR. Frontend/shared Compose/other containers were unchanged. Rollback image: `bufferpad/wms-opc:backup-bufferpad-eight-operations-20260915-104641`. Reports: local `tmp/bufferpad-eight-operations-20260915-104641/deployment-result.json` and remote `/docker/.bufferpad-eight-operations-20260915-104641/deployment-result.json`.

Five deployment checks passed: anonymous page/API interception, maintenance rights, passive device state, 20-row log pages, and logout. They sent no test barcode or PLC business instruction; sessions and local test processes were closed.

No business table/column/index changed. Before/after schema hash: `1c19bfecc4e973f24011ef38d84e558cd994b8bdf53c31cb8709f7317d1f18da`. Then-current R4 upgrade SQL remained `37e48069172c9bb7fed5b45c56f795dc09dd0800f5ef644ab451870b9b0d4178`. The legacy `192.0.2.6` database was untouched. The recorded scope completed with no known unresolved covered issue; it is not broader hardware certification.
