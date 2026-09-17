# 数据库初始化说明

**简体中文** · [English](README.en.md) · [文档目录](../../../docs/README.md)

一键安装使用当前目录中的 `wms_opc.sql` 初始化全新的 MySQL 数据库：

```text
app\db\wms_opc.sql
```

安装脚本会在 `BufferPadMySQL` 启动后创建 `wms_opc` 数据库并导入该文件。2026-09-15已同步为当前版本的完整初始化结构，可直接在全新空库执行，不需要先靠增量迁移补齐表结构。包含12张表：

- `opc_config`、`device_install_position`、`device_info`、`plc_addr`
- `cushion_info`、`cushion_detail`、`scan_log`
- `operation_event`
- `sys_user`、`sys_user_session`、`sys_auth_audit`、`sys_builtin_session`

`scan_log.msg`为TEXT，已包含operation_id、操作类型、状态、设备快照、detail_json等汇总字段，以及操作唯一索引、清理和分页查询相关索引。`operation_event`包含扫码器/PLC上下文和操作关联索引。

初始化只写入默认寿命500次及“上、下、间层1、间层2”四个基础安装位置。设备、PLC地址、缓冲垫、历史日志、用户和会话均为空，不复制测试环境数据。后端首次启动时按安装配置创建普通管理员，用户名默认 `admin`，密码由安装者通过 `BUFFERPAD_ADMIN_PASSWORD` 设置，首次登录不强制改密；`superadmin`为默认关闭、可由部署者配置 BCrypt 哈希启用的维护账号，不在初始化SQL中插入账户或密码。

该文件用于空库初始化，不包含DROP TABLE或覆盖已有表的语句；存量数据库继续使用升级流程。SQL支持MySQL 8.0.20及以上8.0版本。

设备类型编码固定为：

| 编码 | 设备类型 |
| ---: | --- |
| `0` | 扫码器 |
| `1` | 三菱 PLC |
| `2` | 汇川 PLC |
| `3` | 西门子 SIMATIC S7 PLC（适用于 S7-1200/S7-1500） |

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

西门子支持迁移 `20260821_siemens_s7_support.sql` 将 PLC 地址字段扩展到 `VARCHAR(64)`。
随后 `20260825_unify_siemens_s7_device_type.sql` 会将历史设备类型 `4` 归一为 `3`；
`20260825_operation_event_context.sql` 会为运行事件增加并回填可获取的扫码器、PLC 名称/IP。两个迁移可重复执行，且不会删除业务记录。

`operation_event` 保存运行监控中的扫码、计数和 PLC 处理事件。后端默认每天清理30天前的事件；该规则不会删除缓冲垫主数据、使用明细或扫码日志。
