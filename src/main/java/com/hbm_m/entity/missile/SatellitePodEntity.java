package com.hbm_m.entity.missile;

import java.util.Arrays;

import com.hbm_m.blockentity.machines.MachineSatDockBlockEntity;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.util.ParticleUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code EntitySatellitePod} (Landekapsel): bringt die fertige Fracht eines Satelliten zu dem Sat-Dock, das sie
 * angefordert hat. Bremst ab 25 Bloecke ueber dem Dock, faehrt bei 17 Bloecken die Beine aus, entlaedt 5 Sekunden
 * nach dem Aufsetzen in die Dockplaetze 0-14 (Rest faellt heraus) und steigt danach leer wieder bis Hoehe 300 auf.
 * Ab 5 Schaden explodiert sie.
 */
public class SatellitePodEntity extends Entity {

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(SatellitePodEntity.class, EntityDataSerializers.INT);
    public static final int STATE_LEGS_UP = 0;
    public static final int STATE_LEGS_DOWN = 1;

    public ItemStack[] slots = new ItemStack[0];

    /** Wartezeit nach dem Aufsetzen bis zum Entladen. */
    public int timer = 0;
    /** Hoehe des anfordernden Docks, zum rechtzeitigen Bremsen. */
    public int callerYPos;
    public double speed = 0.75D;

    public float legs = 0F;
    public float prevLegs = 0F;
    public static final float LEG_SPEED = 1F / 20F;

    public SatellitePodEntity(EntityType<? extends SatellitePodEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public SatellitePodEntity setup(int caller, ItemStack... cargo) {
        this.callerYPos = caller;
        this.slots = Arrays.stream(cargo).map(s -> s == null ? ItemStack.EMPTY : s.copy()).toArray(ItemStack[]::new);
        return this;
    }

    public boolean doesDeployLegs() { return this.entityData.get(STATE) == STATE_LEGS_DOWN; }
    public void setDeployLegs(boolean deploy) { this.entityData.set(STATE, deploy ? STATE_LEGS_DOWN : STATE_LEGS_UP); }

    public boolean isLanding() { return this.slots.length > 0; }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean fireImmune() { return true; }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) return false;
        if (amount >= 5F && !level().isClientSide && !isRemoved()) {
            this.discard();
            // Original ExplosionVNT 15 ohne Blockschaden, nur Wesen
            level().explode(this, getX(), getY() + 1.5, getZ(), 7.5F, Level.ExplosionInteraction.NONE);
        }
        return true;
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {

            if (this.isLanding()) {
                if (this.timer > 0) {
                    // entladen
                    this.timer++;
                    this.setPos(getX(), Math.ceil(getY()), getZ());
                    if (this.timer >= 100) this.unloadItems();
                } else if (this.onGround()) {
                    // gerade gelandet
                    this.speed = 0D;
                    this.timer = 1;
                    this.setDeployLegs(true);
                } else {
                    // Sinkflug
                    if (getY() < this.callerYPos + 17 && !this.doesDeployLegs()) this.setDeployLegs(true);
                    if (getY() < this.callerYPos + 25) this.speed -= 0.01;
                    this.speed = Mth.clamp(this.speed, 0.025D, 0.75D);
                }
                this.setDeltaMovement(0, -this.speed, 0);

            } else {
                this.setOnGround(false);
                this.speed += 0.01;
                if (this.speed >= 0.2) this.setDeployLegs(false);
                this.speed = Mth.clamp(this.speed, 0D, 2D);
                this.setDeltaMovement(0, this.speed, 0);
                if (getY() > 300) this.discard();
            }

        } else {
            this.prevLegs = this.legs;
            this.legs += doesDeployLegs() ? LEG_SPEED : -LEG_SPEED;
            this.legs = Mth.clamp(this.legs, 0F, 1F);

            // Client: Bewegung kommt per Positionsabgleich, die Geschwindigkeit des Originals ist dort nie gesetzt (0,75)
            double dy = getY() - this.yo;
            if (this.legs > 0 && dy < 0 || dy > 0) {
                ParticleUtil.spawnGasFlame(level(), getX(), getY() + 0.5, getZ(), 0, this.speed - 1, 0);
            }
        }

        // Keine Schwerkraft, kein Luftwiderstand: die Kapsel faehrt nur ihre eigene Geschwindigkeit
        if (!level().isClientSide) {
            Vec3 m = getDeltaMovement();
            this.move(MoverType.SELF, m);
            if (m.y < 0 && this.verticalCollisionBelow) this.setOnGround(true);
        }
    }

    /** Fuellt ein Sat-Dock unter der Kapsel; was nicht passt, faellt heraus. */
    public void unloadItems() {
        BlockPos pos = BlockPos.containing(getX(), getY() - 0.5, getZ());

        if (level().getBlockEntity(pos) instanceof MachineSatDockBlockEntity dock) {
            for (int i = 0; i < this.slots.length; i++) {
                if (slots[i].isEmpty()) continue;
                slots[i] = dock.tryAddToInventory(slots[i]);
            }
        }

        for (ItemStack stack : this.slots) {
            if (!stack.isEmpty()) this.spawnAtLocation(stack, 0.25F);
        }

        this.slots = new ItemStack[0];
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(STATE, STATE_LEGS_UP);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(STATE, STATE_LEGS_UP);
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        this.entityData.set(STATE, nbt.getInt("state"));
        timer = nbt.getInt("timer");
        callerYPos = nbt.getInt("callerYPos");
        speed = nbt.getDouble("speed");

        int itemCount = nbt.getInt("itemCount");
        ListTag items = nbt.getList("items", 10);
        this.slots = new ItemStack[itemCount];
        Arrays.fill(this.slots, ItemStack.EMPTY);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = items.getCompound(i);
            int j = itemTag.getByte("slot") & 255;
            if (j < this.slots.length) this.slots[j] = PlatformHooks.itemStackOf(itemTag, PlatformHooks.bestEffortProvider());
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("state", this.entityData.get(STATE));
        nbt.putInt("timer", timer);
        nbt.putInt("callerYPos", callerYPos);
        nbt.putDouble("speed", speed);

        nbt.putInt("itemCount", this.slots.length);
        ListTag items = new ListTag();
        for (int i = 0; i < this.slots.length; i++) {
            if (this.slots[i].isEmpty()) continue;
            CompoundTag itemTag = new CompoundTag();
            itemTag.putByte("slot", (byte) i);
            PlatformHooks.saveItemStack(this.slots[i], itemTag, PlatformHooks.bestEffortProvider());
            items.add(itemTag);
        }
        nbt.put("items", items);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 500000;
    }
}
