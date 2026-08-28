# Flat World Dimension (flatworld)

一个 NeoForge 1.21.1 模组：新增一个「超平坦」维度。向堆肥桶里扔一枚末影珍珠，即可在主世界与超平坦维度之间双向传送。

## 功能

- 新增超平坦维度 `flatworld:flat_world`（基岩 + 石头 + 泥土 + 草方块的平坦大地，地表在 y≈0）。
- 进入/离开方式（两种）：
  - **命令**：`/flatworld`（需要 OP 权限，level 2），按当前维度双向传送；
  - **末影珍珠**：向**堆肥桶**投掷末影珍珠，珍珠击中堆肥桶后投掷者被传送。
  - 传送方向：在**其它维度**（如主世界）→ 进超平坦维度（世界出生点）；已在**超平坦维度** → 回主世界（优先重生点/床，否则世界出生点）。
- 该维度特性：
  - **永昼**：维度类型 `fixed_time: 6000`（时间固定正午）；
  - **永晴**：维度群系 `has_precipitation: false`（仿照沙漠），即使天气循环触发下雨该维度也不落雨、不打雷；
  - **不刷怪**：维度群系为自定义 `flatworld:flat_plains`（平原外观但 spawners 全空），自然刷怪循环不生成任何生物（刷怪笼、结构刷怪不受影响）。

## 进入 / 离开维度

- 命令：`/flatworld`（OP 权限）。
- 或：将末影珍珠扔向堆肥桶（击中堆肥桶方块），自动按当前维度双向传送。

## 技术要点

- 维度通过数据包 JSON 注册（模组 `resources/data/` 自动作为内建数据包加载）：
  - `data/flatworld/dimension_type/flat_world.json` —— 维度类型（`fixed_time: 6000` 实现永昼）
  - `data/flatworld/dimension/flat_world.json` —— LevelStem（`type` + `minecraft:flat` 生成器，biome 指向 `flatworld:flat_plains`）
  - `data/flatworld/worldgen/biome/flat_plains.json` —— 自定义群系（平原外观，spawners 全空 → 不自然刷怪）
- 传送逻辑监听 `ProjectileImpactEvent`，判断 `ThrownEnderpearl` × `Blocks.COMPOSTER`，调用
  `ServerPlayer#teleportTo(ServerLevel, double, double, double, float, float)` 跨维度传送。
- 落点高度复用原版 `Entity#adjustSpawnLocation` / `getChunkAt().getHeight(MOTION_BLOCKING_NO_LEAVES)`
  先强制加载区块再取地表，避免落地虚空或悬空摔死。
- 永晴由群系 `has_precipitation: false` 实现（同沙漠），无需代码；不刷怪由群系 `spawners` 置空实现。
- 地表垫高（60 层石头）使玩家眼睛高于「海平面 + 32」，避免非 flat 世界雾变暗导致的「维度边缘发黑」。
- 命令监听 `RegisterCommandsEvent` 注册 `/flatworld`（`Commands.literal` + `getPlayerOrException`）。

## 构建

```bash
./gradlew build     # 编译 + 打包，产物在 build/libs/flatworld-1.0.0.jar
./gradlew runClient # 启动开发客户端
```
