# BufferPad frontend

[简体中文](README.md) · **English** · [All documentation](../docs/README.en.md)

Read the [project overview](../README.en.md) and [current build guide](../docs/build-and-package.en.md) first. Deployment records below are sanitized historical examples, not acceptance of the current revision. Current deployments have no shared initial password, and maintenance login is disabled by default.

The Vue 2 / Element UI frontend supports PCB return-line cushion counting, monitoring, summaries, usage records, device status, PLC mappings, and basic settings. SSE carries scan, count, and PLC results. The application currently displays Chinese labels; this document supplies English explanations.

## Modules

| Menu | Function |
|---|---|
| Runtime monitor | Line-level device summary, scan workbench, lifetime progress, alerts, paginated cushions |
| Cushion summary | Usage statistics by date |
| Usage records | Query and export usage details |
| Settings / Cushion lifetime | Global maximum uses |
| Settings / Installation positions | Position names and ordering |
| Settings / Device information | Scanner, Mitsubishi, Inovance, and Siemens connection settings |
| Settings / PLC addresses | Scanner operation-to-register mappings |

The Siemens S7 option suggests TCP `102`, covers S7-1200 and S7-1500, and displays 16-bit absolute-address examples such as `DB1.DBW0`, `MW0`, `IW0`, and `QW0`.

## Runtime monitoring

The top bar summarizes faults and PLC/scanner connections. Full names, positions, IP/ports, transition times, communication times, and errors are in the device drawer. A one-line operation feedback bar truncates long messages; detailed advice, codes, devices, register addresses, and error codes belong in the alerts/recent-operations drawer.

The scan workbench combines input, current cushion, used/lifetime, remaining uses, and progress. Before a scan or after failed input, it shows waiting/failure and `--`, not a misleading red zero. The cushion table defaults to 10 rows, keeps identifiers/dates on one line with hover detail, uses natural vertical scrolling and table-local horizontal scrolling on narrow screens.

Historical visual acceptance on 2026-08-03 recorded 10 visible rows at `1920x1080`, 8 at `1366x768`, and 7 at `1024x768`, with no page-level horizontal overflow and readable drawers. This is not a new browser test.

### Updates and correlation

- Initial entry and line changes fetch device state, the current cushion page, and 20 recent events over HTTP.
- SSE `deviceStatus` updates devices; `cushionInfo` updates the scan result and refetches the cushion page; `operationEvent` adds recent activity.
- Manual scans send `X-Operation-Id`. HTTP, counting, and PLC results for one operation are aggregated into one final notification. State SSE updates do not generate duplicate popups.
- Alert totals count business operations, not low-level events. Expand an operation to inspect its full event sequence.
- Native `EventSource` reconnects automatically; interruption and recovery appear in the feedback bar. Device state also uses snapshot refresh and disconnect/ordering protection described in the [communication design](../wms-opc/docs/device-communication-design.en.md).
- Cushion pages refresh on entry, line/page/search changes, manual scans, and scan SSE, rather than continuously polling while idle.
- Memory retains up to 60 low-level events and displays the latest 20 operations; the backend recent-event limit is 100 and event retention defaults to 30 days.

## Development, tests, and build

From this directory, using the Node 18.19.0 compatibility baseline:

```powershell
npm ci --ignore-scripts --legacy-peer-deps --no-audit --no-fund
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm run dev
```

The development port is `8081`, with `/api` proxied to `BACKEND_URL` or `http://localhost:9001`. Set `$env:BACKEND_URL = 'http://127.0.0.1:9001/'` before starting if needed. Skipping install scripts avoids legacy browser downloads, not an E2E setup.

```powershell
npx eslint src/views/Layout/Running.vue src/modules/running test/unit/specs
npm run unit -- --runInBand
npm run build
```

Production output goes to `dist/`. Empty `API_BASE_URL` in `static/config.js` means same-origin `/api` through nginx, avoiding hardcoded server IPs. Copy built frontend/backend artifacts to the installer with:

