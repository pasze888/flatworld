# KNOWLEDGE.md — FlatWorld (flatworld)

已验证的 API 签名与踩坑记录（NeoForge 1.21.1 / 21.1.244，MC 1.21.1，Parchment 2024.11.17）。

## 维度注册（数据包 JSON 方式）

- `LEVEL_STEM` 与 `DIMENSION_TYPE` 是**原版已有的数据包注册表**，无需
  `DataPackRegistryEvent.NewRegistry` 创建新注册表；直接放 JSON 即可。
- 模组 `src/main/resources/data/` **自动**作为内建数据包加载（`ResourcePackLoader`
  把所有 mod 的 resources 合并为 `mod_data` 隐藏包，`MOD_PACK_SELECTION_CONFIG` 必选）。
  因此维度 JSON 放 `src/main/resources/data/<modid>/dimension/` 与 `.../dimension_type/`
  即可，无需代码注册。
- JSON 路径规则：`data/<modid>/dimension/<dimpath>.json`（LevelStem），
  `data/<modid>/dimension_type/<dimpath>.json`（DimensionType）。两者 path 可同名（如 `flat_world`）。

## 维度 JSON 字段（已核对 Codec）

`DimensionType.CODEC`（`DimensionType.DIRECT_CODEC`）字段：
`ultrawarm` `natural` `piglin_safe` `respawn_anchor_works` `bed_works` `has_raids`
`has_skylight` `has_ceiling` `coordinate_scale` `ambient_light` `fixed_time`
`logical_height` `effects` `infiniburn` `min_y` `height`
`monster_spawn_light_level`（IntProvider，如 `{"type":"minecraft:uniform","min_inclusive":0,"max_inclusive":7}`）
`monster_spawn_block_light_limit`。
约束（构造器抛异常）：`height` % 16 == 0，`min_y` % 16 == 0，`min_y + height <= MAX_Y+1`，`logical_height <= height`。

> ⚠️ **超平坦维度必须复用主世界的维度类型参数**：原版超平坦世界（`WorldPresets.FLAT`）用的是
> `BuiltinDimensionTypes.OVERWORLD`（`min_y: -64, height: 384, logical_height: 384`），只把
> chunk generator 换成 `FlatLevelSource`（见 `WorldPresets.Bootstrap#bootstrap` 与 `makeOverworld`）。
> 若自造维度类型用了 `min_y: 0, height: 256`，天空光传播异常，表现为**远处/维度边缘发黑**（原版超平坦是明亮的）。
> 修复：`min_y: -64, height: 384, logical_height: 384`（`fixed_time` 等其它字段可自定义，永昼保留 `fixed_time: 6000`）。

`LevelStem.CODEC`：`{"type": <dimension_type 引用>, "generator": {...}}`。

`FlatLevelSource.CODEC`（`Registries.CHUNK_GENERATOR` 注册名 `minecraft:flat`，见 `ChunkGenerators.bootstrap`）：
```json
{"type":"minecraft:flat","settings":{"biome":"minecraft:plains","lakes":false,"features":false,"layers":[{"height":1,"block":"minecraft:bedrock"}]}}
```
`FlatLevelGeneratorSettings.CODEC` 字段：`structure_overrides`(可选) `layers`(必需) `lakes`(默认false) `features`(默认false) `biome`(可选)。

## 传送进维度（已验证签名）

- `ServerPlayer#teleportTo(ServerLevel newLevel, double x, double y, double z, float yaw, float pitch)`：跨维度时内部走 `changeDimension`，同维度走 `connection.teleport`。
- `MinecraftServer#getLevel(ResourceKey<Level> dimension)` → `@Nullable ServerLevel`。
- 维度 ResourceKey：`ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(modid, "path"))`。
  注意 `Registries.DIMENSION` 是 `ResourceKey<Registry<Level>>`（Level）；`Registries.LEVEL_STEM` 是同名注册表（LevelStem）。
- `ResourceLocation.fromNamespaceAndPath(ns, path)` 是 1.21.1 可用 API。

## 末影珍珠 × 堆肥桶 传送逻辑（已验证签名）

- 事件：`net.neoforged.neoforge.event.entity.ProjectileImpactEvent`（挂 `NeoForge.EVENT_BUS`，
  implements `ICancellableEvent` → `setCanceled(boolean)`）。`getProjectile()` / `getRayTraceResult()`。
- 末影珍珠实体：`net.minecraft.world.entity.projectile.ThrownEnderpearl`（继承 `ThrowableItemProjectile`）。
- 投掷者：`Projectile#getOwner()` → `@Nullable Entity`（`setOwner` 存 UUID）。判 `instanceof ServerPlayer`。
- 撞击方块：`getRayTraceResult() instanceof BlockHitResult` → `BlockHitResult#getBlockPos()`。
  判堆肥桶：`level.getBlockState(pos).is(Blocks.COMPOSTER)`。
