package io.github.pasze888.flatworld.worldgen.dim;

import io.github.pasze888.flatworld.Config;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.WorldData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * 让 flat_world 维度真正拥有独立的时间与天气。挂在 {@code NeoForge.EVENT_BUS} 上。
 *
 * <p>两步：
 * <ol>
 *   <li>{@link LevelEvent.Load}——把该维度的 levelData 换成 {@link FlatWorldLevelData}。
 *       默认的 {@code DerivedLevelData} 委托主世界且 setter 全空，不换则配置里的模式毫无效果。</li>
 *   <li>{@link LevelTickEvent.Post}——按 {@code Config} 每 tick 强制设定时间与天气。
 *       设定是幂等的，所以配置被重载后无需重启即可切模式。</li>
 * </ol>
 *
 * <p>时间同步到客户端不需要额外发包：{@code MinecraftServer#tickChildren} 每 20 tick 会对每个维度调用
 * {@code synchronizeTime()}，读的正是这里的 {@code getDayTime()}。
 */
public final class FlatWorldTuning {

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != ModDimensions.FLAT_WORLD_LEVEL_KEY) {
            return;
        }
        if (level.getLevelData() instanceof FlatWorldLevelData) {
            return; // 已经换过（同维度重复触发时保持幂等）
        }

        WorldData worldData = level.getServer().getWorldData();
        FlatWorldLevelData data = new FlatWorldLevelData(worldData, worldData.overworldData());
        // 两个字段必须指向同一个对象：advanceWeatherCycle() 读 levelData 却写 serverLevelData。
        level.levelData = data;
        level.serverLevelData = data;
    }

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        // 无需再比对维度键：只有 flat_world 会被 onLevelLoad 换成 FlatWorldLevelData。
        if (!(level.getLevelData() instanceof FlatWorldLevelData data)) {
            return;
        }

        data.applyTime(Config.TIME_MODE.get(),
                level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
        data.applyWeather(Config.WEATHER_MODE.get());
    }
}
