# KNOWLEDGE.md — FlatWorld (flatworld)

已验证的 API 签名与踩坑记录（NeoForge 1.21.1 / 21.1.244，MC 1.21.1，Parchment 2024.11.17）。

## 维度注册（教程式：注册类 + 数据生成）

参考 [Tutorial-Mod-1.21.1-NeoForge](../Tutorial-Mod-1.21.1-NeoForge) 的
`worldgen/dim/ModDimensions` + `datagen/ModWorldGenProvider` 写法。

- 一个维度要三个键，分别对应三个注册表：

  | 键 | 注册表 | 用途 |
  |---|---|---|
  | `FLAT_WORLD_STEM_KEY` | `Registries.LEVEL_STEM` | 维度内容（generator + biome source），即 dimension JSON |
  | `FLAT_WORLD_LEVEL_KEY` | `Registries.DIMENSION` | 世界本身，`player.level().dimension()` 比较、`MinecraftServer#getLevel` 取用 |
  | `FLAT_WORLD_TYPE_KEY` | `Registries.DIMENSION_TYPE` | 维度类型，由 `bootstrap` 构造 |

  `ResourceKey.create(registries, ResourceLocation.fromNamespaceAndPath(modid, path))`；
  三者 path 都是 `flat_world`（**故意同名**：改名会让已存在的存档找不到维度类型而加载失败，
  `WORLDGEN` 数据包里 dimension JSON 的 `type` 也直接引用它）。
- `Registries.LEVEL_STEM` 与 `Registries.DIMENSION` 是**同名的两个不同注册表**
  （前者注册 `LevelStem`，后者注册 `Level`），别混用类型参数。
- 维度类型用构造器声明，无 JSON 手写（已核对 `DimensionType` 1.21.1 构造器参数顺序）：

  ```java
  new DimensionType(
      OptionalLong fixedTime, boolean hasSkyLight, boolean hasCeiling, boolean ultraWarm,
      boolean natural, double coordinateScale, boolean bedWorks, boolean respawnAnchorWorks,
      int minY, int height, int logicalHeight, TagKey<Block> infiniburn,
      ResourceLocation effectsLocation, float ambientLight,
      DimensionType.MonsterSettings monsterSettings)
  ```

  `MonsterSettings` 是 record `(boolean piglinSafe, boolean hasRaids, IntProvider monsterSpawnLightTest, int monsterSpawnBlockLightLimit)`。
- 值约束（构造器抛 `IllegalStateException`）：`height >= 16`、`height % 16 == 0`、
  `minY % 16 == 0`、`min_y + height <= MAX_Y + 1`、`logical_height <= height`。
  本模组 `minY=-64, height=384, logicalHeight=384`。
- 常量来源：`BlockTags.INFINIBURN_OVERWORLD`（`TagKey<Block>`）、
  `BuiltinDimensionTypes.OVERWORLD_EFFECTS`（`ResourceLocation`）、
  `UniformInt.of(0, 7)`（怪物生成光照测试，对应 JSON 的 `monster_spawn_light_level`）。

## 维度类型数据生成（neoforge 1.21.1）

- Provider：`DatapackBuiltinEntriesProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries,
  RegistrySetBuilder datapackEntriesBuilder, Set<String> modIds)` —— 四个参数的构造器存在，直接可用
  （其余重载接收 `RegistrySetBuilder.PatchedRegistries` / `Map<ResourceKey<?>, List<ICondition>>`）。
- `RegistrySetBuilder().add(Registries.DIMENSION_TYPE, ModDimensions::bootstrap)`，
  `bootstrap(BootstrapContext<DimensionType>)` 里 `context.register(key, value)`。
