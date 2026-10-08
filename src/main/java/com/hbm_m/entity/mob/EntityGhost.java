package com.hbm_m.entity.mob;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code EntityGhost}: erscheint 75 Bloecke vor Spielern mit Digamma, unverwundbar, wandert nur umher und
 * verschwindet, sobald ein Spieler naeher als 50 Bloecke kommt.
 */
public class EntityGhost extends PathfinderMob {

    public EntityGhost(EntityType<? extends EntityGhost> type, Level world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }

    @Override
    public void tick() {
        super.tick();

        if (!level().isClientSide) {
            double despawnRange = 50;
            List<Player> players = level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(despawnRange, despawnRange, despawnRange));

            if (!players.isEmpty())
                this.discard();
        }
    }

    @Override
    public void setHealth(float health) {
        super.setHealth(this.getMaxHealth());
    }

    @Override
    public boolean isInvulnerableTo(@NotNull DamageSource source) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double d = this.getBoundingBox().getSize() * 64.0D * 10 * getViewScale();
        return distance < d * d;
    }
}
