package com.hbm_m.blockentity.bomb;

import com.hbm_m.block.bomb.LargeNukeType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.LargeNukeMenu;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * BE большой ядерной бомбы. Один тип BE на все четыре бомбы,
 * конкретный тип хранится в NBT (устойчив к смене версии/мира).
 */
public class LargeNukeBlockEntity extends NukeBaseBlockEntity {

    private LargeNukeType type;

    public LargeNukeBlockEntity(BlockPos pos, BlockState state, LargeNukeType type) {
        super(ModBlockEntities.LARGE_NUKE_BE.get(), pos, state, type.slots());
        this.type = type;
    }

    public LargeNukeType getNukeType() {
        if (type == null) {
            // После загрузки из NBT конструктор мог получить дефолт; восстанавливаем по блоку.
            type = level != null && getBlockState().getBlock() instanceof com.hbm_m.block.bomb.LargeNukeBlock b
                    ? b.type : LargeNukeType.GADGET;
        }
        return type;
    }

    @Override
    public Component getDefaultName() {
        return Component.translatable(getNukeType().containerKey());
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return getNukeType().canPlace(slot, stack);
    }

    @Override
    public boolean isReady() {
        return getNukeType().isReady(slots.toArray(new ItemStack[0]));
    }

    @Override
    public boolean isFilled() {
        return getNukeType().isFilled(slots.toArray(new ItemStack[0]));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new LargeNukeMenu(id, inventory, this);
    }

    @Override
    protected void readNbtData(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
                               @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("NukeType", 8)) {
            this.type = LargeNukeType.byId(tag.getString("NukeType"));
        }
    }

    @Override
    protected void writeNbtData(@org.jetbrains.annotations.NotNull net.minecraft.nbt.CompoundTag tag,
                                @org.jetbrains.annotations.Nullable net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putString("NukeType", getNukeType().id());
    }
}