- 事件：`net.neoforged.neoforge.data.event.GatherDataEvent`（`IModBusEvent` ⇒ 走**模组事件总线**）。
  用 `@EventBusSubscriber(modid = MODID)`（`net.neoforged.fml.common.EventBusSubscriber`，来自
  `net.neoforged.fancymodloader:loader`）标注 datagen 类即可，**监听方法必须是 `static`**。
  21.1.1 起该注解的 `bus()` 已被忽略：实现 `IModBusEvent` 的事件自动挂模组总线，其余挂 `NeoForge.EVENT_BUS`。
- 输出目录：`runData` 把注册表数据写到 `<namespace>/dimension_type/<path>.json` 到
  `--output` 指定的目录。本模组 `build.gradle` 的 `data` run 已配置
  `--output src/generated/resources/`、`--existing src/main/resources/`，且
  `sourceSets.main.resources { srcDir('src/generated/resources') }` 已就位（缺这行生成物不会进 jar）。
- ⚠️ **不要同时保留手写与生成的同名 JSON**：`src/main/resources/data/.../dimension_type/flat_world.json`
  与 `src/generated/resources/...` 同名会形成重复条目，改维度类型只改 `bootstrap` 后重跑 `runData`。
  本次重构已删除手写的那份。
- 只把 `DIMENSION_TYPE` 交给数据生成：LevelStem 要引用 flat 生成器与群系、群系要重建整套
  `features`（几十个 placed feature 引用），手写 JSON 更直观，教程本身也只生成 dimension_type。

## 维度 JSON 字段（已核对 Codec）

`DimensionType.CODEC`（`DimensionType.DIRECT_CODEC`）字段：
`ultrawarm` `natural` `piglin_safe` `respawn_anchor_works` `bed_works` `has_raids`
`has_skylight` `has_ceiling` `coordinate_scale` `ambient_light` `fixed_time`
`logical_height` `effects` `infiniburn` `min_y` `height`
`monster_spawn_light_level`（IntProvider，如 `{"type":"minecraft:uniform","min_inclusive":0,"max_inclusive":7}`）
`monster_spawn_block_light_limit`。

> ⚠️ **自定义超平坦维度「边缘发黑」的根因是雾（fog），不是天空光**：
> `FogRenderer#setupFog` 里 `f5 = (camera.y - min_y) * getClearColorScale()`，非 flat 世界
> `getClearColorScale()` 返回 `0.03125`（即 /32），`f5 < 1` 时雾颜色会 ×f5² 变暗 → 远处发黑。
> 因此需要 `camera.y >= min_y + 32` 雾才正常。`isFlat`/`getClearColorScale` 是世界级标志
> （`PrimaryLevelData#isFlatWorld` → 世界创建预设），自定义额外维度拿不到 flat 待遇。
> **修复（纯数据）**：像 `creative_dimension` 那样用 `layers` 垫高地表，使它比 `min_y` 高至少 32 格。
> 参考 `creative_dimension`：`min_y:-64` + `layers` 1 层 bedrock + 63 层 white_concrete（地表 y≈0）。
> 本模组采用 `min_y:-64` + 1 bedrock + 60 stone + 2 dirt + 1 grass（共 64 层，地表 y≈-1）。
> （若要保留 4 层薄超平坦又不发黑，只能靠客户端 Mixin 改 `getClearColorScale`/`getHorizonHeight`。）

`LevelStem.CODEC`：`{"type": <dimension_type 引用>, "generator": {...}}`。

`FlatLevelSource.CODEC`（`Registries.CHUNK_GENERATOR` 注册名 `minecraft:flat`，见 `ChunkGenerators.bootstrap`）：
```json
{"type":"minecraft:flat","settings":{"biome":"minecraft:plains","lakes":false,"features":false,"layers":[{"height":1,"block":"minecraft:bedrock"}]}}
```
`FlatLevelGeneratorSettings.CODEC` 字段：`structure_overrides`(可选) `layers`(必需) `lakes`(默认false) `features`(默认false) `biome`(可选)。

## 内置数据包路径

