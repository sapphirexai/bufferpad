# Short deployment configuration guide

[简体中文](缓冲垫现场配置简明说明.md) · **English** · [All documentation](../docs/README.en.md)

For operators: follow the sequence and confirm uncertain settings with the PLC engineer. Upgrade migrations retain devices, mappings, and business records rather than replacing them with defaults.

## Prepare and configure in order

Obtain production-line numbers and positions from the site lead, device IP/ports from IT/electrical staff, and PLC models and register allocations from the PLC engineer. Example scanner: `192.0.2.10:15000`. Do not guess register addresses without a point list.

1. **Lifetime** — `设置 → 缓冲垫寿命`: set maximum uses (default `500`) to the process requirement. Reaching the limit triggers lifetime feedback and PLC notification.
2. **Positions** — `设置 → 设备安装位置`: add recognizable names such as upper/lower/middle layers/PLC cabinet; lower sort numbers appear first.
3. **Devices** — `设置 → 设备信息`: select scanner `0`, Mitsubishi `1`, Inovance `2`, or Siemens S7 `3`; supply a recognizable name, actual IP/port, line, and position. Scanner and associated PLC lines must match.
4. **PLC communication** — enable the correct service and access permissions in the PLC engineering tool.
5. **PLC addresses** — map each scanner's outcomes to its approved registers.
6. **Test** — verify counting, logs, actual PLC signals, and readback in isolation.

## Network and PLC setup

| Family | Port example | Required setup |
|---|---:|---|
| Scanner | `15000` | TCPServer data output accessible from the backend |
| Mitsubishi R | `3301` | GX Works3 SLMP connection device / SLMP / TCP; allow approved RUN-mode writes |
| Inovance AM/AC | `502` | Modbus TCP Server, Unit ID `1`, accessible MW registers |
| Siemens S7-1200/1500 | `102` | S7comm over ISO-on-TCP, Rack `0`, Slot `0`, PUT/GET, nonoptimized absolute-address DBs |

For Mitsubishi, select the SLMP entry, TCP (not UDP), the PLC's IP, and port `3301`; reflect settings and close, apply, write to PLC, then reset/restart as required. For Siemens, allow external reads/writes, disable optimized access for addressed DBs, compile and download. Both models share type `3`.

```powershell
Test-NetConnection 192.0.2.16 -Port 3301
Test-NetConnection 192.0.2.17 -Port 102
```

`TcpTestSucceeded=True` proves port reachability only. `Connection refused` typically means the configured TCP service is not listening. For I/O failure with reachable TCP, inspect engineering settings and permissions.

## Registers and operations

See the [complete recommended tables](plc-recommended-configuration.en.md) for all operation types `0..7` and model-specific setup. Only Markdown documentation is distributed; there is no bundled PDF.

| Family | Failure / success / lifetime / readback examples | Restrictions |
|---|---|---|
| Mitsubishi R | `D6600` / `D6602` / `D6601` / `D6606` | D words, not MW; common range `D0..D12287`, confirm extensions |
| Inovance AM/AC | `MW10000` / `MW10002` / `MW10001` / `MW10006` | MW words, not D; confirm actual mapping within the model |
| Siemens | `DB1.DBW0` / `DB1.DBW4` / `DB1.DBW2` / `IW20` | DBW/MW/QW for writes; IW readback only |

Siemens offsets are bytes and words occupy two bytes; use nonoverlapping even offsets and approved nonoptimized DBs. The actual CPU and point list determine valid ranges. Notification operations write `1`; configured PLC heartbeat writes `0`; opening-count operations read signed 16-bit values. Each scanner maps to one PLC and one address per operation type. Keep one heartbeat mapping per PLC.

## Scanner output

| Result | Exact output |
|---|---|
| Barcode `BP-001` | `[TPL_STX]BP-001[TPL_ETX]` |
| Failed read | `NoRead` |
| Supported heartbeat response | `HeartBeat` |

Preserve case, ASCII brackets, and delimiters. Scanners are passive by default: do not enable heartbeat requests without confirmed device support.

## Test and operate

Open runtime monitoring, inspect devices, scan a test code, check creation/counting and logs, and ask the electrical engineer to confirm the intended D/MW/DBW/QW signal. Test `NoRead` as well. A duplicate scan within two hours may not add usage.

- Green operation messages indicate success; yellow messages require reading the advice (for example duplicate scans or missing mappings).
- Red messages need investigation, including lifetime limits, failed counting, and PLC failures. Open `查看设备` (View devices) for detailed states and errors.
- TCP connected, communication unverified can be normal standby; do not equate all connected devices with verified I/O.
- SSE reconnects automatically. Investigate a prolonged interruption at the backend/network.
- PLC notification failure may occur after a successful count. Check the current cushion and list before rescanning. The UI shows the latest 20 operations; structured events retain 30 days by default.

Before commissioning, confirm lifetime, positions, all device records, matching lines, downloaded PLC parameters, correct/nonoverlapping register types, one PLC per scanner, exact framing, and end-to-end receipt. If any item is uncertain, consult the PLC engineer or maintainer before changing addresses. The [full guide](device-configuration.en.md) explains fields and troubleshooting.
