# 参与贡献

欢迎提交可复现的问题、文档改进和代码贡献。请先阅读[快速开始](docs/getting-started.md)及[构建说明](docs/build-and-package.md)。

从 [Issue 选择器](https://github.com/sapphirexai/bufferpad/issues/new/choose)选择 Bug、功能建议或设备兼容反馈。中文和英文均可；安全漏洞请使用 [私密报告](https://github.com/sapphirexai/bufferpad/security/advisories/new)。

Issue 请包含使用的提交或版本、操作系统、组件、预期与实际结果及复现步骤。设备问题注明协议和型号，地址使用虚构示例；不要上传实际凭据、完整数据库或未脱敏的现场日志。

提交 PR 时说明修改原因、行为变化和验证范围。业务修复与历史迁移、依赖升级分别提交。涉及计数、防重复、权限或协议行为时运行对应测试；未进行真实设备验证时请明确说明。

建议先 Fork、创建工作分支，再向 `main` 提交 PR。参考自动填入的 PR 模板；[三个 CI 检查](docs/ci.md)通过后由维护者审阅。后端变更需要维护者用本地 HSL 依赖执行 Java 测试，CI 成功不代表后端已测试。

main 已保护，禁止强推和删除；要求 PR、分支同步及三个指定检查通过。当前单维护者流程不强制另一位维护者批准。首次参与可参考[路线图中的入门任务](ROADMAP.md)，维护细节见[维护流程](docs/maintaining.md)。

Bug reports and pull requests are welcome in Chinese or English. Use the issue forms, describe reproduction steps and validation scope, and submit changes against `main`. Backend tests require a separately obtained HSL dependency. Do not include credentials or deployment artifacts.

源码按现有三个组件维护，数据库变更附升级 SQL 和兼容说明。前端 `build/` 是构建脚本源码，安装程序 `app/db/` 是数据库源码，需要保留。安装包、HSL JAR、编译产物、日志及 PDF 不提交到仓库。

保留原有作者署名和第三方许可。新增依赖应说明用途、版本、来源和许可证。
