# wms-opc 后端

PCB 回流线缓冲垫计数及 PLC 联动服务。系统接收工业读码器或前端手动输入的二维码，完成缓冲垫建档、有效次数累计、寿命判断、操作留痕，并按扫码器配置把处理结果通知对应 PLC。

## 核心业务规则

- 新二维码第一次有效扫码时创建 `cushion_info`，`used_count` 从 `1` 开始，同时写入一条 `cushion_detail`。
- 同一缓冲垫距离上次有效扫码不足 2 小时属于重复扫码，不增加次数，也不新增使用明细。
- 有效复扫通过数据库条件更新原子执行 `used_count + 1`，避免并发重复计数。
- 当 `used_count >= max_use_count` 时，只发送“超过最大次数”类型的 PLC 指令，不再额外发送“扫码成功”指令。
- 缓冲垫计数和 PLC 通知相互解耦。PLC 未配置、离线或拒绝写入时，已成功完成的缓冲垫计数不会回滚。
- 手动扫描新缓冲垫没有扫码器来源，无法唯一确定目标扫码器时会保留计数，并产生“PLC目标无法确定”运行事件。

## 技术栈

- Java 17、Spring Boot 2.7.10、Spring MVC
- MyBatis-Plus、MySQL 8、Druid
- Netty、GreenRobot EventBus
- HSL Communication：三菱 MC/SLMP、汇川 Modbus TCP
- SSE：设备状态、扫码结果和运行事件实时推送
- JUnit 4、Mockito、H2

## 代码结构

```text
cn.tpl.opc
├─ controller                 HTTP/SSE 接口层
├─ application
│  ├─ scan                    扫码用例编排、EventBus 扫码监听
│  └─ plc                     PLC 目标解析、命令派发和结果处理
├─ domain.scan                两小时有效间隔、寿命判断等业务规则
├─ service                    配置、设备、日志和运行事件服务
├─ infrastructure
│  ├─ scanner                 扫码器报文解析
│  ├─ event                   EventBus 发布适配
│  ├─ plc                     PLC I/O 结果与错误分类
│  └─ maintenance             定时数据维护
├─ netty                      设备连接、心跳和报文接收
├─ mapper / entity            MyBatis 数据访问与数据库实体
└─ commons                    DTO、请求模型、枚举和常量
```

`ScanApplicationService` 负责事务内的缓冲垫业务；`PlcNotifyService` 负责寻找扫码器、PLC 和寄存器；`PlcCommandDispatcher` 是 PLC 命令的唯一 EventBus 消费者，负责最终 I/O、状态更新、日志和运行事件。

## 扫码调用链

### 扫码器扫码

```text
扫码器 TCP 数据
  -> MsgHandler.channelRead
  -> ScannerMessageParser
     - HeartBeat：刷新扫码器存活时间
     - NoRead：发布无二维码事件
     - STX/ETX 或 [TPL_STX]/[TPL_ETX]：提取二维码
  -> EventBusMsgCushionQrCode
  -> ScanEventListener
  -> ScanApplicationService.handleScan / handleScanCodeFailed
```

### 手动扫码

```text
POST /cushion/manualCushionInfo
  -> CushionController
  -> ScanCommand
  -> ScanApplicationService.handleScan
```

两条入口最终进入同一个应用服务，因此建档、两小时防重、计数、寿命判断、明细和操作事件规则一致。

## PLC 通知调用链

```text
ScanApplicationService
  -> PlcNotifyService
     -> 根据 scannerId + 操作类型查询 plc_addr
     -> 检查目标 PLC Connection
     -> 可选解析开口数回读地址
  -> EventBusMsgPlcCmd
  -> PlcCommandDispatcher（ASYNC，唯一消费者）
     -> Connection.write
     -> 成功：记录日志、发布成功事件、按需读取开口数
     -> 失败：区分通信失败和 PLC 业务拒绝，更新设备状态并发布故障事件
```

一个扫码器只能关联一个 PLC，同一扫码器的同一种操作类型只能配置一个地址。PLC 和扫码器必须属于同一产线。

PLC 结果处理说明：

- 通信异常会把设备标记为离线。
- PLC 返回错误但网络仍通时标记为“通信受限”，例如错误码 `85` 表示 PLC 禁止运行中写入。
- 成功类指令可携带开口数回读地址；写入成功后读取 16 位整数并更新 `cushion_info` 及最新一条 `cushion_detail`。
- 心跳只证明通信链路可用，不会清除仍未恢复的 PLC 业务拒绝提示；真实业务写入成功后才恢复正常状态。

## 运行事件与前端提示

扫码、计数和 PLC 处理会生成结构化 `operation_event`，并通过 SSE 的 `operationEvent` topic 推送前端。每次扫码都有一个最长64字符的 `operation_id`，同一次扫码产生的计数、地址检查、PLC写入和开口数回读事件共用该编号，供前端合并为一条操作反馈。手动扫码可通过 `X-Operation-Id` 请求头传入编号，未传入以及扫码器扫码时由后端自动生成。主要事件包括：

- 扫码计数完成、未读码、两小时内重复扫码、达到寿命、计数失败
- PLC 地址未配置、手动扫码目标无法确定、开口数地址未配置
- PLC 离线、通知成功、拒绝写入、通信失败、开口数读取失败

