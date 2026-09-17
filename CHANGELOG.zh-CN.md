# 更新日志

**简体中文** · [English](CHANGELOG.md) · [文档目录](docs/README.md)

本日志记录公开版本。更早的开发提交保留在 Git 中，详见[迁移记录](docs/migration-history.md)。

## 未发布

- 补齐项目英文文档、双向语言切换及中英文文档目录，增加文档语言配对与导航检查。
- 移除应用顶部栏、认证页面和介绍截图中的旧企业标识，业务行为不变。

## 0.1.1 — 2026-09-17

- PowerShell 语法检查器显式按 UTF-8 读取源码，避免 Windows PowerShell 5.1 误读部分无 BOM 中文脚本。
- 在 Windows PowerShell 5.1 与 PowerShell 7 下验证检查器，并说明安装器运行兼容性仍需单独验证。
- 没有修改业务逻辑或安装器源码；保留 v0.1.0 标签。

## 0.1.0 — 2026-09-17

首个公开源码预览版。

### 包含内容

- 扫码计数、防重复处理、寿命阈值、事件历史及 PLC 通信适配。
- Vue 前端、Java 后端与 Windows 安装维护脚本合并为一个仓库。
- 经说明中记录的历史清理，保留全部 304 条来源开发提交。
- 通过环境变量配置部署凭据，默认关闭维护登录。
- 图文导览、构建说明、贡献与安全指南。
- GitHub CI：前端单元测试与构建、仓库检查及 PowerShell 语法检查。
- 中英文项目入口与路线图、Issue/PR 模板、私密漏洞报告和受保护 main 流程。

### 依赖与限制

- HslCommunication Java 3.4.0 需自行取得适用授权；仓库不提供 JAR。
- 运行时安装包、前后端成品、日志、PDF 均排除；GitHub 源码下载不是离线安装包。
- 完整后端构建与测试需要本地 HSL，公开 CI 不执行这些测试。
- 本预览版不代表物理 PLC、真实 MySQL 升级、正式服务安装或浏览器 E2E 已验收。

参见[构建说明](docs/build-and-package.md)、[迁移验证](docs/migration-validation.md)及[当前 CI 范围](docs/ci.md)。
