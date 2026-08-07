package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.util.RtgPelletHeat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityMachineRTG} (1.7.10 Original) - a radioisotope thermoelectric generator.
 * 15 pellet slots feed a heat pool every tick, which is converted into stored HE power
 * ({@code power += heat * 5}, matching the original 1:1).
 * <p>
 * SCOPE-Vereinfachung: Das Original laesst RTG-Pellets ueber Jahrzehnte/Jahrhunderte langsam in ein
 * "depleted" Item zerfallen (Halbwertszeit-basierte {@code PELLET_DEPLETION}-NBT, nur aktiv wenn
 * {@code VersatileConfig.rtgDecay()} an ist - standardmaessig AUS). Dieser Port implementiert nur
 * den Nicht-Zerfall-Pfad (konstanter Heat-Wert pro Pellet-Typ, wie es auch beim Original mit
 * deaktiviertem Decay-Config passiert) - der Kernmechanismus (Pellets &rarr; Waerme &rarr; Strom)
 * bleibt vollstaendig erhalten.
 */
public class MachineRtgBlockEntity extends BaseMachineBlockEntity {

    public static final int INVENTORY_SIZE = 15;
    private static final int HEAT_MAX = 200;
    private static final long POWER_MAX = 100_000L;
    private static final long PROVIDE_SPEED = 5_000L;

    private int heat = 0;

    public MachineRtgBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_RTG_BE.get(), pos, state, INVENTORY_SIZE, POWER_MAX, 0L, PROVIDE_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRtgBlockEntity be) {
        if (level.isClientSide) return;

        int newHeat = 0;
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            ItemStack stack = be.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            newHeat += RtgPelletHeat.getHeat(stack.getItem());
        }
        be.heat = Math.min(newHeat, HEAT_MAX);

        if (be.heat > 0) {
            be.setEnergyStored(Math.min(be.getMaxEnergyStored(), be.getEnergyStored() + (long) be.heat * 5L));
        }

        be.setChanged();
    }

    public int getHeat() { return heat; }
    public int getHeatMax() { return HEAT_MAX; }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return RtgPelletHeat.getHeat(stack.getItem()) > 0;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_rtg");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return com.hbm_m.inventory.menu.MachineRtgMenu.create(id, inventory, this);
    }
}
