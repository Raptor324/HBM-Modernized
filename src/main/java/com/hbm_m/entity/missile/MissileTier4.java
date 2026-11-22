package com.hbm_m.entity.missile;

import api.hbm_m.entity.IRadarDetectable;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.explosion.NuclearExplosionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Ракеты уровня 4 (корпус Atlas).
 */
public abstract class MissileTier4 extends MissileBaseEntity {

    /**
     * Радиус ядерного взрыва ракеты. 1.7.10 {@code BombConfig.missileRadius} (дефолт 100).
     * Раньше был захардкожен 50 — дефицит мощности вдвое. Теперь читается из конфига.
     */
    private static int missileNukeRadius() {
        return ModClothConfig.get().missileRadius;
    }

    protected MissileTier4(EntityType<? extends MissileTier4> type, Level level) {
        super(type, level);
    }

    @Override
    public IRadarDetectable.RadarTargetType getTargetType() {
        return IRadarDetectable.RadarTargetType.MISSILE_TIER4;
    }

    @Override
    protected void spawnContrail() {
        Vec3 thrust = new Vec3(0.0D, 0.0D, 1.0D);
        switch (this.getLaunchFacing()) {
            case WEST -> thrust = thrust.yRot((float) -Math.PI / 2.0F);
            case SOUTH -> thrust = thrust.yRot((float) -Math.PI);
            case EAST -> thrust = thrust.yRot((float) (-Math.PI / 2.0F * 3.0F));
            default -> { }
        }
        thrust = thrust.yRot(-(this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));
        thrust = thrust.xRot(-this.getXRot() * ((float) Math.PI / 180.0F));
        thrust = thrust.yRot((this.getYRot() + 90.0F) * ((float) Math.PI / 180.0F));

        // -thrust.z in the y slot is what the original ships; kept for parity.
        spawnContrailWithOffset(thrust.x, thrust.y, thrust.z);
        spawnContrailWithOffset(0.0D, 0.0D, 0.0D);
        spawnContrailWithOffset(-thrust.x, -thrust.z, -thrust.z);
    }

    protected void startNukeAt(BlockPos pos, int radius, int extraFallout) {
        if (level().isClientSide) {
            return;
        }
        NuclearExplosionConfig cfg = NuclearExplosionConfig.builder(radius)
                .fallout(true)
                .radiation(true)
                .extraFalloutRadius(extraFallout)
                .mushroomType(0)
                .build();
        NuclearExplosionAPI.start(level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, cfg);
    }

    public static class MissileNuclear extends MissileTier4 {
        public MissileNuclear(EntityType<? extends MissileNuclear> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, missileNukeRadius(), 0);
        }
    }

    public static class MissileNuclearCluster extends MissileTier4 {
        public MissileNuclearCluster(EntityType<? extends MissileNuclearCluster> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, missileNukeRadius() * 2, 0);
        }
    }

    public static class MissileVolcano extends MissileTier4 {
        public MissileVolcano(EntityType<? extends MissileVolcano> type, Level level) {
            super(type, level);
        }

        @Override
        protected ItemStack getDebrisRareDrop() {
            // 1.7.10 EntityMissileVolcano: rare drop warhead_volcano
            return new ItemStack(com.hbm_m.item.ModItems.WARHEAD_VOLCANO.get());
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            if (level().isClientSide) {
                return;
            }
            // 1.7.10 EntityMissileVolcano: ExplosionLarge.explode(w, x, y, z, 10F, cloud, rubble, shrapnel)
            // (взрыв без огня) + куб вулканической лавы 3×3×3 + volcano_core в центре.
            if (level() instanceof net.minecraft.server.level.ServerLevel server) {
                com.hbm_m.explosion.MissileWarheadEffects.standardExplode(this, server, pos, 10.0F, false, 16);
                com.hbm_m.explosion.MissileWarheadEffects.composeEffectStandard(server, pos);
                com.hbm_m.explosion.MissileWarheadEffects.spawnShrapnelBurst(server,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 10);
                com.hbm_m.explosion.MissileWarheadEffects.spawnLightRubble(server,
                        pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5);
            }
            BlockState lava = com.hbm_m.block.ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState();
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        level().setBlock(pos.offset(x, y, z), lava, 3);
                    }
                }
            }
            level().setBlock(pos, com.hbm_m.block.ModBlocks.VOLCANO_CORE.get().defaultBlockState(), 3);
        }
    }

    public static class MissileDoomsday extends MissileTier4 {
        public MissileDoomsday(EntityType<? extends MissileDoomsday> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, missileNukeRadius() * 2, 100);
        }
    }

    public static class MissileDoomsdayRusted extends MissileDoomsday {
        public MissileDoomsdayRusted(EntityType<? extends MissileDoomsdayRusted> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, missileNukeRadius(), 100);
        }
    }
}
