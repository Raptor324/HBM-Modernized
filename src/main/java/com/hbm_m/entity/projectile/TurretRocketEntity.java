package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Gelenkte Turret-Rakete fuer Richard/Himars (Original: {@code EntityBulletBaseMK4} mit
 * {@code lockonTarget}). Vereinfachtes eigenstaendiges Homing statt einer vollen
 * {@code MissileBaseEntity}-Portierung (die auf feste Block-Zielkoordinaten statt lebende
 * Ziel-Entities ausgelegt ist): dreht die Flugrichtung jeden Tick etwas in Richtung Ziel und
 * explodiert beim Treffer.
 */
public class TurretRocketEntity extends ThrowableItemProjectile {

    private static final EntityDataAccessor<String> ICON_ITEM_ID =
            SynchedEntityData.defineId(TurretRocketEntity.class, EntityDataSerializers.STRING);

    private static final double SPEED = 4.0D;
    private static final double TURN_RATE = 0.12D;

    private LivingEntity homingTarget;
    private float damage = 10.0F;
    private float explosionRadius = 2.0F;

    public TurretRocketEntity(EntityType<? extends ThrowableItemProjectile> type, Level level) {
        super(type, level);
    }

    public static TurretRocketEntity create(Level level, double x, double y, double z,
                                             LivingEntity target, float damage, float explosionRadius, Item iconItem) {
        TurretRocketEntity rocket = new TurretRocketEntity(ModEntities.TURRET_ROCKET.get(), level);
        rocket.setPos(x, y, z);
        Vec3 dir = target.getEyePosition().subtract(x, y, z).normalize();
        rocket.setDeltaMovement(dir.scale(SPEED));
        rocket.homingTarget = target;
        rocket.damage = damage;
        rocket.explosionRadius = explosionRadius;
        rocket.entityData.set(ICON_ITEM_ID, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(iconItem).toString());
        rocket.setNoGravity(true);
        return rocket;
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

        super.defineSynchedData(builder);
        builder.define(ICON_ITEM_ID, "");
    
    }
    *///?}

    @Override
    protected Item getDefaultItem() {
        String id = this.entityData.get(ICON_ITEM_ID);
        if (id.isEmpty()) return ModItems.TURRET_AMMO.get();
        Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.tryParse(id));
        return item == null ? Items.FIRE_CHARGE : item;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && homingTarget != null && homingTarget.isAlive()) {
            Vec3 current = getDeltaMovement();
            Vec3 toTarget = homingTarget.getEyePosition().subtract(position()).normalize().scale(SPEED);
            Vec3 blended = current.scale(1.0D - TURN_RATE).add(toTarget.scale(TURN_RATE)).normalize().scale(SPEED);
            setDeltaMovement(blended);
        }
        if (this.tickCount > 200) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!this.level().isClientSide) {
            explode(result.getEntity());
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide && result.getType() == HitResult.Type.BLOCK) {
            explode(null);
        }
    }

    private void explode(Entity directHit) {
        Vec3 pos = position();
        this.level().explode(this, pos.x, pos.y, pos.z, explosionRadius, Level.ExplosionInteraction.MOB);
        if (directHit instanceof LivingEntity living) {
            living.hurt(this.level().damageSources().generic(), damage);
        }
        this.discard();
    }
}
