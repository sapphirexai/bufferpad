wms-opc（缓冲垫计数与 PLC 联动服务）
==================================

> 面向项目交接与运维：本 README 只讲**核心业务逻辑**和**关键调用链路**，细节文档可参考 `docs/项目交接报告-wms-opc.html`。

## 一、系统在做什么？

- 对产线上的缓冲垫二维码进行**扫码建档与计数**（记录已用次数）。
- 满足「**2 小时有效间隔**」规则：同一个缓冲垫，两次扫码间隔不足 2 小时视为**无效扫码**，不加次数。
- 达到或超过配置的**最大使用次数**后，向 PLC 发出「超限」信号，并通过 SSE 通知前端/大屏。
- 支持两种来源：
  - 工业扫码器（TCP + Netty）
  - 后台手动补码：`POST /cushion/manualCushionInfo/{workLine}/{qrCode}`
- 扫码成功后，可从 PLC 对应寄存器读取**开口数（openCount）**，落库到 `cushion_info` 与最新一条 `cushion_detail`，供后续追溯与导出。

## 二、关键表与字段（业务视角）

| 表名             | 作用说明                                                     |
|------------------|--------------------------------------------------------------|
| `device_info`    | 记录扫码器 / 三菱 PLC / 汇川 PLC：IP、端口、产线、安装顺序等 |
| `plc_addr`       | 维护 PLC 地址配置：某种信号类型（type） + 工位 → 寄存器地址  |
| `cushion_info`   | **缓冲垫主表**：二维码、最大次数、已用次数、开口数等总账    |
| `cushion_detail` | **缓冲垫明细表**：每次有效扫码一行，用于追溯和导出           |
| `scan_log`       | 扫码过程与 PLC 通知文字日志，排障用                         |
| `opc_config`     | 配置默认最大使用次数等（代码中用固定 ID 读取）              |

其中与本说明紧密相关的字段：

| 字段                           | 含义                                                         |
|--------------------------------|--------------------------------------------------------------|
| `cushion_info.used_count`      | 已经扫码计数多少次（只在**有效扫码**时 +1）                 |
| `cushion_info.max_use_count`   | 最大允许使用次数（来自 `opc_config` 或默认常量）            |
| `cushion_info.open_count`      | 从 PLC 寄存器读回来的「开口数」测量值（业务解释由现场定义） |
| `cushion_detail.open_count`    | 同上，但只更新**该二维码最新一条明细**                      |
| `cushion_info.last_scan_date`  | 最近一次有效扫码时间，用于 2 小时有效间隔判断               |

## 三、扫码主业务：onQrCodeReceived 上游调用链

`CushionInfoServiceImpl.onQrCodeReceived(...)` 是**所有「扫码/补码」的总入口**，上游只有两条路径：

### 3.1 路径一：手动补码 HTTP 接口

```text
前端 / 运维
  → POST /cushion/manualCushionInfo/{workLine}/{qrCode}
      （CushionController.manualCushionInfo）
      → ICushionInfoService.onQrCodeReceived(
            workLine,
            scannerHost = null,
            scannerName = null,
            scannerPosition = "手动",
            scannerSeq = null,
            qrCode
        )
```

- 使用场景：现场需要在后台手动录入一个缓冲垫二维码（常见于补录或扫码器异常时）。
- 由于没有实际扫码器，`scannerSeq` 为空，后续涉及 PLC 的通知会通过**遍历所有扫码器工位**的方式执行（见下文 PLC 通知逻辑）。

### 3.2 路径二：扫码器通过 Netty 上报

```text
工业扫码器（TCP）
  → Netty: MsgHandler.channelRead
       - 解析心跳 / NoRead / 实际条码（STX/ETX 或 [TPL_STX]/[TPL_ETX] 包裹）
       - 构造 EventBus 消息：
         EventBusMsgCushionQrCode(
             qrCode, workLine, scannerHost, scannerName, scannerPosition, scannerSeq
         )
  → EventBus.post(...)

  → CushionInfoServiceImpl.onMessageEvent(EventBusMsgCushionQrCode)
       if qrCode 为空:
           → onScanCodeFailed(...)          // 记录失败日志，通知 PLC「失败」，推 SSE
       else:
           → onQrCodeReceived(
                  event.workLine,
                  event.scannerHost,
                  event.scannerName,
                  event.scannerPosition,
                  event.scannerSeq,          // 有实际安装顺序
                  qrCode
              )
```

- 使用场景：正常产线扫码。
- `scannerSeq` 必填：决定后续 PLC 信号和开口数寄存器读写对应哪个工位。

## 四、onQrCodeReceived 内部业务分支（简表）

入口统一是：

```java
ResultDTO<CushionInfoDTO> onQrCodeReceived(
    Integer workLine,
    String scannerHost,
    String scannerName,
    String scannerPosition,
    Integer scannerSeq,
    String qrCode)
```

核心逻辑（摘自 `CushionInfoServiceImpl`）可以抽象为下表：

