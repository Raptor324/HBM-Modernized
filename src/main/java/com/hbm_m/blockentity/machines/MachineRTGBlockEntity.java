package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.inventory.menu.MachineRTGMenu;
import com.hbm_m.item.machine.ItemRTGPellet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityMachineRTG} (1.7.10): fuenfzehn Steckplaetze fuer RTG-Pellets,
 * deren Waerme sich aufsummiert und in Energie umsetzt.
 *
 * <p>Die Formel des Originals ist denkbar knapp: {@code heat} ist die Summe der Pelletwaerme,
 * gedeckelt auf {@link #HEAT_MAX}, und je Tick kommen {@code heat * 5} Energie dazu. Der
 * Generator hat weder Rezept noch Fortschritt - er laeuft einfach, solange Pellets drin sind.</p>
 *
 * <p>Die Pellets <b>zerfallen</b> dabei: jeder Tick nimmt einen von ihrer Lebensdauer, die
 * Heizleistung sinkt mit der Restlaufzeit, und am Ende bleibt das ausgebrannte Pellet zurueck.
 * Ein Bleipellet heizt mit 600 und ist nach Minuten durch, ein Radiumpellet heizt mit 3 und haelt
 * jahrelang - siehe {@link ItemRTGPellet}.</p>
 */
public class MachineRTGBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder {

    /** Original: {@code slots = new ItemStack[15]}. */
    public static final int INVENTORY_SIZE = 15;

    /** Original: {@code heatMax = rtgDecay() ? 600 : 200} - der Zerfall ist an, also 600. */
    private static final int HEAT_MAX = 600;
    /** Original: {@code powerMax = 100000}. */
    private static final long MAX_POWER = 100_000L;
    /** Original: {@code power += heat * 5}. */
    private static final int ENERGY_PER_HEAT = 5;

    /** Alle fuenfzehn Steckplaetze - im Original {@code slot_io}. */
    private static final int[] ALL_SLOTS = java.util.stream.IntStream.range(0, INVENTORY_SIZE).toArray();

    private int heat = 0;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> heat;
                case 1 -> HEAT_MAX;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) { }

        @Override
        public int getCount() {
            return 2;
        }
    };

    public MachineRTGBlockEntity(BlockPos pos, BlockState state) {
        // Reiner Erzeuger: nimmt nichts auf, gibt alles ab.
        super(ModBlockEntities.MACHINE_RTG_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 0L, MAX_POWER);
    }

    @Override
    public int getCurrentMode() {
        return 2; // Nur Ausgabe - das Energienetz behandelt den Block als Generator.
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineRTGBlockEntity be) {
        if (level.isClientSide()) return;

        be.ensureNetworkInitialized();

        // 1:1: RTGUtil.updateRTGs summiert die Waerme und laesst dabei jedes Pellet altern.
        int heat = ItemRTGPellet.updateRTGs(be.inventory, ALL_SLOTS);

        be.heat = Math.min(heat, HEAT_MAX);

        if (be.heat > 0) {
            be.setEnergyStored(Math.min(be.getMaxEnergyStored(),
                    be.getEnergyStored() + (long) be.heat * ENERGY_PER_HEAT));
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    public int getHeat() {
        return heat;
    }

    /** Original: {@code getHeatScaled} / {@code getPowerScaled}. */
    public int getHeatScaled(int scale) {
        return HEAT_MAX <= 0 ? 0 : (heat * scale) / HEAT_MAX;
    }

    public long getPowerScaled(int scale) {
        long max = getMaxEnergyStored();
        return max <= 0 ? 0 : (getEnergyStored() * scale) / max;
    }

    public ContainerData getContainerData() {
        return data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Original: isItemValidForSlot akzeptiert ausschliesslich ItemRTGPellet.
        return stack.getItem() instanceof ItemRTGPellet;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("heat", heat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        heat = tag.getInt("heat");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_rtg");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineRTGMenu(id, inv, this);
    }
}
