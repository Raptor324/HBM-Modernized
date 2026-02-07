
package com.hbm_m.block.entity.custom.machines;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import com.hbm_m.multiblock.IMultiblockController;
import com.hbm_m.multiblock.MultiblockStructureHelper;
import com.hbm_m.block.custom.machines.UniversalMachinePartBlock;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraft.nbt.CompoundTag;

public class MachineOreAcidizerBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler itemHandler = new ItemStackHandler(8);
    private final LazyOptional<IItemHandler> handlerOptional = LazyOptional.of(() -> itemHandler);

    private final FluidTank fluidTank = new FluidTank(16_000);
    private final LazyOptional<IFluidHandler> fluidOptional = LazyOptional.of(() -> fluidTank);

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
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return fluidOptional.cast();
        }
        return super.getCapability(cap, side);
    }
    public MachineOreAcidizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_ACIDIZER.get(), pos, state);
    }

    // Multiblock check based on the controller's structure helper (parts are UniversalMachinePartBlock).
    public boolean isMultiblockFormed() {
        if (level == null) return false;
        BlockPos controllerPos = getBlockPos();
        BlockState controllerState = level.getBlockState(controllerPos);
        if (!(controllerState.getBlock() instanceof IMultiblockController controller)) return false;

        Direction facing = controllerState.getValue(HorizontalDirectionalBlock.FACING);
        MultiblockStructureHelper helper = controller.getStructureHelper();

        for (BlockPos localOffset : helper.getStructureMap().keySet()) {
            if (localOffset.equals(BlockPos.ZERO)) continue;
            BlockPos partPos = helper.getRotatedPos(controllerPos, localOffset, facing);
            if (!(level.getBlockState(partPos).getBlock() instanceof UniversalMachinePartBlock)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Inventory", itemHandler.serializeNBT());
        tag.put("Tank", fluidTank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("Inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("Inventory"));
        }
        if (tag.contains("Tank")) {
            fluidTank.readFromNBT(tag.getCompound("Tank"));
        }
    }
}
