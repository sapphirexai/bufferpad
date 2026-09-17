# Third-party components

[简体中文](THIRD_PARTY_NOTICES.md) · **English** · [All documentation](docs/README.en.md)

The root MIT LICENSE covers project-owned code. Dependencies, third-party assets, and runtimes retain their own terms. This overview does not replace the full licenses supplied with each specific version.

- Frontend dependencies are defined in `bufferpad/package.json` and `package-lock.json`, including Vue, Vue Router, Element UI, ECharts, and Axios. Preserve notices for third-party fonts and icons.
- Backend dependencies are defined in `wms-opc/pom.xml`, including Spring Boot, MyBatis Plus, Druid, Netty, and other libraries. Check each dependency's POM and bundled LICENSE / NOTICE when redistributing.
- Obtain HslCommunication Java 3.4.0 separately. Its binary is absent from this repository and public history. This project's MIT license grants no HSL download, use, or redistribution rights. See the [local dependency instructions](wms-opc/src/main/resources/lib/README.en.md).
- Obtain Windows JDK, MySQL, nginx, WinSW, and VC++ Runtime packages from their official sources; see [runtime packages](bufferpad-installer/packages/README.en.md). This repository supplies preparation and installation scripts, not those packages.

A locally built backend JAR or offline installation directory may contain third-party content. Before distributing it, check the actual versions, licenses, copyright notices, and NOTICE files. Copying this project's MIT LICENSE alone does not cover every included component.
