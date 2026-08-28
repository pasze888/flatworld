package top.flatworld.flatworld;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import org.slf4j.Logger;

@Mod(FlatWorld.MODID)
public class FlatWorld {
    public static final String MODID = "flatworld";
    public static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 超平坦维度的 ResourceKey。维度由 data/flatworld/dimension/flat_world.json 数据包注册，
     * 对应 dimension_type 为 data/flatworld/dimension_type/flat_world.json。
     */
    public static final ResourceKey<Level> FLAT_WORLD = ResourceKey.create(
            Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(MODID, "flat_world"));

    public FlatWorld(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    /**
     * 末影珍珠撞击到堆肥桶时，把投掷者传送进超平坦维度。
     */
    @SubscribeEvent
    public void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof ThrownEnderpearl pearl)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof BlockHitResult blockHit)) {
            return;
        }

        // 只有撞击到堆肥桶才触发
        BlockPos pos = blockHit.getBlockPos();
        if (!pearl.level().getBlockState(pos).is(Blocks.COMPOSTER)) {
            return;
        }

        Entity owner = pearl.getOwner();
        if (!(owner instanceof ServerPlayer player)) {
            return;
        }

        ServerLevel target = player.server.getLevel(FLAT_WORLD);
        if (target == null) {
            LOGGER.warn("Flat world dimension not found: {}", FLAT_WORLD.location());
            return;
        }

        // 取消珍珠原本的撞击行为（不再生成实体/伤害等）
        event.setCanceled(true);
        pearl.discard();

        // 传送到目标维度世界出生点：adjustSpawnLocation 会强制加载出生点区块并按地表高度返回安全落点，
        // 复用原版重生/传送的逻辑，避免落到未生成区块的虚空或悬空摔死。
        BlockPos spawnTarget = player.adjustSpawnLocation(target, target.getSharedSpawnPos());
        player.teleportTo(target, spawnTarget.getX() + 0.5, spawnTarget.getY(), spawnTarget.getZ() + 0.5, player.getYRot(), player.getXRot());
    }
}