事件在事务场景中于事务提交后落库和推送，避免前端收到最终已回滚的数据。历史查询接口默认返回最近 `20` 条，`limit` 最小为 `1`、最大为 `100`：

```http
GET /operationEvents/recent?workLine=1&limit=20
```

`operation_event` 默认只保留最近 30 天。定时任务每天 `02:15` 执行，每批最多删除 `1000` 条，降低大表删除对数据库的影响：

```yaml
operation-event:
  retention:
    days: 30
    cleanup-cron: "0 15 2 * * ?"
```

已有数据库需要执行 `docs/sql/20260803_operation_event_retention.sql` 补充 `created_date` 索引，并执行 `docs/sql/20260804_operation_event_correlation.sql` 增加操作关联字段和索引；两个脚本均可重复执行。关联脚本会用原 `event_id` 回填历史记录，不删除业务数据。新建数据库使用 `docs/sql/20260803_operation_event.sql`，建表时已包含全部字段和索引。

## 主要数据表

| 表 | 用途 |
| --- | --- |
| `cushion_info` | 缓冲垫当前使用次数、寿命、最后扫码时间和最近工位 |
| `cushion_detail` | 每次有效使用明细 |
| `scan_log` | 扫码及 PLC 通知文字日志 |
| `operation_event` | 面向运行监控的结构化事件，保留30天 |
| `opc_config` | 全局缓冲垫寿命，固定且仅保留 `id=1`，默认500次 |
| `device_install_position` | 现场设备安装位置及排序 |
| `device_info` | 扫码器、三菱 PLC、汇川 PLC 及连接参数 |
| `plc_addr` | 扫码器、PLC、操作类型和寄存器地址的映射 |

## 主要接口

| 模块 | 接口前缀 | 说明 |
| --- | --- | --- |
| 缓冲垫 | `/cushion` | 分页、明细、手动扫码、寿命修改、Excel 导出 |
| 设备 | `/device` | 设备 CRUD、连接和状态查询 |
| 安装位置 | `/deviceInstallPositions` | 安装位置 CRUD 和分页 |
| PLC 地址 | `/plcAddr` | PLC 地址 CRUD 和分页 |
| 全局寿命 | `/opcConfig` | 单条全局寿命配置 |
| 下拉选项 | `/options` | 设备类型、操作类型、位置、PLC 和扫码器选项 |
| 扫码日志 | `/scanLogs` | 扫码和 PLC 日志查询 |
| 运行事件 | `/operationEvents/recent` | 最近运行事件，最多100条 |
| SSE | `/sse/devicesStatus/{clientId}` | 实时设备、扫码结果和运行事件 |

## 配置与运行

环境配置位于 `src/main/resources`：

- `application.yml`：公共配置、端口、MyBatis、事件留存
- `application-dev.yml`：开发环境数据库
- `application-test.yml`：测试环境数据库
- `application-prod.yml`：生产环境默认配置，可被环境变量或外部配置覆盖

本地开发：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

执行测试并构建生产包：

```bash
mvn -Pprod clean package
```

生成制品：

```text
target/opc-0.0.1.-SNAPSHOT.jar
```

启动生产环境：

```bash
java -jar target/opc-0.0.1.-SNAPSHOT.jar --spring.profiles.active=prod
```

健康检查：

```http
GET /actuator/health
```

## 数据库脚本

| 文件 | 用途 |
| --- | --- |
| `docs/sql/20260803_operation_event.sql` | 创建运行事件表 |
| `docs/sql/20260803_operation_event_retention.sql` | 为已有事件表补充清理索引，不删除数据 |
| `docs/sql/20260804_operation_event_correlation.sql` | 为已有事件表补充 `operation_id` 和关联索引 |
| `docs/sql/20260803_test_data.sql` | 测试环境前端验收数据，可重复执行 |

`20260803_test_data.sql` 只创建业务测试数据，不创建模拟设备，避免后端把测试设备当成真实扫码器或 PLC 发起连接。

## 测试环境 Docker 部署

| 项目 | 配置 |
| --- | --- |
| 测试服务器 | `192.0.2.4` |
| Compose 文件 | `/docker/docker-compose.yml` |
| Compose 服务/容器 | `bufferpad-wms-opc` |
| 镜像 | `bufferpad/wms-opc:latest` |
| Spring Profile | `prod` |
| 后端端口 | `19001`，使用 host 网络 |
| 日志目录 | `/docker/bufferpad/wms-opc/logs` |

服务器数据库参数由 Compose 环境变量注入，密码存放在服务器 `/docker/.env`，不得提交到仓库。

更新时只操作缓冲垫后端服务：

```bash
cd /docker
docker compose -f docker-compose.yml up -d --no-deps bufferpad-wms-opc
```

验收：

```bash
curl http://127.0.0.1:19001/actuator/health
docker ps --filter name=bufferpad-wms-opc
docker logs --tail 200 bufferpad-wms-opc
tail -n 200 /docker/bufferpad/wms-opc/logs/wms-opc-prod.log
```

健康状态必须为 `UP`。前端通过 Nginx `/api/` 代理访问后端，生产部署不需要在浏览器端写死后端 IP。
