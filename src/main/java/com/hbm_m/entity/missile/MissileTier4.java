package com.hbm_m.entity.missile;

import api.hbm.entity.IRadarDetectable;
import com.hbm_m.explosion.NuclearExplosionAPI;
import com.hbm_m.explosion.NuclearExplosionConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ракеты уровня 4 (корпус Atlas).
 */
public abstract class MissileTier4 extends MissileBaseEntity {

    private static final int MISSILE_NUKE_RADIUS = 50;

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
            startNukeAt(pos, MISSILE_NUKE_RADIUS, 0);
        }
    }

    public static class MissileNuclearCluster extends MissileTier4 {
        public MissileNuclearCluster(EntityType<? extends MissileNuclearCluster> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, MISSILE_NUKE_RADIUS * 2, 0);
        }
    }

    public static class MissileVolcano extends MissileTier4 {
        public MissileVolcano(EntityType<? extends MissileVolcano> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            if (level().isClientSide) {
                return;
            }
            level().explode(this, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    10.0F, Level.ExplosionInteraction.BLOCK);
            // TODO: volcano warhead
        }
    }

    public static class MissileDoomsday extends MissileTier4 {
        public MissileDoomsday(EntityType<? extends MissileDoomsday> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, MISSILE_NUKE_RADIUS * 2, 100);
        }
    }

    public static class MissileDoomsdayRusted extends MissileDoomsday {
        public MissileDoomsdayRusted(EntityType<? extends MissileDoomsdayRusted> type, Level level) {
            super(type, level);
        }

        @Override
        protected void onMissileImpact(BlockPos pos) {
            startNukeAt(pos, MISSILE_NUKE_RADIUS, 100);
        }
    }
}
