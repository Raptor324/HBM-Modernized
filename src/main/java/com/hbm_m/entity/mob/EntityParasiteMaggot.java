package com.hbm_m.entity.mob;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;

/**
 * 1:1 {@code EntityParasiteMaggot}: Made aus befallenen Glyphiden, 8 Leben, 2 Schaden, alte {@code EntityMob}-KI
 * (Spieler bis 16 Bloecke, Nahkampf unter 2 Bloecken alle 20 Ticks), lautlos beim Laufen.
 */
public class EntityParasiteMaggot extends Monster {

    @Nullable private Entity entityToAttack;
    @Nullable private Path pathToEntity;
    private int attackTime;

    public EntityParasiteMaggot(EntityType<? extends EntityParasiteMaggot> type, Level world) {
        super(type, world);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, 16D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (attackTime > 0) attackTime--;

        if (entityToAttack == null) {
            entityToAttack = level().getNearestPlayer(TargetingConditions.forCombat().range(16).ignoreLineOfSight(), this);
            if (entityToAttack != null) pathToEntity = getNavigation().createPath(entityToAttack, 0);
        } else if (entityToAttack.isAlive()) {
            float f = entityToAttack.distanceTo(this);
            if (hasLineOfSight(entityToAttack) && attackTime <= 0 && f < 2.0F
                    && entityToAttack.getBoundingBox().maxY > getBoundingBox().minY && entityToAttack.getBoundingBox().minY < getBoundingBox().maxY) {
                attackTime = 20;
                doHurtTarget(entityToAttack);
            }
        } else {
            entityToAttack = null;
        }

        if (entityToAttack != null && (pathToEntity == null || random.nextInt(20) == 0)) {
            pathToEntity = getNavigation().createPath(entityToAttack, 0);
        } else if ((pathToEntity == null && random.nextInt(180) == 0 || random.nextInt(120) == 0) && noActionTime < 100) {
            // Original EntityCreature.updateWanderPath: bester von zehn Zufallspunkten
            BlockPos best = null;
            float bestValue = -99999.0F;
            for (int l = 0; l < 10; ++l) {
                BlockPos p = BlockPos.containing(getX() + random.nextInt(13) - 6.0D, getY() + random.nextInt(7) - 3.0D, getZ() + random.nextInt(13) - 6.0D);
                float v = getWalkTargetValue(p);
                if (v > bestValue) {
                    bestValue = v;
                    best = p;
                }
            }
            if (best != null) pathToEntity = getNavigation().createPath(best, 0);
        }

        if (pathToEntity != null && getNavigation().getPath() != pathToEntity) {
            // Alte KI: Grundfaktor 0,1, Vorwaertsanteil = Geschwindigkeitswert (hoechstens 1)
            double attr = getAttributeValue(Attributes.MOVEMENT_SPEED);
            getNavigation().moveTo(pathToEntity, attr > 0 ? 0.1D * Math.min(1D, attr) / attr : 0);
        }
        if (getNavigation().isDone()) pathToEntity = null;
    }

    @Override
    public void tick() {
        this.yBodyRot = this.getYRot();
        super.tick();
    }

    /** Original {@code canTriggerWalking() = false}: keine Schritte. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState state) { }

    @Override
    public MobType getMobType() {
        return MobType.ARTHROPOD;
    }
}
