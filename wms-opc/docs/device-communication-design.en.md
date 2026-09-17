# Separating TCP connectivity from communication verification

[简体中文](设备连接与通信验证分离方案.md) · **English** · [All documentation](../../docs/README.en.md)

## Goal and boundaries

A TCP handshake only proves a TCP response; idle equipment is not inherently faulty. Confirm each model's request/heartbeat protocol before enabling active checks. Defaults send no new probes and do not count silent/unverified devices as faults. The historical deployment enabled no unconfirmed scanner heartbeat or probe register.

Runtime connection objects and API DTOs add TCP state, verification state, detection mode, latest request time/type, and consecutive timeouts while retaining existing fields. No database tables/columns are added.

| State | Meaning | Counted as fault? |
|---|---|---|
| `VERIFYING` | TCP connected, communication unverified; long standby allowed | No |
| `ONLINE` | Valid recent communication, with timestamp | No |
| `CONNECTING` / `RETRYING` | Connecting / awaiting retry after an unanswered request | No, shown separately |
| `UNKNOWN` | Frontend cannot obtain current service state | No, shown separately |
| `OFFLINE` | TCP connection failed or disconnected | Yes |
| `TIMEOUT` | Actual requests timed out consecutively to threshold | Yes |
| `DEGRADED` | Protocol rejection or abnormal business value | Yes, distinct from TCP disconnect |

Replacement connections require fresh verification while retaining historical success times. A timeout fault survives a new handshake until valid communication recovers. Passive scanner silence alone never degrades it. Frontend snapshot refresh, disconnect-to-unknown, 15-second refresh, and out-of-order request protection remain.

## Scanners

Default passive mode receives barcodes, `NoRead`, and supported `HeartBeat` messages, with TCP keepalive. The old 90-second idle alarm and automatic application heartbeat are removed.

Explicitly enable the supported STX/HeartBeat/ETX protocol through `device-health.scanner-heartbeats.<device ID>`. Defaults are 30-second intervals, 10-second response waits, and three consecutive timeouts before a fault. Start waiting only after a successful send. Valid inbound messages verify communication and reset timeouts; a successful socket write alone does not. Old-channel messages, timers, and close callbacks must not change replacement-channel state.

## PLCs

Existing business write/readback and mapped type-5 heartbeat writes retain their meaning. Heartbeats do not clear business rejection. Failed business writes are logged immediately and never automatically replayed.

Optional `device-health.plc-probes.<device ID>` requires an explicitly confirmed safe Int16 read address. Disabled by default; default interval is 15 seconds and timeout threshold three. Probes do not modify PLC/cushion data or add `scan_log` rows. They use current HSL read timeout limits and the same per-PLC I/O lock as business traffic, with one probe at a time and no queue buildup.

Protocol rejection identifies address/permission problems separately from transport failures. Explicit disconnection/refusal marks offline immediately; an isolated request timeout retries, and reaching threshold faults/releases the stale connection. Valid protocol success resets timeout counts. Stale results from replaced connections cannot overwrite current state.

## Configuration and deployment

Configure backend YAML/external Spring settings by device ID; restart to apply changes. No device-management UI fields or schema migrations are introduced. Synchronize built frontend/backend artifacts to the installer.

```yaml
device-health:
  failure-threshold: 3
  scanner-heartbeats:
    999001:
      enabled: false
      interval-seconds: 30
      response-timeout-seconds: 10
  plc-probes:
    999002:
      enabled: false
      address: MW10000
      interval-seconds: 15
```

IDs/addresses are illustrative. Scanner enablement applies only to confirmed STX + HeartBeat + ETX responders, not arbitrary vendor query commands. Valid barcodes and `NoRead` also verify communication; unparsable bytes do not. Heartbeat waits must be shorter than intervals. Enabled PLC probes require a nonempty address or startup validation fails.

Load external YAML using `spring.config.additional-location` or normal Spring configuration; no Docker mount is changed automatically. Probe intervals start after the previous request finishes. Detection latency for power loss/cable removal depends on TCP errors, keepalive, and enabled checks; passive mode promises no fixed silent-disconnect deadline.

The original deployment plan used `/docker/docker-compose.yml` and `/docker/nginx/bufferpad`, with image/frontend backups and rollback, preserving other services and business configuration. That is historical context, not authorization to modify a deployment.

## Acceptance matrix

1. TCP handshake without device response remains unverified.
2. Long passive silence sends no application heartbeat and raises no fault; scanning resumes normally.
3. Enabled scanner checks cover response/no response, thresholds/recovery, stale callbacks, and send failure.
4. Disabled PLC checks send nothing extra; enabled checks only read Int16 without count/log changes.
5. PLC responses, timeouts, protocol exceptions, recovery, business concurrency, and replacement races.
6. Frontend counts, dual states, historical times, reconnect, and stale-request protection.
7. Full automated regressions plus TCP simulations; historical deployment observation for at least 120 seconds exceeded the old 90-second false-alarm window, with browser checks.
8. Authentication regression, artifact consistency, installer synchronization, and before/after schema comparison.

Recorded outcomes are in [historical acceptance](device-communication-acceptance.en.md).
