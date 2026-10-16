package com.hbm_m.entity.projectile;

import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.effect.EntityMist;
import com.hbm_m.entity.logic.EntityNukeExplosionMK5;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityBombletZeta} - the free-falling bomblet a bomber drops by the dozen.
 *
 * <p>It has no impact handler: it simply checks whether the block it is currently inside is solid
 * and detonates if so, which is why it goes off on contact with anything rather than only on the
 * face it hit. Four payloads, selected by {@link #setBombType}:</p>
 *
 * <ul>
 *   <li>0 - plain 4-power blast (carpet bombing)</li>
 *   <li>1 - the same, but it sets fire to what it destroys (napalm)</li>
 *   <li>2 - a chlorine gas cloud</li>
 *   <li>4 - a full nuclear detonation</li>
 * </ul>
 *
 * <p>Blowing up an oil refinery or a tank of something flammable with one of these is the
 * original's {@code achInferno}; see {@code MachineRefineryBlock.onBlockExploded}.</p>
 */
public class EntityBombletZeta extends Projectile {

    private static final EntityDataAccessor<Integer> BOMB_TYPE =
            SynchedEntityData.defineId(EntityBombletZeta.class, EntityDataSerializers.INT);

    public static final int TYPE_CARPET   = 0;
    public static final int TYPE_NAPALM   = 1;
    public static final int TYPE_CHLORINE = 2;
    public static final int TYPE_NUKE     = 4;

    /** {@code BombConfig.fatmanRadius * 1.5} in the original. */
    private static final int NUKE_RADIUS = 75;

    public EntityBombletZeta(EntityType<? extends EntityBombletZeta> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public static EntityBombletZeta create(Level level, double x, double y, double z, int bombType) {
        EntityBombletZeta zeta = new EntityBombletZeta(ModEntities.BOMBLET_ZETA.get(), level);
        zeta.setPos(x, y, z);
        zeta.setBombType(bombType);
        return zeta;
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        this.entityData.define(BOMB_TYPE, TYPE_CARPET);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(BOMB_TYPE, TYPE_CARPET);
    }
    *///?}

    public int getBombType()             { return this.entityData.get(BOMB_TYPE); }
    public void setBombType(int type)    { this.entityData.set(BOMB_TYPE, type); }

    @Override
    public void tick() {
        this.xOld = this.xo = this.getX();
        this.yOld = this.yo = this.getY();
        this.zOld = this.zo = this.getZ();

        Vec3 motion = this.getDeltaMovement();
        this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);

        // Horizontal drag, constant gravity - it does not glide.
        this.setDeltaMovement(motion.x * 0.99D, motion.y - 0.05D, motion.z * 0.99D);
        updateRotation();

        // No collision handler: it just asks whether it is inside something solid yet.
        BlockPos pos = BlockPos.containing(this.getX(), this.getY(), this.getZ());
        if (!this.level().getBlockState(pos).isAir()) {
            if (!this.level().isClientSide) {
                detonate();
                this.discard();
            }
        }
    }

    private void detonate() {
        double x = this.getX() + 0.5F;
        double y = this.getY() + 1.5F;
        double z = this.getZ() + 0.5F;

        switch (getBombType()) {
            case TYPE_NAPALM -> {
                // The original's BlockMutatorFire: the blast leaves the crater burning.
                this.level().explode(this, x, y, z, 4F, true, Level.ExplosionInteraction.MOB);
            }
            case TYPE_CHLORINE -> {
                this.level().playSound(null, x, y, z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS,
                        5.0F, 2.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.8F);

                EntityMist mist = new EntityMist(ModEntities.ENTITY_MIST.get(), this.level());
                mist.setFluidType(FluidType.forFluid(ModFluids.CHLORINE.getSource()));
                Vec3 motion = this.getDeltaMovement();
                mist.setPos(this.getX() - motion.x, this.getY() - motion.y, this.getZ() - motion.z);
                mist.setArea(15, 7.5F);
                this.level().addFreshEntity(mist);
            }
            case TYPE_NUKE -> {
                // start() spawns the explosion itself, so there is nothing to add here.
                EntityNukeExplosionMK5.start(this.level(), NUKE_RADIUS,
                        this.getX(), this.getY(), this.getZ());
                this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                        com.hbm_m.sound.ModSounds.MUKE_EXPLOSION.get(), SoundSource.BLOCKS, 15.0F, 1.0F);
            }
            default -> this.level().explode(this, x, y, z, 4F, Level.ExplosionInteraction.MOB);
        }
    }

    /** {@code rotation()}: points the bomb along its own arc so it noses over as it falls. */
    public void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        float flat = (float) Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        this.setYRot((float) (Math.atan2(motion.x, motion.z) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(motion.y, flat) * 180.0D / Math.PI) - 90F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override protected void onHit(@NotNull HitResult result) { }
    @Override public boolean shouldRenderAtSqrDistance(double distance) { return distance < 25000 * 25000; }
    @Override protected boolean canHitEntity(@NotNull Entity entity) { return false; }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("bombType", getBombType());
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setBombType(tag.getInt("bombType"));
    }
}