```powershell
powershell -ExecutionPolicy Bypass -File ..\bufferpad-installer\scripts\prepare-app.ps1
```

## Historical test deployment

| Item | Sanitized historical value |
|---|---|
| Host / web URL | `192.0.2.4` / `http://192.0.2.4:18088/` |
| Host directory | `/docker/nginx/bufferpad` |
| nginx container / directory | `nginx-web` / `/usr/share/nginx/bufferpad` |
| API / SSE upstream | `/api/` → `http://127.0.0.1:19001/`; `/api/sse/` → `http://127.0.0.1:19001/sse/` |

The 2026-09-14 entry release `bufferpad-login-entry-20260914-120100` added server-side session validation for business pages, uncached HTML/runtime configuration, and a login entry when signed out. Anonymous business-page requests redirect to `/login`; APIs return `401`. New accounts no longer require an initial password change; existing passwords remain.

nginx needs `http_auth_request_module` and the rules in `default.conf`. `/login` and `/static/` are public. HTML and `/static/config.js` use `Cache-Control: no-store, no-cache, must-revalidate`. Preserve the full host including port with `proxy_set_header Host $http_host`. Validate configuration before a graceful reload and wait for old workers to release listeners.

Earlier `bufferpad-auth-20260914-104318-r2` recorded forced first-login password changes and asset-hash verification, with backup `/docker/.bufferpad-auth-20260914-104318-r2/backup/frontend`; that initial-password policy is historical. The 2026-08-25 `bufferpad-s7-unified-20260825-091500` release recorded HTTP `200`, one Siemens type in `/api/options/deviceTypes`, and scanner/PLC context in `/api/operationEvents/recent`, with backup `/docker/nginx/backups/bufferpad-s7-unified-20260825-091500`.

### Deployment procedure for that environment

Replace the example host with your authorized environment. Build, archive the contents of `dist`, upload, and retain a timestamped backup:

```powershell
npm run build
$release = "bufferpad-$(Get-Date -Format 'yyyyMMdd-HHmmss')"
tar -cf "$env:TEMP\$release-frontend.tar" -C dist .
ssh root@192.0.2.4 "install -d -m 700 /docker/.$release"
scp "$env:TEMP\$release-frontend.tar" root@192.0.2.4:/docker/.$release/
```

On the server:

```bash
release=bufferpad-YYYYMMDD-HHMMSS
install -d /docker/nginx/backups
cp -a /docker/nginx/bufferpad "/docker/nginx/backups/bufferpad-$release"
tar -xf "/docker/.$release/$release-frontend.tar" -C /docker/nginx/bufferpad
sha256sum /docker/nginx/bufferpad/index.html
docker exec nginx-web sha256sum /usr/share/nginx/bufferpad/index.html
```

Do not rename/replace the bind-mounted directory itself: a running container can retain the old inode. Never overwrite `/docker/nginx/html`, which belongs to another application. If host/container hashes differ, stream the same content into the currently visible container directory instead of restarting shared nginx:

```bash
tar -cf - -C /docker/nginx/bufferpad . \
  | docker exec -i nginx-web tar -xf - -C /usr/share/nginx/bufferpad
```

For rollback, restore the matching backup contents and repeat synchronization. A planned container restart later rebinds the updated host directory. Old hashed assets are unreferenced by the new index and can be cleaned after backup verification in a maintenance window. Static-file updates need no nginx restart. If nginx configuration changes, run `docker exec nginx-web nginx -t` before `docker exec nginx-web nginx -s reload`. Keep credentials outside Git.

## Acceptance

Check `/`, `/index`, and `/summary` anonymously: expect `302` to same-origin `/login`; `/login` should return `200` with `no-store`; protected APIs return `401`. After login, business pages return `200`, and backend `/actuator/health` returns `UP`. Verify devices/recent events load and manual scanning shows a count result or specific PLC error. Check `1920x1080`, `1366x768`, and `1024x768` for scrolling, readable drawers, `--` while idle, and accessible pagination.
