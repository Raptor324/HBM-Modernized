package com.hbm_m.entity.mob;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.damagesource.ModDamageSources;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.item.ModItems;
import com.hbm_m.radiation.ChunkRadiationManager;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 1:1 port of {@code EntityRADBeast} - a blaze that irradiates instead of burning.
 *
 * <p>Two variants share the class, told apart by max health exactly as the original does: the
 * ordinary beast at 120 HP, and the leader at 360, which carries a radiation coin and is the only
 * one worth an advancement. {@code makeLeader()} keeps that arrangement.</p>
 */
public class EntityRADBeast extends Monster implements IRadiationImmune {

    /** dataWatcher slot 16: the id of whoever is currently being irradiated, for the beam render. */
    private static final EntityDataAccessor<Integer> VICTIM_ID =
            SynchedEntityData.defineId(EntityRADBeast.class, EntityDataSerializers.INT);

    /** The health above which this counts as the leader. */
    public static final double LEADER_THRESHOLD = 150.0D;

    private float heightOffset = 0.5F;
    private int heightOffsetUpdateTime;
    private int attackCooldown;

    public EntityRADBeast(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 120.0D)
                .add(Attributes.ATTACK_DAMAGE, 16.0D)
                .add(Attributes.ARMOR, 8.0D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(5, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VICTIM_ID, 0);
    }
    //?} else {
    /*@Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VICTIM_ID, 0);
    }
    *///?}

    /** {@code makeLeader}: triple health and a radiation coin that always drops. */
    public EntityRADBeast makeLeader() {
        var maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) maxHealth.setBaseValue(360.0D);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.COIN_RADIATION.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 1F);
        this.heal(this.getMaxHealth());
        return this;
    }

    public boolean isLeader() {
        return this.getMaxHealth() > LEADER_THRESHOLD;
    }

    @Override public boolean fireImmune()          { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public float getLightLevelDependentMagicValue() { return 1.0F; }

    @Override protected SoundEvent getHurtSound(@NotNull DamageSource source) { return SoundEvents.BLAZE_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_STEP; }
    @Override protected SoundEvent getAmbientSound() { return SoundEvents.BLAZE_AMBIENT; }

    /** The beast is a walking reactor: water is its only real weakness. */
    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            if (this.isInWaterOrRain()) {
                this.hurt(this.damageSources().drown(), 1.0F);
            }

            // The original wanders its hover height every 100 ticks so it bobs unpredictably.
            if (--this.heightOffsetUpdateTime <= 0) {
                this.heightOffsetUpdateTime = 100;
                this.heightOffset = 0.5F + (float) this.random.nextGaussian() * 3.0F;
            }

            LivingEntity target = this.getTarget();
            if (target != null && target.getEyeY() > this.getEyeY() + this.heightOffset) {
                this.setDeltaMovement(this.getDeltaMovement().add(
                        0, (0.3D - this.getDeltaMovement().y) * 0.3D, 0));
            }

            if (this.attackCooldown > 0) this.attackCooldown--;

            // Sync who is being zapped so the renderer can draw the beam.
            this.entityData.set(VICTIM_ID,
                    target != null && this.attackCooldown > 10 ? target.getId() : 0);

            radiationAttack(target);
        } else {
            spawnAmbientParticles();
        }

        // Slowed descent, as in the original.
        if (!this.onGround() && this.getDeltaMovement().y < 0) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1, 0.6D, 1));
        }

        super.aiStep();
    }

    /**
     * The ranged half of the original's {@code attackEntity}: within 30 blocks it irradiates the
     * target directly rather than closing in, and dumps 100 RAD into its own chunk while doing so.
     */
    private void radiationAttack(@Nullable LivingEntity target) {
        if (target == null || this.attackCooldown > 0) return;
        if (this.distanceTo(target) >= 30.0F || !this.hasLineOfSight(target)) return;

        ChunkRadiationManager.incrementRad(this.level(),
                this.getBlockX(), this.getBlockY(), this.getBlockZ(), 100F);
        target.hurt(ModDamageSources.radiation(this.level()), 16.0F);
        this.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        this.playAmbientSound();
        this.attackCooldown = 20;
    }

    /** The leader burns with lava; ordinary beasts glow faintly. */
    private void spawnAmbientParticles() {
        if (!isLeader()) {
            for (int i = 0; i < 6; i++) {
                this.level().addParticle(net.minecraft.core.particles.ParticleTypes.MYCELIUM,
                        this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.5,
                        this.getY() + this.random.nextDouble() * this.getBbHeight(),
                        this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth() * 1.5,
                        0, 0, 0);
            }
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                        this.getY() + this.random.nextDouble() * this.getBbHeight() * 0.75,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                        0, 0, 0);
            }
        } else {
            this.level().addParticle(ParticleTypes.LAVA,
                    this.getX() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    this.getY() + this.random.nextDouble() * this.getBbHeight() * 0.75,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * this.getBbWidth(),
                    0, 0, 0);
        }
    }

    /** {@code getUnfortunateSoul}: whoever the beam is currently pointed at, for the renderer. */
    @Nullable
    public Entity getUnfortunateSoul() {
        int id = this.entityData.get(VICTIM_ID);
        return id == 0 ? null : this.level().getEntity(id);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, @NotNull DamageSource source) {
        return false; // protected void fall(float) {} - it simply does not take fall damage
    }

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        // Only the leader is worth an advancement, and only within 50 blocks.
        if (isLeader()) {
            ModAdvancements.grantNearby(this, 50D, ModAdvancements.BOSS_MELTDOWN);
        }
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!recentlyHit) return;

        if (looting > 0) {
            this.spawnAtLocation(new ItemStack(ModItems.NUGGET_POLONIUM.get(), looting));
        }

        // Wet beasts drop raw waste instead of intact rods, and twice as much of it.
        boolean wet = this.isInWaterOrRain();
        int count = this.random.nextInt(3) + 1;
        for (int i = 0; i < count; i++) {
            ItemStack drop = switch (this.random.nextInt(3)) {
                case 0 -> wet ? new ItemStack(ModItems.WASTE_URANIUM.get(), 2)
                              : new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get());
                case 1 -> wet ? new ItemStack(ModItems.WASTE_MOX.get(), 2)
                              : new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get());
                default -> wet ? new ItemStack(ModItems.WASTE_PLUTONIUM.get(), 2)
                               : new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get());
            };
            this.spawnAtLocation(drop);
        }
    }

    /** Convenience for spawn code and the egg: the leader variant of this type. */
    public static EntityRADBeast createLeader(Level level) {
        EntityRADBeast beast = ModEntities.RAD_BEAST.get().create(level);
        return beast == null ? null : beast.makeLeader();
    }
}
