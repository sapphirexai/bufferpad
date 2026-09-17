# Local runtime packages

[简体中文](README.md) · **English** · [All documentation](../../docs/README.en.md)

This source repository excludes third-party installers. Use `scripts/download-packages.ps1` from the installer directory to download from official entry points, or `scripts/collect-packages.ps1` to collect existing files.

| Component | Recognized filename pattern | Script target / official entry point |
|---|---|---|
| JDK 17 | `*jdk*17*.zip` | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17); Java 17 latest GA saved as `jdk-17-windows-x64.zip` |
| MySQL 8.0 | `mysql-8.0.*-winx64.zip` | [MySQL](https://dev.mysql.com/downloads/mysql/); script requests 8.0.46 |
| nginx Windows | `nginx*.zip` | [nginx](https://nginx.org/en/download.html); tries 1.26.3, 1.26.2, then 1.24.0 |
| WinSW | `WinSW-x64.exe` | [WinSW v2.12.0](https://github.com/winsw/winsw/releases/tag/v2.12.0) |
| VC++ Runtime x64 | `VC_redist.x64.exe` | [Microsoft download](https://aka.ms/vs/17/release/vc_redist.x64.exe) |

The pre-migration delivery directory contained JDK 17.0.19, MySQL 8.0.45, and nginx 1.24.0. That does not certify every version targeted by the download script. If a target disappears, obtain the matching package through official archives or update and validate the script separately.

Verify publisher signatures/checksums. ZIP readability checks do not replace publisher verification. This source migration does not guarantee perpetual link availability or grant redistribution rights for runtime packages.

Only the README language pair and placeholder are tracked here. Do not upload packages to GitHub Releases to bypass the same licensing review.
