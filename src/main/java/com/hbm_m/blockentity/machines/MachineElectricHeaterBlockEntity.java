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
 * <p>1:1: Verbrauch {@code setting^1.4 * 200} HE/t, Speicher {@code Verbrauch * 20}, Waerme {@code setting * 100} TU/t,
 * Abklingen x0.999/t, zieht 85 % der Waerme einer Quelle direkt darunter ab (Durchreichen), Brummen solange an.</p>
 */
public class MachineElectricHeaterBlockEntity extends BaseMachineBlockEntity implements IHeatSource {

    public static final int MAX_SETTING = 10;
    /** Nur fuer Anzeigen; das Original kennt keine Obergrenze. */
    private static final int MAX_HEAT = 100_000;
    /** Original nimmt bis {@code getMaxPower()} beliebig schnell an. */
    private static final long MAX_RECEIVE = 1_000_000_000L;

    private int setting = 0;
    private int heat = 0;
    public boolean isOn = false;

    public MachineElectricHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER_BE.get(), pos, state, 0, 0L, MAX_RECEIVE, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectricHeaterBlockEntity be) {
        if (level.isClientSide) {
            // Original: ELECTRIC_HUM_LOOP, Lautstaerke 0.25, Reichweite 7.5
            com.hbm_m.client.sound.MachineLoopSoundClient.tick(be, "hbm:block.electricHum", be.isOn, 0.25F, 1.0F, 7.5);
            return;
        }

        // Original: getMaxPower() = getConsumption() * 20
        be.setEnergyCapacity(be.getConsumption() * 20);

        be.heat = (int) (be.heat * 0.999D);

        be.tryPullHeat();

        boolean wasOn = be.isOn;
        be.isOn = false;
        if (be.setting > 0 && be.getEnergyStored() >= be.getConsumption()) {
            be.setEnergyStored(be.getEnergyStored() - be.getConsumption());
            be.heat += be.getHeatGen();
            be.isOn = true;
        }

        be.setChanged();
        if (wasOn != be.isOn) be.sendUpdateToClient();
    }

    /** Original {@code tryPullHeat}: 85 % der Waerme des Blocks darunter, die Quelle wird geleert. */
    protected void tryPullHeat() {
        if (level == null) return;
        net.minecraft.world.level.block.entity.BlockEntity con = level.getBlockEntity(worldPosition.below());
        if (con instanceof IHeatSource source) {
            this.heat += (int) (source.getHeatStored() * 0.85);
            source.useUpHeat(source.getHeatStored());
        }
    }

    public long getConsumption() {
        return (long) (Math.pow(setting, 1.4D) * 200D);
    }

    public int getHeatGen() {
        return this.setting * 100;
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
        tag.putBoolean("isOn", isOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        setting = tag.getInt("setting");
        heat = tag.getInt("heat");
        isOn = tag.getBoolean("isOn");
    }
}
