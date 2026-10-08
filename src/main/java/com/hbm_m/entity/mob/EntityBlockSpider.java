package com.hbm_m.entity.mob;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;

/**
 * 1:1 {@code EntityBlockSpider} ({@code entity_taintcrawler}): ein Block auf acht Beinen. {@link #makeBlock} setzt den
 * getragenen Block; die Lebenspunkte entsprechen dann seinem Explosionswiderstand (mindestens 1).
 */
public class EntityBlockSpider extends Monster {

    private static final EntityDataAccessor<Integer> BLOCK = SynchedEntityData.defineId(EntityBlockSpider.class, EntityDataSerializers.INT);

    public EntityBlockSpider(EntityType<? extends EntityBlockSpider> type, Level world) {
        super(type, world);
        this.setPathfindingMalus(BlockPathTypes.WATER, -1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new RandomStrollGoal(this, 0.5F));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 1F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        // Original: Block-ID 1 (Stein), Meta 0
        this.entityData.define(BLOCK, Block.getId(Blocks.STONE.defaultBlockState()));
    }

    public BlockState getBlock() {
        return Block.stateById(this.entityData.get(BLOCK));
    }

    public void makeBlock(BlockState state) {
        this.entityData.set(BLOCK, Block.getId(state));
        double health = Math.max(1D, state.getBlock().getExplosionResistance());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
        this.setHealth(this.getMaxHealth());
    }
}
