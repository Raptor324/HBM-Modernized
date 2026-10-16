package com.hbm_m.blockentity.decorations;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code BlockPedestal.TileEntityPedestal} (1.7.10).
 * Хранит один стек, который клиент рендерит парящим над постаментом
 * ({@link com.hbm_m.client.render.implementations.PedestalRenderer}).
 */
public class PedestalBlockEntity extends BaseHbmBlockEntity {

    private ItemStack item = ItemStack.EMPTY;

    public PedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PEDESTAL_BE.get(), pos, state);
    }

    public ItemStack getItem() {
        return item;
    }

    public void setItem(ItemStack stack) {
        this.item = stack;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        if (!item.isEmpty()) {
            tag.put("item", PlatformHooks.saveItemStack(item, new net.minecraft.nbt.CompoundTag(), registries));
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        item = tag.contains("item") ? PlatformHooks.itemStackOf(tag.getCompound("item"), registries) : ItemStack.EMPTY;
    }
}
