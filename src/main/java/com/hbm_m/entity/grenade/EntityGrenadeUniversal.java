package com.hbm_m.entity.grenade;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.projectile.EntityThrowableInterp;
import com.hbm_m.item.weapon.grenade.ItemGrenadeExtra.EnumGrenadeExtra;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm_m.item.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm_m.item.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm_m.item.weapon.grenade.ItemGrenadeUniversal;
import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.ForgeDirection;
import com.hbm_m.util.MovingObjectPosition;
import com.hbm_m.util.TrackerUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** 1:1 {@code EntityGrenadeUniversal}: geworfene Baukastengranate; Verhalten kommt aus Zuender, Fuellung und Extra. */
public class EntityGrenadeUniversal extends EntityThrowableInterp {

    private static final EntityDataAccessor<ItemStack> DW_GRENADE = SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DW_BOUNCES = SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DW_TRAIL = SynchedEntityData.defineId(EntityGrenadeUniversal.class, EntityDataSerializers.INT);

    public static final int TRAIL_TRIPLET = 1;

    public double prevSpin;
    public double spin;

    public EntityGrenadeUniversal(EntityType<? extends EntityGrenadeUniversal> type, Level world) {
        super(type, world);
    }

    public EntityGrenadeUniversal(Level world, ItemStack grenade) {
        super(ModEntities.GRENADE_UNIVERSAL.get(), world);
        ItemStack copy = grenade.copy();
        copy.setCount(1);
        this.entityData.set(DW_GRENADE, copy);
    }

    public EntityGrenadeUniversal(Level world, Player thrower, ItemStack grenade) {
        super(ModEntities.GRENADE_UNIVERSAL.get(), world);
        this.setThrower(thrower);
        ItemStack copy = grenade.copy();
        copy.setCount(1);
        this.entityData.set(DW_GRENADE, copy);

        Vec3NT offset = new Vec3NT(0.25, -0.25, 0).rotateAroundYDeg(-thrower.getYRot() + 180);

        this.setPos(thrower.getX() + offset.xCoord, thrower.getY() + thrower.getEyeHeight() + offset.yCoord, thrower.getZ() + offset.zCoord);

        EnumGrenadeShell shell = ItemGrenadeUniversal.getShell(grenade);

        Vec3NT yeet = new Vec3NT(thrower.getLookAngle()).normalizeSelf();
        this.setThrowableHeading(yeet.xCoord, yeet.yCoord, yeet.zCoord, (float) shell.getYeetForce(), 0);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        this.entityData.define(DW_GRENADE, ItemStack.EMPTY);
        this.entityData.define(DW_BOUNCES, 0);
        this.entityData.define(DW_TRAIL, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DW_GRENADE, ItemStack.EMPTY);
        builder.define(DW_BOUNCES, 0);
        builder.define(DW_TRAIL, 0);
    }
    *///?}

    public EntityGrenadeUniversal setTrail(int trail) {
        this.entityData.set(DW_TRAIL, trail);
        return this;
    }

    /** Original {@code setPosition(x, y, z)}. */
    public void setPosition(double x, double y, double z) { this.setPos(x, y, z); }

    public ItemStack getGrenadeItem() { return this.entityData.get(DW_GRENADE); }
    public int getBounces() { return this.entityData.get(DW_BOUNCES); }
    public int getTrail() { return this.entityData.get(DW_TRAIL); }

    public EnumGrenadeShell getShell() { return ItemGrenadeUniversal.getShell(getGrenadeItem()); }
    public EnumGrenadeFilling getFilling() { return ItemGrenadeUniversal.getFilling(getGrenadeItem()); }
    public EnumGrenadeFuze getFuze() { return ItemGrenadeUniversal.getFuze(getGrenadeItem()); }
    public EnumGrenadeExtra getExtra() { return ItemGrenadeUniversal.getExtra(getGrenadeItem()); }

    @Override
    public void tick() {
        super.tick();
        if (this.isRemoved()) return;

        EnumGrenadeFuze fuze = this.getFuze();
        EnumGrenadeExtra extra = this.getExtra();

        if (fuze.updateTick != null) fuze.updateTick.accept(this);
        if (extra != null && extra.updateTick != null) extra.updateTick.accept(this);

        if (level().isClientSide) {
            this.prevSpin = this.spin;

            if (this.getBounces() <= 0) {
                this.spin += 15;
            } else {
                this.spin += Math.min(15, new Vec3NT(xo - getX(), 0, zo - getZ()).lengthVector() * 50);
            }

            if (this.spin >= 360) {
                this.prevSpin -= 360;
                this.spin -= 360;
            }

            if (this.getTrail() == TRAIL_TRIPLET) {
                CompoundTag data = new CompoundTag();
                data.putDouble("posX", getX());
                data.putDouble("posY", getY());
                data.putDouble("posZ", getZ());
                data.putString("type", "vanillaExt");
                data.putString("mode", "flame");
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(data);
            }
        }
    }

    @Override
    protected void onImpact(HitResult hit) {
        MovingObjectPosition mop = MovingObjectPosition.of(hit);
        if (mop == null) return;
        EnumGrenadeFuze fuze = this.getFuze();
        EnumGrenadeExtra extra = this.getExtra();

        if (fuze.onImpact != null) fuze.onImpact.accept(this, mop);
        if (extra != null && extra.onImpact != null) extra.onImpact.accept(this, mop);

        if (this.isRemoved()) return; // we assume the grenade has gone off by this point

        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            ForgeDirection dir = ForgeDirection.getOrientation(mop.sideHit);
            this.setPos(mop.hitVec.xCoord + dir.offsetX * 0.05, mop.hitVec.yCoord + dir.offsetY * 0.05, mop.hitVec.zCoord + dir.offsetZ * 0.05);
            EnumGrenadeShell shell = this.getShell();
            var m = this.getDeltaMovement();
            Vec3NT vec = new Vec3NT(m.x, m.y, m.z);
            if (vec.lengthVector() > 0.2) {
                level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.grenadeBounce"), SoundSource.NEUTRAL, 1F, 1F);
            }
            double mx = m.x, my = m.y, mz = m.z;
            if (dir.offsetX != 0) mx *= -shell.getBounce(); else mx *= 0.8;
            if (dir.offsetY != 0) my *= -shell.getBounce(); else my *= 0.8;
            if (dir.offsetZ != 0) mz *= -shell.getBounce(); else mz *= 0.8;
            this.setDeltaMovement(mx, my, mz);
            TrackerUtil.sendTeleport(level(), this);
            this.entityData.set(DW_BOUNCES, this.getBounces() + 1);
        }
    }

    public void explode() {
        this.discard();
        EnumGrenadeFilling filling = this.getFilling();
        if (filling.explode != null) filling.explode.accept(this);
        EnumGrenadeExtra extra = this.getExtra();
        if (extra != null && extra.onExplode != null) extra.onExplode.accept(this);

        if (ModClothConfig.get().enableExtendedLogging) {
            String s = "null";
            if (getThrower() instanceof Player p) s = p.getDisplayName().getString();
            MainRegistry.LOGGER.info("[GREN] Set off grenade at " + ((int) getX()) + " / " + ((int) getY()) + " / " + ((int) getZ()) + " by " + s + "!");
        }
    }

    public int getTimer() { return this.ticksInAir + this.ticksInGround; }

    @Override protected int groundDespawn() { return 0; }
    @Override public boolean fullBlockCollisions() { return true; }
}
