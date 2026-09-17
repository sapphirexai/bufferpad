# Authentication and authorization

[简体中文](用户登录与权限.md) · **English** · [All documentation](../../docs/README.en.md)

Fixed roles are `USER` and `ADMIN`. Empty account tables initialize administrator `admin` (overridable), with a required `BUFFERPAD_ADMIN_PASSWORD` and no mandatory initial change. Restarts/upgrades do not reset existing accounts or passwords.

## Built-in maintenance administrator

`superadmin` is disabled by default in the open-source version. A deployer can enable it with their own BCrypt hash in `BUFFERPAD_MAINTENANCE_PASSWORD_HASH`. No universal password/hash is distributed. Changing the hash and restarting invalidates prior maintenance sessions; clearing it and restarting disables maintenance login and old sessions. Regular database administrators can normally use local recovery instead.

This virtual account has ADMIN business permissions, ID `-1`, `builtIn=true`, and `passwordChangeAllowed=false` in login and `/auth/me` responses. The UI labels it as a super administrator, hides password change, and blocks direct access to that page. It is absent from database user lists; its reserved name cannot be created, disabled, demoted, or reset. It can reset database users through user management, revoking their sessions and requiring a password change.

It is not inserted into `sys_user` or linked through `sys_user_session`. `sys_builtin_session` stores SHA-256 digests of random tokens and creation timestamps, without expiry. Multiple browsers and restart persistence are supported; logout revokes a session. Authentication audit uses `sys_auth_audit` without passwords/raw tokens. Rate limiting, CSRF, cookies, and SSE revocation also apply. The database must remain available.

Startup creates the session table idempotently; standalone SQL is `docs/sql/20260915_builtin_admin_session.sql`, also supplied by the installer. A preexisting database user named `superadmin` cannot authenticate as the built-in account through its old password/session. Check for conflicts before upgrade; no automatic deletion occurs.

## Permissions and endpoints

Anonymous access is limited to `GET /auth/csrf`, `POST /auth/login`, and `GET /actuator/health`. Business HTTP/SSE requires login. USER may query configuration, logs, cushions, and status; export Excel; change their own password; and log out. User management, manual scanning, configuration mutations, and device connect/disconnect require ADMIN. Legacy GET connection endpoints also require ADMIN and CSRF; they are not read-only queries.

| Method/path | Purpose |
|---|---|
| `GET /auth/csrf` | Return token and set `XSRF-TOKEN` cookie |
| `POST /auth/login` | JSON `username/password`; set HttpOnly `BUFFERPAD_SESSION` |
| `GET /auth/me` | Current `id/username/role/mustChangePassword` and built-in flags |
| `POST /auth/password` | JSON `oldPassword/newPassword`; revoke all account sessions |
| `POST /auth/logout` | Revoke current browser session |
| `GET /users` | ADMIN list, without password hashes |
| `POST /users` | Create with `username/password/role` |
| `PUT /users/{id}` | Change `role/enabled`; revoke sessions if changed |
| `POST /users/{id}/password` | ADMIN reset with `password`; require subsequent change |

For writes, obtain `/auth/csrf` and send both cookies and `X-XSRF-TOKEN`. Missing login returns `401`; insufficient permission `403`; required password change `403` with `reason=PASSWORD_CHANGE_REQUIRED`; invalid CSRF uses `reason=CSRF_INVALID`. Reset users must change their password before business access, while new users can proceed immediately.

Usernames are 3–32 lowercase letters/digits/dots/underscores/hyphens and must start with a letter. Passwords are 8–64 characters, at most 72 UTF-8 bytes; self-service changes cannot reuse the old password. Disable users rather than deleting them. The last enabled administrator cannot be removed.

## Persistent sessions

Server sessions have no time-expiry field. SHA-256 token digests are stored in `sys_user_session`; raw tokens exist only in HttpOnly, SameSite=Lax cookies. Cookies last 365 days and renew on successful authentication. Browser clearing/storage restrictions or losing the cookie require login again.

Password changes/resets and role/enabled changes revoke all account sessions; logout revokes only the current one. SSE binds to the session and closes upon revocation. Local recovery in another process is reflected in long connections within at most five seconds. Audit records login, logout, and account changes without secrets.

## Configuration and migration

Startup idempotently applies `src/main/resources/db/auth-schema.sql`; standalone upgrade SQL is `docs/sql/20260914_user_auth.sql`. `20260914_user_first_login_optional.sql` removes the old initial-account forced-change policy while retaining requirements created by resets/recovery, and preserves passwords, roles, and sessions.

| Environment variable | Behavior |
|---|---|
| `BUFFERPAD_ADMIN_USERNAME` / `BUFFERPAD_ADMIN_PASSWORD` | Empty-table initialization; username defaults to `admin`, password has no default |
| `BUFFERPAD_COOKIE_SECURE` | Set `true` for HTTPS; default `false` for HTTP deployments |
| `BUFFERPAD_ALLOWED_ORIGINS` | Comma-separated explicit origins; defaults cover localhost/127.0.0.1 development port `8081` |
| `BUFFERPAD_MAINTENANCE_PASSWORD_HASH` | Optional maintenance BCrypt hash; disabled when absent |

Same-origin nginx does not require listing every client address. Preserve the full Host including port. Direct cross-origin access requires matching frontend `API_BASE_URL` and allowed origins. Device connections, counting, PLC heartbeats, and notifications run independently of browser login. Anonymous health returns minimal status; other management endpoints require ADMIN.

## Verification and local recovery

Run `mvn test` after preparing HSL. `AuthIntegrationTest` uses independent H2 and the real Spring Security chain for roles, CSRF, password changes, revocation, SSE closure, upgrade retention, and concurrent last-admin protection. MySQL/nginx acceptance uses installer `scripts/test-runtime-smoke.ps1`.

Run installed `backend/reset-admin-password.ps1` locally, optionally with `-Username`, and enter a temporary password. The non-Web entry point does not start device connections; it passes the password through a temporary subprocess environment, not command-line arguments. It enables the existing administrator, revokes sessions, and requires a change. Local files/database configuration access is necessary; there is no HTTP recovery API.

Open-source maintenance token digests bind `superadmin:v2` and the current configured hash. Historical `superamin` sessions are not accepted as built-in sessions. Regular database sessions are unaffected, with no schema change or session-table deletion. See the [historical correction record](device-state-admin-acceptance.en.md).
