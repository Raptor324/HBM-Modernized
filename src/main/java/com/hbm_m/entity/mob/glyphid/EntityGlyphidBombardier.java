package com.hbm_m.entity.mob.glyphid;

import com.hbm_m.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm_m.entity.projectile.EntityAcidBomb;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code EntityGlyphidBombardier}: wirft alle drei Sekunden fuenf Saeurekugeln auf vorausberechneter Flugbahn. */
public class EntityGlyphidBombardier extends EntityGlyphid {

    public EntityGlyphidBombardier(EntityType<? extends EntityGlyphidBombardier> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return attributes(GlyphidStats.getStats().getBombardier());
    }

    @Override
    public String getSkinName() {
        return "glyphid_bombardier";
    }

    protected Entity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsBombardier;
    }

    @Override
    public void tick() {
        super.tick();
        Entity e = this.getEntityToAttack();
        if (!this.level().isClientSide && e instanceof LivingEntity) {
            if (this.tickCount % 20 == 0) {
                this.lastTarget = e;
                this.lastX = e.getX();
                this.lastY = e.getY();
                this.lastZ = e.getZ();
            }

            if (this.tickCount % 60 == 1) {

                boolean topAttack = false;

                double velX = e.getX() - lastX;
                double velY = e.getY() - lastY;
                double velZ = e.getZ() - lastZ;

                if (this.lastTarget != e || new Vec3(velX, velY, velZ).length() > 30) {
                    velX = velY = velZ = 0;
                }

                if (this.distanceTo(e) > 20) {
                    topAttack = true;
                }

                int prediction = topAttack ? 60 : 20;
                Vec3 delta = new Vec3(e.getX() - getX() + velX * prediction, (e.getY() + e.getBbHeight() / 2) - (getY() + 1) + velY * prediction, e.getZ() - getZ() + velZ * prediction);
                double len = delta.length();
                if (len < 3) return;
                double targetYaw = -Math.atan2(delta.x, delta.z);

                double x = Math.sqrt(delta.x * delta.x + delta.z * delta.z);
                double y = delta.y;
                double v0 = getV0();
                double v02 = v0 * v0;
                double g = 0.04D;
                double upperLower = topAttack ? 1 : -1;
                double targetPitch = Math.atan((v02 + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02)) * upperLower) / (g * x));

                if (!Double.isNaN(targetPitch)) {

                    Vec3 fireVec = new Vec3(v0, 0, 0).zRot((float) -targetPitch).yRot((float) -(targetYaw + Math.PI * 0.5));

                    for (int i = 0; i < getBombCount(); i++) {
                        EntityAcidBomb bomb = new EntityAcidBomb(level(), getX(), getY() + 1, getZ());
                        bomb.setThrower(this);
                        bomb.setThrowableHeading(fireVec.x, fireVec.y, fireVec.z, (float) v0, i * getSpreadMult());
                        bomb.damage = getBombDamage();
                        level().addFreshEntity(bomb);
                    }

                    this.swingItem();
                }
            }
        }
    }

    public float getBombDamage() {
        return 5F;
    }

    public int getBombCount() {
        return 5;
    }

    public float getSpreadMult() {
        return 1F;
    }

    public double getV0() {
        return 1D;
    }
}
