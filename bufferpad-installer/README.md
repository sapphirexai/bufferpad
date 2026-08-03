# 缓冲垫项目 Windows 离线安装包

`bufferpad-installer` 用于在新的 Windows 10/11 机器上一键安装、启动、卸载缓冲垫项目运行环境。

安装内容包括：

- JDK 17
- MySQL 8.0
- nginx，承载 Vue 前端静态文件
- 缓冲垫 Spring Boot 后端
- MySQL、后端、nginx 的 Windows 服务
- 后端 prod 服务监控计划任务

当前随包应用提供缓冲垫扫码计数、两小时防重复计数、寿命预警、PLC 联动、设备状态、
运行告警和最近操作。运行事件写入 `operation_event` 并通过 SSE 实时显示，后端默认每天
清理30天前的事件数据。

## 开机启动

安装脚本会注册并配置以下开机启动项：

```text
BufferPadMySQL    MySQL 服务，Automatic
BufferPadBackend  后端 prod 服务，Automatic delayed start
BufferPadNginx    nginx 前端服务，Automatic delayed start
```

Vue 前端是静态文件，由 `BufferPadNginx` 提供访问，所以没有单独的前端进程或前端服务。

后端服务通过 WinSW 注册为 Windows 服务，并使用 prod 环境启动：

```text
--spring.profiles.active=prod
```

同时安装脚本会注册后端监控计划任务：

```text
BufferPadBackendMonitor
```

该任务在 Windows 开机后启动，也会在安装完成后立即启动。它执行：

```text
C:\bufferpad\backend\backend-prod-monitor.ps1
```

监控脚本每 15 秒检查一次 `BufferPadBackend`。如果后端服务停止，会自动重新启动后端服务。

## 目录结构

```text
bufferpad-installer/
  packages/              Java、MySQL、nginx、WinSW、VC++ 运行库安装包
  app/backend/opc.jar    后端 jar，由 scripts/prepare-app.ps1 复制生成
  app/frontend/dist/     前端 dist，由 scripts/prepare-app.ps1 复制生成
  app/db/wms_opc.sql     数据库备份 SQL
  config/                安装配置和模板
  scripts/               安装、卸载、维护、测试脚本
```

安装到目标机器后，默认运行目录为：

```text
C:\bufferpad
```

安装后主要目录：

```text
C:\bufferpad\java       安装器自带 JDK
C:\bufferpad\mysql      安装器自带 MySQL
C:\bufferpad\nginx      安装器自带 nginx
C:\bufferpad\backend    后端 jar 和后端维护脚本
C:\bufferpad\frontend   前端静态文件
C:\bufferpad\conf       后端、MySQL 配置
C:\bufferpad\service    WinSW 服务程序和服务 XML
C:\bufferpad\logs       日志
C:\bufferpad\data       MySQL 数据目录
```

## 在开发机准备安装包

先在项目根目录完成测试和正式构建：

```powershell
mvn -f .\wms-opc\pom.xml -Pprod clean package
npm --prefix .\bufferpad run unit -- --runInBand
npm --prefix .\bufferpad run build
```

测试和构建成功后执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\prepare-app.ps1
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\collect-packages.ps1 -SourceDir C:\Users\YOUR_USER\Downloads\deploy
```

`prepare-app.ps1` 会复制当前后端 jar 和前端 dist 到安装包目录。

它会完整替换 `app\frontend\dist`，避免旧哈希资源被误打包，并把最新后端制品统一命名为
`app\backend\opc.jar`。执行后应确认两个文件的修改时间属于本次构建：

```powershell
Get-Item .\bufferpad-installer\app\backend\opc.jar
Get-Item .\bufferpad-installer\app\frontend\dist\index.html
```

`collect-packages.ps1` 会从指定目录收集离线安装包，例如：

```text
OpenJDK17U-jdk_x64_windows_hotspot_17.0.19_10.zip
mysql-8.0.45-winx64.zip
nginx-1.24.0.zip
WinSW-x64.exe
VC_redist.x64.exe
wms_opc.sql
```

数据库备份文件需要放在：

```text
bufferpad-installer\app\db\wms_opc.sql
```

数据库备份必须包含当前表结构，尤其是 `device_install_position`、`operation_event` 以及
`operation_event.created_date` 的索引 `idx_operation_event_created_date`。否则安装后运行监控
无法查询最近操作，或30天定时清理效率会明显下降。

如果 `packages` 目录缺少安装包，也可以尝试从官方地址下载：

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\download-packages.ps1
```

