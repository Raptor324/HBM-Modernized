package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Port of {@code TileEntityHeaterElectric} (1.7.10 Original) - HE-powered heat generator, output
 * scales with a screwdriver-adjustable 0..10 "setting".
 * <p>
 * <p>Er belegt wie im Original zwei Felder in der Tiefe und drei in der Breite
 * ({@code getDimensions {0,0,1,2,1,1}}, Setzversatz 2).</p>
 *
 * <p><b>Offen:</b> die Zusatzwaerme, die das Original passiv vom Block direkt darunter zieht.
 */
public class MachineElectricHeaterBlockEntity extends BaseMachineBlockEntity implements IHeatSource {

    public static final int MAX_SETTING = 10;
    private static final int MAX_HEAT = 100_000;
    private static final long CAPACITY = 200_000L;
    private static final long MAX_RECEIVE = 4_096L;

    private int setting = 0;
    private int heat = 0;

    public MachineElectricHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER_BE.get(), pos, state, 0, CAPACITY, MAX_RECEIVE, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectricHeaterBlockEntity be) {
        if (level.isClientSide) return;

        be.heat = (int) (be.heat * 0.999D);

        if (be.setting > 0) {
            int consumption = (int) (Math.pow(be.setting, 1.4D) * 200D);
            if (be.getEnergyStored() >= consumption) {
                be.setEnergyStored(be.getEnergyStored() - consumption);
                be.heat = Math.min(MAX_HEAT, be.heat + be.setting * 100);
            }
        }

        be.setChanged();
    }

    public void cycleSetting() {
        setting = (setting + 1) % (MAX_SETTING + 1);
        setChanged();
    }

    public int getSetting() {
        return setting;
    }

    @Override
    public int getHeatStored() {
        return heat;
    }

    @Override
    public int getMaxHeatStored() {
        return MAX_HEAT;
    }

    @Override
    public void useUpHeat(int amount) {
        heat = Math.max(0, heat - amount);
        setChanged();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_electric_heater");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("setting", setting);
        tag.putInt("heat", heat);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        setting = tag.getInt("setting");
        heat = tag.getInt("heat");
    }
}
