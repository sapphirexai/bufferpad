# Windows offline deployment scripts

[简体中文](README.md) · **English** · [All documentation](../docs/README.en.md)

Start with the [overview](../README.en.md) and [current build/packaging guide](../docs/build-and-package.en.md). This is source for assembling a Windows 10/11 installation, **not a bundled offline installer**. Obtain runtime packages and HSL separately and build the applications locally. Historical examples are not current production acceptance. See [CI runtime limitations](../docs/ci.en.md), especially direct execution of BOM-less Chinese scripts under Windows PowerShell 5.1.

New installations use administrator `admin` with your own `BUFFERPAD_ADMIN_PASSWORD`; there is no common default password. Existing accounts are preserved. See [login and recovery](user-login.en.md).

## Installed components and startup

The assembled installer supplies JDK 17, MySQL 8.0, nginx-hosted Vue files, the Spring Boot backend, Windows services, and a backend monitoring task. The application provides scanning, two-hour deduplication, lifetime warnings, PLC integration, status, and operation events. Structured events retain 30 days by default. Mitsubishi MC/SLMP, Inovance Modbus TCP, and Siemens S7comm adapters are included; Siemens S7-1200/1500 share type `3`, TCP `102`, Rack/Slot `0/0`, with historical type `4` normalized during migration.

After startup, the backend connects configured scanners/PLCs without a browser and retries disconnected devices.

| Service/task | Startup and purpose |
|---|---|
| `BufferPadMySQL` | Automatic MySQL service |
| `BufferPadBackend` | Automatic delayed start, WinSW, `--spring.profiles.active=prod` |
| `BufferPadNginx` | Automatic delayed start, frontend static hosting |
| `BufferPadBackendMonitor` | Starts at boot and after install; checks/restarts backend every 15 seconds |

There is no separate Vue process. The monitoring task runs `C:\bufferpad\backend\backend-prod-monitor.ps1` with the default install root.

## Directories

| Source/assembly path | Contents |
|---|---|
| `packages/` | Locally supplied Java, MySQL, nginx, WinSW, VC++ installers |
| `app/backend/opc.jar` | Backend copied by `prepare-app.ps1` |
| `app/frontend/dist/` | Frontend copied by `prepare-app.ps1` |
| `app/db/wms_opc.sql` | Tracked empty-database initialization source |
| `app/db/migrations/` | Tracked incremental SQL run in filename order |
| `config/`, `scripts/` | Configuration/templates and installation/maintenance/test scripts |

Default installed root: `C:\bufferpad`, containing `java`, `mysql`, `nginx`, `backend`, `frontend`, `conf`, `service`, `logs`, and `data`. WinSW binaries/XML live in `service`, MySQL data in `data`, and external production configuration in `conf/application-prod.yml`.

## Prepare on a development machine

