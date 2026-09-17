# 自动化检查范围

[CI 运行记录](https://github.com/sapphirexai/bufferpad/actions/workflows/ci.yml)

推送 main、codex 工作分支、向 main 提交 PR 或手动触发时，运行三个检查：

| 检查 | 范围 |
|---|---|
| Frontend | Node 18.19.0 下按锁文件安装、Jest 单元测试和生产构建 |
| Repository checks | 已跟踪 Markdown 的本地链接目标、禁止提交的成品路径、大文件及部分凭据标记 |
| PowerShell | 已跟踪脚本语法和全新克隆缺少 HSL 时的构建提示 |

CI **不运行后端完整编译或 Java 测试**，因为公开仓库不包含 HSL JAR。后端变更需要维护者准备合规依赖，在本地运行 `scripts/build-backend.ps1 -PublicRepositories`，并在 PR 写明结果。缺依赖提示检查不是后端测试通过证明。

CI 不安装 Windows 服务，不连接真实 PLC，不验证现场 MySQL 升级，也不运行浏览器 E2E。仓库检查不检查历史、外部网址可达性、Markdown 锚点或图片内容，不替代安全审查。

PowerShell CI 分别用 `pwsh`（PowerShell 7）和 Windows PowerShell 5.1 执行语法检查器。检查器显式按 UTF-8 读取源码；这不代表安装器已在两个运行时中完成正式安装验收。Windows PowerShell 5.1 直接执行部分无 BOM 中文脚本可能产生本地编码问题，现场部署前需单独验证；其余脚本检查的基线为 PowerShell 7。

Node 18.19.0 是当前旧前端的兼容性基线；选择它是为了可复现已有验证，不代表推荐作为新项目运行时。依赖升级需要单独测试。

本地复现：

```powershell
python scripts/check-repository.py
powershell -ExecutionPolicy Bypass -File scripts/check-powershell.ps1
npm --prefix bufferpad run unit -- --runInBand
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm --prefix bufferpad run build
```

前端安装步骤见[快速开始](getting-started.md)。工作流仅申请读取仓库权限，使用固定提交版本的官方 Actions，不需要部署凭据。
