package com.hbm_m.entity.mob;

import com.hbm_m.advancement.ModAdvancements;
import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.entity.projectile.TurretBulletEntity;
import com.hbm_m.item.ModItems;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
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
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * 1:1 port of {@code EntityMaskMan} - a 1000 HP boss that shrugs off most of what you throw at it
 * and can be killed instantly by a thrown egg.
 *
 * <p>The damage table is the interesting part and is reproduced exactly: fire and magic do
 * nothing, projectiles and explosions are halved, and everything above 50 in a single hit is
 * halved past that threshold, so burst damage is heavily punished. Half health triggers a one-off
 * explosion above its head, and its face changes to a skull - see the renderer.</p>
 *
 * <p><b>Substitution:</b> the original's three-phase laser gun fires {@code EntityBulletBaseNT}
 * with configs from {@code BulletConfigSyncingUtil}, and its minigun fires 7.62 FMJ from
 * {@code XFactory762mm}. Neither system exists in this port, so all four projectile types are
 * carried by {@link TurretBulletEntity} with the original's damage, counts and cadences kept
 * intact. The attack pattern is faithful; the bullet entity underneath is not.</p>
 */
public class EntityMaskMan extends Monster implements IRadiationImmune {

    /** Above this in a single hit, further damage is halved. */
    private static final float SOFT_CAP = 50F;
    /** The original's one-in-ten instant kill when hit by a thrown egg. */
    private static final int EGG_KILL_CHANCE = 10;

    private float lastHealth;
    private boolean halfHealthBlown;

    // ─── Laser gun state ─────────────────────────────────────────────────────

    /** {@code EnumLaserAttack}: delay between shots and how many before switching. */
    private enum LaserAttack {
        ORB(60, 5),
        MISSILE(10, 10),
        SPLASH(40, 3);

        final int delay;
        final int amount;

        LaserAttack(int delay, int amount) {
            this.delay = delay;
            this.amount = amount;
        }
    }

    private LaserAttack attack = LaserAttack.ORB;
    private int attackCount;
    private int laserTimer;
    private int minigunTimer;