- 出生点：`ServerLevel#getSharedSpawnPos()` → `BlockPos`（⚠️ 其 Y 不可靠，勿直接用于跨维度落地）。
- 取消原撞击：`event.setCanceled(true)` + `pearl.discard()`。
- **跨维度安全落点（最终方案）**：`Entity#adjustSpawnLocation(ServerLevel level, BlockPos pos)` —— 原版重生/传送用，
  内部用 `level.getChunkAt(spawnPos)` **强制加载出生点区块**后取
  `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` 高度 +1，返回安全落点 BlockPos。
  传送到其它维度请用 `player.adjustSpawnLocation(target, target.getSharedSpawnPos())`，勿手算。
- 坑 1：用 `getSharedSpawnPos()`+固定偏移落地会悬空，生存模式摔死。
- 坑 2：直接 `LevelReader#getHeight(MOTION_BLOCKING, x, z)` 对**未加载区块**返回错误值（min_y/0），
  玩家会被放进虚空——必须先 `getChunkAt` 等强制加载区块，或直接用 `adjustSpawnLocation`。
- **双向传送判断当前维度**：`player.level().dimension()` → `ResourceKey<Level>`，与
  `Level.OVERWORLD`/`FLAT_WORLD`（`ResourceKey.create(Registries.DIMENSION, ...)`）`==` 比较。
  主世界 ServerLevel 取法：`player.server.overworld()`。
- **玩家重生点**：`ServerPlayer#getRespawnPosition()` → `@Nullable BlockPos`（睡眠设置的床）；
  `getRespawnDimension()` → `ResourceKey<Level>`（无重生点时默认 `Level.OVERWORLD`）；
  `getRespawnAngle()` → `float`。判断"有主世界重生点"用
  `respawn != null && getRespawnDimension() == Level.OVERWORLD`。
- 自定义水平坐标的安全落点（非世界出生点）：`level.getChunkAt(horizontal).getHeight(
  Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1` —— `Level#getChunkAt(BlockPos)` 返回
  `LevelChunk`，其 `getHeight(...)` 已强制加载区块。

## 永昼 / 永晴 / 不刷怪

- **永昼**：`DimensionType` 的 `fixed_time` 字段（`OptionalLong`，JSON `"fixed_time": 6000`=正午）。
  设了后 `DimensionType#timeOfDay` 永远返回固定值，时间不流动。
- **不刷怪（群系方案，纯数据）**：自然刷怪由群系的 `MobSpawnSettings`（`spawners` map）决定。
  把 biome JSON 的 `spawners` 各分类置空数组 `[]`（参考原版 `deep_dark.json`），自然刷怪循环就不生成生物。
  注意 `spawners` 与 `spawn_costs` 是 `MobSpawnSettings.CODEC` 的**必需** `fieldOf`，须显式给 `{}` 空对象。
  只影响**自然刷怪**；刷怪笼（SPAWNER）、结构（STRUCTURE）不受影响。
- 自定义群系注册路径：`data/<ns>/worldgen/biome/<name>.json`（注册表 `Registries.BIOME`）；
  维度 JSON 里 `generator.settings.biome` 引用 `"<ns>:<name>"`。
- **永晴（数据方案，仿沙漠）**：biome JSON 的 `has_precipitation: false` 使
  `Biome#getPrecipitationAt` 永远返回 `Precipitation.NONE`，进而在 `Level#isRainingAt(pos)` 判
  `!= Precipitation.RAIN` → 该群系位置永不落雨、不打雷（同沙漠）。纯数据、无代码开销。
  边界：全局天气循环仍会推进，`isRaining()` 仍可能为 true（天空云层可能短暂变灰），但地面不落雨。
  若要连天空也永晴（彻底清全局天气），才需代码监听（`LevelTickEvent` + `setWeatherParameters`）。
  `ClimateSettings.CODEC` 字段：`has_precipitation`(bool, 必填)、`temperature`(float)、
  `temperature_modifier`(可选)、`downfall`(float)。沙漠参考值为 `has_precipitation:false,
  temperature:2.0, downfall:0.0`。

## 踩坑：构建环境

- **Java 版本**：系统默认 `JAVA_HOME` 指向 zulu17-jdk（Java 17），但 NeoForge 1.21.1 + Gradle 9.2 需 **Java 21**。
  本机可用 `C:\Users\lzp\scoop\apps\dragonwell21-jdk\current`（21.0.10）。构建前需 `$env:JAVA_HOME` 指向 Java 21。
- **Gradle wrapper 锁**：`~/.gradle/wrapper/dists/gradle-9.2.1-bin/.../gradle-9.2.1-bin.zip.lck`
  在沙箱下会因 workspace 外写权限被拒，报 `FileNotFoundException (... 拒绝访问)`；需用更宽沙箱权限跑 `./gradlew build`。
