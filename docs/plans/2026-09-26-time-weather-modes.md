# 实现计划：超平坦维度的时间与天气模式

> 落点依据工作区 `AGENTS.md` §7.2（实现计划 → `docs/plans/YYYY-MM-DD-<feature>.md`）。
> 设计取舍见 [docs/design/time-weather-modes.md](../design/time-weather-modes.md)；
> 验证过的 API 事实见 [docs/reference/dimension-registration.md](../reference/dimension-registration.md)。

## 目标

把 `flat_world` 的「永昼 + 永晴」从**写死的纯数据**改为**可配置的模式**：

- 时间：`DAY`（固定正午）/ `NIGHT`（固定午夜）/ `CYCLE`（自行流动，尊重 `doDaylightCycle`）
- 天气：`CLEAR` / `RAIN` / `THUNDER`

范围限定为「精简版」：**不做** `FOLLOW`（跟随主世界），也**不做**运行时命令。

## 前置结论（决定了改动的必然性）

三项改动必须同时落地，缺任何一项功能都不成立：

1. 去掉 `DimensionType` 的 `fixed_time` —— 否则 `timeOfDay()` 永远丢弃自定义 dayTime；
2. 替换该维度的 `levelData` —— 否则 `setDayTime` / `setRaining` 是空实现，写不进任何状态；
3. 群系 `has_precipitation` 改回 `true` —— 否则 `RAIN`/`THUNDER` 只有 `rainLevel` 变化、地面不落雨。

## 改动清单

| 文件 | 动作 |
|---|---|
| `src/main/resources/META-INF/accesstransformer.cfg` | **新建**。`public-f` 两行：`Level.levelData`、`ServerLevel.serverLevelData` |
| `worldgen/dim/FlatWorldLevelData.java` | **新建**。`extends DerivedLevelData`；独立 dayTime + 天气六字段；`applyTime(TimeMode, boolean)` / `applyWeather(WeatherMode)`；天气 setter 一律落空；`getScheduledEvents()` 返回自建 `TimerQueue` |
| `worldgen/dim/FlatWorldTuning.java` | **新建**。`LevelEvent.Load` 换 levelData（幂等）；`LevelTickEvent.Post` 每 tick 按 `Config` 施加模式 |
| `worldgen/dim/ModDimensions.java` | `OptionalLong.of(6000L)` → `OptionalLong.empty()`，并更新 `bootstrap` 的 javadoc |
| `Config.java` | 新增 `TIME_MODE` / `WEATHER_MODE`（`defineEnum`） |
| `src/main/resources/data/flatworld/worldgen/biome/flat_plains.json` | `has_precipitation`: `false` → `true` |
| `FlatWorld.java` | 构造函数注册 `new FlatWorldTuning()` 到 `NeoForge.EVENT_BUS` |
| `src/generated/resources/data/flatworld/dimension_type/flat_world.json` | 由 `runData` 重新生成（移除 `fixed_time`） |

刻意**不改** `ServerLevel.tickTime`（保持原版 `false`）：`tickTime=true` 会让
`tickTime()` 连带 tick `getScheduledEvents()`，而 `DerivedLevelData` 把它委托给主世界队列，
会造成主世界计划事件被 tick 两次。时间推进改由 `LevelTickEvent.Post` 负责。

## 验证记录

| 步骤 | 结果 |
|---|---|
| `./gradlew build` | ✅ BUILD SUCCESSFUL in 1m 16s。**同时证明 AT 生效**——否则 `level.levelData = data` 会因 `protected final` 编译失败 |
| `./gradlew runData` | ✅ 真正执行（`> Task :runData`，13:14:53 启动 NeoForge）。生成物 `flat_world.json` 已移除 `fixed_time` |
| jar 内容抽查 | ✅ `META-INF/accesstransformer.cfg`、`FlatWorldLevelData.class`（含 `$TimeMode`/`$WeatherMode`）、`FlatWorldTuning.class` 均已打包 |
| `git status` | ✅ 仅预期变更，无杂散文件 |

## 未完成 / 需人工验证

以下三项需要启动开发版客户端手动确认，无法由构建或数据生成自动覆盖：

1. 进 `flat_world` 后 `/time set day`、`/time set night`：默认 `DAY` 模式下应**无效果**（被每 tick 钉回 6000）；
   把 `timeMode` 改成 `CYCLE` 后应生效。
2. 把 `weatherMode` 改为 `RAIN` / `THUNDER`：地面应落雨 / 打雷；改回 `CLEAR` 后 1~2 秒内雨止。
3. 跨维度往返 `OVERWORLD ↔ flat_world` 若干次：时间/天气不串，主世界时间不受影响。

顺带应确认（本次改动的行为变更）：

4. 该维度执行 `/time set` 在固定模式下无效属于**预期行为**（配置是唯一事实来源），
   需在 README 中说明，避免被当成 bug。

## 后续

- README 已更新（2026-09-26）：`README.md` 改为英文源，新增 `README.zh-CN.md` 中文同步；
  配置表补上 `timeMode` / `weatherMode`，永昼与永晴的实现说明改写为配置驱动，
  原先的「技术要点」按 §7.3 归位到 `docs/design/` 与 `docs/reference/`。
