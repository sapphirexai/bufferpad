# Contributing

[简体中文](CONTRIBUTING.md) · **English** · [All documentation](docs/README.en.md)

We welcome reproducible bug reports, documentation improvements, and code contributions. Start with [Getting started](docs/getting-started.en.md) and [Build and package](docs/build-and-package.en.md).

Use the [issue chooser](https://github.com/sapphirexai/bufferpad/issues/new/choose) for bugs, feature requests, or device compatibility reports. Chinese and English are both welcome. Report vulnerabilities through [private reporting](https://github.com/sapphirexai/bufferpad/security/advisories/new).

Include the commit/version, operating system, affected component, expected and actual behavior, and reproduction steps. For devices, include the protocol and model, using fictional addresses. Never attach credentials, full databases, or unsanitized deployment logs.

Fork the repository, create a branch, and submit a PR against `main`. Explain the reason, behavior change, and validation scope using the PR template. Keep business fixes, history migration, and dependency upgrades separate. Run relevant tests for counting, deduplication, authorization, and protocol changes; state explicitly when physical devices were not tested. Database changes need upgrade SQL and compatibility notes.

All [three CI checks](docs/ci.en.md) must pass before maintainer review. Backend changes also require local Java tests with a separately obtained HSL dependency: green CI does not mean the backend was tested. Protected `main` requires a PR, an up-to-date branch, and required checks; force pushes and deletion are prohibited. The single-maintainer workflow does not require a second approver. See [first contribution ideas](ROADMAP.en.md) and the [maintenance workflow](docs/maintaining.en.md).

Keep changes within the existing component structure. `bufferpad/build/` contains build-script source, and `bufferpad-installer/app/db/` contains database source; retain both. Do not commit installers, HSL JARs, compiled output, logs, or PDFs. Preserve author attribution and third-party licenses; explain the purpose, version, source, and license of new dependencies.

For documentation changes, update both language versions and their navigation. [The documentation index](docs/README.en.md) lists all pairs. Keep commands, API names, configuration keys, SQL filenames, and historical test results consistent across languages. Do not present a historical validation result as a test of the current revision.
