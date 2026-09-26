# 超平坦维度 (flatworld)

[English](README.md) | [简体中文](README.zh-CN.md)

一个 NeoForge 1.21.1 模组：为世界新增一个「超平坦维度」，并用 `/flatworld` 命令在主世界与它之间双向
传送。该维度的时间、天气与刷怪都可配置。

## 功能

- 新增超平坦维度 `flatworld:flat_world`——基岩 + 石头 + 泥土 + 草方块的平坦大地（地表在 y≈-1）。
- **时间可配置**：固定正午（`DAY`）、固定午夜（`NIGHT`），或自行流动（`CYCLE`）。
- **天气可配置**：`CLEAR`（晴）、`RAIN`（雨）、`THUNDER`（雷雨）。
- **不自然刷怪**：该维度使用自己的群系 `flatworld:flat_plains`，其 `spawners` 全空，自然刷怪循环
  不生成任何生物；刷怪笼与结构刷怪不受影响。
- `/flatworld` 双向传送，并自动选取地表附近的安全落点。

## 安装

1. 安装 Minecraft 1.21.1 与 NeoForge 21.1.x。
2. 把 `flatworld-<version>.jar` 放进 `mods/` 目录。

## 快速开始

执行 `/flatworld`，按当前所在维度双向传送：

- 在**其它维度**（如主世界）→ 进入超平坦维度（落在世界出生点）；
- 已在**超平坦维度** → 回到主世界（优先重生点/床，否则世界出生点）。

命令所需权限等级由配置 `commandPermissionLevel` 决定，默认 `0`（所有玩家可用）。

## 配置

配置文件为 `config/flatworld-common.toml`，首次启动后生成：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `commandPermissionLevel` | `0` | 执行 `/flatworld` 所需的最小权限等级（0=所有玩家，1~4=OP 等级）。 |
| `timeMode` | `DAY` | `DAY`=固定正午（6000）、`NIGHT`=固定午夜（18000）、`CYCLE`=自行流动（仍受 `doDaylightCycle` 游戏规则约束）。 |
| `weatherMode` | `CLEAR` | `CLEAR`（晴）/ `RAIN`（雨）/ `THUNDER`（雷雨）。 |

> `timeMode` 为 `DAY` 或 `NIGHT` 时，该维度内 `/time set` 无效；`weatherMode` 为固定档时 `/weather`
> 同样无效——配置是唯一的事实来源。切换模式立即生效，无需重启。

## 常用命令

| 命令 | 说明 |
|---|---|
| `/flatworld` | 在主世界与超平坦维度之间双向传送。 |

## 文档

- [设计：时间与天气模式](docs/design/time-weather-modes.md)
- [参考：维度注册与已验证的 API 事实](docs/reference/dimension-registration.md)
- [环境与构建坑](docs/troubleshooting.md)

## 构建

```bash
./gradlew build     # 编译并打包到 build/libs/flatworld-<version>.jar
./gradlew runData   # 重新生成维度类型 JSON（改过 ModDimensions#bootstrap 后执行）
./gradlew runClient # 启动开发版客户端
```

## 许可

MIT 许可，见 [LICENSE](LICENSE)。
