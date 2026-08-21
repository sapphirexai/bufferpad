# 数据库备份说明

一键安装使用当前目录中的 `wms_opc.sql` 初始化全新的 MySQL 数据库：

```text
app\db\wms_opc.sql
```

安装脚本会在 `BufferPadMySQL` 启动后创建 `wms_opc` 数据库并导入该文件。提交或发布安装包前，应确认备份来自当前版本数据库，并至少包含：

- `opc_config`、`device_install_position`、`device_info`、`plc_addr`
- `cushion_info`、`cushion_detail`、`scan_log`
- `operation_event`
- `operation_event.created_date` 索引 `idx_operation_event_created_date`

设备类型编码固定为：

| 编码 | 设备类型 |
| ---: | --- |
| `0` | 扫码器 |
| `1` | 三菱 PLC |
| `2` | 汇川 PLC |
| `3` | 西门子 SIMATIC S7-1200 PLC |
| `4` | 西门子 SIMATIC S7-1500 PLC |

西门子 S7 使用 S7comm over ISO-on-TCP，默认端口为 `102`，当前按标准机架参数
`rack=0`、`slot=0` 连接。`plc_addr.addr` 必须至少支持 64 个字符，以保存
`DB1.DBW0`、`MW0`、`QW0`、`IW0` 等地址。其中 `IWoffset` 是只读输入区，只能用于
开口数读取，不能用于扫码结果或心跳写入。

## 增量迁移

增量迁移脚本位于：

```text
app\db\migrations\
```

无论是全新安装还是存量升级，安装程序都会按文件名顺序自动执行目录内的 `.sql`。检测到
已有 MySQL 数据时，安装程序会跳过 `wms_opc.sql` 整库初始化，只执行增量迁移；迁移不会
覆盖或删除已有设备、PLC 地址、缓冲垫、使用明细和日志数据。

西门子支持迁移 `20260821_siemens_s7_support.sql` 只更新字段定义：登记设备类型 `3/4`，并将
PLC 地址字段扩展到 `VARCHAR(64)`。它不插入、更新或删除任何业务记录，可重复执行。

`operation_event` 保存运行监控中的扫码、计数和 PLC 处理事件。后端默认每天清理30天前的事件；该规则不会删除缓冲垫主数据、使用明细或扫码日志。
