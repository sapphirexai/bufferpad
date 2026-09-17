# 文档目录

**简体中文** · [English](README.en.md) · [文档目录](README.md)

从首页进入后，可使用每页顶部的「简体中文 / English」切换到对应语言。英文文档解释现有中文界面，未改变软件界面语言。历史验收是原交付记录，不代表当前提交重新执行了这些测试。

## 项目与贡献

- [参与贡献](../CONTRIBUTING.md)
- [BufferPad](../README.md)
- [Roadmap / 维护路线图](../ROADMAP.md)
- [安全说明与报告](../SECURITY.md)
- [第三方组件说明](../THIRD_PARTY_NOTICES.md)
- [更新日志](../CHANGELOG.zh-CN.md)

## 入门、构建与维护

- [构建与本地打包](build-and-package.md)
- [自动化检查范围](ci.md)
- [快速开始](getting-started.md)
- [维护与发布流程](maintaining.md)
- [仓库迁移记录](migration-history.md)
- [开源迁移验证（2026-09-17）](migration-validation.md)
- [BufferPad 图文导览](product-tour.md)

## 组件与部署

- [缓冲垫项目 PLC 推荐配置](../bufferpad-installer/PLC推荐配置.md)
- [缓冲垫项目 Windows 离线安装包](../bufferpad-installer/README.md)
- [本地后端制品](../bufferpad-installer/app/backend/README.md)
- [数据库初始化说明](../bufferpad-installer/app/db/README.md)
- [本地前端制品](../bufferpad-installer/app/frontend/README.md)
- [本地运行时安装包](../bufferpad-installer/packages/README.md)
- [用户登录与权限部署说明](../bufferpad-installer/用户登录说明.md)
- [缓冲垫项目现场配置简明说明](../bufferpad-installer/缓冲垫现场配置简明说明.md)
- [缓冲垫项目设备配置说明](../bufferpad-installer/设备配置说明.md)
- [bufferpad 前端](../bufferpad/README.md)
- [wms-opc 后端](../wms-opc/README.md)
- [HslCommunication 本地依赖](../wms-opc/src/main/resources/lib/README.md)

## 后端运行与设备验收

- [八类PLC操作链路测试与验收](../wms-opc/docs/八类PLC操作链路测试验收.md)
- [扫码操作日志维护说明](../wms-opc/docs/扫码操作日志维护说明.md)
- [日志分页查询](../wms-opc/docs/日志分页查询.md)
- [日志保留和缓冲垫时间段导出](../wms-opc/docs/日志清理与时间段导出.md)
- [用户登录与权限](../wms-opc/docs/用户登录与权限.md)
- [设备状态与超级管理员更正（2026-09-15）](../wms-opc/docs/设备状态与超级管理员更正验收.md)
- [设备连接与通信验证分离：实现及验收方案](../wms-opc/docs/设备连接与通信验证分离方案.md)
- [设备连接与通信验证分离验收](../wms-opc/docs/设备连接与通信验证分离验收.md)

## 数据库升级

- [旧库升级说明（当前交付：20260916-r5）](../wms-opc/docs/sql/20260914_旧库升级说明.md)
- [旧库升级SQL验收（20260916-r5）](../wms-opc/docs/sql/20260914_旧库升级验收.md)
- [旧库升级说明（当前交付：20260916-r5）](../wms-opc/docs/sql/20260915_旧库升级说明.md)
- [旧库升级SQL验收（20260916-r5）](../wms-opc/docs/sql/20260915_旧库升级验收.md)

## 公开版本

- [BufferPad v0.1.0 — Source preview / 源码预览版](releases/v0.1.0.md)
- [BufferPad v0.1.1 — Source preview validation fix](releases/v0.1.1.md)

## 文档维护

中英文配对记录于 `docs/languages.json`；新增或改名时更新该文件、两份目录及两侧切换链接。提交前运行 `python scripts/check-repository.py`。PR 模板为单份双语文件。命令、配置键、协议标记和历史证据文件名保留原值，工作区私有报告不作为开源文档发布。
