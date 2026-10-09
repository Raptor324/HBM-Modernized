package com.hbm_m.entity.projectile;

import com.hbm_m.explosion.ExplosionLarge;
import com.hbm_m.explosion.ExplosionNukeGeneric;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityBurningFOEQ}: abstuerzender, brennender FOEQ-Satellit ({@code enableCataclysm}). Beschleunigt um
 * 0.1/Tick bis -4, beim Aufschlag 25 Explosionen (Staerke 10) und Atommuell im Radius 35.
 */
public class EntityBurningFOEQ extends Entity {

    public EntityBurningFOEQ(EntityType<? extends EntityBurningFOEQ> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    public void tick() {
        this.xo = this.xOld = getX();
        this.yo = this.yOld = getY();
        this.zo = this.zOld = getZ();

        Vec3 m = getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

        if (m.y > -4)
            this.setDeltaMovement(m.x, m.y - 0.1, m.z);

        this.rotation();

        if (!level().getBlockState(new BlockPos((int) getX(), (int) getY(), (int) getZ())).isAir()) {
            if (!level().isClientSide) {
                for (int i = 0; i < 25; i++)
                    ExplosionLarge.explode(level(), getX() + 0.5F + random.nextGaussian() * 5, getY() + 0.5F + random.nextGaussian() * 5, getZ() + 0.5F + random.nextGaussian() * 5, 10.0F, random.nextBoolean(), false, false);
                ExplosionNukeGeneric.waste(level(), (int) getX(), (int) getY(), (int) getZ(), 35);
            }
            this.discard();
        }
    }

    public void rotation() {
        Vec3 m = getDeltaMovement();
        double f2 = Math.sqrt(m.x * m.x + m.z * m.z);
        this.setYRot((float) (Math.atan2(m.x, m.z) * 180.0D / Math.PI));

        float pitch = (float) (Math.atan2(m.y, f2) * 180.0D / Math.PI) - 90;
        while (pitch - this.xRotO < -180.0F) this.xRotO -= 360.0F;
        while (pitch - this.xRotO >= 180.0F) this.xRotO += 360.0F;
        this.setXRot(pitch);

        while (this.getYRot() - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (this.getYRot() - this.yRotO >= 180.0F) this.yRotO += 360.0F;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) { }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) { }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 100000;
    }
}
