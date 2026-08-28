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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
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
     * 末影珍珠撞击到堆肥桶时，按投掷者当前所在维度双向传送：
     * 在其它维度 → 进超平坦维度；已在超平坦维度 → 返回主世界。
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

        // 取消珍珠原本的撞击行为（不再生成实体/伤害等）
        event.setCanceled(true);
        pearl.discard();

        if (player.level().dimension() == FLAT_WORLD) {
            teleportToOverworld(player);
        } else {
            teleportToFlatWorld(player);
        }
    }

    /**
     * 传送到超平坦维度：落在该维度世界出生点的安全地表。
     */
    private void teleportToFlatWorld(ServerPlayer player) {
        ServerLevel target = player.server.getLevel(FLAT_WORLD);
        if (target == null) {
            LOGGER.warn("Flat world dimension not found: {}", FLAT_WORLD.location());
            return;
        }

        // adjustSpawnLocation 会强制加载出生点区块并按地表高度返回安全落点，复用原版重生/传送逻辑，
        // 避免落到未生成区块的虚空或悬空摔死。
        BlockPos spawnTarget = player.adjustSpawnLocation(target, target.getSharedSpawnPos());
        player.teleportTo(target, spawnTarget.getX() + 0.5, spawnTarget.getY(), spawnTarget.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * 返回主世界：优先落在玩家重生点（床），没有主世界重生点则落在世界出生点。
     */
    private void teleportToOverworld(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();

        // 确定水平落点：有主世界重生点（床）就用它，否则用世界出生点。
        BlockPos horizontal;
        BlockPos respawn = player.getRespawnPosition();
        if (respawn != null && player.getRespawnDimension() == Level.OVERWORLD) {
            horizontal = respawn;
        } else {
            horizontal = overworld.getSharedSpawnPos();
        }

        // 强制加载目标区块后按地表高度取安全落点。
        BlockPos target = safeSurfacePos(overworld, horizontal);
        player.teleportTo(overworld, target.getX() + 0.5, target.getY(), target.getZ() + 0.5, player.getYRot(), player.getXRot());
    }

    /**
     * 超平坦维度保持永晴：每个维度 tick 末尾检查，若该维度在下雨/打雷则重置为晴朗。
     */
    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.dimension() != FLAT_WORLD) {
            return;
        }
        if (serverLevel.isRaining() || serverLevel.isThundering()) {
            // 设一段晴朗时间，覆盖天气循环刚刚推进出的雨/雷。
            serverLevel.setWeatherParameters(72000, 0, false, false);
        }
    }

    /**
     * 以给定水平坐标为准，强制加载该区块后返回该维度的安全地表落点（地表方块之上）。
     */
    private static BlockPos safeSurfacePos(ServerLevel level, BlockPos horizontal) {
        int y = level.getChunkAt(horizontal).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, horizontal.getX(), horizontal.getZ()) + 1;
        return new BlockPos(horizontal.getX(), y, horizontal.getZ());
    }
}
