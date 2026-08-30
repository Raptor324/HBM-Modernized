package com.hbm_m.explosion.vanillant.standard;

import com.hbm_m.explosion.vanillant.ExplosionVNT;
import com.hbm_m.explosion.vanillant.interfaces.ICustomDamageHandler;
import com.hbm_m.explosion.vanillant.interfaces.IEntityProcessor;
import com.hbm_m.explosion.vanillant.interfaces.IEntityRangeMutator;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

//? if forge {
import net.minecraftforge.event.ForgeEventFactory;
//?}

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class EntityProcessorCross implements IEntityProcessor {

    protected double nodeDist;
    protected IEntityRangeMutator range;
    protected ICustomDamageHandler damage;
    protected double knockbackMult = 1D;
    protected boolean allowSelfDamage = false;

    public EntityProcessorCross(double nodeDist) {
        this.nodeDist = nodeDist;
    }

    public EntityProcessorCross setAllowSelfDamage() {
        this.allowSelfDamage = true;
        return this;
    }

    public EntityProcessorCross setKnockback(double mult) {
        this.knockbackMult = mult;
        return this;
    }

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

        List<Entity> list = level.getEntities(allowSelfDamage ? null : explosion.exploder, new AABB(minX, minY, minZ, maxX, maxY, maxZ));
        //? if forge {
        ForgeEventFactory.onExplosionDetonate(level, explosion.compat, list, size);
        //?}

        Vec3[] nodes;

        if (this.nodeDist > 0) {
            nodes = new Vec3[7];
            for (int i = 0; i < 7; i++) {
                Direction dir = Direction.from3DDataValue(i);
                nodes[i] = new Vec3(x + dir.getStepX() * nodeDist, y + dir.getStepY() * nodeDist, z + dir.getStepZ() * nodeDist);
            }
        } else {
            nodes = new Vec3[1];
            nodes[0] = new Vec3(x, y, z);
        }

        HashMap<Entity, Float> damageMap = new HashMap<>();

        for (Entity entity : list) {
            //? if < 1.21.1 {
            boolean ignore = entity.ignoreExplosion();
            //?} else {
            /*boolean ignore = entity.ignoreExplosion(explosion.compat);
            *///?}
            if (!ignore) {

                AABB entityBoundingBox = entity.getBoundingBox();
                double xDist = (entityBoundingBox.minX <= x && entityBoundingBox.maxX >= x) ? 0 : Math.min(Math.abs(entityBoundingBox.minX - x), Math.abs(entityBoundingBox.maxX - x));
                double yDist = (entityBoundingBox.minY <= y && entityBoundingBox.maxY >= y) ? 0 : Math.min(Math.abs(entityBoundingBox.minY - y), Math.abs(entityBoundingBox.maxY - y));
                double zDist = (entityBoundingBox.minZ <= z && entityBoundingBox.maxZ >= z) ? 0 : Math.min(Math.abs(entityBoundingBox.minZ - z), Math.abs(entityBoundingBox.maxZ - z));
                double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
                double distanceScaled = dist / size;

                if (distanceScaled <= 1.0D) {
                    double deltaX = entity.getX() - x;
                    double deltaY = entity.getY() + entity.getEyeHeight() - y;
                    double deltaZ = entity.getZ() - z;
                    double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

                    if (distance != 0.0D) {

                        deltaX /= distance;
                        deltaY /= distance;
                        deltaZ /= distance;

                        double density = 0;

                        for (Vec3 vec : nodes) {
                            double d = Explosion.getSeenPercent(vec, entity);
                            if (d > density) {
                                density = d;
                            }
                        }

                        double knockback = (1.0D - distanceScaled) * density;

                        float dmg = calculateDamage(distanceScaled, density, knockback, size);
                        if (!damageMap.containsKey(entity) || damageMap.get(entity) < dmg) damageMap.put(entity, dmg);

                        double enchKnockback;
                        if (entity instanceof LivingEntity livingEntity) {
                            enchKnockback = PlatformHooks.getExplosionKnockbackAfterDampener(livingEntity, knockback);
                        } else {
                            enchKnockback = knockback;
                        }

                        Vec3 velocity = new Vec3(
                                deltaX * enchKnockback * knockbackMult,
                                deltaY * enchKnockback * knockbackMult,
                                deltaZ * enchKnockback * knockbackMult
                        );

                        entity.setDeltaMovement(entity.getDeltaMovement().add(velocity));

                        if (entity instanceof Player player) {
                            if (!player.isSpectator() && !player.getAbilities().flying) {
                                player.hurtMarked = true;
                                affectedPlayers.put(player, velocity);
                            }
                        }
                    }
                }
            }
        }

        for (Entry<Entity, Float> entry : damageMap.entrySet()) {

            Entity entity = entry.getKey();
            attackEntity(entity, explosion, entry.getValue());

            if (damage != null) {
                AABB entityBoundingBox = entity.getBoundingBox();
                double xDist = (entityBoundingBox.minX <= x && entityBoundingBox.maxX >= x) ? 0 : Math.min(Math.abs(entityBoundingBox.minX - x), Math.abs(entityBoundingBox.maxX - x));
                double yDist = (entityBoundingBox.minY <= y && entityBoundingBox.maxY >= y) ? 0 : Math.min(Math.abs(entityBoundingBox.minY - y), Math.abs(entityBoundingBox.maxY - y));
                double zDist = (entityBoundingBox.minZ <= z && entityBoundingBox.maxZ >= z) ? 0 : Math.min(Math.abs(entityBoundingBox.minZ - z), Math.abs(entityBoundingBox.maxZ - z));
                double dist = Math.sqrt(xDist * xDist + yDist * yDist + zDist * zDist);
                double distanceScaled = dist / size;
                damage.handleAttack(explosion, entity, distanceScaled);
            }
        }

        return affectedPlayers;
    }

    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        entity.hurt(setExplosionSource(entity.level(), source.compat), amount);
    }

    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) ((int) ((knockback * knockback + knockback) / 2.0D * 8.0D * size + 1.0D));
    }

    public static DamageSource setExplosionSource(Level level, Explosion explosion) {
        DamageSources sources = level.damageSources();
        Entity causing = explosion.getIndirectSourceEntity();
        Entity direct = explosion.getDirectSourceEntity();
        return sources.explosion(causing, direct);
    }

    public EntityProcessorCross withRangeMod(float mod) {
        range = (explosion, range) -> range * mod;
        return this;
    }

    public EntityProcessorCross withDamageMod(ICustomDamageHandler damage) {
        this.damage = damage;
        return this;
    }
}