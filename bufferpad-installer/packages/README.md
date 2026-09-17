# 本地运行时安装包

**简体中文** · [English](README.en.md) · [文档目录](../../docs/README.md)

源码仓库不包含这些第三方安装包。可运行 `scripts/download-packages.ps1` 从官方入口获取，或用 `scripts/collect-packages.ps1` 收集已有文件。

| 组件 | 安装器识别模式 | 下载脚本当前目标 / 官方入口 |
|---|---|---|
| JDK 17 | `*jdk*17*.zip` | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=17)，脚本使用 Java 17 latest GA，保存为 `jdk-17-windows-x64.zip` |
| MySQL 8.0 | `mysql-8.0.*-winx64.zip` | [MySQL](https://dev.mysql.com/downloads/mysql/)，脚本当前请求 8.0.46 |
| nginx Windows | `nginx*.zip` | [nginx](https://nginx.org/en/download.html)，脚本依次尝试 1.26.3、1.26.2、1.24.0 |
| WinSW | `WinSW-x64.exe` | [WinSW v2.12.0](https://github.com/winsw/winsw/releases/tag/v2.12.0) |
| VC++ Runtime x64 | `VC_redist.x64.exe` | [Microsoft 下载入口](https://aka.ms/vs/17/release/vc_redist.x64.exe) |

迁移前交付目录曾包含 JDK 17.0.19、MySQL 8.0.45 和 nginx 1.24.0；这不等于下载脚本的其他版本已通过当前版本实机验收。若官方不再提供脚本中的版本，请从官方历史版本入口准备匹配包，或单独更新下载脚本并验证。

下载后核对发布方的签名或校验值。脚本能检查 ZIP 可读性，但这不能替代发布方校验。本次源码迁移不承诺每个下载链接始终可用，也不附带对所有运行时包的再分发授权。

本目录除 README 和占位文件外都被 Git 忽略。也不要把这些文件放到 GitHub Release 以绕过相同的许可核实。
