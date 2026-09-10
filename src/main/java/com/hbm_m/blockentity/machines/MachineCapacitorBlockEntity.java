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
 * <p><b>Die Sammelschiene.</b> Wie im Original laesst sich der Ausgang verlaengern: hinter dem
 * Kondensator duerfen beliebig viele {@code capacitor_bus}-Bloecke in gerader Linie stehen, und
 * abgegeben wird am Ende dieser Kette. Alle Schienen muessen in dieselbe Richtung zeigen - knickt
 * die Reihe, gilt die ganze Kette als ungueltig und der Kondensator gibt nichts ab. So laesst sich
 * eine Batteriebank hinter einer Wand verstecken und trotzdem sauber anzapfen.</p>
 *
 * <p><b>Nicht portiert:</b> Redstone-over-Radio und OpenComputers - beides hat hier keine
 * Entsprechung.
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

    /**
     * 1:1-Port der Schienenschleife aus {@code TileEntityCapacitor.updateEntity}: von der
     * Rueckseite aus wird der Kette gefolgt, solange dort Schienen mit gleicher Ausrichtung
     * stehen. Das Ende ist die Stelle, an der abgegeben wird.
     */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        if (level == null) return new BlockPos[0];

        Direction facing = getBlockState().getValue(MachineCapacitorBlock.FACING);
        BlockPos pos = worldPosition.relative(facing.getOpposite());

        Direction last = null;
        // Ohne Deckel: eine sehr lange Kette ist zulaessig, eine endlose nicht.
        for (int step = 0; step < 64; step++) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() != com.hbm_m.block.ModBlocks.CAPACITOR_BUS.get()) break;
            if (!state.hasProperty(MachineCapacitorBlock.FACING)) break;

            Direction current = state.getValue(MachineCapacitorBlock.FACING);
            if (last == null) last = current;

            // Original: knickt die Reihe, faellt die ganze Kette weg.
            if (last != current) return new BlockPos[0];

            pos = pos.relative(current);
        }

        // Keine Schiene gefunden - der Kondensator gibt an seiner eigenen Seite ab.
        if (last == null) return new BlockPos[0];

        return new BlockPos[] { pos };
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
