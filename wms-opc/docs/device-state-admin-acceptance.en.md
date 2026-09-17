# Device state and maintenance administrator correction — historical record

[简体中文](设备状态与超级管理员更正验收.md) · **English** · [All documentation](../../docs/README.en.md)

**2026-09-15, before public migration.** Addresses are sanitized. Historical credentials/policies below are superseded by [current authentication settings](authentication.en.md): maintenance login is now disabled unless the deployer supplies a hash. Private `tmp/` evidence is not distributed; this translation does not rerun these tests.

## Network evidence at 09:28

A Python AF_PACKET capture on historical host interface `ens192`, independent of HSL/Netty/application code, recorded SYN from `192.0.2.4:39221` to `192.0.2.21:15000`, SYN-ACK about 13 ms later, then local ACK and successful `connect()` with `SO_ERROR=0`. Three pings from the same host received no replies (100% loss). Routing was via `192.0.2.2 dev ens192`; the received Ethernet source MAC was `00:a1:67:77:55:db`.

TCP therefore succeeded despite absent ICMP responses. This does not prove a real scanner existed or identify an intermediary responder; routing through a gateway alone does not make that gateway the TCP endpoint. Gateway-side capture/rules were still needed. No application, device, or network setting changed. Evidence: historical `/tmp/bufferpad-network-proof-20260915.json`.

## Problems and corrections at that time

- Configured and some unconfigured IP/port combinations completed TCP, but PLC operations timed out. An intermediary response was a hypothesis, not an identified device.
- Runtime state no longer inherited configured “online” values. A handshake became `VERIFYING`, without advancing valid-communication time; valid PLC I/O or scanner messages confirmed online.
- This intermediate version still used scanner 90-second silence warnings and 30-second heartbeats. **The later [communication separation change](device-communication-acceptance.en.md) removed these defaults.** PLCs without verified traffic stayed unverified; no guessed registers were probed.
- Scanner reconnect exclusion lasted through asynchronous completion; stale channel closes/messages/errors could not alter replacements. Heartbeat send failure closed the current connection.
- Frontend SSE triggered complete snapshots, with unknown on failure/disconnect, reconnect refresh, 15-second refresh, and stale-request protection.
- The reserved built-in identity became `superadmin`; generation-namespaced token digests rejected old `superamin` cookies while preserving regular sessions and restart persistence. Historical fixed credentials were later replaced by deployer configuration in the open-source adaptation.
- Upgrade/verification SQL changed the reserved-name check without changing business records or schema.

## Recorded local validation

- Backend: 116 passed, no failures/errors.
- Frontend: 15 suites / 60 passed; production build passed.
- Real isolated MySQL/application: eight groups passed, covering old/new JAR transition, rejection of old built-in cookie/password, regular-account/session retention, built-in rights/change restrictions, and restart persistence.
- Evidence: `tmp/device-truth-backend-test.log`, `tmp/device-truth-frontend-test.log`, `tmp/device-truth-runtime/result.json`.

## Historical deployment and upgrade validation

Release `bufferpad-device-truth-20260915-085437` used `/docker` Compose and `/docker/nginx/bufferpad`. Health was `UP`; 20 HTTP asset content checks passed; existing users, other containers, and shared configuration were unchanged.

Thirteen snapshots over 120 seconds found no false ONLINE state among 10 devices. Under this intermediate version, silent scanners became DEGRADED and unverified PLCs stayed VERIFYING. No manual scan, PLC control, or configuration write was performed. Current-name login and logout/restrictions passed; test sessions were revoked. Two browser-automation attempts timed out, so this delivery did **not** complete visual browser acceptance; it relied on 60 frontend tests and HTTP/resource checks.

Upgrade SQL passed 27 groups including one million logs, about 150000 details, historical digest preservation, rerun/interruption recovery, reserved-name conflicts, and application login. Three isolated databases passed 45 verification checks. No full upgrade ran against the two business databases. Evidence: `tmp/bufferpad-device-truth-20260915-085437/external-verification.json` and `tmp/db-upgrade-r4-test-20260915-085358/`.
