# Repository migration history

[简体中文](migration-history.md) · **English** · [All documentation](README.en.md)

On 2026-09-17, BufferPad combined three independently maintained Git repositories:

| Component | Imported branch | Source commits |
|---|---|---:|
| `bufferpad/` frontend | `main` | 48 |
| `wms-opc/` backend | `master` | 243 |
| `bufferpad-installer/` installer | `main` | 13 |

All 304 source commits retain authors, committers, timestamps, and parent relationships. Import was followed by merge and open-source adaptation commits, continuing from GitHub's original MIT LICENSE initial commit.

Public history excludes third-party installers, build artifacts, the HSL JAR, runtime logs, deployment keys, IDE metadata, and PDFs. Deployment addresses, shared credentials, and workstation paths in configuration/documentation were sanitized. Historical example values are not deployment credentials.

Adding component prefixes and cleaning content changed source commit SHAs. The maintainer retains private originals and old-to-new mappings. Commits that only changed removed content were preserved as empty commits: five in this migration. Other branch tips are retained under `archive/<component>/<source>/<original-branch>`; these are historical records, not recommended deployment versions.

Migration checked metadata, parent relationships, and filtered file trees for all 304 commits. Subsequent adaptation requires a deployer-chosen initial administrator password and disables shared maintenance login by default. Those changes are separate commits after import, without changing original development dates.

Historical acceptance documents record earlier engineering activity, not tests of the current commit. Revalidate current code using the build instructions. The migration does not include original GitLab issues, merge requests, pipeline records, or attachments.