准备完成后，把整个 `bufferpad-installer` 目录复制到目标 Windows 机器。

提交安装程序仓库前建议执行完整静态检查：

```powershell
powershell -ExecutionPolicy Bypass -File .\bufferpad-installer\scripts\test-installer.ps1 -CheckPackages
```

## 一键安装

推荐双击：

```text
bufferpad-installer\scripts\install-admin.cmd
```

该脚本会自动请求管理员权限。安装完成后窗口不会直接关闭：

- 成功显示 `安装成功`
- 失败显示失败原因或退出码
- 最后显示 `按任意键退出...`

也可以在管理员 PowerShell 中执行：

```powershell
cd C:\path\to\bufferpad-installer
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
```

默认端口：

```text
MySQL:    3306
Backend:  9001
Frontend: 18088
```

安装成功后访问：

```text
http://127.0.0.1:18088
http://<本机IP>:18088
```

安装脚本会在安装过程中完成：

- 解压 Java、MySQL、nginx
- 复制后端 jar 和前端 dist
- 生成 `application-prod.yml`、`my.ini`、`nginx.conf`
- 初始化 MySQL 数据目录
- 设置 MySQL root 密码和应用数据库
- 导入 `wms_opc.sql`
- 注册并启动 `BufferPadMySQL`
- 注册并启动 `BufferPadBackend`
- 注册并启动 `BufferPadNginx`
- 注册并启动 `BufferPadBackendMonitor`
- 配置 Windows 防火墙前端端口
- 校验后端健康检查、前端页面、nginx 代理接口

## 安装配置

安装前可修改：

```text
bufferpad-installer\config\install.config.ps1
```

常用配置：

```powershell
InstallRoot = 'C:\bufferpad'

FrontendPort = 18088
BackendPort  = 9001
MysqlPort    = 3306

DatabaseName      = 'wms_opc'
MysqlRootPassword = 'CHANGE_ME_DB_PASSWORD'
MysqlUser         = 'root'
MysqlPassword     = 'CHANGE_ME_DB_PASSWORD'

MysqlServiceName   = 'BufferPadMySQL'
BackendServiceName = 'BufferPadBackend'
NginxServiceName   = 'BufferPadNginx'

BackendMonitorTaskName        = 'BufferPadBackendMonitor'
BackendMonitorIntervalSeconds = 15
```

后端 prod 外部配置安装后生成在：

```text
C:\bufferpad\conf\application-prod.yml
```

## 更新已有安装

保留 MySQL 数据，只更新程序、配置和服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 -Force
```

保留 MySQL 数据，并跳过 SQL 导入：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 -Force -SkipDbImport
```

重置 MySQL 数据目录并重新导入 SQL：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1 -Force -ResetData
```

## 状态和维护

查看状态：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\status.ps1
```

启动所有服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start.ps1
```

停止所有服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop.ps1
```

