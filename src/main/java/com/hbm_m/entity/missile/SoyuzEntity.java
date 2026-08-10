package com.hbm_m.entity.missile;

import java.util.List;

import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.particle.ModParticleTypes;

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
 * Port of legacy {@code com.hbm.entity.missile.EntitySoyuz}: accelerates straight up,
 * damages/ignites anything caught underneath, and at altitude either "deploys" its
 * satellite payload (mode 0 - no orbital simulation exists in this port, see plan
 * notes on SoyuzLauncherBlockEntity) or spawns a {@link SoyuzCapsuleEntity} carrying
 * cargo down to the designated target (mode 1).
 */
public class SoyuzEntity extends Entity {

    private static final double DEPLOY_HEIGHT = 600.0D;

    public int mode;
    public int targetX;
    public int targetZ;
    private double acceleration = 0.0D;
    private boolean firedOnce = false;

    private final NonNullList<ItemStack> payload = NonNullList.withSize(18, ItemStack.EMPTY);

    public SoyuzEntity(EntityType<? extends SoyuzEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.noPhysics = false;
    }

    public void initLaunch(double x, double y, double z, int mode) {
        this.setPos(x, y, z);
        this.mode = mode;
        // Small instant kick so the rocket is visibly already moving the moment the
        // takeoff sound fires, instead of creeping up imperceptibly for the first
        // second or two of the 0.00025/tick acceleration ramp below.
        this.setDeltaMovement(0.0D, 0.02D, 0.0D);
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
        move(net.minecraft.world.entity.MoverType.SELF, getDeltaMovement());

        if (!level().isClientSide) {
            AABB exhaustZone = new AABB(getX() - 5, getY() - 15, getZ() - 5, getX() + 5, getY(), getZ() + 5);
            List<Entity> caught = level().getEntities(this, exhaustZone);
            for (Entity e : caught) {
                e.setSecondsOnFire(15);
                DamageSource exhaust = ModDamageSources.exhaust(level());
                e.hurt(exhaust, 100.0F);
                firedOnce = true;
            }
        } else {
            spawnExhaust(getX(), getY(), getZ());
            spawnExhaust(getX() + 2.75, getY(), getZ());
            spawnExhaust(getX() - 2.75, getY(), getZ());
            spawnExhaust(getX(), getY(), getZ() + 2.75);
            spawnExhaust(getX(), getY(), getZ() - 2.75);
            spawnEngineFlames();
        }

        if (getY() > DEPLOY_HEIGHT) {
            deployPayload();
        }
    }

    private void spawnExhaust(double x, double y, double z) {
        if (!(level().isClientSide)) return;
        level().addParticle(ModParticleTypes.SMOKE_COLUMN.get(), x, y, z, 0.0D, -0.05D, 0.0D);
    }

    /** Big yellow engine glare at the base of the rocket, for the whole ascent (until deployPayload). */
    private void spawnEngineFlames() {
        if (!(level() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel)) return;

        Vec3 step = new Vec3(getX() - xOld, getY() - yOld, getZ() - zOld);
        double baseY = getY();
        final float scale = 3.5F;
        double[][] offsets = {
                {0.0, 0.0}, {2.75, 0.0}, {-2.75, 0.0}, {0.0, 2.75}, {0.0, -2.75}
        };
        for (double[] off : offsets) {
            com.hbm_m.client.missile.track.MissileNozzleFlare.spawn(
                    clientLevel, getX() + off[0], baseY, getZ() + off[1], 0.0F, 0.0F, step, scale);
        }
    }

    private void deployPayload() {
        if (level().isClientSide) {
            this.discard();
            return;
        }

        if (mode == 0) {
            // Satellite mode: consume whatever chip was placed in the satellite slot.
            ItemStack chip = payload.isEmpty() ? ItemStack.EMPTY : payload.get(0);
            if (!chip.isEmpty() && chip.is(ModItems.FLAME_PONY.get()) && level() instanceof ServerLevel server) {
                for (var player : server.players()) {
                    server.sendParticles(net.minecraft.core.particles.ParticleTypes.FIREWORK,
                            getX(), getY(), getZ(), 40, 1.0, 1.0, 1.0, 0.1);
                }
            } else if (!chip.isEmpty() && chip.getItem() instanceof com.hbm_m.item.ISatChip
                    && level() instanceof ServerLevel server) {
                com.hbm_m.satellite.SatelliteManager.get(server).orbit(
                        server, com.hbm_m.item.ISatChip.getFreqS(chip), chip.getItem(), getX(), getY(), getZ());
            }
        } else if (mode == 1 && level() instanceof ServerLevel server) {
            SoyuzCapsuleEntity capsule = ModEntities.SOYUZ_CAPSULE.get().create(server);
            if (capsule != null) {
                capsule.setPayload(payload);
                capsule.setPos(targetX + 0.5, DEPLOY_HEIGHT, targetZ + 0.5);
                server.getChunkSource().addRegionTicket(net.minecraft.server.level.TicketType.FORCED,
                        new net.minecraft.world.level.ChunkPos(targetX >> 4, targetZ >> 4), 2, net.minecraft.world.level.ChunkPos.ZERO);
                server.addFreshEntity(capsule);
            }
        }

        this.discard();
    }

    //? if < 1.21.1 {

    @Override
    protected void defineSynchedData() {
        // No synced fields needed - mode/skin only matter server-side and for our own render pose.
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {

        // No synced fields needed - mode/skin only matter server-side and for our own render pose.
    
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.mode = tag.getInt("mode");
        this.targetX = tag.getInt("targetX");
        this.targetZ = tag.getInt("targetZ");
        this.acceleration = tag.getDouble("acceleration");

        ListTag list = tag.getList("payload", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag itemTag = list.getCompound(i);
            int slot = itemTag.getInt("Slot");
            if (slot >= 0 && slot < payload.size()) {
                payload.set(slot, ItemStack.of(itemTag));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("mode", mode);
        tag.putInt("targetX", targetX);
        tag.putInt("targetZ", targetZ);
        tag.putDouble("acceleration", acceleration);

        ListTag list = new ListTag();
        for (int i = 0; i < payload.size(); i++) {
            ItemStack stack = payload.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putInt("Slot", i);
                stack.save(itemTag);
                list.add(itemTag);
            }
        }
        tag.put("payload", list);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500_000.0D * 500_000.0D;
    }

    @Override
    public boolean fireImmune() {
        return true;
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
