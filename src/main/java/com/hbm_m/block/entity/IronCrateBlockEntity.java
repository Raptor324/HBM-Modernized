package com.hbm_m.block.entity;

import com.hbm_m.menu.IronCrateMenu;
import com.hbm_m.block.entity.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class IronCrateBlockEntity extends BlockEntity implements MenuProvider {


    private static final int SLOTS = 36;


    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public IronCrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_CRATE_BE.get(), pos, state);
    }

    // ==================== CAPABILITY SYSTEM ====================


    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    // ==================== NBT SERIALIZATION ====================

    /**
     * Сохранение инвентаря в NBT (при сохранении мира)
     */
    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    /**
     * Загрузка инвентаря из NBT (при загрузке мира)
     */
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    // ==================== ITEM DROPPING ====================

    /**
     * Выбрасывает все предметы в мир при разрушении блока
     */
    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ==================== MENU PROVIDER ====================

    /**
     * Название для GUI (будет переведено через локализацию)
     */
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.crate_iron");
    }

    /**
     * Создаёт Menu для GUI
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new IronCrateMenu(containerId, playerInventory, this);
    }

    // ==================== GETTERS ====================

    /**
     * Доступ к ItemHandler для Menu
     */
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}