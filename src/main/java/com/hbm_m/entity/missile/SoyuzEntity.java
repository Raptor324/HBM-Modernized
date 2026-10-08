package com.hbm_m.entity.missile;

import java.util.List;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModParticleTypes;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntitySoyuz}: steigt beschleunigt senkrecht auf (ohne Kollision), verbrennt alles im Abgasstrahl und
 * liefert auf Hoehe 600 die Nutzlast aus: Satellitenmodus (0) bringt den Satelliten per
 * {@link com.hbm_m.satellite.SatelliteManager#orbit} in die Umlaufbahn, Frachtmodus (1) schickt eine
 * {@link SoyuzCapsuleEntity} mit der Fracht zum Ziel des Zielmarkierers.
 */
public class SoyuzEntity extends Entity {

    private static final double DEPLOY_HEIGHT = 600.0D;

    /** Original Datawatcher 8: Skin der Rakete (0-2). */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> SKIN =
            net.minecraft.network.syncher.SynchedEntityData.defineId(SoyuzEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

    public void setSkin(int i) { this.entityData.set(SKIN, Math.max(0, i)); }
    public int getSkin() { return this.entityData.get(SKIN); }

    public int mode;
    public int targetX;
    public int targetZ;
    private double acceleration = 0.0D;
    private boolean memed = false;

    private final NonNullList<ItemStack> payload = NonNullList.withSize(18, ItemStack.EMPTY);

    public SoyuzEntity(EntityType<? extends SoyuzEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = true;
    }

    public void initLaunch(double x, double y, double z, int mode) {
        this.setPos(x, y, z);
        this.mode = mode;
    }

    public void setTarget(int x, int z) {
        this.targetX = x;
        this.targetZ = z;
    }

    public void setPayload(List<ItemStack> items) {
        for (int i = 0; i < items.size() && i < payload.size(); i++) {
            payload.set(i, items.get(i));
        }
    }

    @Override
    public void tick() {
        super.tick();

        if (getDeltaMovement().y < 2.0D) {
            acceleration += 0.00025D;
            setDeltaMovement(getDeltaMovement().x, getDeltaMovement().y + acceleration, getDeltaMovement().z);
        }

        // Original setLocationAndAngles: fliegt ohne Kollision durch alles hindurch
        Vec3 m = getDeltaMovement();
        this.setPos(getX() + m.x, getY() + m.y, getZ() + m.z);
        this.setYRot(0);
        this.setXRot(0);

        if (!level().isClientSide) {

            AABB exhaustZone = new AABB(getX() - 5, getY() - 15, getZ() - 5, getX() + 5, getY(), getZ() + 5);
            List<Entity> caught = level().getEntities(this, exhaustZone);

            for (Entity e : caught) {
                PlatformHooks.setSecondsOnFire(e, 15);
                DamageSource exhaust = ModDamageSources.exhaust(level());
                e.hurt(exhaust, 100.0F);

                if (e instanceof net.minecraft.world.entity.player.Player player) {
                    if (!memed) {
                        memed = true;
                        level().playSound(null, getX(), getY(), getZ(), com.hbm_m.sound.HbmSoundsNT.get("hbm:alarm.soyuzed"), net.minecraft.sounds.SoundSource.NEUTRAL, 100, 1.0F);
                    }

                    com.hbm_m.advancement.ModAdvancements.grant(player, com.hbm_m.advancement.ModAdvancements.SOYUZ);
                }
            }
        }

        if (level().isClientSide) {
            spawnExhaust(getX(), getY(), getZ());
            spawnExhaust(getX() + 2.75, getY(), getZ());
            spawnExhaust(getX() - 2.75, getY(), getZ());
            spawnExhaust(getX(), getY(), getZ() + 2.75);
            spawnExhaust(getX(), getY(), getZ() - 2.75);
        }

        if (getY() > DEPLOY_HEIGHT) {
            deployPayload();
        }
    }

    /** Original: {@code effectNT} type exhaust, mode soyuz (Breite wie im Original rand * 0.25 - 0.5). */
    private void spawnExhaust(double x, double y, double z) {
        CompoundTag data = new CompoundTag();
        data.putString("type", "exhaust");
        data.putString("mode", "soyuz");
        data.putInt("count", 1);
        data.putDouble("width", level().random.nextDouble() * 0.25 - 0.5);
        data.putDouble("posX", x);
        data.putDouble("posY", y);
        data.putDouble("posZ", z);
        com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
    }

    private void deployPayload() {
        if (level().isClientSide) {
            this.discard();
            return;
        }

        if (mode == 0 && level() instanceof ServerLevel server) {
            // 1:1 EntitySoyuz.deployPayload
            ItemStack load = payload.isEmpty() ? ItemStack.EMPTY : payload.get(0);

            if (!load.isEmpty()) {

                if (load.is(ModItems.FLAME_PONY.get())) {
                    com.hbm_m.explosion.ExplosionLarge.spawnTracers(server, getX(), getY(), getZ(), 25);
                    com.hbm_m.advancement.ModAdvancements.grantAll(level(), com.hbm_m.advancement.ModAdvancements.SPACE);
                }

                if (load.is(ModItems.SAT_FOEQ.get())) {
                    com.hbm_m.advancement.ModAdvancements.grantAll(level(), com.hbm_m.advancement.ModAdvancements.FOEQ);
                }

                if (load.getItem() instanceof com.hbm_m.item.ISatChip) {
                    int freq = com.hbm_m.item.ISatChip.getFreqS(load);
                    com.hbm_m.satellite.SatelliteManager.get(server).orbit(server, load, freq, getX(), getY(), getZ());
                }
            }
        } else if (mode == 1 && level() instanceof ServerLevel server) {
            SoyuzCapsuleEntity capsule = ModEntities.SOYUZ_CAPSULE.get().create(server);
            if (capsule != null) {
                capsule.setPayload(payload);
                capsule.soyuz = this.getSkin();
                capsule.setPos(targetX + 0.5, DEPLOY_HEIGHT, targetZ + 0.5);
                // Original provider.loadChunk: Zielchunk einmal laden (kein dauerhaftes Ticket)
                server.getChunk(targetX >> 4, targetZ >> 4);
                server.addFreshEntity(capsule);
            }
        }

        this.discard();
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        this.entityData.define(SKIN, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        builder.define(SKIN, 0);
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.mode = tag.getInt("mode");
        this.setSkin(tag.getInt("skin"));
        this.targetX = tag.getInt("targetX");
        this.targetZ = tag.getInt("targetZ");
        this.acceleration = tag.getDouble("acceleration");

        ListTag list = tag.getList("items", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getByte("slot");
            if (slot >= 0 && slot < payload.size()) {
                payload.set(slot, PlatformHooks.itemStackOf(itemTag, PlatformHooks.bestEffortProvider()));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("mode", mode);
        tag.putInt("skin", getSkin());
        tag.putInt("targetX", targetX);
        tag.putInt("targetZ", targetZ);
        tag.putDouble("acceleration", acceleration);

        ListTag list = new ListTag();
        for (int i = 0; i < payload.size(); i++) {
            ItemStack stack = payload.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("slot", (byte) i);
                PlatformHooks.saveItemStack(stack, itemTag, PlatformHooks.bestEffortProvider());
                list.add(itemTag);
            }
        }
        tag.put("items", list);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }


    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }
}
