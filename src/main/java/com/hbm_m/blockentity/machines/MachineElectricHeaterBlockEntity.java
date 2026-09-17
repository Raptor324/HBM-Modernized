package com.hbm_m.blockentity.machines;

import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IHeatSource;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Порт {@code TileEntityHeaterElectric} (1.7.10) — электрический нагреватель,
 * 4×3×1 мультиблок. Настройка 0..10 крутится отвёрткой: потребление
 * {@code setting^1.4 * 200} HE/тик, тепловыделение {@code setting * 100} TU/тик.
 * Буфер энергии = 20 тиков потребления. Как и все нагреватели, затягивает тепло
 * снизу с КПД 85% (стек с другими источниками тепла), собственный запас
 * распадается на 0.1% за тик.
 */
public class MachineElectricHeaterBlockEntity extends BaseMachineBlockEntity implements IHeatSource, com.hbm_m.interfaces.ICopiable {

    public static final int MAX_SETTING = 10;
    private static final int MAX_HEAT = 100_000;

    private int setting = 0;
    private int heat = 0;
    private boolean isOn = false;
    private boolean wasOnSynced = false;

    public MachineElectricHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER_BE.get(), pos, state, 0, 200_000L, 4_096L, 0L);
    }

    /** Разделение тикера: клиент — гул, сервер — логика. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, MachineElectricHeaterBlockEntity be) {
        // Порт createAudioLoop: зацикленный ELECTRIC_HUM (0.25 громкость, LINEAR)
        com.hbm_m.sound.ClientSoundBootstrap.updateSound(be, be.isOn, () -> newHumInstance(pos));
    }

    private static Object newHumInstance(BlockPos pos) {
        try {
            return Class.forName("com.hbm_m.sound.ElectricHeaterHumSoundInstance")
                    .getConstructor(BlockPos.class).newInstance(pos);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectricHeaterBlockEntity be) {
        if (level.isClientSide) {
            clientTick(level, pos, state, be);
            return;
        }

        be.ensureNetworkInitialized();

        // Оригинал: раз в секунду подписка на энергосеть в 3 блоках впереди (порт подключения);
        // здесь сеть энергии подписывается сама через EnergySubscriptions (ensureNetworkInitialized).

        // Распад запаса тепла
        be.heat = (int) (be.heat * 0.999D);

        // Затяжка тепла снизу с КПД 85% (порт tryPullHeat)
        be.tryPullHeatFromBelow();

        // Генерация тепла из энергии
        boolean generating = false;
        if (be.setting > 0) {
            long consumption = be.getConsumption();
            if (be.getEnergyStored() >= consumption) {
                be.setEnergyStored(be.getEnergyStored() - consumption);
                be.heat = Math.min(MAX_HEAT, be.heat + be.getHeatGen());
                generating = true;
            }
        }
        be.isOn = generating;

        // Буфер = 20 тиков потребления при текущей настройке (оригинал getMaxPower)
        be.setEnergyCapacity(be.getConsumption() * 20);

        if (be.isOn != be.wasOnSynced) {
            be.wasOnSynced = be.isOn;
            be.setChanged();
            be.sendUpdateToClient();
        } else {
            be.setChanged();
        }
    }

    /** Порт tryPullHeat: всё тепло снизу снимается, +85% начисляется себе. */
    private void tryPullHeatFromBelow() {
        if (level == null) return;
        BlockEntity below = level.getBlockEntity(worldPosition.below());
        if (below instanceof IHeatSource source) {
            int stored = source.getHeatStored();
            this.heat = Math.min(MAX_HEAT, this.heat + (int) (stored * 0.85D));
            source.useUpHeat(stored);
        }
    }

    /** Порт toggleSetting: 0..10 с заворотом. */
    public void cycleSetting() {
        setting++;
        if (setting > MAX_SETTING) setting = 0;
        setChanged();
        sendUpdateToClient();
    }

    public int getSetting() {
        return setting;
    }

    public boolean isOn() {
        return isOn;
    }

    /** Порт getConsumption: pow(setting, 1.4) * 200 HE за тик. */
    public long getConsumption() {
        return (long) (Math.pow(setting, 1.4D) * 200D);
    }

    /** Порт getHeatGen: setting * 100 TU за тик. */
    public int getHeatGen() {
        return setting * 100;
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

    /** Точка подключения энергосети — 3 блока вперёд по FACING (как trySubscribe в оригинале). */
    @Override
    protected BlockPos[] getExtraEnergyPorts() {
        Direction facing = getBlockState().hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                ? getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                : Direction.NORTH;
        return new BlockPos[] { worldPosition.relative(facing, 3) };
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
        wasOnSynced = isOn;
    }

    /* ── Устройство настройки: мощность нагрева (оригинал) ──────────────── */

    @Override
    public CompoundTag getSettings(Level level, BlockPos pos) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("setting", setting);
        return nbt;
    }

    @Override
    public void pasteSettings(CompoundTag nbt, int index, Level level, Player player, BlockPos pos) {
        setting = Math.min(nbt.getInt("setting"), MAX_SETTING);
        setChanged();
        sendUpdateToClient();
    }
}