- 模组 `src/main/resources/data/` **自动**作为内建数据包加载（`ResourcePackLoader`
  把所有 mod 的 resources 合并为 `mod_data` 隐藏包，`MOD_PACK_SELECTION_CONFIG` 必选），
  `src/generated/resources/data/` 同理（同一 sourceSet）。
- JSON 路径规则：`data/<modid>/dimension/<dimpath>.json`（LevelStem）、
  `data/<modid>/dimension_type/<dimpath>.json`（DimensionType）、
  `data/<modid>/worldgen/biome/<name>.json`（Biome）。

## 传送进维度（已验证签名）

- `ServerPlayer#teleportTo(ServerLevel newLevel, double x, double y, double z, float yaw, float pitch)`：跨维度时内部走 `changeDimension`，同维度走 `connection.teleport`。
- `MinecraftServer#getLevel(ResourceKey<Level> dimension)` → `@Nullable ServerLevel`。
- 维度 ResourceKey：`ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(modid, "path"))`。
  注意 `Registries.DIMENSION` 是 `ResourceKey<Registry<Level>>`（Level）；`Registries.LEVEL_STEM` 是同名注册表（LevelStem）。
- `ResourceLocation.fromNamespaceAndPath(ns, path)` 是 1.21.1 可用 API。

## 安全落点（踩坑记录）

- 出生点：`ServerLevel#getSharedSpawnPos()` → `BlockPos`（⚠️ 其 Y 不可靠，勿直接用于跨维度落地）。
- **跨维度安全落点（最终方案）**：`Entity#adjustSpawnLocation(ServerLevel level, BlockPos pos)` —— 原版重生/传送用，
  内部用 `level.getChunkAt(spawnPos)` **强制加载出生点区块**后取
  `Heightmap.Types.MOTION_BLOCKING_NO_LEAVES` 高度 +1，返回安全落点 BlockPos。
  传送到其它维度请用 `player.adjustSpawnLocation(target, target.getSharedSpawnPos())`，勿手算。
- 坑 1：用 `getSharedSpawnPos()`+固定偏移落地会悬空，生存模式摔死。
- 坑 2：直接 `LevelReader#getHeight(MOTION_BLOCKING, x, z)` 对**未加载区块**返回错误值（min_y/0），
  玩家会被放进虚空——必须先 `getChunkAt` 等强制加载区块，或直接用 `adjustSpawnLocation`。
- **双向传送判断当前维度**：`player.level().dimension()` → `ResourceKey<Level>`，与
  `Level.OVERWORLD`/`ModDimensions.FLAT_WORLD_LEVEL_KEY`（`ResourceKey.create(Registries.DIMENSION, ...)`）`==` 比较。
  主世界 ServerLevel 取法：`player.server.overworld()`。
- **玩家重生点**：`ServerPlayer#getRespawnPosition()` → `@Nullable BlockPos`（睡眠设置的床）；
  `getRespawnDimension()` → `ResourceKey<Level>`（无重生点时默认 `Level.OVERWORLD`）；
  `getRespawnAngle()` → `float`。判断"有主世界重生点"用
  `respawn != null && getRespawnDimension() == Level.OVERWORLD`。
- 自定义水平坐标的安全落点（非世界出生点）：`level.getChunkAt(horizontal).getHeight(
  Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) + 1` —— `Level#getChunkAt(BlockPos)` 返回
  `LevelChunk`，其 `getHeight(...)` 已强制加载区块。

## 永昼 / 永晴 / 不刷怪

- **永昼**：`DimensionType` 的 `fixedTime`（构造器首参 `OptionalLong`，`OptionalLong.of(6000)`=正午；
  JSON 对应 `"fixed_time": 6000`）。设了后 `DimensionType#timeOfDay` 永远返回固定值，时间不流动。
