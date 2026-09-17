# Illustrated BufferPad tour

[简体中文](product-tour.md) · **English** · [All documentation](README.en.md)

[Project overview](../README.en.md) · [Getting started](getting-started.en.md)

BufferPad serves manufacturing lines that reuse industrial cushions: scan an identifier, record usage, check lifetime thresholds, and notify configured PLCs. These screenshots explain monitoring and diagnostics without requiring installation.

The maintainer supplied the screenshots. They retain the recorded data and states; deployment addresses are covered and former company branding is removed. They are not a live demo or proof that a particular device model passed acceptance. The interface and screenshots are currently in Chinese; English documentation does not change the application language.

## 1. Runtime monitoring and lifetime lookup

![Runtime monitor: production line, scan state, lifetime threshold, and usage history](images/runtime-monitor.png)

- **Select a production line** (`产线`): the top bar summarizes its PLC and scanner connections and faults.
- **Wait for a scan or enter a code manually** (`等待扫码` / `手动输入`): the workbench displays the current cushion and scan result.
- **Check lifetime**: the table lists identifier, position, first/last use, current count, and lifetime. The 95% threshold shown is that screenshot's configuration, not a universal default.
- **Trace usage**: search, details, log queries, and exports help investigate records.

Counting and PLC notifications depend on configuration and business rules. Repeated scans do not necessarily increment usage.

## 2. PLC connections and diagnostics

![PLC drawer: TCP connectivity, communication verification, and consecutive timeouts](images/plc-status.png)

Open **View devices** (`查看设备`) and the PLC tab to inspect device type, connection state, detection mode, update time, and recent request results.

Faults are intentionally retained in the screenshot: the first three devices report communication timeouts, while lower devices have TCP connections but unverified communication. **A connected TCP socket does not prove successful business reads/writes.** Investigate using detection modes, request records, and device settings.

## 3. Scanner connections and standby

![Scanner drawer: connected TCP sockets in passive receive mode](images/scanner-status.png)

Switch to **Barcode scanners** (`读码器`). These devices use passive receive mode. A connected scanner without a verified incoming message may simply be on standby; absence of a barcode alone should not be treated as a fault.

## From screenshots to deployment

1. Follow [Getting started](getting-started.en.md) to prepare Java, Node, MySQL, and local HSL.
2. [Build and package](build-and-package.en.md) the application. Windows scripts do not obtain HSL authorization for you.
3. Configure and verify devices in an isolated environment using the [device configuration guide](../bufferpad-installer/device-configuration.en.md).

There is no hosted demo or one-click simulated-device mode at present. This page provides a written tour; video production is outside this delivery.
