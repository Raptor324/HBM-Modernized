package com.hbm_m.handler.guncfg;

import java.util.List;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.projectile.EntityBulletBaseNT;
import com.hbm_m.entity.projectile.EntityBulletBaseNT.IBulletUpdateBehaviorNT;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.handler.BulletConfigSyncingUtil;
import com.hbm_m.handler.BulletConfiguration;
import com.hbm_m.particle.helper.IParticleCreator;
import com.hbm_m.sound.HbmSoundsNT;
import com.hbm_m.util.BobMathUtil;
import com.hbm_m.util.Vec3NT;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 1:1 {@code GunNPCFactory}: Geschosse von Maskman, BOT Prime (Wurm) und UFO-Raketen. */
@Deprecated
public final class GunNPCFactory {

    private GunNPCFactory() { }

    public static BulletConfiguration getMaskmanOrb() {
        BulletConfiguration bullet = new BulletConfiguration();
        bullet.velocity = 0.25F;
        bullet.spread = 0.000F;
        bullet.bulletsMin = 1;
        bullet.bulletsMax = 1;
        bullet.dmgMin = 100;
        bullet.dmgMax = 100;
        bullet.gravity = 0.0D;
        bullet.maxAge = 60;
        bullet.doesRicochet = false;
        bullet.ricochetAngle = 0;
        bullet.HBRC = 0;
        bullet.LBRC = 0;
        bullet.bounceMod = 1.0;
        bullet.doesPenetrate = false;
        bullet.doesBreakGlass = false;
        bullet.style = BulletConfiguration.STYLE_ORB;
        bullet.trail = 1;
        bullet.explosive = 1.5F;

        bullet.bntUpdate = (bulletnt) -> {
            if (bulletnt.level().isClientSide) return;
            if (bulletnt.tickCount % 10 != 5) return;

            List<Player> players = bulletnt.level().getEntitiesOfClass(Player.class, bulletnt.getBoundingBox().inflate(50, 50, 50));
            for (Player player : players) {
                Vec3 motion = new Vec3(player.getX() - bulletnt.getX(), (player.getY() + player.getEyeHeight()) - bulletnt.getY(), player.getZ() - bulletnt.getZ()).normalize();
                EntityBulletBaseNT bolt = EntityBulletBaseNT.create(bulletnt.level(), BulletConfigSyncingUtil.MASKMAN_BOLT);
                bolt.setThrower(bulletnt.getThrower());
                bolt.setPos(bulletnt.getX(), bulletnt.getY(), bulletnt.getZ());
                bolt.setThrowableHeading(motion.x, motion.y, motion.z, 0.5F, 0.05F);
                bulletnt.level().addFreshEntity(bolt);
            }
        };
        return bullet;
    }

