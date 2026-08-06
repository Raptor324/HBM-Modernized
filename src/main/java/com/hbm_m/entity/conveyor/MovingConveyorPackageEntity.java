package com.hbm_m.entity.conveyor;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterablePackageBlock;
import com.hbm_m.entity.ModEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Port von {@code com.hbm.entity.item.EntityMovingPackage} (1.7.10 Original) - traegt mehrere
 * {@link ItemStack}s gebuendelt als "Paket" ueber Foerderbaender, benutzt von Crane Boxer/Unboxer.
 * Bewegungslogik 1:1 aus {@link MovingConveyorItemEntity} uebernommen (dort bereits als
 * "abstrakte Basis in konkrete Klasse gefaltet" dokumentiert - hier fuer den Paket-Payload-Typ
 * dupliziert, da Java keine Mehrfachvererbung erlaubt und eine gemeinsame Basisklasse fuer nur
 * zwei Konkretisierungen keinen Mehrwert haette).
 * <p>
 * SCOPE-Vereinfachung: Rendering nutzt {@code ThrownItemRenderer} ueber {@link #getItem()} (zeigt
 * nur den ersten Inhalt), kein eigenes Kisten-Modell - konsistent mit der bereits dokumentierten
 * Vereinfachung an {@link MovingConveyorItemEntity}.
 */
public class MovingConveyorPackageEntity extends Entity implements ItemSupplier {

    private static final EntityDataAccessor<ItemStack> DISPLAY_ITEM =
            SynchedEntityData.defineId(MovingConveyorPackageEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final double MOVE_SPEED = 0.0625D;

    private ItemStack[] contents = new ItemStack[0];

    public MovingConveyorPackageEntity(EntityType<? extends MovingConveyorPackageEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static MovingConveyorPackageEntity create(Level level, double x, double y, double z, ItemStack[] contents) {
        MovingConveyorPackageEntity entity = new MovingConveyorPackageEntity(ModEntities.MOVING_CONVEYOR_PACKAGE.get(), level);
        entity.setPos(x, y, z);
        entity.setContents(contents);
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DISPLAY_ITEM, ItemStack.EMPTY);
    }

    public void setContents(ItemStack[] contents) {
        this.contents = contents;
        this.entityData.set(DISPLAY_ITEM, contents.length > 0 ? contents[0] : ItemStack.EMPTY);
    }

    public ItemStack[] getContents() {
        return contents;
    }

    @Override
    public ItemStack getItem() {
        return this.entityData.get(DISPLAY_ITEM);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean skipAttackInteraction(Entity attacker) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            dropAll();
        }
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            dropAll();
        }
        return true;
    }

    private void dropAll() {
        this.discard();
        for (ItemStack stack : contents) {
            if (stack.isEmpty()) continue;
            ItemEntity item = new ItemEntity(this.level(), getX(), getY() + 0.125, getZ(), stack);
            item.setDeltaMovement(getDeltaMovement());
            this.level().addFreshEntity(item);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && !this.isRemoved()) {
            List<ItemStack> leftover = new ArrayList<>();
            for (ItemStack stack : contents) {
                if (stack.isEmpty()) continue;
                if (!player.getInventory().add(stack.copy())) {
                    leftover.add(stack);
                }
            }
            this.discard();
            for (ItemStack stack : leftover) {
                ItemEntity item = new ItemEntity(this.level(), getX(), getY(), getZ(), stack);
                this.level().addFreshEntity(item);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) return;
        if (this.tickCount <= 5) return;

        BlockPos blockPos = BlockPos.containing(getX(), getY(), getZ());
        BlockState state = this.level().getBlockState(blockPos);
        Vec3 currentPos = new Vec3(getX(), getY(), getZ());

        boolean onConveyor = state.getBlock() instanceof IConveyorBelt belt
                && belt.canItemStay(this.level(), blockPos, currentPos);

        if (!onConveyor) {
            dropAll();
            return;
        }

        IConveyorBelt belt = (IConveyorBelt) state.getBlock();
        Vec3 target = belt.getTravelLocation(this.level(), blockPos, currentPos, MOVE_SPEED);
        this.setDeltaMovement(target.x - getX(), target.y - getY(), target.z - getZ());

        BlockPos before = BlockPos.containing(getX(), getY(), getZ());
        move(getDeltaMovement());
        BlockPos after = BlockPos.containing(getX(), getY(), getZ());

        if (!before.equals(after)) {
            Block enteredBlock = this.level().getBlockState(after).getBlock();
            if (enteredBlock instanceof IEnterablePackageBlock enterable) {
                enterable.onPackageEnter(this.level(), after, this);
                this.discard();
            }
        }
    }

    private void move(Vec3 delta) {
        this.setPos(getX() + delta.x, getY() + delta.y, getZ() + delta.z);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        ListTag list = tag.getList("contents", 10);
        List<ItemStack> loaded = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = ItemStack.of(list.getCompound(i));
            if (!stack.isEmpty()) loaded.add(stack);
        }
        if (loaded.isEmpty()) {
            this.discard();
        } else {
            setContents(loaded.toArray(new ItemStack[0]));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) list.add(stack.save(new CompoundTag()));
        }
        tag.put("contents", list);
    }

    @Override
    public boolean shouldBeSaved() {
        return contents.length > 0 && super.shouldBeSaved();
    }
}
