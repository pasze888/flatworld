package io.github.pasze888.flatworld;

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
     * 是否允许「末影珍珠扔进堆肥桶」触发传送（true=启用，false=禁用）。
     */
    public static final ModConfigSpec.BooleanValue ENABLE_COMPOSTER_TELEPORT = BUILDER
            .comment("Whether throwing an ender pearl into a composter triggers teleporting between dimensions.")
            .define("enableComposterTeleport", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
