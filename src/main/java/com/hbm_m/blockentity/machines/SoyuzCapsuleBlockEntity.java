package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.SoyuzCapsuleMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** 1:1 {@code TileEntitySoyuzCapsule}: Inventar mit 19 Plaetzen, Name {@code container.soyuzCapsule}. */
public class SoyuzCapsuleBlockEntity extends BaseContainerBlockEntity {

    private NonNullList<ItemStack> slots = NonNullList.withSize(19, ItemStack.EMPTY);

    public SoyuzCapsuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOYUZ_CAPSULE_BE.get(), pos, state);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.hbm_m.soyuz_capsule");
    }

    @Override
    protected @NotNull AbstractContainerMenu createMenu(int id, @NotNull Inventory inv) {
        return new SoyuzCapsuleMenu(id, inv, this);
    }

    @Override public int getContainerSize() { return slots.size(); }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : slots) if (!s.isEmpty()) return false;
        return true;
    }

    @Override public @NotNull ItemStack getItem(int slot) { return slots.get(slot); }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        ItemStack ret = ContainerHelper.removeItem(slots, slot, amount);
        if (!ret.isEmpty()) setChanged();
        return ret;
    }

    @Override public @NotNull ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(slots, slot); }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        slots.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return level != null && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override public void clearContent() { slots.clear(); }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        slots = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(nbt, slots);
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag nbt) {
        super.saveAdditional(nbt);
        ContainerHelper.saveAllItems(nbt, slots);
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY() - 1, worldPosition.getZ() - 1, worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
    }
}
