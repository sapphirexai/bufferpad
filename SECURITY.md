# 安全说明与报告

**简体中文** · [English](SECURITY.en.md) · [文档目录](docs/README.md)

## 私密报告 / Private reporting

已开启 GitHub 私密漏洞报告。请使用 [Report a vulnerability](https://github.com/sapphirexai/bufferpad/security/advisories/new)，也可以从仓库 Security → Advisories 进入。此入口需要登录 GitHub；不要把漏洞利用细节转发到公开 Issue。

报告请包含受影响版本或提交、复现步骤、影响范围，以及经过脱敏的最小示例。不要上传可用密码、会话令牌、私钥、完整数据库或真实现场连接资料。报告与修复讨论在私密渠道进行，公开披露时间由维护者与报告者协调。个人维护项目暂不承诺固定响应时限。

For security vulnerabilities, use GitHub's private [Report a vulnerability](https://github.com/sapphirexai/bufferpad/security/advisories/new) form. Include the affected version/commit, reproduction steps, impact, and a minimal sanitized example. Do not disclose exploit details or credentials in public issues. This volunteer-maintained project does not promise a fixed response time.

## 支持范围

安全修复优先针对当前 main 和最新公开预览版。`archive/` 分支仅用于历史追溯，不接受独立维护或安全回补。部署者应评估最新修复后再更新，不把历史快照当作受支持版本。

## 部署注意事项

新部署必须自行设置初始管理员密码，维护账号默认关闭。更改维护密码哈希并重启会撤销旧维护会话；已有普通账户不会被初始管理员配置重置。应用目前包含持久会话机制，应按实际部署边界配置 HTTPS、Cookie 和访问控制。

设备通信代码可向 PLC 写入业务数据。请在隔离的测试环境核实设备类型、地址与写入行为，之后再应用到实际产线。代码回环测试不等于设备或工艺认证。

历史配置已做公开迁移清理。若过去实际使用的凭据曾被泄露，应在相应环境撤销或更换；仅从仓库删除文件不能撤销凭据。
