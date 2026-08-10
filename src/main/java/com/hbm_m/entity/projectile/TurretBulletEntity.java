package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Gerade Munitions-Geschoss-Vorlage fuer die hitscan-artigen Turret-Typen (Sentry/Chekhov/Friendly/
 * Jeremy/Howard), Port des Original-Prinzips (eigene {@code EntityBulletBaseMK4}) stark vereinfacht:
 * kein Fall-off/Penetration, feste Geschwindigkeit, Schaden aus dem feuernden Turret uebernommen.
 * Nutzt {@link ThrowableItemProjectile}, damit ein simples Item-Icon automatisch ueber
 * {@code ThrownItemRenderer} gerendert wird - kein eigenes Rendersystem noetig.
 */
public class TurretBulletEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<String> ICON_ITEM_ID =
            SynchedEntityData.defineId(TurretBulletEntity.class, EntityDataSerializers.STRING);

    private float damage = 4.0F;

    public TurretBulletEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public static TurretBulletEntity create(Level level, double x, double y, double z,
                                             double dx, double dy, double dz, float damage, Item iconItem) {
        TurretBulletEntity bullet = new TurretBulletEntity(ModEntities.TURRET_BULLET.get(), level);
        bullet.setPos(x, y, z);
        bullet.setDeltaMovement(dx, dy, dz);
        bullet.damage = damage;
        bullet.entityData.set(ICON_ITEM_ID, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(iconItem).toString());
        bullet.setNoGravity(true);
        return bullet;
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ICON_ITEM_ID, "");
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        super.defineSynchedData();
        builder.define(ICON_ITEM_ID, "");
    
    }
    *///?}

    @Override
    protected Item getDefaultItem() {
        String id = this.entityData.get(ICON_ITEM_ID);
        if (id.isEmpty()) return ModItems.TURRET_AMMO.get();
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.tryParse(id));
        return item == null ? Items.SNOWBALL : item;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            var target = result.getEntity();
            if (target instanceof LivingEntity living) {
                DamageSource source = this.getOwner() != null
                        ? this.level().damageSources().mobProjectile(this, this.getOwner() instanceof LivingEntity le ? le : null)
                        : this.level().damageSources().generic();
                living.hurt(source, damage);
            }
            this.discard();
        }
    }

    @Override
    protected void onHit(net.minecraft.world.phys.HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide && result.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.tickCount > 60) {
            this.discard();
        }
    }
}
