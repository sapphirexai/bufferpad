# Maintenance roadmap

[简体中文](ROADMAP.md) · **English** · [All documentation](docs/README.en.md)

This roadmap describes priorities, not promised delivery dates or hardware certification. Priorities may change with real feedback. Suggest concrete use cases and acceptance criteria through the [issue forms](https://github.com/sapphirexai/bufferpad/issues/new/choose).

## Available now

- [x] Scanning, cushion lifecycle history, device status, and Windows deployment sources.
- [x] A monorepo preserving the three original repositories' development history.
- [x] Chinese and English documentation, illustrated tour, and prerequisite instructions.
- [x] Frontend, repository, and PowerShell CI; issue forms and private vulnerability reporting.

## Near-term maintenance

- [ ] Gather independent users' fresh-clone setup feedback and fix actual installation obstacles.
- [ ] Audit dependencies and known risks; establish compatibility checks before incrementally upgrading legacy build tools.
- [ ] Align project versioning and package metadata without breaking existing deployment paths.
- [ ] Publish an evidence-based device matrix distinguishing physical hardware, simulation, and untested combinations.
- [ ] Validate full installation, database upgrades, and uninstall in isolated Windows/MySQL environments.
- [ ] Resolve BOM-less UTF-8 script execution compatibility in Windows PowerShell 5.1 and define supported installer runtimes.

## Under consideration

- [ ] Isolate the HSL driver dependency from core logic so more backend tests can run publicly.
- [ ] Provide sample data and a simulated-device mode to simplify evaluation.
- [ ] Evaluate reproducible packaging and artifact releases after dependency and redistribution boundaries are clear.

These capabilities need design and validation and are not currently available. Video production is not scheduled in this round.

## Good first contributions

- Improve Chinese or English instructions based on a real setup attempt.
- Submit sanitized reproductions with environment versions.
- Add meaningful boundary tests for existing behavior.
- Share isolated device test evidence through the device feedback form.

Discuss the scope before substantial implementation. Contributions should solve an actual problem; empty changes or artificial issues are unnecessary.
