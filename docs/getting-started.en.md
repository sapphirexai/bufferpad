# Getting started

[简体中文](getting-started.md) · **English** · [All documentation](README.en.md)

Run commands from the repository root unless stated otherwise. The frontend builds independently; complete functionality needs the Java backend, MySQL, and the local HSL dependency.

## Environment

- Java 17 and Maven 3.8 or a compatible newer version.
- Node.js 18.19.0 / npm: the compatibility baseline used for migration validation. The frontend uses Vue 2 and Webpack 3; old minimum-version declarations in `package.json` do not prove newer Node versions work.
- MySQL 8.0. Windows installation also needs PowerShell, WinSW, nginx, and related runtimes.
- HslCommunication 3.4.0, obtained according to the [local dependency instructions](../wms-opc/src/main/resources/lib/README.en.md).

## Database

Create a dedicated **empty** database. `bufferpad-installer/app/db/wms_opc.sql` supplies initial tables and base configuration. For existing databases, follow the [upgrade instructions](../bufferpad-installer/app/db/README.en.md); do not reimport initialization SQL.

Open the MySQL client at the repository root:

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

Then execute:

```sql
CREATE DATABASE wms_opc CHARACTER SET utf8mb4;
USE wms_opc;
SOURCE bufferpad-installer/app/db/wms_opc.sql;
```

Create an application database account with suitable permissions. The initialization SQL contains no deployment devices, business history, or user passwords. The backend creates the initial administrator when the account table is empty.

## Backend

Prepare the HSL JAR first, then set environment variables in the current terminal. Hidden input avoids writing passwords into tracked files:

```powershell
$env:BUFFERPAD_DB_USERNAME = 'bufferpad'
$env:BUFFERPAD_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Database password' -AsSecureString)).Password
$env:BUFFERPAD_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Initial administrator password' -AsSecureString)).Password
powershell -ExecutionPolicy Bypass -File .\scripts\build-backend.ps1 -PublicRepositories
java -jar .\wms-opc\target\opc-0.0.1.-SNAPSHOT.jar --spring.profiles.active=dev
```

The initial administrator password must contain 8–64 characters and at most 72 UTF-8 bytes. Once accounts exist, the initialization password is no longer needed at every startup. The default backend port is `9001`; health status is at `http://localhost:9001/actuator/health`.

Set `BUFFERPAD_DB_URL` to a complete JDBC URL if the database location differs. Druid administration and maintenance login are disabled by default. No HSL or PLC cloud-service account is required by this setup.

## Frontend

Open another terminal at the repository root:

```powershell
Set-Location .\bufferpad
npm ci --ignore-scripts --legacy-peer-deps --no-audit --no-fund
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm run dev
```

Open `http://localhost:8081`. The development server proxies `/api` to `http://localhost:9001`; override this with `BACKEND_URL`. Sign in with the account you initialized and optionally change its password in the interface.

`--ignore-scripts` skips automatic browser downloads from the old E2E tooling; it does not install an E2E browser environment. The OpenSSL compatibility option affects legacy Webpack builds, not application TLS.

## Windows installation and next steps

See [Build and package](build-and-package.en.md). GitHub provides source: prepare runtime packages and application artifacts before installation. PowerShell syntax validation handles UTF-8 explicitly, but direct execution of some BOM-less Chinese scripts in Windows PowerShell 5.1 remains unverified; see [CI scope](ci.en.md).

Before connecting physical equipment, verify protocols, addresses, counting rules, and write behavior in an isolated test environment.
