package com.hbm_m.entity.grenades;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 1:1 {@code com.hbm.entity.grenade.EntityGrenadeBouncyBase}: geworfene Granate, die von Bloecken abprallt
 * (Geschwindigkeit x {@link #getBounceMod}) und nach {@link #getMaxTimer} Ticks explodiert.
 */
public abstract class EntityGrenadeBouncyBase extends Entity {

    @Nullable protected LivingEntity thrower;
    @Nullable protected UUID throwerId;
    protected int timer = 0;

    protected EntityGrenadeBouncyBase(EntityType<? extends EntityGrenadeBouncyBase> type, Level world) {
        super(type, world);
    }

    protected EntityGrenadeBouncyBase(EntityType<? extends EntityGrenadeBouncyBase> type, Level world, LivingEntity living) {
        this(type, world);
        this.thrower = living;
        this.throwerId = living.getUUID();
        float yaw = living.getYRot(), pitch = living.getXRot();
        double x = living.getX() - Mth.cos(yaw / 180.0F * (float) Math.PI) * 0.16F;
        double y = living.getEyeY() - 0.10000000149011612D;
        double z = living.getZ() - Mth.sin(yaw / 180.0F * (float) Math.PI) * 0.16F;
        this.moveTo(x, y, z, yaw, pitch);
        float f = 0.4F;
        double mx = -Mth.sin(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * f;
        double mz = Mth.cos(yaw / 180.0F * (float) Math.PI) * Mth.cos(pitch / 180.0F * (float) Math.PI) * f;
        double my = -Mth.sin((pitch + this.getThrowPitchOffset()) / 180.0F * (float) Math.PI) * f;
        this.setThrowableHeading(mx, my, mz, this.getThrowSpeed(), 1.0F);
        this.setXRot(0);
        this.xRotO = 0;
    }

    protected EntityGrenadeBouncyBase(EntityType<? extends EntityGrenadeBouncyBase> type, Level world, double x, double y, double z) {
        this(type, world);
        this.setPos(x, y, z);
    }

    @Override
    protected void defineSynchedData() { }

    /** {@code func_70182_d} */
    protected float getThrowSpeed() {
        return 1.5F;
    }

    /** {@code func_70183_g} */
    protected float getThrowPitchOffset() {
        return 0.0F;
    }

    protected float getGravityVelocity() {
        return 0.03F;
    }

    public void setThrowableHeading(double motionX, double motionY, double motionZ, float f0, float f1) {
        double f2 = Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= f2;
        motionY /= f2;
        motionZ /= f2;
        motionX += this.random.nextGaussian() * 0.007499999832361937D * f1;
        motionY += this.random.nextGaussian() * 0.007499999832361937D * f1;
        motionZ += this.random.nextGaussian() * 0.007499999832361937D * f1;
        motionX *= f0;
        motionY *= f0;
        motionZ *= f0;
        this.setDeltaMovement(motionX, motionY, motionZ);
        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        this.setYRot(yaw);
        this.yRotO = yaw;
    }

    @Override
    public void tick() {
        super.tick();
        this.xOld = this.getX();
        this.yOld = this.getY();
        this.zOld = this.getZ();
        Vec3 mot = this.getDeltaMovement();
        double motionX = mot.x, motionY = mot.y, motionZ = mot.z;
        this.xRotO = this.getXRot();
        this.setXRot((float) (this.getXRot() - mot.length() * 25));

        double posX = this.getX(), posY = this.getY(), posZ = this.getZ();

        //Bounce here
        boolean bounce = false;
        Vec3 vec3 = new Vec3(posX, posY, posZ);
        Vec3 vec31 = new Vec3(posX + motionX, posY + motionY, posZ + motionZ);
        BlockHitResult mop = this.level().clip(new ClipContext(vec3, vec31, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

        if (mop.getType() == HitResult.Type.BLOCK) {
            posX += (mop.getLocation().x - posX) * 0.6;
            posY += (mop.getLocation().y - posY) * 0.6;
            posZ += (mop.getLocation().z - posZ) * 0.6;

            switch (mop.getDirection().getAxis()) {
                case Y -> motionY *= -1;
                case Z -> motionZ *= -1;
                case X -> motionX *= -1;
            }

            bounce = true;

            if (new Vec3(motionX, motionY, motionZ).length() > 0.05)
                this.level().playSound(null, getX(), getY(), getZ(), HbmSoundsNT.get("hbm:weapon.gBounce"), SoundSource.NEUTRAL, 2.0F, 1.0F);

            motionX *= getBounceMod();
            motionY *= getBounceMod();
            motionZ *= getBounceMod();
        }
        //Bounce here [END]

        if (!bounce) {
            posX += motionX;
            posY += motionY;
            posZ += motionZ;
        }

        float yaw = (float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI);
        while (yaw - this.yRotO < -180.0F) this.yRotO -= 360.0F;
        while (yaw - this.yRotO >= 180.0F) this.yRotO += 360.0F;
        this.setYRot(this.yRotO + (yaw - this.yRotO) * 0.2F);

        float f2 = 0.99F;
        float f3 = this.getGravityVelocity();

        if (this.isInWater()) {
            for (int i = 0; i < 4; ++i) {
                float f4 = 0.25F;
                this.level().addParticle(ParticleTypes.BUBBLE, posX - motionX * f4, posY - motionY * f4, posZ - motionZ * f4, motionX, motionY, motionZ);
            }
            f2 = 0.8F;
        }

        motionX *= f2;
        motionY *= f2;
        motionZ *= f2;
        motionY -= f3;
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.setPos(posX, posY, posZ);

        timer++;

        if (timer >= getMaxTimer() && !this.level().isClientSide) {
            explode();
            if (com.hbm_m.config.ModClothConfig.get().enableExtendedLogging) {
                String s = "null";
                if (getThrower() instanceof Player p) s = p.getName().getString();
                MainRegistry.LOGGER.info("[GREN] Set off grenade at " + ((int) posX) + " / " + ((int) posY) + " / " + ((int) posZ) + " by " + s + "!");
            }
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        double d1 = this.getBoundingBox().getSize() * 4.0D;
        d1 *= 64.0D;
        return dist < d1 * d1;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag nbt) {
        timer = nbt.getInt("timer");
        if (nbt.hasUUID("thrower")) throwerId = nbt.getUUID("thrower");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag nbt) {
        nbt.putInt("timer", timer);
        if (throwerId != null) nbt.putUUID("thrower", throwerId);
    }

    @Nullable
    public LivingEntity getThrower() {
        if (this.thrower == null && this.throwerId != null && this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            if (server.getEntity(this.throwerId) instanceof LivingEntity l) this.thrower = l;
        }
        return this.thrower;
    }

    public abstract void explode();

    protected abstract int getMaxTimer();

    protected abstract double getBounceMod();
}
