# Login, permissions, and recovery for deployments

[简体中文](用户登录说明.md) · **English** · [All documentation](../docs/README.en.md)

## First installation

The default administrator username is `admin`; set your own `BUFFERPAD_ADMIN_PASSWORD`. There is no universal password. New users can enter business pages immediately and optionally change their password from the top bar. `AdminUsername` / `InitialAdminPassword` in `config/install.config.ps1` affect empty-database initialization only; restarting/upgrading/changing those values never overwrites existing accounts.

Administrators use Settings → User management to create users, choose USER/ADMIN roles, enable/disable accounts, and reset passwords. Regular users can view all lines and configuration, export Excel, and change their own password, but cannot manually scan or modify settings.

## Session persistence and revocation

Database-backed sessions can survive browser closure and backend restart. Logout affects the current browser only. Password changes/resets and role/enabled changes revoke all sessions for that account. Clearing browser data or losing the cookie requires another login.

## Updating

Build both applications, then run `scripts/prepare-app.ps1` to synchronize the JAR, frontend, and authentication migrations. `scripts/install.ps1 -Force` retains MySQL data and applies migrations; do not use `-ResetData` for a normal upgrade. Repeatable authentication migrations preserve accounts, hashes, and sessions.

Startup also checks auth tables idempotently and initializes an administrator only when no accounts exist. Health checks use local backend `/actuator/health` and nginx `/api/auth/csrf`, without bypassing business authentication.

`20260914_user_first_login_optional.sql` clears the old initial-account forced-change flag while preserving passwords, roles, and valid sessions. Password changes required after an administrator reset or local recovery remain required.

## Forgotten administrator password

For the default installation, run locally:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File C:\bufferpad\backend\reset-admin-password.ps1
```

Enter a temporary password when prompted; use `-Username` to target an existing administrator. Recovery revokes that administrator's sessions and requires a change at the next login. There is no unauthenticated remote recovery endpoint.

## Validation

- `scripts/test-installer.ps1 -CheckPackages`: syntax, templates, offline packages, uninstall guards.
- `scripts/test-db-import.ps1`: temporary MySQL initialization and repeatable migrations; checks historical data/accounts/passwords/sessions are retained.
- `scripts/test-runtime-smoke.ps1`: isolated Java/MySQL/nginx auth, permissions, exports, SSE and logout closure, restart persistence, and generated recovery script. It registers no Windows services; device addresses are cleared in the temporary database to avoid physical hardware.
- Add `-KeepRunning` to retain the successful isolated environment for browser checks. Its log directory contains `runtime-state.json` with ports/processes. Test credentials in `test-auth-http.ps1` belong only to temporary accounts and are not production initialization credentials.

For complete API/session details, see [backend authentication](../wms-opc/docs/authentication.en.md).
