package com.hbm_m.entity.logic;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm_m.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm_m.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm_m.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm_m.explosion.vanillant.standard.PlayerProcessorStandard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code com.hbm.entity.logic.EntityOrbitalLaser} (Orbitaler Praezisionslaser): Explosion der Staerke 5 mit
 * 1000 Schaden (durchschlagend 50/0.5) und Waffen-Effekt, danach 5 Ticks roter Strahl.
 */
public class EntityOrbitalLaser extends Entity {

    public static final int maxAge = 5;

    public EntityOrbitalLaser(EntityType<? extends EntityOrbitalLaser> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public EntityOrbitalLaser(Level world) {
        this(ModEntities.ORBITAL_LASER.get(), world);
    }

    //? if < 1.21.1 {
    @Override protected void defineSynchedData() { }
    //?} else {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }

    @Override
    public void tick() {
        if (this.tickCount >= maxAge && !level().isClientSide) this.discard();
    }

    public void explode() {
        ExplosionVNT vnt = new ExplosionVNT(level(), getX(), getY(), getZ(), 5F);
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        // Original: .setDamageClass(DamageClass.LASER) - die Port-Variante kennt keine Schadensklassen
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 1_000F).setupPiercing(50F, 0.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(15, 3.5F, 1.25F));
        vnt.explode();
    }

    @Override
    public float getLightLevelDependentMagicValue() {
        return 1.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 25000;
    }
}
