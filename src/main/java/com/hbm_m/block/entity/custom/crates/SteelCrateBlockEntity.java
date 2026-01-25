package com.hbm_m.block.entity.custom.crates;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.menu.SteelCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity для Steel Crate (54 слота)
 * ✅ ТОЧНАЯ КОПИЯ IronCrateBlockEntity с 54 слотами
 */
public class SteelCrateBlockEntity extends BlockEntity implements MenuProvider {

    private static final int SLOTS = 54; // ✅ 54 СЛОТА ДЛЯ STEEL

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged(); // Обновляем при изменении содержимого
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true; // Разрешаем любые предметы
        }
    };

    private LazyOptional<ItemStackHandler> lazyItemHandler = LazyOptional.empty();

    public SteelCrateBlockEntity(BlockPos pos, BlockState state) {
        // ✅ ТОЧНО КАК В IRON
        super(com.hbm_m.block.entity.ModBlockEntities.STEEL_CRATE_BE.get(), pos, state);
    }

    // ✅ ТОЧНО КАК В IRON
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    // ✅ ТОЧНО КАК В IRON
    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    // ✅ ТОЧНО КАК В IRON
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // ✅ ТОЧНО КАК В IRON
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    // ✅ ТОЧНО КАК В IRON
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }
    }

    /**
     * Проверяет, пустой ли ящик
     */
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Сохраняет содержимое в ItemStack при разрушении блока
     */
    public void saveToItem(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        if (!tag.isEmpty()) {
            stack.addTagElement("BlockEntityTag", tag);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.crate_steel");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // ✅ SteelCrateMenu вместо IronCrateMenu
        return new SteelCrateMenu(containerId, playerInventory, this);
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
