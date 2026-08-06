package com.hbm_m.entity.conveyor;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Port of {@code com.hbm.entity.item.EntityMovingItem} (which extends the abstract
 * {@code EntityMovingConveyorObject}, 1.7.10 Original) - a lightweight entity that carries a single
 * {@link ItemStack} smoothly along a chain of conveyor belt blocks, snapping to lane positions and
 * following curves/lifts/chutes via {@link IConveyorBelt}. Renders via vanilla {@code ThrownItemRenderer}
 * since this entity implements {@link ItemSupplier} (see {@code ClientSetup} registration).
 * <p>
 * Simplification: the original's separate abstract base class ({@code EntityMovingConveyorObject},
 * meant to be reusable for non-item "packages") is folded directly into this concrete class since
 * this port only needs the item-carrying variant - no other conveyor payload type exists here.
 * The 25-entities-in-one-spot "jam explosion" safety net from the original is kept 1:1.
 */
public class MovingConveyorItemEntity extends Entity implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> ITEM =
            SynchedEntityData.defineId(MovingConveyorItemEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final double MOVE_SPEED = 0.0625D;

    public MovingConveyorItemEntity(EntityType<? extends MovingConveyorItemEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static MovingConveyorItemEntity create(Level level, double x, double y, double z, ItemStack stack) {
        MovingConveyorItemEntity entity = new MovingConveyorItemEntity(ModEntities.MOVING_CONVEYOR_ITEM.get(), level);
        entity.setPos(x, y, z);
        entity.setItem(stack);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ITEM, ItemStack.EMPTY);
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(ITEM);
    }

    public void setItem(ItemStack stack) {
        this.entityData.set(ITEM, stack);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            knockOff();
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            knockOff();
        }
        return true;
    }

    private void knockOff() {
        this.discard();
        ItemEntity item = new ItemEntity(this.level(), getX(), getY(), getZ(), this.getItem());
        item.setDeltaMovement(getDeltaMovement());
        this.level().addFreshEntity(item);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            if (player.getInventory().add(this.getItem().copy())) {
                this.discard();
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;
        if (this.tickCount <= 5) return;

        // Jam safety net: if too many conveyor items pile up in one spot, blow the belt (1:1 w/ original).
        if ((this.tickCount + this.getId()) % 400 == 0) {
            var nearby = this.level().getEntitiesOfClass(MovingConveyorItemEntity.class,
                    this.getBoundingBox().inflate(0.125, 0.125, 0.125));
            if (nearby.size() >= 25) {
                for (MovingConveyorItemEntity obj : nearby) obj.discard();
                BlockPos pos = BlockPos.containing(getX(), getY(), getZ());
                this.level().explode(this, getX(), getY() + 0.125, getZ(), 1.0F, Level.ExplosionInteraction.BLOCK);
                if (this.level().getBlockState(pos).getBlock() instanceof IConveyorBelt) {
                    this.level().removeBlock(pos, false);
                }
                return;
            }
        }

        BlockPos blockPos = BlockPos.containing(getX(), getY(), getZ());
        BlockState state = this.level().getBlockState(blockPos);
        Vec3 currentPos = new Vec3(getX(), getY(), getZ());

        boolean onConveyor = state.getBlock() instanceof IConveyorBelt belt
                && belt.canItemStay(this.level(), blockPos, currentPos);

        if (!onConveyor) {
            knockOff();
            return;
        }

        IConveyorBelt belt = (IConveyorBelt) state.getBlock();
        Vec3 target = belt.getTravelLocation(this.level(), blockPos, currentPos, getMoveSpeed());
        this.setDeltaMovement(target.x - getX(), target.y - getY(), target.z - getZ());

        BlockPos before = BlockPos.containing(getX(), getY(), getZ());
        move(getDeltaMovement());
        BlockPos after = BlockPos.containing(getX(), getY(), getZ());

        if (!before.equals(after)) {
            Block enteredBlock = this.level().getBlockState(after).getBlock();
            if (enteredBlock instanceof com.hbm_m.block.network.IEnterableBlock enterable) {
                enterable.onItemEnter(this.level(), after, this);
            }
        }
    }

    private void move(Vec3 delta) {
        this.setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
    }

    public double getMoveSpeed() {
        return MOVE_SPEED;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Item")) {
            ItemStack stack = ItemStack.of(tag.getCompound("Item"));
            if (stack.isEmpty()) {
                this.discard();
            } else {
                setItem(stack);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ItemStack stack = getItem();
        if (!stack.isEmpty()) {
            tag.put("Item", stack.save(new CompoundTag()));
        }
    }

    @Override
    public boolean shouldBeSaved() {
        return !this.getItem().isEmpty() && super.shouldBeSaved();
    }
}
