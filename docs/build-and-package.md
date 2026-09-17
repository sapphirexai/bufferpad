# 构建与本地打包

**简体中文** · [English](build-and-package.en.md) · [文档目录](README.md)

## 1. 第三方依赖

HSL JAR 放入 `wms-opc/src/main/resources/lib/HslCommunication-3.4.0.jar`。从作者或其他合法渠道取得适用版本和授权，详见[依赖说明](../wms-opc/src/main/resources/lib/README.md)。该文件、Maven target 和前端 dist 都被 Git 忽略。

## 2. 构建与测试

从仓库根目录运行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-backend.ps1 -PublicRepositories
npm --prefix .\bufferpad ci --ignore-scripts --legacy-peer-deps --no-audit --no-fund
npm --prefix .\bufferpad run unit -- --runInBand
$env:NODE_OPTIONS = '--openssl-legacy-provider'
npm --prefix .\bufferpad run build
```

后端入口先检查本地 HSL JAR，随后执行 Maven 测试和生产打包。`-PublicRepositories` 使用仓库内的独立 Maven Central settings，覆盖本机可能配置的内部镜像；需要公司代理或专用仓库时可省略该参数，使用自己的 Maven 配置。`-SkipTests` 只用于已完成验证的后续打包，不代表测试通过。

产物为 `wms-opc/target/opc-0.0.1.-SNAPSHOT.jar` 和 `bufferpad/dist/`。后端运行 profile 通过 `SPRING_PROFILES_ACTIVE` 或 `--spring.profiles.active` 设置，Maven 的 `-Pprod` 不替代运行时 profile。

## 3. 组装 Windows 应用目录

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\prepare-app.ps1
```

该脚本从相邻源码目录复制后端 JAR、前端 dist，并同步部分数据库升级脚本。应用成品放入 `app/backend`、`app/frontend`；`app/db` 是保留在 Git 中的数据库源码，不属于应删除的构建产物。

## 4. 准备运行时安装包

按 [packages/README.md](../bufferpad-installer/packages/README.md) 准备第三方包。可使用下载脚本，或将自己已取得的包放入目录：

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\download-packages.ps1
```

此命令会下载较大的第三方文件。各软件依照其许可证使用；获取到文件不表示具有任意再分发权。打包得到的后端 JAR 也可能包含 HSL，因此请核实许可后再分发。

## 5. 设置安装凭据并预检

在即将运行安装器的 PowerShell 会话中设置：

```powershell
$env:BUFFERPAD_ADMIN_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host '初始管理员密码' -AsSecureString)).Password
$env:BUFFERPAD_MYSQL_ROOT_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'MySQL root 密码' -AsSecureString)).Password
$env:BUFFERPAD_DB_PASSWORD = $env:BUFFERPAD_MYSQL_ROOT_PASSWORD
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\install.ps1 -DryRun -SkipPackageDownload -SkipDbImport
```

最后一行按安装器默认的 `MysqlUser = 'root'` 示例设置。若配置了独立应用账户，请分别填写对应密码。配置项见 `config/install.config.ps1`。真正安装时不接受空值和 `CHANGE_ME` 占位符；DryRun 可使用临时占位值检查模板，不会据此安装可用账号。

实际安装需要管理员终端，并会安装服务、创建文件及初始化或升级数据库。确认预检结果和独立安装目录后，按[安装程序说明](../bufferpad-installer/README.md)运行正式安装。

## 6. 验证范围

- Jest、Java 单元测试、H2 认证测试与回环协议测试用于验证代码行为。
- PowerShell 语法检查和 DryRun 用于验证安装脚本及配置生成。
- 实际 Windows 服务安装、真实 MySQL 升级及物理 PLC 联调需在各自测试环境验证。

根目录及安装程序的忽略规则会阻止重新提交本地安装包、JAR、dist 和日志。提交前检查 `git status`，数据库升级 SQL 的变更应单独审阅。
