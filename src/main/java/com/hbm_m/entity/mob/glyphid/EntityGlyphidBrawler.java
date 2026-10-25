package com.hbm_m.entity.mob.glyphid;

import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityGlyphidBrawler}: springt alle vier bis fuenf Sekunden auf sein Ziel zu. */
public class EntityGlyphidBrawler extends EntityGlyphid {

    public EntityGlyphidBrawler(EntityType<? extends EntityGlyphidBrawler> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getBrawler());
    }

    public int timer = 0;
    protected Entity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getEntityToAttack();
        if (e != null && this.isAlive()) {

            this.lastX = e.getX();
            this.lastY = e.getY();
            this.lastZ = e.getZ();

            if (--timer <= 0) {
                leap();
                timer = 80 + level().random.nextInt(30);
            }
        }
    }

    /** Mainly composed of repurposed bombardier code**/
    public void leap() {
        if (!level().isClientSide && entityToAttack instanceof LivingEntity && this.distanceTo(entityToAttack) < 20) {
            Entity e = this.getEntityToAttack();

            double velX = e.getX() - lastX;
            double velY = e.getY() - lastY;
            double velZ = e.getZ() - lastZ;

            if (this.lastTarget != e) {
                velX = velY = velZ = 0;
            }

            int prediction = 60;
            Vec3 delta = new Vec3(e.getX() - getX() + velX * prediction, (e.getY() + e.getBbHeight() / 2) - (getY() + 1) + velY * prediction, e.getZ() - getZ() + velZ * prediction);
            double len = delta.length();
            if (len < 3) return;
            double targetYaw = -Math.atan2(delta.x, delta.z);

            double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
            double y = delta.y;
            double v0 = 1.5;
            double v02 = v0 * v0;
            double g = 0.01;
            double targetPitch = Math.atan((v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * 1) / (g * x));
            Vec3 fireVec = null;
            if (!Double.isNaN(targetPitch)) {

                fireVec = new Vec3(v0, 0, 0).zRot((float) (-targetPitch / 3.5)).yRot((float) -(targetYaw + Math.PI * 0.5));
            }
            if (fireVec != null)
                this.setThrowableHeading(fireVec.x, fireVec.y, fireVec.z, (float) v0, random.nextFloat());
        }
    }

    //yeag this is now a motherfucking projectile
    public void setThrowableHeading(double motionX, double motionY, double motionZ, float velocity, float inaccuracy) {
        float throwLen = (float) Math.sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ);
        motionX /= throwLen;
        motionY /= throwLen;
        motionZ /= throwLen;
        motionX += this.random.nextGaussian() * 0.0075D * inaccuracy;
        motionY += this.random.nextGaussian() * 0.0075D * inaccuracy;
        motionZ += this.random.nextGaussian() * 0.0075D * inaccuracy;
        motionX *= velocity;
        motionY *= velocity;
        motionZ *= velocity;
        this.setDeltaMovement(motionX, motionY, motionZ);
        this.hurtMarked = true;
        float hyp = (float) Math.sqrt(motionX * motionX + motionZ * motionZ);
        this.setYRot((float) (Math.atan2(motionX, motionZ) * 180.0D / Math.PI));
        this.setXRot((float) (Math.atan2(motionY, hyp) * 180.0D / Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public String getSkinName() {
        return "glyphid_brawler";
    }

    @Override
    public double getGlyphidScale() {
        return 1.25D;
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBrawler;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        //allows brawlers to get no damage on short leaps, but still affected by fall damage on big drops
        if (source.is(DamageTypeTags.IS_FALL) && amount <= 10) return false;
        return super.hurt(source, amount);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return this.random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }
}
