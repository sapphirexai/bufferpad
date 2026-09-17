# Changelog

[简体中文](CHANGELOG.zh-CN.md) · **English** · [All documentation](docs/README.en.md)

This log describes public releases. Earlier development history is retained in Git; see [migration history](docs/migration-history.en.md).

## Unreleased

- Add English documentation throughout the project, reciprocal language links, complete documentation indexes, and checks for language pairing/navigation.
- Remove former company branding from the application header, authentication screens, and documentation screenshots. Business behavior is unchanged.

## 0.1.1 — 2026-09-17

- Fix the new PowerShell syntax checker to read source as UTF-8 explicitly; Windows PowerShell 5.1 otherwise misinterprets some BOM-less Chinese scripts.
- Verify the checker under Windows PowerShell 5.1 and PowerShell 7, and document the separate, unverified installer runtime compatibility.
- No business logic or installer source changes; the v0.1.0 tag remains unchanged.

## 0.1.0 — 2026-09-17

First public source preview / 首个公开源码预览版。

### Included

- Barcode-based cushion usage tracking, duplicate-scan handling, lifetime thresholds, event history, and PLC communication adapters.
- Vue frontend, Java backend, Windows installation and maintenance scripts in one repository.
- All 304 source development commits retained through documented history cleanup.
- Environment-configured deployment credentials; maintenance login disabled by default.
- Illustrated product tour, build instructions, and contribution/security guidance.
- GitHub CI for frontend unit tests and build, repository checks, and PowerShell syntax.
- Bilingual project overview and roadmap, issue/PR templates, private vulnerability reporting, and a protected-main maintenance workflow.

### Requirements and limits

- HslCommunication Java 3.4.0 must be obtained separately with appropriate authorization. Its JAR is not distributed here.
- Runtime installers, frontend/backend binaries, logs, and PDF documents are excluded. GitHub's source downloads are not an offline installer.
- Complete backend builds and tests require the local HSL dependency. Public CI does not execute those tests.
- Physical PLC validation, live MySQL upgrades, production service installation, and browser E2E are not certified by this preview.

See [build instructions](docs/build-and-package.en.md), [migration validation](docs/migration-validation.en.md), and [current CI scope](docs/ci.en.md).
