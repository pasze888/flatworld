# 设计：超平坦维度的时间与天气模式

> 落点依据工作区 `AGENTS.md` §7.2（设计说明 → `docs/design/`）。
> 本文回答「怎么建、为什么这么建」；已验证的 API 事实见
> [docs/reference/dimension-registration.md](../reference/dimension-registration.md) 的
> 「维度的时间与天气」一节；实现与验证记录见
> [docs/plans/2026-09-26-time-weather-modes.md](../plans/2026-09-26-time-weather-modes.md)。

## 背景

`flat_world` 最初用两个**纯数据**手段固定环境：

- 永昼：`DimensionType` 的 `fixed_time: 6000`；
- 永晴：群系 `has_precipitation: false`（仿沙漠）；
- 不刷怪：群系 `spawners` 全空。

三者都是零代码、零 hack 的，但只能表达**一个固定结果**——想要「白天/黑夜可选」「晴天/雨天可选」
就必须让维度拥有自己的时间与天气状态。

## 约束（决定了方案形态）

这三条约束都有源码依据，且互相咬合：

1. **非主世界维度的 `levelData` 是 `DerivedLevelData`**，`setDayTime` / `setRaining` 等 setter
   全是空实现 ⇒ 光调用 `level.setDayTime(...)`、`setWeatherParameters(...)` 毫无效果。
2. **`DimensionType#timeOfDay(long)` 用 `fixedTime.orElse(dayTime)`** ⇒ 只要 `fixed_time` 还在，
   写进 dayTime 的任何值都会被丢弃。
3. **群系 `has_precipitation: false` 会让 `RAIN`/`THUNDER` 无视觉**（`rainLevel` 仍会变化，
   但地面不落雨）。

结论：要支持模式切换，必须**替换该维度的 levelData**，且**同时**去掉 `fixed_time`、
把 `has_precipitation` 改回 `true`。三者是一个整体，缺一不可。

## 关键决策

### 借用哪部分、不借用哪部分（Re-Avaritia Infinity Ring）

「用一个自实现的 `ServerLevelData` 覆写时间与天气访问器，让维度脱离主世界时钟」这一思路，
来自 Re-Avaritia 的 `PersonalLevelData`
（[Nova-Committee/Re-Avaritia](https://github.com/Nova-Committee/Re-Avaritia)，
MIT License, Copyright (c) 2022 cnlimiter；版权声明见
[README 的「许可与署名」](../../README.zh-CN.md)）。本模组的 `FlatWorldLevelData` 是独立实现：
门控标志由 4 个减为 1 个、去掉 FOLLOW 系列模式、不覆写 gameTime、计划事件改用维度自己的
`TimerQueue`。

它**其余部分（`DynamicDimensions`）不适用**：那套是「运行时动态注册 + 每玩家一个维度」，
依赖 8+ 处 AT（含 `MappedRegistry.byId/toId` 手工维护注册表内部数组）与两个 deprecated 内部 API
（`forgeGetWorldMap` / `markWorldsDirty`），而本模组只需要给**一个静态维度**换 levelData。

因此本设计**只借鉴 `PersonalLevelData` 这一个类的思路**，AT 收敛到 2 行。

### 为什么自己扛起「时间推进」而不用原版 `tickTime`

原版让一个维度自行推进时间的开关是 `ServerLevel.tickTime=true`，但 `tickTime()` 里除了推进
dayTime 还会 `serverLevelData.getScheduledEvents().tick(...)`；`DerivedLevelData` 把
`getScheduledEvents()` 委托给**主世界**的队列，于是主世界的计划事件会被两个维度各 tick 一次。

所以本模组：

- `tickTime` 保持原版的 `false`；
- 时间推进改由 `LevelTickEvent.Post` 按配置写入；
- `FlatWorldLevelData#getScheduledEvents()` 返回**自建**的 `TimerQueue`，杜绝委托。

这也让 AT 从 3 行减到 2 行（不必再碰 `ServerLevel.tickTime`）。

### 为什么用 `NeoForge.EVENT_BUS.register(...)` 而不是 `@EventBusSubscriber`

`LevelEvent` / `LevelTickEvent` 都不是 `IModBusEvent`，走的是游戏总线。显式注册与
`FlatWorld` 主类现有风格一致，且不依赖注解在版本间的总线路由行为变化——零歧义优先。

### 为什么天气 setter 一律忽略写入

`ServerLevel#advanceWeatherCycle()`（每 tick，`hasSkyLight` 为真时）会读 `levelData` 的天气、
按原版随机规则算出新状态，再写回 `serverLevelData`。若自定义 levelData 接受这些写入，
算出的随机天气就会盖掉配置值，还会让 `rainLevel` 的渐变来回抖动。

让写入全部落空后，天气只能由 `FlatWorldLevelData#applyWeather(WeatherMode)` 单方面设定——
配置是唯一事实来源。

### 为什么保留 `applyTime` 的 gamerule 判断

`CYCLE` 模式尊重 `doDaylightCycle`（与原版语义一致，玩家用该规则表达「不要时间流动」时不该被绕过）；
`DAY`/`NIGHT` 固定模式不受该规则影响——钉住一个时刻本来就是与时间流动正交的诉求。

## 组件

| 组件 | 职责 |
|---|---|
| `FlatWorldLevelData` | 领域状态：持有独立的 dayTime / 天气六个字段，实现 `applyTime` / `applyWeather`，提供自建 `TimerQueue` |
| `FlatWorldTuning` | 事件接线：`LevelEvent.Load` 换 levelData（幂等）；`LevelTickEvent.Post` 每 tick 按 `Config` 施加模式 |
| `Config.TIME_MODE` / `Config.WEATHER_MODE` | 唯一事实来源。每 tick 读取，因此配置重载后**无需重启**即可切模式 |
| `accesstransformer.cfg` | 打开 `Level.levelData`、`ServerLevel.serverLevelData`（`public-f`） |

数据流：

```
Config ──每 tick──> FlatWorldTuning.onLevelTick ──> FlatWorldLevelData.applyTime/applyWeather
                                        │
LevelEvent.Load ──> new FlatWorldLevelData ──> level.levelData = level.serverLevelData = data
                                        │
MinecraftServer#tickChildren（每 20 tick）──> synchronizeTime(level).getDayTime() ──> 客户端
```

## 模式集合

- **时间** `DAY`（6000，正午）/ `NIGHT`（18000，午夜）/ `CYCLE`（自走）。
- **天气** `CLEAR` / `RAIN` / `THUNDER`。

刻意**不做** `FOLLOW`（跟随主世界）——本维度的设计意图就是独立于主世界，跟随语义与之相悖，
且会引入 `super.getDayTime()` 分叉与「pin 还是 follow」的双重状态。

## 可演化方向

- 若要**运行时命令**切换（`/flatworld time <mode>`），只需在命令里写完 `Config` 或直接调用
  `FlatWorldLevelData#applyTime` —— 每 tick 的施加逻辑无需改动。注意 `Config` 的值本身不可
  由模组代码回写（NeoForge 配置是只读快照），需引入一份运行时覆盖值。
- 若要**每玩家维度**，本设计不能直接扩展，需要 `DynamicDimensions` 那一套（见上文取舍）。
- 若将来把 `flatworld` 升到 1.21.2+，`accesstransformer.cfg` 里的两个字段需重新核对
  （`Level.levelData` 的类型与可见性在版本间有过调整）。
