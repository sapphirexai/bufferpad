# Roadmap / 维护路线图

[中文首页](README.md) · [English overview](README.en.md)

此路线图用于说明维护方向，不是发布日期或硬件兼容承诺。优先级会根据真实反馈调整。建议通过 [Issue](https://github.com/sapphirexai/bufferpad/issues/new/choose)提供场景和验收标准。

This roadmap describes priorities, not promised delivery dates or hardware certification. Priorities may change with real user feedback; propose concrete use cases through the issue forms.

## 已具备 / Available now

- [x] 扫码、寿命记录、设备状态与 Windows 部署源码 / Scanning, lifecycle history, device status, and Windows deployment sources.
- [x] 三仓库合并并保留开发历史 / Monorepo with preserved development history.
- [x] 中英项目入口、图文导览和依赖说明 / Bilingual overview, screenshots, and prerequisite documentation.
- [x] 前端/仓库/PowerShell CI、反馈模板和私密漏洞报告 / Scoped CI, issue forms, and private security reporting.

## 近期维护 / Near-term maintenance

- [ ] 收集独立用户从克隆到运行的反馈，修复安装说明中的实际障碍 / Validate the fresh-clone setup experience with independent users.
- [ ] 核对依赖清单和已知风险，分批升级旧构建工具；先建立兼容性验证再变更基线 / Audit dependencies and modernize the legacy build incrementally.
- [ ] 整理项目版本和包元信息，减少历史名称造成的混淆 / Align versioning and package metadata without breaking deployment paths.
- [ ] 建立有证据的设备兼容表，分别标注实物、模拟和未验证状态 / Publish an evidence-based device matrix separating hardware, simulator, and untested results.
- [ ] 在隔离 Windows/MySQL 环境执行完整安装、升级与卸载回归 / Validate installation, database upgrades, and uninstall in isolated environments.

## 后续评估 / Under consideration

- [ ] 评估将 HSL 驱动依赖与核心逻辑隔离，使更多后端测试可公开运行 / Evaluate isolating HSL adapters so more backend tests can run publicly.
- [ ] 评估带示例数据的模拟设备模式，降低体验门槛 / Evaluate a sample-data and simulated-device mode.
- [ ] 在依赖和授权边界明确后评估可复现打包及制品发布 / Evaluate reproducible packaging and distribution after prerequisite and licensing review.

上述项目需要设计和验证；当前不提供这些能力。视频演示暂不安排。
These capabilities are not currently available. Video production is not scheduled in this round.

## 适合首次参与 / Good first contributions

- 根据实际安装过程改进中英文说明 / Improve documentation using a real setup attempt.
- 提供脱敏的错误复现与环境版本 / Submit a sanitized reproduction and environment details.
- 补充已有行为的边界测试 / Add meaningful boundary tests for existing behavior.
- 按设备反馈模板提供隔离环境测试证据 / Share isolated device test evidence.

先确认问题或范围，再实现较大的改动；不需要为参与而提交空改动或制造 Issue。
Discuss the scope of larger changes first; contributions should solve an actual problem.