Follow the root [build guide](../docs/build-and-package.en.md), including tests, authorized HSL, locked frontend installation, and the legacy OpenSSL build option. Then run from the monorepo root:

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\prepare-app.ps1
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\collect-packages.ps1 -SourceDir C:\Users\YOUR_USER\Downloads\deploy
Get-Item .\bufferpad-installer\app\backend\opc.jar
Get-Item .\bufferpad-installer\app\frontend\dist\index.html
```

Preparation replaces frontend `dist` to avoid stale hashed assets and names the backend `opc.jar`. Confirm timestamps match the intended build. Runtime examples from earlier packages include `OpenJDK17U-jdk_x64_windows_hotspot_17.0.19_10.zip`, `mysql-8.0.45-winx64.zip`, `nginx-1.24.0.zip`, `WinSW-x64.exe`, and `VC_redist.x64.exe`; use [package instructions](packages/README.en.md) for script targets and licensing boundaries.

Use the tracked sanitized SQL for fresh installations. A private deployment backup must never replace public source in Git. Any privately assembled database source needs current tables, including `device_install_position`, `operation_event`, and the event cleanup index `idx_operation_event_created_date`.

The installer runs every `app/db/migrations/*.sql` in filename order for both fresh installs and upgrades. S7 migration preserves data, expands address storage, normalizes type `4` to `3`, and event-context migration backfills available scanner/PLC names and IPs without replacing deployment settings.

Alternatively download missing runtimes with `scripts/download-packages.ps1`. Copy the assembled installer directory to the target machine and run `scripts/test-installer.ps1 -CheckPackages` before installation.

## Install and configure

Set the required credentials in the session launching installation, following [preflight instructions](../docs/build-and-package.en.md). Defaults are MySQL `3306`, backend `9001`, frontend `18088`. Configuration is in `config/install.config.ps1`:

```powershell
InstallRoot = 'C:\bufferpad'
FrontendPort = 18088
BackendPort = 9001
MysqlPort = 3306
DatabaseName = 'wms_opc'
MysqlRootPassword = $env:BUFFERPAD_MYSQL_ROOT_PASSWORD
MysqlUser = 'root'
MysqlPassword = $env:BUFFERPAD_DB_PASSWORD
MysqlServiceName = 'BufferPadMySQL'
BackendServiceName = 'BufferPadBackend'
NginxServiceName = 'BufferPadNginx'
BackendMonitorTaskName = 'BufferPadBackendMonitor'
BackendMonitorIntervalSeconds = 15
```

These are configuration entries, not a standalone PowerShell script. When `MysqlUser` is `root`, its password must match the root password; configure a separate application's credentials separately.

`scripts/install-admin.cmd` requests elevation and retains its window to show success/error and “Press any key”. Alternatively, from an elevated terminal in this installer directory:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

Installation extracts runtimes, copies applications, generates `application-prod.yml`, `my.ini`, and `nginx.conf`, initializes MySQL, sets credentials/database, imports fresh SQL where appropriate, runs migrations, registers/starts services and monitoring, configures the frontend firewall rule, and checks backend health/frontend/proxy endpoints. Open `http://127.0.0.1:18088` or the host's address.

## Update an installation

Back up and verify recovery before changing an existing installation:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 -Force
```

With existing MySQL data, `-Force` skips whole-database initialization but still runs incremental migrations. `-Force -SkipDbImport` also continues running migrations. Existing devices, mappings, cushions, details, logs, and account data are preserved according to those migrations.

`-Force -ResetData` rebuilds the MySQL data directory and **clears the existing database**; use it only for an explicitly intended fresh initialization with recoverable backups. Do not use it for a routine upgrade.

## Status and backend maintenance

Run `scripts/status.ps1`, `scripts/start.ps1`, or `scripts/stop.ps1` using the same PowerShell invocation pattern. `scripts/import-db.ps1` reimports SQL; review its effect and backup before using it on an existing database.

Generated backend scripts also have `.cmd` wrappers:

| Script | Purpose |
|---|---|
| `backend-prod-monitor.ps1` | Monitor loop used by the scheduled task |
| `backend-prod-monitor-start.ps1` | Register/start monitoring |
| `backend-prod-monitor-stop.ps1` | Stop/remove monitoring |
| `backend-prod-start-once.ps1` | Start backend once and restore nginx |
| `backend-prod-stop-once.ps1` | Stop backend once |
| `backend-script-wrapper.ps1` | Status/pause handling for `.cmd` entry points |

Stop monitoring first if the backend must stay stopped; otherwise the task restarts it:

```powershell
powershell -ExecutionPolicy Bypass -File C:\bufferpad\backend\backend-prod-monitor-stop.ps1
```

Wrappers display success or failure/exit code and wait for a key instead of closing immediately.

## Uninstall

**Full uninstall removes the installation directory and its data.** Back up needed data before `scripts/uninstall-admin.cmd` or elevated `scripts/uninstall-oneclick.ps1`. Full cleanup removes the monitor, three services, remaining processes under the install root, its frontend firewall rule, root-specific `JAVA_HOME`/`Path` entries, and the directory. Other existing Java installations are preserved.

Use `uninstall-oneclick.ps1 -KeepData` to retain MySQL data, or `uninstall.ps1` for conservative service/monitor removal while retaining files and data. Compatibility option `uninstall.ps1 -RemoveData` delegates to full cleanup.

Full-uninstall verification checks that services/task/root and its bundled Java are absent and environment entries no longer point there. Test destructive uninstall/reinstall sequences only in an isolated disposable installation, then verify status and `http://127.0.0.1:18088`.

## Validation scripts

| Script | Scope |
|---|---|
| `test-installer.ps1` | Static parsing/templates/guards without elevation; `-CheckPackages` also checks package presence |
| `test-db-import.ps1` | Temporary MySQL initialization and repeatable migrations |
| `test-runtime-smoke.ps1` | Temporary Java/MySQL/nginx full smoke test without registering Windows services |
| `test-page-entry.ps1 -BaseUrl http://HOST:PORT` | Read-only anonymous page-entry checks |

Smoke tests check session-aware page entry, permissions, exports, SSE/revocation, restart-persistent login, and the generated administrator recovery script. Anonymous/invalid sessions redirect to `/login`; valid users/admins can load business pages. nginx disables caching for HTML/runtime configuration, and installation health checks use public `/login`. New users do not require a first-login password change. Actual Windows service installation/uninstall requires elevation and separate validation.

Configure devices using [the full guide](device-configuration.en.md), [operator quick guide](site-configuration.en.md), and [PLC tables](plc-recommended-configuration.en.md).
