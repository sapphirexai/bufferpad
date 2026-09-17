# Recommended PLC configuration

[简体中文](PLC推荐配置.md) · **English** · [All documentation](../docs/README.en.md)

These project configuration examples cover Mitsubishi, Inovance, and Siemens adapters. Each address block belongs to **one scanner**. Multiple scanners may share a PLC only with nonoverlapping address blocks. Confirm actual models, memory allocation, and permissions with the PLC engineer before use; example ranges are not hardware certification.

`PLC端口/地址` in Settings → PLC addresses means a **register address**, not an IP address or TCP port. Configure network ports under Device information.

## Shared operation rules and address tables

| Code | Operation | Action | Mitsubishi R | Inovance AM/AC | Siemens S7 |
|---:|---|---|---|---|---|
| `0` | Scan failed (`NoRead`) | Write Int16 `1` | `D6600` | `MW10000` | `DB100.DBW0` |
| `1` | Scan reached/exceeded lifetime | Write `1` | `D6601` | `MW10001` | `DB100.DBW2` |
| `2` | Scan succeeded | Write `1`, then read type `6` if configured | `D6602` | `MW10002` | `DB100.DBW4` |
| `3` | Rescan reached/exceeded lifetime | Write `1` | `D6603` | `MW10003` | `DB100.DBW6` |
| `4` | Rescan succeeded | Write `1`, then read type `7` if configured | `D6604` | `MW10004` | `DB100.DBW8` |
| `5` | Configured PLC heartbeat | Write `0` every 2 seconds | `D6605` | `MW10005` | `DB100.DBW10` |
| `6` | Opening-count readback after scan | Read signed Int16 | `D6606` | `MW10006` | `DB100.DBW12` |
| `7` | Opening-count readback after rescan | Read signed Int16 | `D6607` | `MW10007` | `DB100.DBW14` |

The PLC must consume and clear notification value `1`; it need not write a response to heartbeat value `0`. Readbacks run immediately after successful type `2/4` writes, so keep their values up to date. Opening count (`开口数`, the process quantity read from the PLC) should be `0..32767`; all three adapters use 16-bit values.

One scanner maps to one PLC, with at most one address per operation type. Both devices must belong to the same production line. Configure only one heartbeat mapping per PLC. For a second scanner, reserve another 16-word / 32-byte block starting at `D6616`, `MW10016`, or `DB100.DBW32`.

## Connection parameters

| Family | Device code | Protocol | Example address | Port | Parameters |
|---|---:|---|---|---:|---|
| Mitsubishi MELSEC iQ-R / R | `1` | MC/SLMP over TCP | `192.0.2.12` | `3301` | SLMP connection device, TCP |
| Inovance AM / AC | `2` | Inovance Modbus TCP | `192.0.2.13` | `502` | Modbus TCP Server, Unit ID `1` |
| Siemens S7-1200 / S7-1500 | `3` | S7comm over ISO-on-TCP | `192.0.2.14` | `102` | Rack `0`, Slot `0` |

Use recognizable names such as `Line1-MainPLC`, production line `1`, and an installation position such as `PLC cabinet`. Both Siemens models use type `3`; do not select historical type `4`.

## Mitsubishi R: engineering setup

In GX Works3 built-in Ethernet / external device configuration, select or add **SLMP connection device** (`SLMP连接设备`), communication `SLMP`, protocol **TCP**, the PLC's own IP, and port `3301`. Apply “Reflect settings and close”, then “Apply”, write parameters through Online → Write to PLC, and reset/restart the PLC according to the deployment procedure. Do not put the port under a MELSOFT connection device or select UDP.

Allow MC/SLMP external clients to write registers while the PLC is running. Remove a “writing prohibited during RUN” restriction when appropriate for the approved PLC design, download the changed parameters, and reset/restart as required. A previously observed connected-but-rejected case was caused by this setting.

Use `D` data registers, not `M`, `X`, `Y` bit devices or another family's syntax. Treat each D register as a 16-bit word. Test TCP access from the backend host:

```powershell
Test-NetConnection 192.0.2.12 -Port 3301
```

For `Connection refused`, check the SLMP entry, TCP selection, downloaded parameters, and reset. For accepted TCP but rejected writes, check runtime write permissions.

## Inovance AM/AC: engineering setup

Enable Modbus TCP Server, TCP `502`, Unit ID `1`, and remotely accessible `MW` holding registers. Allow the backend host through relevant firewall/IP restrictions. The current connector fixes Unit ID to `1`; other station numbers need a backend change and validation before deployment.

Use the table's `MW10000..MW10007` words and keep readback registers current. H5U/EASY families using `D` or `R`, and coil/bit schemes, cannot simply reuse this AM/AC example: first verify the driver and address model.

```powershell
Test-NetConnection 192.0.2.13 -Port 502
```

If the port is reachable but I/O fails, check server enablement, Unit ID, register mapping/permissions, and network restrictions.

## Siemens S7: engineering setup

In TIA Portal, allow remote PUT/GET, select CPU protection/access settings that permit the approved external reads and writes, and disable optimized block access for `DB100`. Compile and download the project. Declare the table's variables as `INT`. DBW numbers are **byte offsets**: words occupy two bytes and should start at `0, 2, 4, ...`.

```powershell
Test-NetConnection 192.0.2.14 -Port 102
```

Use standard Rack `0`, Slot `0`. Avoid `DBX` and `MX` bit addresses. `IWoffset` is read-only and may only supply opening-count readback, never a notification or heartbeat write. Confirm CPU access rights, PUT/GET, nonoptimized DBs, and the downloaded project if TCP works but I/O fails.

## Commissioning and diagnosis

1. Create installation positions, including a PLC cabinet if needed.
2. Add the scanner and PLC with matching production lines and the correct ports.
3. For each scanner, map operation types `0..7` to the intended PLC and its unique address block; retain only one heartbeat per PLC.
4. Open the runtime monitor and inspect TCP and communication states separately.
5. Send `[TPL_STX]TEST-001[TPL_ETX]`; verify type `2` writes `1` and type `6` reads the opening count.
6. Test `NoRead`, lifetime limits, and manual rescans, checking types `0`, `1`, `3`, `4`, and `7`.

TCP reachability alone does not prove write permission. Check the PLC service, device IP/port, matching production line, scanner ownership of mappings, and downloaded PLC program. Do not rescan merely to compensate for failed PLC communication: a cushion count may already have committed. See the [full configuration guide](device-configuration.en.md) and [communication verification design](../wms-opc/docs/device-communication-design.en.md).
