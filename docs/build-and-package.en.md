# Build and local packaging

[简体中文](build-and-package.md) · **English** · [All documentation](README.en.md)

## 1. Third-party dependency

Place the authorized HSL JAR at `wms-opc/src/main/resources/lib/HslCommunication-3.4.0.jar`. Obtain the applicable version and authorization from its author or another legitimate provider; see [dependency notes](../wms-opc/src/main/resources/lib/README.en.md). Git ignores the JAR, Maven `target`, and frontend `dist`.

## 2. Build and test

From the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-backend.ps1 -PublicRepositories
npm --prefix .\bufferpad ci --ignore-scripts --legacy-peer-deps --no-audit --no-fund
npm --prefix .\bufferpad run unit -- --runInBand
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm --prefix .\bufferpad run build
```

The backend entry point first checks the local HSL JAR, then runs Maven tests and production packaging. `-PublicRepositories` uses the repository's independent Maven Central settings, overriding locally configured internal mirrors. Omit it when you need your own proxy or private repository configuration. `-SkipTests` is for subsequent packaging after validation; using it does not establish test success.

Outputs: `wms-opc/target/opc-0.0.1.-SNAPSHOT.jar` and `bufferpad/dist/`. Set the runtime profile with `SPRING_PROFILES_ACTIVE` or `--spring.profiles.active`; Maven `-Pprod` does not replace a runtime profile.

## 3. Assemble the Windows application directory

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\prepare-app.ps1
```

This copies the backend JAR and frontend `dist` from adjacent source directories and synchronizes selected database upgrade scripts. Generated applications go into `app/backend` and `app/frontend`; `app/db` remains tracked database source, not disposable build output.

## 4. Prepare runtime packages

Follow [runtime package instructions](../bufferpad-installer/packages/README.en.md). Supply packages you already obtained or use:

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\download-packages.ps1
```

This downloads large third-party files. Acquiring them does not grant unrestricted redistribution rights. The packaged backend may also include HSL; verify applicable terms before distribution.

## 5. Set installation credentials and preflight

In the PowerShell session that will launch the installer:

```powershell
$env:BUFFERPAD_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Initial administrator password' -AsSecureString)).Password
$env:BUFFERPAD_MYSQL_ROOT_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'MySQL root password' -AsSecureString)).Password
$env:BUFFERPAD_DB_PASSWORD = $env:BUFFERPAD_MYSQL_ROOT_PASSWORD
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\install.ps1 -DryRun -SkipPackageDownload -SkipDbImport
```

The shared database password above matches the installer's default `MysqlUser = 'root'`. If you configure a separate application account, supply its password separately. Review `config/install.config.ps1`. Actual installation rejects empty values and `CHANGE_ME`; DryRun can use temporary placeholders for template checks without installing usable accounts.

Actual installation needs an elevated terminal and creates services/files and initializes or upgrades the database. Review preflight results and the dedicated target directory, then follow the [installer guide](../bufferpad-installer/README.en.md).

## 6. Validation boundaries

- Jest, Java unit tests, H2 authentication tests, and protocol loopback tests check code behavior.
- PowerShell parsing and DryRun check scripts and generated configuration.
- Actual Windows services, real MySQL upgrades, and physical PLC integration need separate environment-specific validation.

The root and installer ignore rules prevent re-adding packages, JARs, generated applications, and logs. Check `git status` before committing and review database upgrade SQL separately. See [CI scope](ci.en.md) for PowerShell runtime limitations.
