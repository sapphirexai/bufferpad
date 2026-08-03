# 数据库备份说明

一键安装使用当前目录中的 `wms_opc.sql` 初始化 MySQL 数据库：

```text
app\db\wms_opc.sql
```

安装脚本会在 `BufferPadMySQL` 启动后创建 `wms_opc` 数据库并导入该文件。提交或发布安装包前，应确认备份来自当前版本数据库，并至少包含：

- `opc_config`、`device_install_position`、`device_info`、`plc_addr`
- `cushion_info`、`cushion_detail`、`scan_log`
- `operation_event`
- `operation_event.created_date` 索引 `idx_operation_event_created_date`

`operation_event` 保存运行监控中的扫码、计数和 PLC 处理事件。后端默认每天清理30天前的事件；该规则不会删除缓冲垫主数据、使用明细或扫码日志。
