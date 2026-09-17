# 快速开始

**简体中文** · [English](getting-started.en.md) · [文档目录](README.md)

从仓库根目录执行命令。前端可单独构建；完整功能需要 Java 后端、MySQL 及 HSL 本地依赖。

## 环境

- Java 17、Maven 3.8 或更新的兼容版本。
- Node.js 18.19.0 / npm 是此次迁移使用的兼容性验证环境。前端沿用 Vue 2 和 Webpack 3；不要仅根据旧 package.json 的最低版本约束推断新 Node 版本可直接运行。
- MySQL 8.0；Windows 安装方式另需 PowerShell、WinSW、nginx 和相关运行时。
- HslCommunication 3.4.0，按照[本地依赖说明](../wms-opc/src/main/resources/lib/README.md)自行准备。

## 数据库

为本项目创建一个独立的空数据库。源文件 `bufferpad-installer/app/db/wms_opc.sql` 包含初始表结构和基础配置；已有数据库请阅读升级说明，不要直接重新导入初始化脚本。

在仓库根目录打开 MySQL 客户端：

```powershell
mysql --default-character-set=utf8mb4 -u root -p
```

在客户端中执行：

```sql
CREATE DATABASE wms_opc CHARACTER SET utf8mb4;
USE wms_opc;
SOURCE bufferpad-installer/app/db/wms_opc.sql;
```

为应用准备有相应权限的数据库账户。初始化 SQL 不包含现场设备、业务历史或用户密码。后端启动时会创建空库的初始管理员。

## 后端

先准备 HSL JAR，再设置当前终端会话的环境变量。下面使用隐藏输入，密码不必写入被 Git 跟踪的配置文件：

```powershell
$env:BUFFERPAD_DB_USERNAME = 'bufferpad'
$env:BUFFERPAD_DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host '数据库密码' -AsSecureString)).Password
$env:BUFFERPAD_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host '首次初始化管理员密码' -AsSecureString)).Password
powershell -ExecutionPolicy Bypass -File .\scripts\build-backend.ps1 -PublicRepositories
java -jar .\wms-opc\target\opc-0.0.1.-SNAPSHOT.jar --spring.profiles.active=dev
```

初始管理员密码要求 8–64 个字符，UTF-8 编码不超过 72 字节。已有用户后无需在每次启动时继续提供初始密码。后端默认端口 `9001`，健康检查为 `http://localhost:9001/actuator/health`。

若数据库地址不同，设置完整 JDBC URL 到 `BUFFERPAD_DB_URL`。默认关闭 Druid 管理页面及维护账号，不需要 HSL 或 PLC 的云端服务账户。

## 前端

另开终端：

```powershell
Set-Location .\bufferpad
npm ci --ignore-scripts --legacy-peer-deps --no-audit --no-fund
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm run dev
```

开发页面默认是 `http://localhost:8081`，`/api` 代理至 `http://localhost:9001`。可通过 `BACKEND_URL` 覆盖后端地址。登录使用自己初始化的账户，之后可在界面修改密码。

`--ignore-scripts` 用于跳过旧版 E2E 工具的自动浏览器下载；此步骤不代表 E2E 浏览器环境已安装。OpenSSL 兼容参数用于旧 Webpack 的构建过程，不改变服务 TLS 配置。

## Windows 安装与后续使用

参见[构建与打包](build-and-package.md)。从 GitHub 获取的是源码，必须先准备运行时依赖和应用制品。开始连接真实设备前，在测试环境核实协议、地址、计数规则和写入行为。
