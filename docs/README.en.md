# Documentation index

[简体中文](README.md) · **English** · [All documentation](README.en.md)

Use the language links at the top of each page to switch editions. English documentation explains the existing Chinese interface; it does not change the application language. Historical acceptance records describe their original deliveries, not freshly executed tests of the current commit.

## Project and contributions

- [Contributing](../CONTRIBUTING.en.md)
- [BufferPad](../README.en.md)
- [Maintenance roadmap](../ROADMAP.en.md)
- [Security policy and reporting](../SECURITY.en.md)
- [Third-party components](../THIRD_PARTY_NOTICES.en.md)
- [Changelog](../CHANGELOG.md)

## Setup, build, and maintenance

- [Build and local packaging](build-and-package.en.md)
- [Automated checks](ci.en.md)
- [Getting started](getting-started.en.md)
- [Maintenance and release workflow](maintaining.en.md)
- [Repository migration history](migration-history.en.md)
- [Open-source migration validation — 2026-09-17](migration-validation.en.md)
- [Illustrated BufferPad tour](product-tour.en.md)

## Components and deployment

- [Recommended PLC configuration](../bufferpad-installer/plc-recommended-configuration.en.md)
- [Windows offline deployment scripts](../bufferpad-installer/README.en.md)
- [Local backend artifact](../bufferpad-installer/app/backend/README.en.md)
- [Database initialization](../bufferpad-installer/app/db/README.en.md)
- [Local frontend artifacts](../bufferpad-installer/app/frontend/README.en.md)
- [Local runtime packages](../bufferpad-installer/packages/README.en.md)
- [Login, permissions, and recovery for deployments](../bufferpad-installer/user-login.en.md)
- [Short deployment configuration guide](../bufferpad-installer/site-configuration.en.md)
- [Device configuration guide](../bufferpad-installer/device-configuration.en.md)
- [BufferPad frontend](../bufferpad/README.en.md)
- [BufferPad backend (`wms-opc`)](../wms-opc/README.en.md)
- [Local HslCommunication dependency](../wms-opc/src/main/resources/lib/README.en.md)

## Backend operations and device records

- [Eight PLC operation types: historical chain acceptance](../wms-opc/docs/plc-operation-acceptance.en.md)
- [Scan-operation log maintenance](../wms-opc/docs/scan-operation-logs.en.md)
- [Log pagination](../wms-opc/docs/log-pagination.en.md)
- [Log retention and cushion time-range export](../wms-opc/docs/log-retention-export.en.md)
- [Authentication and authorization](../wms-opc/docs/authentication.en.md)
- [Device state and maintenance administrator correction — historical record](../wms-opc/docs/device-state-admin-acceptance.en.md)
- [Separating TCP connectivity from communication verification](../wms-opc/docs/device-communication-design.en.md)
- [Device communication verification: historical acceptance](../wms-opc/docs/device-communication-acceptance.en.md)

## Database upgrades

- [Legacy upgrade guide — compatibility entry](../wms-opc/docs/sql/20260914-legacy-upgrade.en.md)
- [Legacy upgrade acceptance — compatibility entry](../wms-opc/docs/sql/20260914-legacy-upgrade-acceptance.en.md)
- [Legacy database upgrade — delivery 20260916-r5](../wms-opc/docs/sql/20260915-legacy-upgrade.en.md)
- [Legacy SQL upgrade acceptance — 20260916-r5](../wms-opc/docs/sql/20260915-legacy-upgrade-acceptance.en.md)

## Public releases

- [BufferPad v0.1.0 — Source preview](releases/v0.1.0.en.md)
- [BufferPad v0.1.1 — Source preview validation fix](releases/v0.1.1.en.md)

## Maintaining documentation

Language pairs are listed in `docs/languages.json`. When adding or renaming a document, update that manifest, both indexes, and reciprocal links. Run `python scripts/check-repository.py` before committing. The PR template is a single bilingual file. Preserve command/configuration/protocol identifiers and historical evidence filenames; private workspace reports are not public documentation.
