# Open-source migration validation — 2026-09-17

[简体中文](migration-validation.md) · **English** · [All documentation](README.en.md)

This records the first public adaptation after importing 304 source commits. Other pre-migration acceptance documents are historical records.

| Check | Recorded result |
|---|---|
| Source mapping | 48 frontend, 243 backend, 13 installer commits mapped; five cleaned empty commits retained |
| Metadata and parent relationships | All 304 checked individually |
| Historical trees | Compared every source tree after exclusion, sanitization, and directory prefixing |
| Frontend dependencies | Locked installation passed on Node 18.19.0; legacy E2E downloads skipped |
| Frontend unit tests | 16 suites, 65 tests passed |
| Frontend production build | Passed with the legacy Webpack OpenSSL option |
| Backend build | Passed with public Maven repositories and a local HSL JAR |
| Backend tests | 28 suites, 152 tests; no failures, errors, or skips |
| PowerShell | Parsing, application preparation, DryRun templates, and service configuration passed |
| Uninstall guards | Unmarked targets rejected; marked DryRun retained files; drive roots rejected |
| Missing prerequisites | Missing HSL produced a clear error; actual installation rejected a missing initial password early |
| Markdown | Local documentation links passed |

Backend validation used an existing local HSL JAR and confers no redistribution rights. The JAR, runtime packages, generated applications, and logs are not distributed with source.

This migration did not perform physical PLC integration, production Windows service installation, real MySQL legacy upgrades, or browser E2E tests. It did not redownload and validate every third-party runtime package. Authentication integration tests used independent H2 databases; protocol tests used local loopback or simulated devices.

Legacy upgrade SQL was adapted to preserve existing accounts and delegate empty-table administrator initialization to backend deployment settings. These scripts were not run against an actual deployment database during this migration.

Legacy dependency deprecation and coverage-template warnings did not fail these builds or tests. Validate future upgrades separately.
