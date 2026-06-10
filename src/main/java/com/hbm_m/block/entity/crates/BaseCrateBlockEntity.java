package com.hbm_m.block.entity.crates;

import com.hbm_m.block.machines.crates.CrateValidation;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.platform.ModItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Базовый BlockEntity для всех ящиков HBM.
 * Управляет инвентарём, сериализацией NBT и capability.
 */
public abstract class BaseCrateBlockEntity extends BlockEntity implements MenuProvider {

    protected final ModItemStackHandler itemHandler;

    protected BaseCrateBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.itemHandler = new ModItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return CrateValidation.isValidForCrate(stack);
            }
        };
    }

    // Forge item handler capabilities removed for Fabric compilation.

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }
    }

    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void saveToItem(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        if (!tag.isEmpty()) {
            stack.addTagElement("BlockEntityTag", tag);
        }
    }

    public ModItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public int getSlotCount() {
        return itemHandler.getSlots();
    }
}
