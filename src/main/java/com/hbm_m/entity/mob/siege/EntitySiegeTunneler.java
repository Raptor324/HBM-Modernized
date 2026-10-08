package com.hbm_m.entity.mob.siege;

import java.util.UUID;

import javax.annotation.Nullable;

import com.hbm_m.entity.mob.EntityBurrowingSwingingBase;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * 1:1 {@code com.hbm.entity.mob.siege.EntitySiegeTunneler}: grabender Belagerungsbohrer (1x1). Panzerung, Leben
 * (Stufe x 0,5) und Schadensmultiplikator kommen aus der {@link SiegeTier}.
 *
 * <p>Im Original nicht in {@code EntityMappings} eingetragen; im Port registriert (per /summon), ohne Spawn-Ei.</p>
 */
public class EntitySiegeTunneler extends EntityBurrowingSwingingBase {

    private static final EntityDataAccessor<Integer> TIER = SynchedEntityData.defineId(EntitySiegeTunneler.class, EntityDataSerializers.INT);
    private static final UUID TIER_DAMAGE_UUID = UUID.fromString("5b0c2e60-6f6d-4c1d-9b1e-7d0a3c51e7a2");

    public EntitySiegeTunneler(EntityType<? extends EntitySiegeTunneler> type, Level world) {
        super(type, world);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.ATTACK_DAMAGE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.15D);
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {

        if (this.isInvulnerableTo(source))
            return false;

        SiegeTier tier = this.getTier();

        if (tier.fireProof && source.is(DamageTypeTags.IS_FIRE)) {
            this.clearFire();
            return false;
        }

        // noFF: von anderen Mobs nicht verletzbar
        if (tier.noFriendlyFire && source.getEntity() != null && !(source.getEntity() instanceof Player))
            return false;

        damage -= tier.dt;

        if (damage < 0) {
            return false;
        }

        damage *= (1F - tier.dr);

        return super.hurt(source, damage);
    }

    //? if < 1.21.1 {
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TIER, 0);
    }
    //?}

    public void setTier(SiegeTier tier) {
        this.entityData.set(TIER, tier.id);

        AttributeInstance attack = this.getAttribute(Attributes.ATTACK_DAMAGE);
        attack.removeModifier(TIER_DAMAGE_UUID);
        attack.addPermanentModifier(new AttributeModifier(TIER_DAMAGE_UUID, "Tier Damage Mod", tier.damageMod, AttributeModifier.Operation.MULTIPLY_BASE));
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(tier.health * 0.5);
        this.setHealth(this.getMaxHealth());
    }

    public SiegeTier getTier() {
        SiegeTier tier = SiegeTier.tiers[this.entityData.get(TIER)];
        return tier != null ? tier : SiegeTier.CLAY;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("siegeTier", this.getTier().id);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        SiegeTier tier = SiegeTier.tiers[nbt.getInt("siegeTier")];
        this.setTier(tier != null ? tier : SiegeTier.CLAY);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data, @Nullable CompoundTag tag) {
        this.setTier(SiegeTier.tiers[random.nextInt(SiegeTier.getLength())]);
        return super.finalizeSpawn(level, difficulty, reason, data, tag);
    }
}
