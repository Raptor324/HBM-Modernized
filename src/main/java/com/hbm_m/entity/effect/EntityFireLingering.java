package com.hbm_m.entity.effect;

import java.util.List;

import com.hbm_m.extprop.HbmLivingProps;
import com.hbm_m.particle.helper.FlameCreator;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityFireLingering}: Flaechenbrand (Diesel, Balefire, Phosphor, Oxy, Schwarzfeuer) fuer eine feste Dauer.
 * Setzt bei Lebewesen die HBM-Feuerzaehler, andere Entities werden angezuendet. Wird nicht gespeichert.
 */
public class EntityFireLingering extends Entity {

    public static int TYPE_DIESEL = 0;
    public static int TYPE_BALEFIRE = 1;
    public static int TYPE_PHOSPHORUS = 2;
    public static int TYPE_OXY = 3;
    public static int TYPE_BLACK = 4;
    public int maxAge = 150;

    private static final EntityDataAccessor<Integer> DATA_TYPE = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_WIDTH = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT = SynchedEntityData.defineId(EntityFireLingering.class, EntityDataSerializers.FLOAT);

    public EntityFireLingering(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Original {@code new EntityFireLingering(world)}. */
    public EntityFireLingering(Level level) {
        this(com.hbm_m.entity.ModEntities.FIRE_LINGERING.get(), level);
    }

    public EntityFireLingering setArea(float width, float height) {
        this.entityData.set(DATA_WIDTH, width);
        this.entityData.set(DATA_HEIGHT, height);
        return this;
    }

    public EntityFireLingering setDuration(int duration) {
        this.maxAge = duration;
        return this;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DATA_TYPE, 0);
        this.entityData.define(DATA_WIDTH, 0F);
        this.entityData.define(DATA_HEIGHT, 0F);
    }

    public EntityFireLingering setType(int type) {
        this.entityData.set(DATA_TYPE, type);
        return this;
    }

    public int getFireType() {
        return this.entityData.get(DATA_TYPE);
    }

    /** Original {@code setPosition(x, y, z)}. */
    public void setPosition(double x, double y, double z) {
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        this.baseTick();

        float width = this.entityData.get(DATA_WIDTH);
        float height = this.entityData.get(DATA_HEIGHT);
        this.setBoundingBox(new AABB(getX() - width / 2, getY(), getZ() - width / 2, getX() + width / 2, getY() + height, getZ() + width / 2));

        if (!level().isClientSide) {

            if (this.tickCount >= maxAge) {
                this.discard();
            }

            List<Entity> affected = level().getEntities(this, new AABB(getX() - width / 2, getY(), getZ() - width / 2, getX() + width / 2, getY() + height, getZ() + width / 2));

            for (Entity e : affected) {
                if (e instanceof LivingEntity livng) {
                    if (this.getFireType() == TYPE_DIESEL) if (HbmLivingProps.getFire(livng) < 60) HbmLivingProps.setFire(livng, 60);
                    if (this.getFireType() == TYPE_PHOSPHORUS) if (HbmLivingProps.getFire(livng) < 300) HbmLivingProps.setFire(livng, 300);
                    if (this.getFireType() == TYPE_BALEFIRE) if (HbmLivingProps.getBalefire(livng) < 100) HbmLivingProps.setBalefire(livng, 100);
                    if (this.getFireType() == TYPE_BLACK) {
                        if (HbmLivingProps.getBlackFire(livng) < 200) HbmLivingProps.setBlackFire(livng, 200);
                        else HbmLivingProps.setBlackFire(livng, HbmLivingProps.getBlackFire(livng) + 5);
                    }
                } else {
                    e.setSecondsOnFire(4);
                }
            }
        } else {

            for (int i = 0; i < (width >= 5 ? 2 : 1); i++) {
                double x = getX() - width / 2 + random.nextDouble() * width;
                double z = getZ() - width / 2 + random.nextDouble() * width;

                Vec3 up = new Vec3(x, getY() + height, z);
                Vec3 down = new Vec3(x, getY() - height, z);
                BlockHitResult mop = level().clip(new ClipContext(up, down, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
                if (mop.getType() == HitResult.Type.BLOCK) down = mop.getLocation();
                if (this.getFireType() == TYPE_DIESEL) FlameCreator.composeEffectClient(x, down.y, z, FlameCreator.META_FIRE);
                if (this.getFireType() == TYPE_PHOSPHORUS) FlameCreator.composeEffectClient(x, down.y, z, FlameCreator.META_FIRE);
                if (this.getFireType() == TYPE_BALEFIRE) FlameCreator.composeEffectClient(x, down.y, z, FlameCreator.META_BALEFIRE);
                if (this.getFireType() == TYPE_BLACK) FlameCreator.composeEffectClient(x, down.y, z, FlameCreator.META_BLACK);
            }
        }
    }

    @Override public boolean displayFireAnimation() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override protected void addAdditionalSaveData(CompoundTag nbt) { }
    @Override protected void readAdditionalSaveData(CompoundTag nbt) { this.discard(); }
}
