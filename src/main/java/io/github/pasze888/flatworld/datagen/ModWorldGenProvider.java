package io.github.pasze888.flatworld.datagen;

import io.github.pasze888.flatworld.FlatWorld;
import io.github.pasze888.flatworld.worldgen.dim.ModDimensions;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 数据生成维度类型（{@code data/flatworld/dimension_type/}）。
 *
 * <p>维度内容（LevelStem）与群系仍是手写 JSON：LevelStem 要引用 flat 生成器与群系，
 * 群系要重建整套 features，走数据生成收益不大。
 */
public class ModWorldGenProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, ModDimensions::bootstrap);

    public ModWorldGenProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(FlatWorld.MODID));
    }
}