- **不刷怪（群系方案，纯数据）**：自然刷怪由群系的 `MobSpawnSettings`（`spawners` map）决定。
  把 biome JSON 的 `spawners` 各分类置空数组 `[]`（参考原版 `deep_dark.json`），自然刷怪循环就不生成生物。
  注意 `spawners` 与 `spawn_costs` 是 `MobSpawnSettings.CODEC` 的**必需** `fieldOf`，须显式给 `{}` 空对象。
  只影响**自然刷怪**；刷怪笼（SPAWNER）、结构（STRUCTURE）不受影响。
- **永晴（数据方案，仿沙漠）**：biome JSON 的 `has_precipitation: false` 使
  `Biome#getPrecipitationAt` 永远返回 `Precipitation.NONE`，进而在 `Level#isRainingAt(pos)` 判
  `!= Precipitation.RAIN` → 该群系位置永不落雨、不打雷（同沙漠）。纯数据、无代码开销。
  边界：全局天气循环仍会推进，`isRaining()` 仍可能为 true（天空云层可能短暂变灰），但地面不落雨。
  若要连天空也永晴（彻底清全局天气），才需代码监听（`LevelTickEvent` + `setWeatherParameters`）。
  `ClimateSettings.CODEC` 字段：`has_precipitation`(bool, 必填)、`temperature`(float)、
  `temperature_modifier`(可选)、`downfall`(float)。沙漠参考值为 `has_precipitation:false,
  temperature:2.0, downfall:0.0`。

## 命令注册（已验证签名）

- 事件：`net.neoforged.neoforge.event.RegisterCommandsEvent`（挂 `NeoForge.EVENT_BUS`）；
  `getDispatcher()` → `CommandDispatcher<CommandSourceStack>`，`getBuildContext()` → `CommandBuildContext`。
- 注册：`dispatcher.register(Commands.literal("flatworld").requires(s -> s.hasPermission(2))
  .executes(ctx -> {...}))`；`CommandSourceStack#getPlayerOrException()` 抛 `CommandSyntaxException`。
- 反馈：`CommandSourceStack#sendSuccess(Supplier<Component>, boolean)` / `sendFailure(Component)`；
  组件 `Component.translatable("command.flatworld.xxx")` 走语言文件。

## 配置（ModConfigSpec，已验证签名）

- 注册：主类构造函数参数里加 `ModContainer modContainer`（FML 自动注入），调用
  `modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC)`。
- 定义：`ModConfigSpec.Builder#comment(...).defineInRange(String path, int default, int min, int max)`
  → `ModConfigSpec.IntValue`；`IntValue#get()` 返回当前值（继承 `ConfigValue<Integer>`）。
  布尔项：`Builder#define(String path, boolean default)` → `ModConfigSpec.BooleanValue`，`get()` 返回 boolean。
- 配置项在 `registerCommands` 的 `requires` 谓词里读 `Config.X.get()`（运行时求值，改 config 生效）。
- 删除配置项后，旧的 `config/flatworld-common.toml` 里会残留已删除的键——NeoForge 会忽略并
  在下次写盘时清理；若启动报未知键，删掉 `run/config/flatworld-common*.toml*` 重新生成即可。

## 踩坑：构建环境

- **Java 版本**：系统默认 `JAVA_HOME` 指向 zulu17-jdk（Java 17），但 NeoForge 1.21.1 + Gradle 9.2 需 **Java 21**。
  本机可用 `C:\Users\lzp\scoop\apps\dragonwell21-jdk\current`（21.0.10）。构建前需 `$env:JAVA_HOME` 指向 Java 21。
- **Gradle wrapper 锁**：`~/.gradle/wrapper/dists/gradle-9.2.1-bin/.../gradle-9.2.1-bin.zip.lck`
  在沙箱下会因 workspace 外写权限被拒，报 `FileNotFoundException (... 拒绝访问)`；需用更宽沙箱权限跑
  `./gradlew runData` / `./gradlew build`（`runData` 会启动完整开发版游戏，首次较慢）。