    public static BulletConfiguration getMaskmanBolt() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardBulletConfig();
        bullet.spread = 0.0F;
        bullet.dmgMin = 15;
        bullet.dmgMax = 20;
        bullet.leadChance = 0;
        bullet.explosive = 0.5F;
        bullet.setToBolt(BulletConfiguration.BOLT_LACUNAE);
        bullet.vPFX = "reddust";
        bullet.damageType = ModDamageTypes.LASER;
        return bullet;
    }

    public static BulletConfiguration getMaskmanTracer() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardBulletConfig();
        bullet.spread = 0.0F;
        bullet.dmgMin = 15;
        bullet.dmgMax = 20;
        bullet.leadChance = 0;
        bullet.setToBolt(BulletConfiguration.BOLT_NIGHTMARE);
        bullet.vPFX = "reddust";
        bullet.damageType = ModDamageTypes.LASER;

        bullet.bntImpact = (bulletnt, x, y, z, sideHit) -> {
            if (bulletnt.level().isClientSide) return;
            EntityBulletBaseNT meteor = EntityBulletBaseNT.create(bulletnt.level(), BulletConfigSyncingUtil.MASKMAN_METEOR);
            meteor.setPos(bulletnt.getX(), bulletnt.getY() + 30 + meteor.level().random.nextInt(10), bulletnt.getZ());
            meteor.setDeltaMovement(meteor.getDeltaMovement().x, -1D, meteor.getDeltaMovement().z);
            meteor.setThrower(bulletnt.getThrower());
            bulletnt.level().addFreshEntity(meteor);
        };
        return bullet;
    }

    public static BulletConfiguration getMaskmanRocket() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardGrenadeConfig();
        bullet.gravity = 0.1D;
        bullet.velocity = 1.0F;
        bullet.dmgMin = 15;
        bullet.dmgMax = 20;
        bullet.blockDamage = false;
        bullet.explosive = 5.0F;
        bullet.style = BulletConfiguration.STYLE_ROCKET;
        return bullet;
    }

    public static BulletConfiguration getMaskmanMeteor() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardGrenadeConfig();
        bullet.gravity = 0.1D;
        bullet.velocity = 1.0F;
        bullet.dmgMin = 20;
        bullet.dmgMax = 30;
        bullet.blockDamage = false;
        bullet.incendiary = 3;
        bullet.explosive = 2.5F;
        bullet.style = BulletConfiguration.STYLE_METEOR;

        bullet.bntUpdate = (bulletnt) -> {
            if (!bulletnt.level().isClientSide) return;
            RandomSource rand = bulletnt.level().random;
            for (int i = 0; i < 5; i++) {
                CompoundTag nbt = new CompoundTag();
                nbt.putString("type", "vanillaExt");
                nbt.putString("mode", "flame");
                nbt.putDouble("posX", bulletnt.getX() + rand.nextDouble() * 0.5 - 0.25);
                nbt.putDouble("posY", bulletnt.getY() + rand.nextDouble() * 0.5 - 0.25);
                nbt.putDouble("posZ", bulletnt.getZ() + rand.nextDouble() * 0.5 - 0.25);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(nbt);
            }
        };
        return bullet;
    }

    public static BulletConfiguration getWormBolt() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardBulletConfig();
        bullet.spread = 0.0F;
        bullet.maxAge = 60;
        bullet.dmgMin = 15;
        bullet.dmgMax = 25;
        bullet.leadChance = 0;
        bullet.doesRicochet = false;
        bullet.setToBolt(BulletConfiguration.BOLT_WORM);
        bullet.damageType = ModDamageTypes.LASER;
        return bullet;
    }

    public static BulletConfiguration getWormHeadBolt() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.standardBulletConfig();
        bullet.spread = 0.0F;
        bullet.maxAge = 100;
        bullet.dmgMin = 35;
        bullet.dmgMax = 60;
        bullet.leadChance = 0;
        bullet.doesRicochet = false;
        bullet.setToBolt(BulletConfiguration.BOLT_LASER);
        bullet.damageType = ModDamageTypes.LASER;
        return bullet;
    }

    public static BulletConfiguration getRocketUFOConfig() {
        BulletConfiguration bullet = LegacyBulletConfigFactory.getRocketConfig();
        bullet.vPFX = "reddust";
        bullet.destroysBlocks = false;
        bullet.explosive = 0F;

        bullet.bntUpdate = new IBulletUpdateBehaviorNT() {

            final double angle = 90;
            final double range = 100;

            @Override
            public void behaveUpdate(EntityBulletBaseNT bullet) {
                if (bullet.level().isClientSide) return;

                if (bullet.level().getEntity(bullet.homingTarget) == null) chooseTarget(bullet);

                Entity target = bullet.level().getEntity(bullet.homingTarget);
                if (target != null) {
                    if (bullet.distanceToSqr(target) < 5) {
                        bullet.getConfig().bntImpact.behaveBlockHit(bullet, -1, -1, -1, -1);
                        bullet.discard();
                        return;
                    }
                    Vec3 delta = new Vec3(target.getX() - bullet.getX(), target.getY() + target.getBbHeight() / 2 - bullet.getY(), target.getZ() - bullet.getZ()).normalize();
                    double vel = bullet.getDeltaMovement().length();
                    bullet.setDeltaMovement(delta.x * vel, delta.y * vel, delta.z * vel);
                }
            }

            private void chooseTarget(EntityBulletBaseNT bullet) {
                List<LivingEntity> entities = bullet.level().getEntitiesOfClass(LivingEntity.class, bullet.getBoundingBox().inflate(range, range, range));
                Vec3 m = bullet.getDeltaMovement();
                Vec3NT mot = new Vec3NT(m.x, m.y, m.z);
                LivingEntity target = null;
                double targetAngle = angle;

                for (LivingEntity e : entities) {
                    if (!e.isAlive() || e == bullet.getThrower()) continue;
                    Vec3NT delta = new Vec3NT(e.getX() - bullet.getX(), e.getY() + e.getBbHeight() / 2 - bullet.getY(), e.getZ() - bullet.getZ());
                    // func_147447_a(..., false, true, false) != null -> Sichtlinie blockiert
                    if (bullet.level().clip(new ClipContext(bullet.position(), new Vec3(e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ()),
                            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet)).getType() != HitResult.Type.MISS) continue;

                    double dist = e.distanceToSqr(bullet);
                    if (dist < range * range) {
                        double deltaAngle = BobMathUtil.getCrossAngle(mot, delta);
                        if (deltaAngle < targetAngle) {
                            target = e;
                            targetAngle = deltaAngle;
                        }
                    }
                }
                if (target != null) bullet.homingTarget = target.getId();
            }
        };

        bullet.bntImpact = (bulletnt, x, y, z, sideHit) -> {
            var level = bulletnt.level();
            level.playSound(null, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), HbmSoundsNT.get("hbm:entity.ufoBlast"), SoundSource.HOSTILE, 5.0F, 0.9F + level.random.nextFloat() * 0.2F);
            level.playSound(null, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 5.0F, 0.5F);
            ExplosionNukeGeneric.dealDamage(level, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 10, 50);

            if (level instanceof ServerLevel server) {
                for (int i = 0; i < 3; i++) {
                    CompoundTag data = new CompoundTag();
                    data.putString("type", "plasmablast");
                    data.putFloat("r", 0.0F);
                    data.putFloat("g", 0.75F);
                    data.putFloat("b", 1.0F);
                    data.putFloat("pitch", -30F + 30F * i);
                    data.putFloat("yaw", level.random.nextFloat() * 180F);
                    data.putFloat("scale", 5F);
                    IParticleCreator.sendPacket(server, bulletnt.getX(), bulletnt.getY(), bulletnt.getZ(), 100, data);
                }
            }
        };
        return bullet;
    }
}
