package com.hbm_m.entity.missile;

import com.hbm_m.item.ISatChip;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityRocketLambda} (mit {@code EntityRocketBase}): steigt beschleunigt senkrecht auf und bringt auf
 * Hoehe 600 den Satelliten aus dem einzigen Nutzlastplatz per {@link com.hbm_m.satellite.SatelliteManager#orbit} in
 * die Umlaufbahn (bzw. liefert ihn als Bauteil an einen vorhandenen Satelliten derselben Frequenz).
 */
public class LambdaRocketEntity extends Entity {

    private static final double DEPLOY_HEIGHT = 600.0D;

    private ItemStack payload = ItemStack.EMPTY;
    private double acceleration = 0.0D;

    public LambdaRocketEntity(EntityType<? extends LambdaRocketEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    public void setSat(ItemStack stack) {
        this.payload = stack == null ? ItemStack.EMPTY : stack.copy();
    }

    @Override
    public void tick() {
        super.tick();

        if (getDeltaMovement().y < 2.0D) {
            acceleration += 0.00025D;
            setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y + acceleration, getDeltaMovement().z);
        }

        Vec3 m = getDeltaMovement();
        this.xo = getX();
        this.yo = getY();
        this.zo = getZ();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);

        if (level().isClientSide) {
            spawnExhaust(getX(), getY() + 1, getZ());
        }

        if (getY() > DEPLOY_HEIGHT) {
            if (!level().isClientSide) deployPayload();
            this.discard();
        }
    }

    private void spawnExhaust(double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "exhaust");
        data.putString("mode", "lambda");
        data.putInt("count", 1);
        data.putDouble("width", 0);
        data.putDouble("posX", x);
        data.putDouble("posY", y);
        data.putDouble("posZ", z);
        com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);

        if (this.tickCount % 10 == 0) {
            Vec3 vec = new Vec3(1, 0, 0).yRot((float) Math.toRadians(45 * this.random.nextInt(8)));
            double j = 0.5;
            CompoundTag trail = new CompoundTag();
            trail.putString("type", "missileContrail");
            trail.putDouble("posX", getX() - vec.x * j);
            trail.putDouble("posY", getY() - vec.y * j);
            trail.putDouble("posZ", getZ() - vec.z * j);
            trail.putFloat("scale", 1F);
            trail.putDouble("moX", vec.x);
            trail.putDouble("moY", getDeltaMovement().y - 0.5);
            trail.putDouble("moZ", vec.z);
            trail.putInt("maxAge", 60 + random.nextInt(20));
            com.hbm_m.particle.helper.ParticleEffectClient.effectNT(trail);
        }
    }

    private void deployPayload() {
        if (payload.isEmpty() || !(level() instanceof ServerLevel server)) return;
        if (payload.getItem() instanceof ISatChip) {
            int freq = ISatChip.getFreqS(payload);
            com.hbm_m.satellite.SatelliteManager.get(server).orbit(server, payload, freq, getX(), getY(), getZ());
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() { }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) { }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.acceleration = tag.getDouble("acceleration");
        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            if (itemTag.getByte("slot") == 0) payload = com.hbm_m.platform.PlatformHooks.itemStackOf(itemTag, com.hbm_m.platform.PlatformHooks.bestEffortProvider());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("acceleration", acceleration);
        ListTag list = new ListTag();
        if (!payload.isEmpty()) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("slot", (byte) 0);
            com.hbm_m.platform.PlatformHooks.saveItemStack(payload, itemTag, com.hbm_m.platform.PlatformHooks.bestEffortProvider());
            list.add(itemTag);
        }
        tag.put("items", list);
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
