# Device configuration guide

[简体中文](设备配置说明.md) · **English** · [All documentation](../docs/README.en.md)

For administrators, deployment engineers, and maintainers using the frontend Settings (`设置`) menu. Upgrade migrations preserve existing devices, PLC addresses, cushions, details, and logs; they do not replace deployment settings with defaults.

Configure in order: **lifetime → installation positions → devices → PLC addresses → verification**. PLC mappings depend on devices, and scanner devices depend on positions. For complete model-specific connection steps and all eight address mappings, use [Recommended PLC configuration](plc-recommended-configuration.en.md).

## 1. Cushion lifetime

Menu: `设置 → 缓冲垫寿命` (Settings → Cushion lifetime).

The single global configuration has fixed ID `1` and a required maximum-use count, default `500`. A newly created cushion copies this value. At `used_count >= max_use_count`, scanning follows the lifetime-exceeded branch and notifies the PLC. Reset restores `500`. Changing the global value does not bulk-update existing cushions.

## 2. Installation positions

Menu: `设置 → 设备安装位置` (Settings → Device installation positions).

| Field | Required | Meaning |
|---|---|---|
| Position name | Yes | Recognizable, stable name shown in devices, mappings, and monitoring |
| Sort number | No | Smaller values appear first; it is not a business identifier |

Example names are `上` (upper), `下` (lower), `间层1` (middle layer 1), `间层2` (middle layer 2), and `PLC柜` (PLC cabinet), with sort numbers `1..4`. Referenced positions cannot be deleted. Renaming a position updates related display information and refreshes connected-device metadata.

## 3. Device information

Menu: `设置 → 设备信息` (Settings → Device information). The backend actively connects to configured devices.

| Field | Required | Meaning / example |
|---|---|---|
| Device type | Yes | Scanner `0`, Mitsubishi `1`, Inovance `2`, Siemens S7 `3` |
| Device name | Yes | Identify line and position, e.g. `Line1-UpperScanner` |
| IP | Yes | Actual device address; documentation example `192.0.2.10` |
| Port | Yes | Device TCP service port, not a register address |
| Production line | Yes | Defaults to `1`; scanner and PLC must match |
| Installation position | Yes | Selected position; backend `installSeq` stores its ID |

| Type | Protocol | Project port example | Notes |
|---|---|---:|---|
| Scanner `0` | TCP receive | `15000` | Barcode, `NoRead`, supported `HeartBeat` messages |
| Mitsubishi R `1` | MC/SLMP TCP | `3301` | `MelsecMcNet(ip, port)` requires a matching SLMP TCP entry |
| Inovance AM/AC `2` | Modbus TCP | `502` | Unit ID `1`, `MW` address model |
| Siemens S7-1200/1500 `3` | S7comm over ISO-on-TCP | `102` | Rack `0`, Slot `0`; shared device type |

Create one record per physical device and use distinguishable names. PLCs also need an installation position. Referenced devices cannot be deleted, moved to another production line, or changed to incompatible types until their PLC mappings are removed. Editing a connected device refreshes its connection. Some query/connect APIs use line `0` to mean all lines; use real line numbers for actual device records.

### PLC connection setup

For Mitsubishi R, configure **SLMP connection device / SLMP / TCP / 3301** in GX Works3, using the PLC's own IP. Reflect settings and close, apply, write parameters to the PLC, and reset/restart as required. A MELSOFT entry or UDP does not configure the needed connection. Permit approved external writes during RUN.

For Siemens, enable remote PUT/GET, allow external read/write through CPU protection settings, disable optimized access for DBs addressed absolutely, compile/download, and allow TCP `102`. Current standard parameters are Rack `0`, Slot `0`. TCP connectivity does not prove I/O permissions.

```powershell
Test-NetConnection 192.0.2.16 -Port 3301
Test-NetConnection 192.0.2.14 -Port 102
```

Example devices: upper scanner `192.0.2.10:15000`, lower scanner `192.0.2.11:15000`, Mitsubishi `192.0.2.12:3301`, Siemens `192.0.2.14:102`, all on line `1`; a separate Siemens `192.0.2.20:102` may belong to line `2`. These are fictional addresses.

### Scanner output

Accepted success framing:

```text
\x02BARCODE\x03
[TPL_STX]BARCODE[TPL_ETX]
```

In the first form, `\x02` and `\x03` represent actual STX/ETX control bytes. In the visible form, send ASCII `[TPL_STX]`, the scanned content, then `[TPL_ETX]`. For example: `[TPL_STX]BP-001[TPL_ETX]` or `[TPL_STX]PCB20260617001[TPL_ETX]`. Failure output must be exactly `NoRead`, including case.

For the referenced VI-series scanner client, connect the device, open Format configuration → Data processing → Data template, then insert text `[TPL_STX]`, the **barcode-content field**, and text `[TPL_ETX]`. Verify the preview (for example `[TPL_STX]ABC123456[TPL_ETX]`), save the template, set failure output to `NoRead`, and use Configuration management → Save all configurations so settings survive restart. Vendor manual sections referenced by the original guide are 5.4, 5.4.2, and 5.7; that PDF is not distributed here. Labels may differ by client version.

