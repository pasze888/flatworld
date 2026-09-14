package io.github.pasze888.flatworld.worldgen.dim;

import io.github.pasze888.flatworld.FlatWorld;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;

import java.util.OptionalLong;

/**
 * 超平坦维度的注册表键与维度类型数据生成。
 *
 * <p>三个键分别对应三种注册表：{@link Registries#LEVEL_STEM}（维度内容，即 dimension JSON）、
 * {@link Registries#DIMENSION}（世界本身，运行时用 {@code ServerLevel} 取用）、
 * {@link Registries#DIMENSION_TYPE}（维度类型，由 {@code bootstrap} 数据生成）。
 *
 * <p>维度类型与维度同名（都是 {@code flatworld:flat_world}）：dimension JSON 里的
 * {@code type} 直接引用它，同时保持与旧存档一致（改名会让已存在的存档找不到维度类型而加载失败）。
 */
public class ModDimensions {
    /**
     * LevelStem 键，对应数据包 {@code data/flatworld/dimension/flat_world.json}（手写，含 flat 生成器与群系）。
     */
    public static final ResourceKey<LevelStem> FLAT_WORLD_STEM_KEY = ResourceKey.create(Registries.LEVEL_STEM,
            ResourceLocation.fromNamespaceAndPath(FlatWorld.MODID, "flat_world"));

    /**
     * 世界键，用于 {@code player.level().dimension()} 比较、{@code MinecraftServer#getLevel} 取维度。
     */
    public static final ResourceKey<Level> FLAT_WORLD_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(FlatWorld.MODID, "flat_world"));

    /**
     * 维度类型键，由 {@link #bootstrap} 数据生成到
     * {@code src/generated/resources/data/flatworld/dimension_type/flat_world.json}。
     */
    public static final ResourceKey<DimensionType> FLAT_WORLD_TYPE_KEY = ResourceKey.create(Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(FlatWorld.MODID, "flat_world"));

    /**
     * 数据生成入口：注册维度类型。参数含义依次为
     * fixedTime（固定时间，6000=正午，实现永昼）、hasSkyLight、hasCeiling、ultrawarm、natural、
     * coordinateScale（坐标缩放）、bedWorks、respawnAnchorWorks、minY、height、logicalHeight、
     * infiniburn（不灭火方块标签）、effectsLocation（环境效果）、ambientLight、monsterSettings。
     */
    public static void bootstrap(BootstrapContext<DimensionType> context) {
        context.register(FLAT_WORLD_TYPE_KEY, new DimensionType(
                OptionalLong.of(6000L),
                true,
                false,
                false,
                true,
                1.0,
                true,
                false,
                -64,
                384,
                384,
                BlockTags.INFINIBURN_OVERWORLD,
                BuiltinDimensionTypes.OVERWORLD_EFFECTS,
                0.0F,
                new DimensionType.MonsterSettings(false, true, UniformInt.of(0, 7), 0)
        ));
    }
}
