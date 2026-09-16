# 环境 / 构建坑

> 按工作区 `AGENTS.md` §7 落点表：环境、构建、运行、工具链坑归本文；
> 已验证的 API 与领域事实见 [docs/reference/dimension-registration.md](reference/dimension-registration.md)。

## 构建环境

- **Java 版本**：系统默认 `JAVA_HOME` 指向 zulu17-jdk（Java 17），但 NeoForge 1.21.1 + Gradle 9.2 需 **Java 21**。
  本机可用 `C:\Users\lzp\scoop\apps\dragonwell21-jdk\current`（21.0.10）。构建前需 `$env:JAVA_HOME` 指向 Java 21。
- **Gradle wrapper 锁**：`~/.gradle/wrapper/dists/gradle-9.2.1-bin/.../gradle-9.2.1-bin.zip.lck`
  在沙箱下会因 workspace 外写权限被拒，报 `FileNotFoundException (... 拒绝访问)`；需用更宽沙箱权限跑
  `./gradlew runData` / `./gradlew build`（`runData` 会启动完整开发版游戏，首次较慢）。
