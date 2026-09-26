package io.github.pasze888.flatworld;

import io.github.pasze888.flatworld.worldgen.dim.FlatWorldLevelData;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置（COMMON 类型，服务端/客户端均加载）。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /**
     * 执行 /flatworld 命令所需的最小权限等级（0=所有玩家，1~4=OP 等级）。
     */
    public static final ModConfigSpec.IntValue COMMAND_PERMISSION_LEVEL = BUILDER
            .comment("Minimum permission level required to use the /flatworld command (0 = all players, 1-4 = operator levels).")
            .defineInRange("commandPermissionLevel", 0, 0, 4);

    /**
     * 超平坦维度的时间模式，每 tick 由 {@code FlatWorldTuning} 施加。
     */
    public static final ModConfigSpec.EnumValue<FlatWorldLevelData.TimeMode> TIME_MODE = BUILDER
            .comment("Time mode of the flat world dimension.",
                    "DAY = pinned to noon (6000), NIGHT = pinned to midnight (18000),",
                    "CYCLE = advances on its own and still obeys the doDaylightCycle game rule.",
                    "Pinned modes ignore /time set; CYCLE accepts it.")
            .defineEnum("timeMode", FlatWorldLevelData.TimeMode.DAY);

    /**
     * 超平坦维度的天气模式，每 tick 由 {@code FlatWorldTuning} 施加。
     */
    public static final ModConfigSpec.EnumValue<FlatWorldLevelData.WeatherMode> WEATHER_MODE = BUILDER
            .comment("Weather mode of the flat world dimension: CLEAR, RAIN or THUNDER.",
                    "Note that the biome must allow precipitation for RAIN/THUNDER to be visible.")
            .defineEnum("weatherMode", FlatWorldLevelData.WeatherMode.CLEAR);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
