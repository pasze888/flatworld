# Flat World Dimension (flatworld)

一个 NeoForge 1.21.1 模组：新增一个「超平坦」维度。向堆肥桶里扔一枚末影珍珠，即可传送进入该维度。

## 功能

- 新增超平坦维度 `flatworld:flat_world`（基岩 1 层 + 泥土 2 层 + 草方块 1 层，与原版默认超平坦一致）。
- 进入方式：向**堆肥桶**投掷**末影珍珠**，珍珠击中堆肥桶后投掷者被传送到超平坦维度。

## 进入维度

1. 合成/找到一枚末影珍珠和一个堆肥桶。
2. 将末影珍珠扔向堆肥桶（击中堆肥桶方块）。
3. 自动传送至超平坦维度。

离开维度可用原版命令 `/execute in minecraft:overworld run tp @s ...`，或再次在超平坦维度里朝堆肥桶丢珍珠（当前回到超平坦自身）。

## 技术要点

- 维度通过数据包 JSON 注册（模组 `resources/data/` 自动作为内建数据包加载）：
  - `data/flatworld/dimension_type/flat_world.json` —— 维度类型
  - `data/flatworld/dimension/flat_world.json` —— LevelStem（`type` + `minecraft:flat` 生成器）
- 传送逻辑监听 `ProjectileImpactEvent`，判断 `ThrownEnderpearl` × `Blocks.COMPOSTER`，调用
  `ServerPlayer#teleportTo(ServerLevel, double, double, double, float, float)` 跨维度传送。

## 构建

```bash
./gradlew build     # 编译 + 打包，产物在 build/libs/flatworld-1.0.0.jar
./gradlew runClient # 启动开发客户端
```
