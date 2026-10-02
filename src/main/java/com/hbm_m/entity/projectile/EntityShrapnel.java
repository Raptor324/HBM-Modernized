package com.hbm_m.entity.projectile;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.explosion.ExplosionNT;
import com.hbm_m.explosion.ExplosionNT.ExAttrib;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 1:1-Port von {@code EntityShrapnel} (1.7.10): Splitter aus Explosionen. Typ im Datenwort 16:
 * 0 = normal, 1 = mit Flammenspur, 2 = Vulkanbrocken, 3 = Watz-Schlamm, 4 = verstrahlter
 * Vulkanbrocken. Trifft ein Splitter nach mehr als fuenf Ticks, verschwindet er zischend; die
 * Vulkanvarianten setzen beim Fallen Lava plus Kohlenmonoxidwolke, beim Steigen eine kleine
 * {@link ExplosionNT} mit Lava-Attribut; der Watz-Splitter hinterlaesst giftigen Schlamm.
 * Wird nie gespeichert (Original: {@code writeToNBTOptional = false}).
 */
public class EntityShrapnel extends ThrowableProjectile {

    private static final EntityDataAccessor<Byte> TYPE = SynchedEntityData.defineId(EntityShrapnel.class, EntityDataSerializers.BYTE);

    public EntityShrapnel(EntityType<? extends EntityShrapnel> type, Level level) {
        super(type, level);
    }

    public EntityShrapnel(Level level) {
        this(ModEntities.SHRAPNEL.get(), level);
    }

    public EntityShrapnel(Level level, LivingEntity thrower) {
        super(ModEntities.SHRAPNEL.get(), thrower, level);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(TYPE, (byte) 0);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    public byte getShrapnelType() {
        return this.entityData.get(TYPE);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && getShrapnelType() == 1) {
            level().addParticle(ParticleTypes.FLAME, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        result.getEntity().hurt(ModDamageSources.shrapnel(level()), 15);
    }

    @Override
    protected void onHit(HitResult mop) {
        super.onHit(mop);

        if (this.tickCount > 5) {
            if (!level().isClientSide) this.discard();

            int b = getShrapnelType();
            BlockPos hit = mop instanceof BlockHitResult bhr ? bhr.getBlockPos() : BlockPos.containing(mop.getLocation());

            if (b == 2 || b == 4) {
                if (!level().isClientSide) {
                    if (getDeltaMovement().y < -0.2D) {
                        BlockPos above = hit.above();
                        if (level().getBlockState(above).canBeReplaced()) {
                            level().setBlockAndUpdate(above, (b == 2 ? ModBlocks.VOLCANIC_LAVA_BLOCK : ModBlocks.RAD_LAVA_BLOCK).get().defaultBlockState());
                        }
                        for (int x = hit.getX() - 1; x <= hit.getX() + 1; x++) {
                            for (int y = hit.getY(); y <= hit.getY() + 2; y++) {
                                for (int z = hit.getZ() - 1; z <= hit.getZ() + 1; z++) {
                                    BlockPos p = new BlockPos(x, y, z);
                                    if (level().getBlockState(p).isAir()) level().setBlockAndUpdate(p, ModBlocks.GAS_MONOXIDE.get().defaultBlockState());
                                }
                            }
                        }
                    }
                    if (getDeltaMovement().y > 0) {
                        ExplosionNT explosion = new ExplosionNT(level(), null, hit.getX() + 0.5, hit.getY() + 0.5, hit.getZ() + 0.5, 7);
                        explosion.addAttrib(ExAttrib.NODROP);
                        explosion.addAttrib(b == 2 ? ExAttrib.LAVA_V : ExAttrib.LAVA_R);
                        explosion.addAttrib(ExAttrib.NOSOUND);
                        explosion.addAttrib(ExAttrib.ALLMOD);
                        explosion.addAttrib(ExAttrib.NOHURT);
                        explosion.explode();
                    }
                }
            } else if (b == 3) {
                BlockPos above = hit.above();
                if (level().getBlockState(above).canBeReplaced()) {
                    level().setBlockAndUpdate(above, ModBlocks.MUD_BLOCK.get().defaultBlockState());
                }
            } else {
                for (int i = 0; i < 5; i++) level().addParticle(ParticleTypes.LAVA, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }

            level().playSound(null, getX(), getY(), getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    public void setTrail(boolean b) { this.entityData.set(TYPE, (byte) (b ? 1 : 0)); }
    public void setVolcano(boolean b) { this.entityData.set(TYPE, (byte) (b ? 2 : 0)); }
    public void setWatz(boolean b) { this.entityData.set(TYPE, (byte) (b ? 3 : 0)); }
    public void setRadVolcano(boolean b) { this.entityData.set(TYPE, (byte) (b ? 4 : 0)); }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.discard();
    }
}
