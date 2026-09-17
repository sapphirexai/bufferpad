# Maintenance and release workflow

[简体中文](maintaining.md) · **English** · [All documentation](README.en.md)

[Contributing](../CONTRIBUTING.en.md) · [CI scope](ci.en.md) · [Roadmap](../ROADMAP.en.md)

## Routine changes

1. Create a working branch from current `main` and make focused changes for a real problem.
2. Open a PR describing behavior, tests, and unverified scope.
3. Wait for `Frontend`, `Repository checks`, and `PowerShell` to pass. Update the branch if `main` changes.
4. Resolve discussions, review the diff, and merge. The single-maintainer workflow does not require a second approver, but administrators must also satisfy required checks. Do not force-push or delete `main`.

Preserve ordinary merge history; there is no requirement to linearize migration merges. `archive/` branches preserve sanitized source history and are not development targets. Never merge unsanitized original GitLab history back into the public repository.

## Source releases

1. Update the changelog and relevant `docs/releases/` notes in both languages, separating verified and unverified scope.
2. After PR/CI success, select an exact `main` commit and create its version tag and GitHub Release.
3. Mark previews as prereleases. Tags and notes must match the code; original development dates are not the first public release date.
4. Initial previews distribute only GitHub-generated source archives, without HSL, third-party installers, or compiled bundles containing them. Review actual contents and redistribution conditions before future binary releases.
5. Clone from the remote and check tags, files, dependency guidance, and CI. GitHub source ZIPs omit Git history; use `git clone` for the full record.

Repository cloning does not back up GitHub settings. Record changes to protection rules, topics, or private reporting, then read back those settings to verify them.
