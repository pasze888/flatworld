# Flat World Dimension (flatworld)

一个 NeoForge 1.21.1 模组：新增一个「超平坦」维度，用 `/flatworld` 在主世界与它之间双向传送。

## 功能

- 新增超平坦维度 `flatworld:flat_world`（基岩 + 石头 + 泥土 + 草方块的平坦大地，地表在 y≈-1）。
- 进出方式：命令 `/flatworld`，按执行者当前所在维度双向传送：
  - 在**其它维度**（如主世界）→ 进超平坦维度（世界出生点）；
  - 已在**超平坦维度** → 回主世界（优先重生点/床，否则世界出生点）。
  - 权限等级由配置 `commandPermissionLevel` 决定，默认 0（所有玩家可用）。
- 该维度特性：
  - **永昼**：维度类型 `fixed_time: 6000`（时间固定正午）；
  - **永晴**：维度群系 `has_precipitation: false`（仿照沙漠），即使天气循环触发下雨该维度也不落雨、不打雷；
  - **不刷怪**：维度群系为自定义 `flatworld:flat_plains`（平原外观但 spawners 全空），自然刷怪循环不生成任何生物（刷怪笼、结构刷怪不受影响）。

## 进入 / 离开维度

- 命令：`/flatworld`（默认所有玩家可用，权限等级可在配置 `commandPermissionLevel` 中调整，0=所有玩家，1-4=OP 等级）。

## 技术要点

- 维度注册键与代码入口集中在 `worldgen/dim/ModDimensions`：
  - `FLAT_WORLD_STEM_KEY`（`Registries.LEVEL_STEM`）、`FLAT_WORLD_LEVEL_KEY`（`Registries.DIMENSION`）、
    `FLAT_WORLD_TYPE_KEY`（`Registries.DIMENSION_TYPE`）；
  - `bootstrap(BootstrapContext<DimensionType>)` 用 `DimensionType` 构造器声明维度类型（永昼、高度、怪物规则等），
    由数据生成写出 JSON。
- **维度类型走数据生成**：`datagen/ModDataGenerator`（`GatherDataEvent`，`@EventBusSubscriber`）
  + `datagen/ModWorldGenProvider`（`DatapackBuiltinEntriesProvider` + `RegistrySetBuilder` 注册 `Registries.DIMENSION_TYPE`）。
  `./gradlew runData` 输出 `src/generated/resources/data/flatworld/dimension_type/flat_world.json`（已提交）。
  改维度类型请改 `ModDimensions#bootstrap` 后重跑 `runData`，不要手改生成产物。
- **维度内容与群系仍是手写 JSON**（放 `src/main/resources`，模组 resources 自动作为内建数据包加载）：
  - `data/flatworld/dimension/flat_world.json` —— LevelStem（`type: flatworld:flat_world` + `minecraft:flat` 生成器，
    biome 指向 `flatworld:flat_plains`）
  - `data/flatworld/worldgen/biome/flat_plains.json` —— 自定义群系（平原外观，spawners 全空 → 不自然刷怪）
  - 维度类型与维度同名（都是 `flatworld:flat_world`），改名会让已存在的存档找不到维度类型而加载失败。
- 传送逻辑监听 `RegisterCommandsEvent` 注册 `/flatworld`（`Commands.literal` + `getPlayerOrException`），
  调用 `ServerPlayer#teleportTo(ServerLevel, double, double, double, float, float)` 跨维度传送。
- 落点高度复用原版 `Entity#adjustSpawnLocation` / `getChunkAt().getHeight(MOTION_BLOCKING_NO_LEAVES)`
  先强制加载区块再取地表，避免落地虚空或悬空摔死。
- 永晴由群系 `has_precipitation: false` 实现（同沙漠），无需代码；不刷怪由群系 `spawners` 置空实现。
- 地表垫高（60 层石头）使玩家眼睛高于「海平面 + 32」，避免非 flat 世界雾变暗导致的「维度边缘发黑」。

## 配置

配置文件位于 `config/flatworld-common.toml`（首次启动后生成）：

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `commandPermissionLevel` | `0` | 执行 `/flatworld` 所需的最小权限等级（0=所有玩家，1~4=OP 等级） |

## 构建

```bash
./gradlew build     # 编译 + 打包，产物在 build/libs/flatworld-1.0.0.jar
./gradlew runData   # 重新生成维度类型 JSON（改 ModDimensions#bootstrap 后执行）
./gradlew runClient # 启动开发客户端
```
