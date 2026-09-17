# 第三方组件说明

**简体中文** · [English](THIRD_PARTY_NOTICES.en.md) · [文档目录](docs/README.md)

根目录 MIT LICENSE 适用于项目自有代码。依赖、第三方资源及运行时软件仍受各自许可证约束；以下是入口说明，不替代相应版本随附的完整许可证。

- 前端依赖以 `bufferpad/package.json` 和 `package-lock.json` 为准，包含 Vue、Vue Router、Element UI、ECharts、Axios 等；第三方字体和图标应保留所属组件声明。
- 后端依赖以 `wms-opc/pom.xml` 为准，包含 Spring Boot、MyBatis Plus、Druid、Netty 及其他库。查看具体依赖的 POM 和随包 LICENSE / NOTICE，并在再分发时遵守要求。
- HslCommunication Java 3.4.0 由用户自行准备。它不在本仓库的二进制文件或公开历史中，项目 MIT 许可不授予该库的下载、使用或再分发权。说明见[本地依赖目录](wms-opc/src/main/resources/lib/README.md)。
- Windows 安装使用的 JDK、MySQL、nginx、WinSW、VC++ Runtime 从各自官方来源准备，见[安装包说明](bufferpad-installer/packages/README.md)。本仓库提供准备与安装脚本，不附带这些安装包。

本地生成的后端 JAR 和离线安装目录可能包含第三方内容。在公开分发成品前，需要核对实际包含的版本、许可证、版权和 NOTICE 文件，不能仅复制本项目的 MIT LICENSE 作为全部许可。