Do not use full-width brackets or a fixed test code as the barcode-content field. Trailing CR/LF can generally remain; parsing removes framing and trims whitespace. Use a TCP diagnostic tool to inspect actual output if parsing fails. The scanner normally acts as TCPServer and the backend as client: allow access to its actual IP and data port (the guide's example is `15000`).

## 4. PLC addresses

Menu: `设置 → PLC地址` (Settings → PLC addresses). Each row selects a PLC, scanner, operation type, and register address; all are required. Devices must already exist and share a production line. The displayed installation position is derived from the scanner, not independently selected here.

| Family | Register syntax | Scope |
|---|---|---|
| Mitsubishi R | `D` + decimal number, e.g. `D6600` | Common data-register range `D0..D12287`; extended ranges need engineer confirmation |
| Inovance AM/AC | `MW` + decimal number, e.g. `MW10000` | `MW0..MW65535` word-address model; confirm actual PLC mapping |
| Siemens S7 | `DBn.DBWoffset`, `MWoffset`, `QWoffset` | 16-bit notification/heartbeat writes or readback |
| Siemens input area | `IWoffset` | Opening-count readback only |

Do not interchange Mitsubishi `D` and Inovance `MW`, add spaces/units, or use Siemens bit syntax or a TIA `%` prefix. Siemens offsets are **bytes**, with two bytes per word. Allocate `DBW0, DBW2, DBW4, ...`; identical or partially overlapping intervals such as `DBW0` and `DBW1` on one PLC are rejected. Backend syntax bounds are DB `1..65535` and word-start offset `0..2097150`; actual CPU/DB limits still apply. Nonoptimized DB access and permission are required.

Inovance H5U/EASY `D`/`R` models need separate driver/address verification. Mitsubishi extended `D12288+` registers require confirmed PLC parameters. Examples do not replace the PLC engineer's approved point list.

Use the [complete operation/address table](plc-recommended-configuration.en.md): types `0..4` write `1`, configured type `5` writes `0`, types `6/7` read signed Int16 opening counts. Declare Siemens quantities as `INT`, range `0..32767`; a `WORD` value above `32767` would be negative when interpreted as signed. Bit, string, and floating-point addresses are not this business model.

There is no separate duplicate-scan PLC type. A scan within the two-hour counting interval does not add usage, but still checks current lifetime: types `1/3` at the limit, types `2/4` below it, with duplicate feedback in the UI/log. Manual input uses rescan logic. **If a manual scan has no uniquely resolvable scanner/PLC target, the count is retained and a target-unresolved event is emitted; it does not broadcast to every scanner.**

Each scanner maps to one PLC and one row per operation type. A PLC may serve multiple scanners through disjoint blocks. Configure types `0,1,2,3,4,6,7` as required; add type `5` only when the PLC design needs the existing heartbeat write. Keep one heartbeat per PLC, attached to one associated scanner. Do not duplicate its address across scanners.

The full examples use `D6600..D6607`, `MW10000..MW10007`, or `DB100.DBW0..DB100.DBW14` at even offsets. PLC logic consumes/clears `1` and continuously maintains readback values because readback is immediate. Reserve another 32 bytes for a second scanner, e.g. `DB100.DBW32` onward.

## 5. Verification

1. Start/restart the backend and wait for startup; it connects all configured devices without needing a browser.
2. Open monitoring and inspect TCP connectivity and communication verification. Offline devices retry after network recovery.
3. Scan a test barcode or use manual input.
4. Confirm creation/count changes according to deduplication rules.
5. Inspect success/failure/lifetime logs.
6. Verify actual PLC notification and opening-count readback.

## 6. Monitoring and faults

The device drawer shows IP/port, connection state, communication time, and errors. Recent operations show the latest 20 operations with green success, yellow advisory, and red failure/attention states. PLC disconnection, missing mappings, or rejected writes do **not** mean the count failed: inspect the barcode, count, register, and advice before rescanning. SSE updates scan results, devices, and events and reconnects automatically. Investigate persistent disconnection at the backend/network. Structured operation events retain 30 days by default without removing cushion master/detail records.

## 7. Troubleshooting

| Symptom | Check |
|---|---|
| No PLC/scanner in a mapping selector | Device exists and has the correct type |
| Production-line mismatch | Both devices use the same line; remove dependent mappings before changing it |
| Scanner already associated with a PLC | Review the design and remove old mappings before reassignment |
| Duplicate operation type | Edit the existing row instead of adding another |
| Cannot delete position/device | Remove or change dependent devices/mappings first |
| Mitsubishi `Connection refused` | Matching `3301`, SLMP entry, TCP, applied/downloaded parameters, reset, reachable network; this occurs before register access |
| Siemens TCP reachable but I/O fails | Type `3`, port `102`, PUT/GET, CPU access, downloaded nonoptimized DB, no `IW` writes, Rack/Slot `0/0` |
| Wrong address family | Use the PLC point list; never copy another family's syntax |

## 8. Before commissioning

- [ ] Set the required lifetime and all recognizable installation positions.
- [ ] Add every physical scanner/PLC with verified types, addresses, ports, and lines.
- [ ] Apply/download PLC communication and permission settings; reset where required.
- [ ] Ensure each scanner maps to one PLC with unique operations and nonoverlapping registers.
- [ ] Validate scanner framing, `NoRead`, all notification paths, and readbacks.
- [ ] Confirm monitoring, operation feedback, counting/deduplication, and actual PLC receipt.

## Device-state revision — 2026-09-15

Scanners default to passive receiving: long idle periods do not alarm, and application `HeartBeat` is not sent by default. Enable device-ID-based detection only after confirming protocol support. New safe read-only PLC probes are disabled by default; existing type-5 heartbeat writes retain their meaning. This changes no database tables. See [communication verification design](../wms-opc/docs/device-communication-design.en.md).
