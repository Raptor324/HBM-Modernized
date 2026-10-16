package com.hbm_m.blockentity.machines;

import com.hbm_m.block.machines.MachineCapacitorBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyReceiver;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code MachineCapacitor.TileEntityCapacitor} (1.7.10 Original) - a directional HE energy
 * buffer with no GUI/inventory. Charges 3x faster than it discharges (matching the original's fixed
 * {@code maxPower/200} receive vs {@code maxPower/600} provide ratio) and sits at LOW network
 * priority, i.e. a "last resort" sink/source. Only connects on the face it was placed against.
 * <p>
 * SCOPE-Vereinfachung: Die {@code capacitor_bus}-Verlaengerungskette (ein Kondensator "leiht" die
 * Anschlussseite eines in gerader Linie anschliessenden {@code capacitor_bus}-Blocks) entfaellt -
 * der Kondensator verbindet sich direkt ueber seine eigene Facing-Seite. Redstone-Over-Radio und
 * OpenComputers-Anbindung ebenso nicht portiert (siehe Klassenkommentar-Konvention dieses Ports).
 */
public class MachineCapacitorBlockEntity extends BaseMachineBlockEntity {

    public MachineCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_CAPACITOR_BE.get(), pos, state, 0,
                capacityFor(state), Math.max(1, capacityFor(state) / 200), Math.max(1, capacityFor(state) / 600));
    }

    private static long capacityFor(BlockState state) {
        return state.getBlock() instanceof MachineCapacitorBlock b ? b.getCapacity() : 1_000_000L;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return IEnergyReceiver.Priority.LOW;
    }

    @Override
    public boolean canConnectEnergy(Direction side) {
        return getBlockState().getValue(MachineCapacitorBlock.FACING) == side;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_capacitor");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }
}
