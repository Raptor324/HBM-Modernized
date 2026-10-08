package com.hbm_m.entity.projectile;

import com.hbm_m.item.weapon.ItemAmmoArty;
import com.hbm_m.item.weapon.ItemAmmoArty.ArtilleryShell;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.MovingObjectPosition;

import api.hbm_m.entity.IRadarDetectable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntityArtilleryShell}: Granate der Arty (Typ = Meta von {@code ammo_arty}, siehe {@link ItemAmmoArty}).
 * Der Server simuliert, der Client interpoliert zur Serverposition ({@link EntityThrowableInterp}) und raucht,
 * sobald er sie erreicht hat. Pfeift einmal am Ziel, wenn sie in weniger als 18 Ticks dort ist. Haelt ihren Chunk geladen.
 */
public class EntityArtilleryShell extends EntityThrowableInterp implements IRadarDetectable {

    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(EntityArtilleryShell.class, EntityDataSerializers.INT);

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean shouldWhistle = false;
    private boolean didWhistle = false;

    private ItemStack cargo = ItemStack.EMPTY;

    public EntityArtilleryShell(EntityType<? extends EntityArtilleryShell> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(TYPE, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(TYPE, 0);
    }
    *///?}

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryShell setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public ArtilleryShell getShellType() {
        try {
            return ItemAmmoArty.itemTypes[this.entityData.get(TYPE)];
        } catch (Exception ex) {
            return ItemAmmoArty.itemTypes[0];
        }
    }

    public double[] getTarget() {
        return new double[] { this.targetX, this.targetY, this.targetZ };
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public double getTargetHeight() {
        return this.targetY;
    }

    public void setWhistle(boolean whistle) {
        this.shouldWhistle = whistle;
    }

    public boolean getWhistle() {
        return this.shouldWhistle;
    }

    public boolean didWhistle() {
        return this.didWhistle;
    }

    @Override
    public void tick() {

        if (!level().isClientSide) {
            super.tick();
            if (this.isRemoved()) return;

            if (!didWhistle && this.shouldWhistle) {
                Vec3 m = this.getDeltaMovement();
                double speed = Math.sqrt(m.x * m.x + m.z * m.z);
                double deltaX = this.getX() - this.targetX;
                double deltaZ = this.getZ() - this.targetZ;
                double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (speed * 18 > dist) {
                    level().playSound(null, this.targetX, this.targetY, this.targetZ, HbmSoundsNT.get("hbm:turret.mortarWhistle"), SoundSource.NEUTRAL, 15.0F, 0.9F + random.nextFloat() * 0.2F);
                    this.didWhistle = true;
                }
            }

            ArtilleryChunkLoader.load(this, (int) Math.floor(getX() / 16D), (int) Math.floor(getZ() / 16D));
            this.getShellType().onUpdate(this);

        } else {
            super.tick();

            if (new Vec3(this.syncPosX - this.getX(), this.syncPosY - this.getY(), this.syncPosZ - this.getZ()).length() < 0.2) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.5, getZ(), 0.0, 0.1, 0.0);
            }
        }
    }

    @Override
    protected void onImpact(HitResult hit) {

        if (!level().isClientSide) {

            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof EntityArtilleryShell) return;
            MovingObjectPosition mop = MovingObjectPosition.of(hit);
            if (mop != null) this.getShellType().onImpact(this, mop);
        }
    }

    public void killAndClear() {
        this.discard();
        this.clearChunkLoader();
    }

    public void clearChunkLoader() {
        if (!level().isClientSide) ArtilleryChunkLoader.release(this);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // ForgeChunkManager gab das Ticket mit der gebundenen Entity frei
        clearChunkLoader();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);

        nbt.putInt("type", this.entityData.get(TYPE));
        nbt.putBoolean("shouldWhistle", this.shouldWhistle);
        nbt.putBoolean("didWhistle", this.didWhistle);
        nbt.putDouble("targetX", this.targetX);
        nbt.putDouble("targetY", this.targetY);
        nbt.putDouble("targetZ", this.targetZ);

        if (!this.cargo.isEmpty())
            nbt.put("cargo", PlatformHooks.saveItemStack(this.cargo, new CompoundTag(), PlatformHooks.bestEffortProvider()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);

        this.entityData.set(TYPE, nbt.getInt("type"));
        this.shouldWhistle = nbt.getBoolean("shouldWhistle");
        this.didWhistle = nbt.getBoolean("didWhistle");
        this.targetX = nbt.getDouble("targetX");
        this.targetY = nbt.getDouble("targetY");
        this.targetZ = nbt.getDouble("targetZ");

        this.setCargo(nbt.contains("cargo") ? PlatformHooks.itemStackOf(nbt.getCompound("cargo"), PlatformHooks.bestEffortProvider()) : ItemStack.EMPTY);
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    public double getGravityVelocity() {
        return 9.81 * 0.05;
    }

    @Override
    protected int groundDespawn() {
        return !cargo.isEmpty() ? 0 : 1200;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    public void setCargo(ItemStack stack) {
        this.cargo = stack == null ? ItemStack.EMPTY : stack;
    }

    /** Original {@code interactFirst}: Fracht in das Inventar, Granate verschwindet. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {

        if (!level().isClientSide) {
            if (!this.cargo.isEmpty()) {
                player.getInventory().add(this.cargo.copy());
                player.inventoryMenu.broadcastChanges();
            }
            this.discard();
        }

        return InteractionResult.PASS;
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.ARTILLERY;
    }
}
