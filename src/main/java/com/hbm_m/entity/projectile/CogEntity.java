package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1-Port von {@code EntityCog} (1.7.10, {@code extends EntityThrowableInterp}): das Zahnrad, das
 * aus einem ueberdrehten Stirlingmotor fliegt.
 *
 * <p>Das Rad <b>toetet, was es trifft</b> (1000 Schaden, rubble), <b>sprengt beim Aufprall</b> auf
 * einen Block (Staerke 3, Bloecke unter Widerstand 50 zerbrechen) und prallt dabei ab - erst unter
 * 0.75 Tempo bleibt es liegen. Dann laesst es sich mit einem Rechtsklick aufsammeln.
 * Flugbahn: EntityThrowableNT mit Schwerkraft 0.03, Luftwiderstand 0.99.</p>
 */
public class CogEntity extends EntityThrowableInterp {

    /** Original: {@code dataWatcher} 10 - unter 6 fliegt es, ab 6 liegt es. */
    private static final EntityDataAccessor<Integer> ORIENTATION =
            SynchedEntityData.defineId(CogEntity.class, EntityDataSerializers.INT);

    /** Original: {@code dataWatcher} 11 - Bauart des Motors (0 normal, 1 Stahl, 2 kreativ). */
    private static final EntityDataAccessor<Integer> META =
            SynchedEntityData.defineId(CogEntity.class, EntityDataSerializers.INT);

    public CogEntity(EntityType<? extends CogEntity> type, Level level) {
        super(type, level);
    }

    public static CogEntity create(Level level, double x, double y, double z, Direction facing) {
        CogEntity cog = new CogEntity(ModEntities.COG.get(), level);
        cog.setPos(x, y, z);
        cog.entityData.set(ORIENTATION, facing.ordinal());
        return cog;
    }

    /** Fuer abgeleitete Wurfteile ({@link SawbladeEntity}). */
    protected void setOrientationValue(int orientation) {
        entityData.set(ORIENTATION, orientation);
    }

    /** Was das liegende Teil beim Aufheben zurueckgibt ({@code gear_large} mit Meta). */
    protected ItemStack pickupStack() {
        return com.hbm_m.blockentity.machines.MachineStirlingBlockEntity.gearFor(getMeta());
    }

    public CogEntity setMeta(int meta) {
        entityData.set(META, meta);
        return this;
    }

    public int getMeta() { return entityData.get(META); }

    //? if < 1.21.1 {
    @Override
    protected void defineExtraData() {
        entityData.define(ORIENTATION, 0);
        entityData.define(META, 0);
    }
    //?} else {
    /*@Override
    protected void defineExtraData(SynchedEntityData.Builder builder) {
        builder.define(ORIENTATION, 0);
        builder.define(META, 0);
    }
    *///?}

    public int getOrientation() { return entityData.get(ORIENTATION); }

    /** Original: {@code orientation >= 6} - es liegt und dreht sich nicht mehr. */
    public boolean isResting() { return getOrientation() >= 6; }

    /** 1:1 {@code interactFirst}: aufheben gibt das Zahnrad zurueck. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide()) {
            if (player.getInventory().add(pickupStack())) {
                discard();
            }
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (mop instanceof EntityHitResult ehr && ehr.getEntity().isAlive()) {
            Entity e = ehr.getEntity();
            //? if < 1.21.1 {
            e.hurt(com.hbm_m.damagesource.ModDamageSources.rubble(level()), 1000);
            //?} else {
            /*if (level() instanceof ServerLevel serverLevel) e.hurt(serverLevel, com.hbm_m.damagesource.ModDamageSources.rubble(level()), 1000);
            *///?}
            if (!e.isAlive() && e instanceof LivingEntity && level() instanceof ServerLevel sl) {
                CompoundTag vdat = new CompoundTag();
                vdat.putString("type", "giblets");
                vdat.putInt("ent", e.getId());
                vdat.putInt("cDiv", 5);
                com.hbm_m.particle.helper.IParticleCreator.sendPacket(sl, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 150, vdat);

                // "mob.zombie.woodbreak"
                level().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.95F + level().random.nextFloat() * 0.2F);
            }
        }

        if (this.tickCount > 1 && mop instanceof BlockHitResult bhr) {

            int orientation = this.entityData.get(ORIENTATION);

            if (orientation < 6) {

                Vec3 motion = getDeltaMovement();
                if (motion.length() < 0.75) {
                    this.entityData.set(ORIENTATION, orientation + 6);
                    orientation += 6;
                } else {
                    Direction side = bhr.getDirection();
                    setDeltaMovement(
                            motion.x * (1 - (Math.abs(side.getStepX()) * 2)),
                            motion.y * (1 - (Math.abs(side.getStepY()) * 2)),
                            motion.z * (1 - (Math.abs(side.getStepZ()) * 2)));
                    if (!level().isClientSide()) {
                        // createExplosion(this, ..., 3F, false): ohne Feuer, mit Blockschaden
                        level().explode(this, getX(), getY(), getZ(), 3F, Level.ExplosionInteraction.TNT);

                        BlockPos bp = bhr.getBlockPos();
                        if (level().getBlockState(bp).getBlock().getExplosionResistance() < 50) {
                            level().destroyBlock(bp, false);
                        }
                    }
                }
            }

            if (orientation >= 6) {
                setDeltaMovement(Vec3.ZERO);
                this.inGround = true;
            }
        }
    }

    @Override
    public void tick() {

        if (!level().isClientSide()) {
            int orientation = this.entityData.get(ORIENTATION);
            if (orientation >= 6 && !this.inGround) {
                this.entityData.set(ORIENTATION, orientation - 6);
            }
        }

        super.tick();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    public double getGravityVelocity() {
        return inGround ? 0 : 0.03D;
    }

    @Override
    protected int groundDespawn() {
        return 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("rot", this.getOrientation());
        nbt.putInt("meta", this.getMeta());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.entityData.set(ORIENTATION, nbt.getInt("rot"));
        this.entityData.set(META, nbt.getInt("meta"));
    }
}