    public EntityMaskMan(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 100;
        this.lastHealth = this.getMaxHealth();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 1000.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FOLLOW_RANGE, 100.0D)
                .add(Attributes.ATTACK_DAMAGE, 15.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        // EntityAIMaskmanCasualApproach: it walks, it does not charge.
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override public boolean fireImmune()                { return true; }
    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override public boolean canBeAffected(@NotNull net.minecraft.world.effect.MobEffectInstance effect) { return false; }

    @Override protected SoundEvent getHurtSound(@NotNull DamageSource source) { return SoundEvents.IRON_GOLEM_HURT; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    /** Half health is what flips the face to a skull; the renderer reads this. */
    public boolean isUnmasked() {
        return this.getHealth() < this.getMaxHealth() / 2;
    }

    // ─── Damage table ────────────────────────────────────────────────────────

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        // A thrown egg has a one-in-ten chance to end it outright, worth no XP at all.
        if (source.getDirectEntity() instanceof ThrownEgg && this.random.nextInt(EGG_KILL_CHANCE) == 0) {
            this.xpReward = 0;
            // 1.7.10 runs die() out of its own tick once health hits zero, so setHealth(0) was
            // enough there. 1.20 only calls die() from hurt(), so setting health directly left a
            // corpse that never dropped its mask and never awarded the advancement. Route the
            // kill through super.hurt instead, which skips the damage table below by design.
            return super.hurt(source, Float.MAX_VALUE);
        }

        if (source.is(DamageTypes.IN_FIRE) || source.is(DamageTypes.ON_FIRE)
                || source.is(DamageTypes.LAVA) || source.is(DamageTypes.HOT_FLOOR)) {
            amount = 0;
        }
        if (source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC)) {
            amount = 0;
        }
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)) amount *= 0.5F;
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= 0.5F;

        if (amount > SOFT_CAP) {
            amount = SOFT_CAP + (amount - SOFT_CAP) * 0.5F;
        }

        return super.hurt(source, amount);
    }

    // ─── Behaviour ───────────────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Crossing half health once blows a hole above its head.
        if (!halfHealthBlown && this.lastHealth >= this.getMaxHealth() / 2
                && this.getHealth() < this.getMaxHealth() / 2 && this.isAlive()) {
            halfHealthBlown = true;
            if (!this.level().isClientSide) {
                this.level().explode(this, this.getX(), this.getY() + 4, this.getZ(),
                        2.5F, Level.ExplosionInteraction.MOB);
            }
        }
        this.lastHealth = this.getHealth();

        if (!this.level().isClientSide) {
            updateBossBar();

            LivingEntity target = this.getTarget();
            if (target != null && this.hasLineOfSight(target)) {
                this.getLookControl().setLookAt(target, 15F, 15F);
                double distance = this.distanceTo(target);
                // EntityAIMaskmanMinigun: only in the 5-10 block band.
                if (distance > 5 && distance < 10) minigun(target);
                laserGun(target);
            }
        }
    }

    /** {@code EntityAIMaskmanMinigun}: a shot every three ticks while in the mid band. */
    private void minigun(LivingEntity target) {
        if (--this.minigunTimer > 0) return;
        this.minigunTimer = 3;

        fireAt(target, 5F, 1.5D, 0D, ModItems.BOLT_STEEL.get());
        emitSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F);
    }

    /** {@code EntityAIMaskmanLasergun}: cycles orb, missile and splash volleys. */
    private void laserGun(LivingEntity target) {
        if (--this.laserTimer > 0) return;
        this.laserTimer = this.attack.delay;

        switch (this.attack) {
            case ORB -> {
                TurretBulletEntity orb = fireAt(target, 2F, 1.2D, 0.5D, ModItems.PARTICLE_DIGAMMA.get());
                if (orb != null) emitSound(SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.0F);
            }
            case MISSILE -> {
                // Lobbed rather than aimed: a flat push towards the target plus a steep arc.
                Vec3 flat = new Vec3(target.getX() - this.getX(), 0, target.getZ() - this.getZ());
                TurretBulletEntity missile = TurretBulletEntity.create(this.level(),
                        this.getX(), this.getEyeY(), this.getZ(),
                        flat.x * 0.05D, 0.5D + this.random.nextDouble() * 0.5D, flat.z * 0.05D,
                        1F, ModItems.MISSILE_NUCLEAR.get());
                missile.setOwner(this);
                missile.setNoGravity(false);
                this.level().addFreshEntity(missile);
                emitSound(SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
            }
            case SPLASH -> {
                for (int i = 0; i < 5; i++) {
                    fireAt(target, 1F, 1.2D, 0.05D, ModItems.BOLT_STEEL.get());
                }
            }
        }

        if (++this.attackCount >= this.attack.amount) {
            this.attackCount = 0;
            // The original advances by a random non-zero step so it never repeats the same phase.
            int next = this.attack.ordinal() + this.random.nextInt(LaserAttack.values().length - 1);
            this.attack = LaserAttack.values()[next % LaserAttack.values().length];
        }
    }

    private TurretBulletEntity fireAt(LivingEntity target, float damage, double speed, double spread,
                                      net.minecraft.world.item.Item icon) {
        Vec3 dir = new Vec3(
                target.getX() - this.getX(),
                target.getEyeY() - this.getEyeY(),
                target.getZ() - this.getZ()).normalize();

        TurretBulletEntity bullet = TurretBulletEntity.create(this.level(),
                this.getX(), this.getEyeY(), this.getZ(),
                dir.x * speed + this.random.nextGaussian() * spread,
                dir.y * speed + this.random.nextGaussian() * spread,
                dir.z * speed + this.random.nextGaussian() * spread,
                damage, icon);
        bullet.setOwner(this);
        this.level().addFreshEntity(bullet);
        return bullet;
    }

    private void emitSound(SoundEvent sound, float volume, float pitch) {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                sound, SoundSource.HOSTILE, volume, pitch);
    }

    // ─── Death ───────────────────────────────────────────────────────────────

    @Override
    public void die(@NotNull DamageSource source) {
        super.die(source);
        // The original credits everyone within 100 blocks, not 50 like the other bosses.
        ModAdvancements.grantNearby(this, 100D, ModAdvancements.BOSS_MASKMAN);
    }

    @Override
    //? if < 1.21.1 {
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
    //?} else {
    /*protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
    *///?}
        if (this.level().isClientSide) return;

        // The original hands over its own gas mask with a combo filter already installed; the
        // port has no filter-install helper, so the two drop separately.
        this.spawnAtLocation(new ItemStack(ModItems.GAS_MASK_M65.get()));
        this.spawnAtLocation(new ItemStack(ModItems.GAS_MASK_FILTER_COMBO.get()));
        this.spawnAtLocation(new ItemStack(ModItems.COIN_MASKMAN.get()));
        this.spawnAtLocation(new ItemStack(ModItems.BOTTLED_CLOUD.get()));
        this.spawnAtLocation(new ItemStack(Items.SKELETON_SKULL));
    }

    /**
     * Without persisting these, a reloaded MaskMan below half health starts with lastHealth at
     * full and the flag cleared, so he detonates over his own head again on the first tick after
     * every world load.
     */
    @Override
    public void addAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("halfHealthBlown", this.halfHealthBlown);
    }

    @Override
    public void readAdditionalSaveData(@NotNull net.minecraft.nbt.CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.halfHealthBlown = tag.getBoolean("halfHealthBlown");
        this.lastHealth = this.getHealth();
    }

    // ─── Boss bar ────────────────────────────────────────────────────────────
    // The original calls BossStatus.setBossStatus from its renderer every frame, which is how
    // 1.7.10 did boss bars. 1.20 has a real server-side ServerBossEvent instead, so the bar is
    // driven from the entity and correctly disappears when it dies or unloads.

    private final net.minecraft.server.level.ServerBossEvent bossEvent =
            new net.minecraft.server.level.ServerBossEvent(this.getDisplayName(),
                    net.minecraft.world.BossEvent.BossBarColor.RED,
                    net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);

    @Override
    public void startSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(@NotNull net.minecraft.server.level.ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@org.jetbrains.annotations.Nullable net.minecraft.network.chat.Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    /** Keeps the bar in step with the health bar; call once per server tick. */
    protected void updateBossBar() {
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
    }

}
