# Automated checks

[简体中文](ci.md) · **English** · [All documentation](README.en.md)

[CI runs](https://github.com/sapphirexai/bufferpad/actions/workflows/ci.yml)

Pushes to `main` and `codex/**`, PRs targeting `main`, and manual dispatch run three checks:

| Check | Scope |
|---|---|
| Frontend | Locked dependency installation, Jest tests, and production build on Node 18.19.0 |
| Repository checks | Tracked Markdown local links, excluded artifact paths, large files, selected credential markers, and bilingual navigation |
| PowerShell | Tracked script syntax and missing-HSL guidance in a clean checkout |

CI **does not compile the complete backend or run Java tests**, because the public repository excludes the HSL JAR. Backend changes require an appropriately obtained local dependency and `scripts/build-backend.ps1 -PublicRepositories`; report results in the PR. A missing-dependency guard check is not backend test evidence.

CI does not install Windows services, connect physical PLCs, validate deployment MySQL upgrades, or run browser E2E. Repository checks do not inspect Git history, external URL availability, Markdown anchors, or image content, and do not replace security review.

PowerShell CI runs the syntax checker under both PowerShell 7 (`pwsh`) and Windows PowerShell 5.1. The checker reads UTF-8 explicitly. This does not certify actual installer execution under both runtimes: Windows PowerShell 5.1 may misread some BOM-less Chinese scripts when executing them directly. Validate that separately before deployment; PowerShell 7 is the other script-validation baseline.

Node 18.19.0 reproduces the legacy frontend's tested environment; it is not a recommendation for new projects. Test dependency upgrades separately.

Reproduce locally:

```powershell
python scripts/check-repository.py
powershell -ExecutionPolicy Bypass -File scripts/check-powershell.ps1
npm --prefix bufferpad run unit -- --runInBand
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm --prefix bufferpad run build
```

See [Getting started](getting-started.en.md) for dependency installation. The workflow requests read-only repository permissions, pins official Actions to commits, and needs no deployment credentials.
