package com.hbm_m.entity.effect;

import com.hbm_m.item.ModItems;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Joke counterpart to the 1.7.10 joke stage family (EUPH "ice cream" etc.): the giant translucent freaky alien face
 * hovering over the detonation site. No 1.7.10 analog exists - user-requested content, not a port.
 * Levitation 255 within 100 blocks, a rain of pigs (and the occasional cheese), 10 s lifetime, bass-boosted theme.
 */
public class EntityFreakyAlienFace extends Entity {

    public static final int LIFETIME = 200;      // 10 s, matches the theme cut
    public static final double EFFECT_RADIUS = 100;
    public static final double TERRAIN_RADIUS = 40;
    private static final int MAX_PIGS = 120;

    private boolean primary = true;
    private int pigsSpawned;

    public EntityFreakyAlienFace(EntityType<? extends EntityFreakyAlienFace> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public EntityFreakyAlienFace(Level level, double x, double y, double z) {
        this(level, x, y, z, true);
    }

    public EntityFreakyAlienFace(Level level, double x, double y, double z, boolean primary) {
        this(com.hbm_m.entity.ModEntities.FREAKY_ALIEN_FACE.get(), level);
        this.primary = primary;
        setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide) {
            if (this.tickCount % 4 == 0) {
                level().addParticle(ParticleTypes.PORTAL,
                        getX() + (random.nextDouble() - 0.5) * 8, getY() + (random.nextDouble() - 0.5) * 8,
                        getZ() + (random.nextDouble() - 0.5) * 8, 0, 0, 0);
            }
            return;
        }

        if (this.primary) {
            // Levitation 255 for living entities in 100 blocks (skip spawned pigs so they actually rain down)
            if (this.tickCount % 10 == 0) {
                for (net.minecraft.world.entity.LivingEntity living : level().getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, affectBox(),
                        e -> e.isAlive() && !(e instanceof Pig))) {
                    living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 60, 255, false, true));
                }
            }

            // Rain of pigs (and the odd cheese wheel) from the stratosphere.
            // Slow Falling keeps them alive on impact - otherwise they all splat mid-drop.
            if (this.tickCount > 10 && this.tickCount < LIFETIME - 20 && this.pigsSpawned < MAX_PIGS && random.nextFloat() < 0.85F) {
                double a = random.nextDouble() * Math.PI * 2;
                double r = random.nextDouble() * 80;
                double x = getX() + Math.cos(a) * r;
                double z = getZ() + Math.sin(a) * r;
                double y = Math.min(getY() + 40 + random.nextDouble() * 25, level().getMaxBuildHeight() - 1);

                if (random.nextFloat() < 0.1F) {
                    ItemEntity cheese = new ItemEntity(level(), x, y, z, new ItemStack(ModItems.CHEESE.get()));
                    level().addFreshEntity(cheese);
                } else {
                    Pig pig = new Pig(EntityType.PIG, level());
                    pig.setPos(x, y, z);
                    pig.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, LIFETIME * 3, 0, false, false));
                    level().addFreshEntity(pig);
                    this.pigsSpawned++;
                }
            }
        }

        if (this.tickCount >= LIFETIME) {
            discard();
        }
    }

    private AABB affectBox() {
        return new AABB(getX() - EFFECT_RADIUS, getY() - EFFECT_RADIUS, getZ() - EFFECT_RADIUS,
                getX() + EFFECT_RADIUS, getY() + EFFECT_RADIUS, getZ() + EFFECT_RADIUS);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
    }
    *///?}

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        this.primary = !tag.contains("Primary") || tag.getBoolean("Primary");
        this.pigsSpawned = tag.getInt("PigsSpawned");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        tag.putBoolean("Primary", this.primary);
        tag.putInt("PigsSpawned", this.pigsSpawned);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }
}
