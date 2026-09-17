# HslCommunication 本地依赖

后端目前使用 `HslCommunication 3.4.0`，`pom.xml` 通过本地 `systemPath` 引用：

```text
wms-opc/src/main/resources/lib/HslCommunication-3.4.0.jar
```

这个 JAR 不包含在仓库、Git 历史或公开安装包中。请向[作者官方项目](https://github.com/dathlin/HslCommunication)或合法提供方核实 **Java 3.4.0** 的获取方式及适用授权，再将文件放在这里。当前作者网站的其他版本或授权说明不能直接当成这个固定版本的许可。

未准备该依赖时，现有后端不能完整编译。不要使用空 JAR、任意重命名的新版本或不明镜像代替；需要切换驱动或版本时，应单独实现和验证。

该文件被 Git 忽略。Spring Boot 本地打包会包含 system-scope 依赖；在分发打包结果前仍需确认第三方再分发权。项目 MIT LICENSE 不授予 HSL 的额外权利。