重新导入数据库：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\import-db.ps1
```

## 后端维护脚本

安装后，后端 jar 同目录会生成以下脚本：

```text
C:\bufferpad\backend\backend-prod-monitor.ps1
C:\bufferpad\backend\backend-prod-monitor-start.ps1
C:\bufferpad\backend\backend-prod-monitor-stop.ps1
C:\bufferpad\backend\backend-prod-start-once.ps1
C:\bufferpad\backend\backend-prod-stop-once.ps1
C:\bufferpad\backend\backend-script-wrapper.ps1
```

同时会生成同名 `.cmd` 包装器。

用途：

```text
backend-prod-monitor.ps1        后端 prod 监控脚本，由计划任务调用
backend-prod-monitor-start.ps1  注册并启动后端监控计划任务
backend-prod-monitor-stop.ps1   停止并删除后端监控计划任务
backend-prod-start-once.ps1     一次性启动后端 prod，并恢复 nginx 前端服务
backend-prod-stop-once.ps1      一次性停止后端 prod
backend-script-wrapper.ps1      .cmd 入口使用的提示和按键退出包装脚本
```

注意：如果后端监控任务正在运行，手动停止后端后，监控任务会把后端重新启动。需要让后端保持停止时，应先执行：

```powershell
powershell -ExecutionPolicy Bypass -File C:\bufferpad\backend\backend-prod-monitor-stop.ps1
```

人工双击 `.cmd` 包装器时，窗口不会直接关闭：

```text
backend-prod-monitor-start.cmd  成功显示“监控启动成功”
backend-prod-monitor-stop.cmd   成功显示“监控停止成功”
backend-prod-start-once.cmd     成功显示“一次性启动成功”
backend-prod-stop-once.cmd      成功显示“一次性停止成功”
```

失败时会显示失败原因或退出码，最后统一显示 `按任意键退出...`。

## 一键卸载

推荐双击：

```text
bufferpad-installer\scripts\uninstall-admin.cmd
```

该脚本会自动请求管理员权限。卸载完成后窗口不会直接关闭：

- 成功显示 `卸载成功`
- 失败显示失败原因或退出码
- 最后显示 `按任意键退出...`

也可以在管理员 PowerShell 中执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall-oneclick.ps1
```

完整卸载会清理：

- 后端监控计划任务 `BufferPadBackendMonitor`
- nginx 前端服务 `BufferPadNginx`
- 后端服务 `BufferPadBackend`
- MySQL 服务 `BufferPadMySQL`
- 指向 `C:\bufferpad` 的残留进程
- Windows 防火墙规则 `BufferPad Frontend 18088`
- Machine/User 中指向 `C:\bufferpad` 的 `JAVA_HOME`
- Machine/User `Path` 中指向 `C:\bufferpad` 的条目
- 安装目录 `C:\bufferpad`

完整卸载后会校验：

```text
BufferPadMySQL 不存在
BufferPadBackend 不存在
BufferPadNginx 不存在
BufferPadBackendMonitor 不存在
C:\bufferpad 不存在
C:\bufferpad\java 不存在
JAVA_HOME/Path 不再指向 C:\bufferpad
```

如果系统里还有其他 Java，例如：

```text
C:\Users\<用户>\.jdks\ms-17.0.15
```

卸载脚本会保留它。脚本只删除缓冲垫安装器自己安装到 `C:\bufferpad\java` 的 Java，不会删除系统或用户已有的其他 Java。

保留 MySQL 数据目录卸载：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall-oneclick.ps1 -KeepData
```

保守卸载，只移除服务和监控，保留安装目录和数据：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall.ps1
```

兼容命令：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall.ps1 -RemoveData
```

该命令会转交给 `uninstall-oneclick.ps1` 做完整清理。

## 测试完整安装卸载流程

人工测试建议顺序：

```text
1. 双击 bufferpad-installer\scripts\uninstall-admin.cmd
2. 确认显示“卸载成功”
3. 双击 bufferpad-installer\scripts\install-admin.cmd
4. 确认显示“安装成功”
5. 访问 http://127.0.0.1:18088
```

命令行测试：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\uninstall-oneclick.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\install.ps1
powershell -ExecutionPolicy Bypass -File .\scripts\status.ps1
```

## 本地验证脚本

无需管理员权限的脚本检查：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-installer.ps1
```

同时检查安装包是否齐全：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-installer.ps1 -CheckPackages
```

使用临时 MySQL 验证 SQL 可导入：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-db-import.ps1
```

使用临时 Java、MySQL、nginx 运行完整前后端冒烟测试，不注册 Windows 服务：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-runtime-smoke.ps1
```

真实安装、卸载 Windows 服务必须使用管理员权限。
