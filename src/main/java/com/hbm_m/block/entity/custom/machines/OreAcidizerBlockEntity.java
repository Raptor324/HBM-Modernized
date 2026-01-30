
package com.hbm_m.block.entity.custom.machines;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.network.chat.Component;
// removed unused import
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
// removed invalid import
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import com.hbm_m.menu.OreAcidizerMenu;
import net.minecraft.world.level.block.state.BlockState;
import com.hbm_m.block.entity.ModBlockEntities;

public class OreAcidizerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(8);
    private final LazyOptional<IItemHandler> handlerOptional = LazyOptional.of(() -> itemHandler);

    public IItemHandler getInventory() {
        return itemHandler;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.hbm_m.ore_acidizer");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new OreAcidizerMenu(id, inv, this, new net.minecraft.world.inventory.SimpleContainerData(1));
    }

    public boolean stillValid(Player player) {
        return !this.isRemoved() && player.distanceToSqr(this.getBlockPos().getCenter()) <= 64.0D;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return handlerOptional.cast();
        }
        return super.getCapability(cap, side);
    }
    public OreAcidizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_ACIDIZER.get(), pos, state);
    }

    // Multiblock check: 3x3x7 (controller is at bottom center)
    public boolean isMultiblockFormed() {
        if (level == null) return false;
        BlockPos base = getBlockPos();
        // Check a 3x3x7 pillar, controller at (0,0,0) (bottom center)
        for (int y = 0; y < 7; y++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    BlockPos checkPos = base.offset(x, y, z);
                    if (!(level.getBlockState(checkPos).getBlock() instanceof com.hbm_m.block.custom.machines.OreAcidizerBlock)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
