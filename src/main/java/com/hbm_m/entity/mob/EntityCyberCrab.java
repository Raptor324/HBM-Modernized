package com.hbm_m.entity.mob;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.api.entity.IRadiationImmune;
import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.entity.projectile.EntityBullet;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * 1:1 {@code EntityCyberCrab}: kleine Robo-Krabbe (4 HP) mit Tau-Geschossen (RangedAttack 60-80 Ticks, 15 Bloecke),
 * meidet Wasser, nimmt in Naesse/Feuer 10 Schaden pro Tick, platzt beim Tod mit einer Mini-Explosion und ist gegen
 * Tau immun. Nahkampf macht keinen Schaden.
 */
public class EntityCyberCrab extends Monster implements RangedAttackMob, IRadiationImmune {

    public EntityCyberCrab(EntityType<? extends EntityCyberCrab> type, Level world) {
        super(type, world);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    @Override
    protected void registerGoals() {
        if (!(this instanceof EntityTaintCrab))
            this.goalSelector.addGoal(0, new PanicGoal(this, 0.75D));

        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5F));
        this.goalSelector.addGoal(4, arrowAI());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, 0, true, true,
                e -> !(e instanceof EntityCyberCrab || e instanceof Creeper)));
    }

    protected RangedAttackGoal arrowAI() {
        return new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.75F);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {

        if (source.is(ModDamageTypes.TAU))
            return false;

        return super.hurt(source, amount);
    }

    @Override
    public int getMaxFallDistance() {
        return this.getTarget() == null ? 3 : 3 + (int) (this.getHealth() - 1.0F);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.isInWater() || this.isInWaterRainOrBubble() || this.isOnFire())
            this.hurt(this.damageSources().generic(), 10F);

        if (this.getHealth() <= 0 && !this.isRemoved()) {
            this.discard();

            if (!level().isClientSide) {
                if (this instanceof EntityTaintCrab)
                    level().explode(this, getX(), getY(), getZ(), 3F, false, Level.ExplosionInteraction.MOB);
                else
                    level().explode(this, getX(), getY(), getZ(), 0.1F, false, Level.ExplosionInteraction.MOB);
            }
        }
    }

    @Override
    protected SoundEvent getHurtSound(@NotNull DamageSource source) {
        return HbmSoundsNT.get("hbm:entity.cybercrab");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HbmSoundsNT.get("hbm:entity.cybercrab");
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity target) {
        return true;
    }

    @Override
    public void performRangedAttack(@NotNull LivingEntity entity, float f) {
        EntityBullet bullet = new EntityBullet(level(), this, entity, 1.6F, 2);
        bullet.setIsCritical(true);
        bullet.setTau(true);
        bullet.damage = 3;
        level().addFreshEntity(bullet);
        this.playSound(HbmSoundsNT.get("hbm:weapon.sawShoot"), 1.0F, 2.0F);
    }

    /** {@code dropRareDrop}: 1.7-Formel {@code rand(200) - looting < 5} bei Spielertreffer. */
    protected void dropRareDrop() { }

    @Override
    protected void dropCustomDeathLoot(@NotNull DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (recentlyHit && this.random.nextInt(200) - looting < 5) dropRareDrop();
    }
}