| 步骤 | 判断条件                                   | 主表 `cushion_info` 行为                                      | 明细表 `cushion_detail` 行为        | 备注                             |
|------|--------------------------------------------|---------------------------------------------------------------|--------------------------------------|----------------------------------|
| 1    | 任何情况                                   | 写一条 `scan_log` 日志（区分「手动」/「扫码器」/新旧）        | 不影响                               | 用于审计、界面查询               |
| 2    | 主表不存在该 `qrCode`（新码）             | 调用 `add(...)` → `insertSelective` 新增一行                  | 调用 `addDetail(...)` → `insert` 一行 | 计数从 1 起算                    |
| 3    | 主表存在，且距离 `lastScanDate` < 2 小时  | 不改 `usedCount` 与 `openCount`                              | 不新增                                | 视为「无效扫码」，只记录日志与 PLC 提示 |
| 4    | 主表存在，且距离上次 ≥ 2 小时（有效复扫） | `usedCount++` 后通过 `modifyUsedCountByQrCode(...)` 做 `update` | `addDetail(...)` 再 `insert` 一行     | 有效次数 +1                      |
| 5    | 每次成功扫码后（新码 + 有效复扫）         | 若 `usedCount >= maxUseCount`，保持 `usedCount` 并更新状态    | 不再增明细，但会记录超限类日志        | 通过 `onScanMax` 通知 PLC「超最大次数」 |

> 注意：无论扫码来自手动接口还是扫描枪，**只要能走到成功分支**，上面的计数和明细逻辑完全一致，区别只在于参数中的 `scannerSeq` 是否为空以及后续的 PLC 通知策略。

## 五、PLC 通知与开口数回读调用链（EventBusMsgReadOpenCountFromPLC）

当一次扫码被判为**成功**时，系统会根据不同情况向 PLC 发送两个层面的信号：

1. **扫码结果信号**：例如「成功」「失败」「超最大次数」等，由 `EventBusMsgPlcCmd` 驱动。
2. **开口数回读信号**：在「成功」场景下，根据不同工位和是否补码，决定是否从 PLC 再读一个 16 位整数作为「开口数」。

### 5.1 从扫码成功到 PLC 写入 & 开口数回读（上游）

| 步骤 | 场景             | 调用链                                                   | 说明                                                         |
|------|------------------|----------------------------------------------------------|--------------------------------------------------------------|
| A1   | 新码成功 / 复扫成功 | `onScanNew` 或 `onScanSuccess` 内调用 `notifyPLC(...)`   | `plcAddrType` 由业务常量决定（成功/失败/超限等）            |
| A2   | 通知 PLC         | `notifyPLC(...)` 内：`EventBus.post(EventBusMsgPlcCmd)` | 由 `Connection` 订阅并通过 HSL 写 PLC 寄存器                |
| A3   | 决定是否读开口数 | `notifyPLC(...)` 内：当 `plcAddrType` 属于“成功类”时，调用 `readOpenCountFromPLC(isReScan, qrCode, scannerSeq, workLine)` | 只在「成功」时触发，且必须有 `scannerSeq`                   |
| A4   | 查找寄存器地址   | `readOpenCountFromPLC` 内：`getPlcAddrType(...)` → `plcAddrService.findByTypeAndScannerSeq(...)` | 查表 `plc_addr` 找「开口数」寄存器地址                      |
| A5   | 发送回读事件     | `readOpenCountFromPLC` 最终：`EventBus.post(new EventBusMsgReadOpenCountFromPLC(qrCode, addr, workLine))` | 上游链路到此结束，交给 PLC 连接去处理                       |

### 5.2 从 EventBus 读 PLC 到更新开口数（下游）

`EventBusMsgReadOpenCountFromPLC` 下游逻辑在 `cn.tpl.opc.netty.Connection` 中：

| 步骤 | 类与方法                                       | 行为                                                         |
|------|------------------------------------------------|--------------------------------------------------------------|
| B1   | `Connection.onMessageEvent(EventBusMsgReadOpenCountFromPLC)` | 校验连接是否存活、网络 ping 是否可达、产线号是否匹配        |
| B2   |                                                | 使用 HSL 通信 `ReadInt16(address)` 从 PLC 读出 `Short content` |
| B3   |                                                | `ICushionInfoService cs = ApplicationContextAwareImpl.getCushionService()` |
| B4   |                                                | 调用 `cs.modifyOpenCountByQrCode(qrCode, content)`           |

`CushionInfoServiceImpl.modifyOpenCountByQrCode(qrCode, openCount)` 再做两件事：

1. 更新主表：`cushionInfoEntityMapper.modifyOpenCountByQrCode(cushionInfo)`  
   → `update cushion_info set open_count = ? where qr_code = ?`
2. 更新最新一条明细：`cushionDetailEntityMapper.modifyOpenCountByQrCode(cushionDetail)`  
   → 只更新该二维码**最新一条** `cushion_detail`（`order by created_date desc limit 1`）

这样可以保证：

- 主表 `cushion_info.open_count` 保存的是**当前缓冲垫的最新开口数**；
- 明细表中，**最新一条记录的 openCount 与主表同步**，方便按时间线追溯。

## 六、简单记忆版（交接给同事用）

- 看业务：只记住「**2 小时有效间隔 + 最大次数 + 开口数从 PLC 来**」三件事。
- 看接口：不管是**扫描枪**还是**手动补码**，最后都会走 `onQrCodeReceived`，再决定是不是有效扫码、要不要 +1 和插明细。
- 看 PLC：扫码成功后会先告诉 PLC「这次结果怎样」，再看需不需要从 PLC 读一个开口数回写数据库。

更多字段级、接口级细节，可参考 `docs/项目交接报告-wms-opc.html` 中的详细 HTML 报告。
