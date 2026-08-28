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

        // 传送到目标维度：水平坐标沿用玩家当前位置，Y 取目标维度该列地表高度，避免落地空中摔死。
        int x = BlockPos.containing(player.position()).getX();
        int z = BlockPos.containing(player.position()).getZ();
        int surfaceY = target.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        player.teleportTo(target, x + 0.5, surfaceY, z + 0.5, player.getYRot(), player.getXRot());
    }
}
