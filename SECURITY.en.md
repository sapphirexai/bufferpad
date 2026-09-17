# Security policy and reporting

[简体中文](SECURITY.md) · **English** · [All documentation](docs/README.en.md)

## Private reporting

GitHub private vulnerability reporting is enabled. Use [Report a vulnerability](https://github.com/sapphirexai/bufferpad/security/advisories/new), also available under Security → Advisories. GitHub sign-in is required. Do not publish exploit details in public issues.

Include the affected version or commit, reproduction steps, impact, and a minimal sanitized example. Do not upload usable passwords, session tokens, private keys, full databases, or real deployment connection details. Reports and remediation discussions stay private; the maintainer and reporter coordinate disclosure. This individually maintained project does not promise a fixed response time.

## Supported versions

Security fixes prioritize current `main` and the latest public preview. `archive/` branches exist for historical reference and receive no independent maintenance or security backports. Evaluate current fixes before upgrading; historical snapshots are not supported releases.

## Deployment considerations

Set your own initial administrator password for a new deployment. Maintenance login is disabled by default. Changing its configured password hash and restarting revokes old maintenance sessions; initial administrator settings do not reset existing regular accounts. The application uses persistent sessions: configure HTTPS, cookies, and access controls for your deployment boundary.

The device code can write business data to PLCs. Validate device types, addresses, and write behavior in isolation before connecting a production line. Protocol loopback tests are not hardware or process certification.

Historical configuration was sanitized for public migration. Revoke or rotate any previously exposed credentials in their actual environments; deleting a repository file does not revoke a credential.
