# Local HslCommunication dependency

[简体中文](README.md) · **English** · [All documentation](../../../../../docs/README.en.md)

The backend uses `HslCommunication 3.4.0` through a local Maven `systemPath`:

```text
wms-opc/src/main/resources/lib/HslCommunication-3.4.0.jar
```

The JAR is absent from this repository, public Git history, and public installation bundles. Confirm how to obtain **Java 3.4.0** and the applicable authorization through the [author's project](https://github.com/dathlin/HslCommunication) or a legitimate provider, then place it here. Terms for another version on the author's current website do not automatically cover this pinned version.

The current backend cannot compile completely without it. Do not substitute an empty JAR, an arbitrarily renamed newer version, or an unknown mirror. Driver/version changes need separate implementation and validation.

Git ignores this file. Local Spring Boot packaging includes system-scope dependencies; verify redistribution rights before distributing the resulting application. This project's MIT LICENSE grants no additional rights to HSL.
