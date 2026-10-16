package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm_m.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm_m.explosion.vanillant.interfaces.IEntityRangeMutator;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;

@Deprecated
public class EntityProcessorStandard implements IEntityProcessor {

    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected boolean allowSelfDamage = false;

    @Override
    public HashMap<Player, Vec3> process(ExplosionVNT explosion, Level level, double x, double y, double z, float size) {

        HashMap<Player, Vec3> affectedPlayers = new HashMap<>();

        size *= 2.0F;

        if (range != null) {
            size = range.mutateRange(explosion, size);
        }

        double minX = x - (double) size - 1.0D;
        double maxX = x + (double) size + 1.0D;
        double minY = y - (double) size - 1.0D;
        double maxY = y + (double) size + 1.0D;
        double minZ = z - (double) size - 1.0D;
        double maxZ = z + (double) size + 1.0D;

        List<Entity> entities = level.getEntities(allowSelfDamage ? null : explosion.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        com.hbm_m.platform.PlatformHooks.fireExplosionDetonate(level, explosion.compat, entities, size);
        Vec3 vec3 = new Vec3(x, y, z);

        for (Entity entity : entities) {

            // 1.7.10 uses entity.getDistance(x, y, z), a linear distance; distanceToSqr is squared,
            // so the effective radius had shrunk to sqrt(size). Same shape as EntityProcessorCross.
            double distanceScaled = Math.sqrt(entity.distanceToSqr(x, y, z)) / size;

            if (distanceScaled <= 1.0D) {

                double deltaX = entity.getX() - x;
                double deltaY = entity.getEyeY() - y;
                double deltaZ = entity.getZ() - z;
                double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                if (distance != 0.0D) {

                    deltaX /= distance;
                    deltaY /= distance;
                    deltaZ /= distance;

                    double density = Explosion.getSeenPercent(vec3, entity);
                    // Two separate values, as in the original and in EntityProcessorCross:100+.
                    // knockback carries the distance falloff and feeds calculateDamage; the
                    // enchantment dampener applies only to the velocity. Folding the dampener into
                    // knockback before calculateDamage made blast protection cut damage a second
                    // time, and quadratically, since the formula squares it.
                    double knockback = (1.0D - distanceScaled) * density;
                    double enchKnockback = entity instanceof LivingEntity livingEntity
                            ? PlatformHooks.getExplosionKnockbackAfterDampener(livingEntity, knockback)
                            : knockback;

                    entity.hurt(setExplosionSource(level, explosion.compat), calculateDamage(distanceScaled, density, knockback, size));

                    Vec3 velocity = new Vec3(
                            deltaX * enchKnockback,
                            deltaY * enchKnockback,
                            deltaZ * enchKnockback
                    );

                    if (entity instanceof Player player) {
                        if (!player.isSpectator() && !player.getAbilities().flying) {
                            player.hurtMarked = true;
                            affectedPlayers.put(player, velocity);
                        }
                    }

                    if (damage != null) {
                        damage.handleAttack(explosion, entity, distanceScaled);
                    }
                }
            }
        }

        return affectedPlayers;
    }

    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D));
    }

    public static DamageSource setExplosionSource(Level level, Explosion explosion) {
        DamageSources sources = level.damageSources();
        Entity causing = explosion.getIndirectSourceEntity();
        Entity direct  = explosion.getDirectSourceEntity();
        return sources.explosion(causing, direct);
    }

    public EntityProcessorStandard withRangeMod(float mod) {
        range = new IEntityRangeMutator() {
            @Override
            public float mutateRange(ExplosionVNT explosion, float range) {
                return range * mod;
            }
        };
        return this;
    }

    public EntityProcessorStandard withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }

    public EntityProcessorStandard allowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }
}