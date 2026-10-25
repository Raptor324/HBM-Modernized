package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.particle.helper.ExplosionCreator;
import com.hbm_m.particle.helper.IParticleCreator;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.entity.projectile.EntityTorpedo}: ein vom Himmel fallender Torpedo (".44 Torpedo"-Munition).
 * Erzeugt beim ersten Tick 15 Wolkenpartikel ("bf"), faellt mit bis zu 2.5 Bloecken/Tick und explodiert beim
 * Aufschlag mit einer Standard-ExplosionVNT der Staerke 20.
 */
public class EntityTorpedo extends Entity {

    public EntityTorpedo(EntityType<? extends EntityTorpedo> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityTorpedo(Level world) {
        this(ModEntities.TORPEDO.get(), world);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public void tick() {
        // Original ueberschreibt onUpdate vollstaendig (kein Entity-Grundtick)
        if (level() instanceof ServerLevel server && this.tickCount == 1) {
            for (int i = 0; i < 15; i++) {
                CompoundTag data = new CompoundTag();
                data.putString("type", "bf");
                IParticleCreator.sendPacket(server,
                        getX() + (random.nextDouble() - 0.5) * 2,
                        getY() + (random.nextDouble() - 0.5) * 1,
                        getZ() + (random.nextDouble() - 0.5) * 2, 150, data);
            }
        }

        this.xOld = this.xo = getX();
        this.yOld = this.yo = getY();
        this.zOld = this.zo = getZ();

        this.setPos(getX() + getDeltaMovement().x, getY() + getDeltaMovement().y, getZ() + getDeltaMovement().z);

        double motionY = getDeltaMovement().y - 0.04;
        if (motionY < -2.5) motionY = -2.5;
        this.setDeltaMovement(getDeltaMovement().x, motionY, getDeltaMovement().z);

        // Original: (int) posX usw. (Richtung null gerundet)
        if (!this.level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            if (level() instanceof ServerLevel server) {
                this.discard();
                ExplosionCreator.composeEffectStandard(server, getX(), getY() + 1, getZ());
                ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 20F);
                vnt.makeStandard();
                vnt.explode();
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }
}
