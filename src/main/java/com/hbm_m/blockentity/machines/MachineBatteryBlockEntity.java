package com.hbm_m.blockentity.machines;

import com.hbm_m.block.machines.MachineBatteryBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IEnergyModeHolder;
import com.hbm_m.inventory.menu.MachineBatteryMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Энергохранилище с настраиваемыми режимами работы.
 * Режимы: 0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED
 */
@SuppressWarnings("UnstableApiUsage")
public class MachineBatteryBlockEntity extends BaseMachineBlockEntity implements IEnergyModeHolder, com.hbm_m.api.energy.PowerBuffer, com.hbm_m.api.block.IPersistentNBT,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    private static final int SLOT_CHARGE = 0;
    private static final int SLOT_DISCHARGE = 1;
    private static final int SLOT_COUNT = 2;

    private static final long DEFAULT_CAPACITY_FALLBACK = 9_000_000_000_000_000_000L;
    private static final long TRANSFER_RATE = 100_000_000_000L;

    // Режимы работы (0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED)
    // Original: redLow = mode_input, redHigh = mode_output (Port-Zaehlung: 1 = INPUT, 2 = OUTPUT)
    public int modeOnNoSignal = 1;
    public int modeOnSignal = 2;
    private Priority priority = Priority.LOW;

    private long lastEnergySample = 0;
    private long averagedEnergyDelta = 0;

    protected final ContainerData data;

    public MachineBatteryBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MACHINE_BATTERY_BE.get(), pos, state, getCapacityFromState(state), TRANSFER_RATE);
    }

    /** Fuer Unterklassen mit eigenem Typ, eigener Kapazitaet und eigenem Uebertragungslimit (FEnSU). */
    protected MachineBatteryBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        long capacity, long transferRate) {
        super(
                type,
                pos,
                state,
                SLOT_COUNT,
                capacity,
                transferRate,
                transferRate
        );

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> modeOnNoSignal;
                    case 1 -> modeOnSignal;
                    case 2 -> priority.ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> modeOnNoSignal = value;
                    case 1 -> modeOnSignal = value;
                    case 2 -> priority = Priority.values()[Math.max(0, Math.min(value, Priority.values().length - 1))];
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    private static long getCapacityFromState(BlockState state) {
        return state.getBlock() instanceof MachineBatteryBlock b ? b.getCapacity() : DEFAULT_CAPACITY_FALLBACK;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_battery");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot != SLOT_CHARGE && slot != SLOT_DISCHARGE) return false;
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof ItemCreativeBattery) return true;
        //? if forge {
        return stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).isPresent()
                || stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_PROVIDER).isPresent()
                || stack.getCapability(com.hbm_m.capability.ModCapabilities.HBM_ENERGY_RECEIVER).isPresent();
        //?}
        //? if fabric {
        /*return team.reborn.energy.api.EnergyStorage.ITEM.find(stack, null) != null;
        *///?}
        //? if neoforge {
        /*// NeoForge: FE через Capabilities.EnergyStorage.ITEM + HBM через ItemEnergyAccess.
        return stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM) != null
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(stack).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(stack).isPresent();
        *///?}
    }

    @Override
    public int getCurrentMode() {
        if (level == null) return modeOnNoSignal;
        return level.hasNeighborSignal(this.worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    // Паритет с оригиналом (TileEntityMachineBattery.getProviderSpeed/getReceiverSpeed):
    // скорость обмена с сетью зависит от режима — INPUT не отдает, OUTPUT не принимает.
    @Override
    public long getProvideSpeed() {
        int mode = getCurrentMode();
        if (mode != 0 && mode != 2) return 0;
        // Original: getMaxPower() / 600 (FEnSU: fester maxTransfer)
        return this instanceof MachineFENSUBlockEntity ? super.getProvideSpeed() : getMaxEnergyStored() / 600;
    }

    @Override
    public long getReceiveSpeed() {
        int mode = getCurrentMode();
        if (mode != 0 && mode != 1) return 0;
        // Original: getMaxPower() / 200 (FEnSU: fester maxTransfer)
        return this instanceof MachineFENSUBlockEntity ? super.getReceiveSpeed() : getMaxEnergyStored() / 200;
    }

    public long getEnergyDeltaAveraged() {
        return this.averagedEnergyDelta;
    }

    @Override
    public Priority getPriority() {
        return this.priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
        setChanged();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBatteryBlockEntity be) {
        if (level.isClientSide) return;

        be.ensureNetworkInitialized();

        long gameTime = level.getGameTime();
        if (gameTime % 10 == 0) {
            long cur = be.getEnergyStored();
            be.averagedEnergyDelta = (cur - be.lastEnergySample) / 10;
            be.lastEnergySample = cur;
        }

        int mode = be.getCurrentMode();
        if (mode == 0 || mode == 1) {
            be.chargeFromBatterySlot(SLOT_CHARGE);
        }
        if (mode == 0 || mode == 2) {
            be.chargeItemInSlot(SLOT_DISCHARGE);
        }
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> this.data.set(0, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(1, (this.modeOnSignal + 1) % 4);
            case 2 -> {
                // Паритет с оригиналом: батареи ограничены приоритетами LOW..HIGH (без LOWEST/HIGHEST)
                Priority[] priorities = Priority.values();
                int currentIndex = Math.max(1, Math.min(this.priority.ordinal(), priorities.length - 2));
                int nextIndex = (currentIndex >= priorities.length - 2) ? 1 : currentIndex + 1;
                this.data.set(2, nextIndex);
            }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new MachineBatteryMenu(windowId, playerInventory, this, this.data);
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("modeOnNoSignal", this.modeOnNoSignal);
        tag.putInt("modeOnSignal", this.modeOnSignal);
        tag.putInt("priorityV2", this.priority.ordinal());
        tag.putLong("lastEnergySample", this.lastEnergySample);
        tag.putLong("averagedEnergyDelta", this.averagedEnergyDelta);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.modeOnNoSignal = tag.getInt("modeOnNoSignal");
        this.modeOnSignal = tag.getInt("modeOnSignal");
        // priorityV2 — ординал нового 5-уровневого enum'а (LOWEST..HIGHEST).
        // Старый ключ "priority" писался для 3-уровневого enum'а (LOW,NORMAL,HIGH = 0,1,2),
        // поэтому сдвигаем на +1 (в новом enum'е они имеют ординалы 1,2,3).
        if (tag.contains("priorityV2")) {
            int priorityIndex = tag.getInt("priorityV2");
            this.priority = Priority.values()[Math.max(0, Math.min(priorityIndex, Priority.values().length - 1))];
        } else if (tag.contains("priority")) {
            int priorityIndex = tag.getInt("priority") + 1;
            this.priority = Priority.values()[Math.max(0, Math.min(priorityIndex, Priority.values().length - 1))];
        }
        this.lastEnergySample = tag.getLong("lastEnergySample");
        this.averagedEnergyDelta = tag.getLong("averagedEnergyDelta");
    }

    /** Original {@code writeNBT}: nur Ladung, Redstone-Modi und Prioritaet wandern in den Drop, nicht das Inventar. */
    @Override
    public void writeNBT(CompoundTag nbt) {
        nbt.putLong("energy", this.getEnergyStored());
        nbt.putInt("modeOnNoSignal", this.modeOnNoSignal);
        nbt.putInt("modeOnSignal", this.modeOnSignal);
        nbt.putInt("priorityV2", this.priority.ordinal());
    }

    /**
     * 1.21.1: copy_nbt в лут-таблицах удалён (1.20.5+), поэтому перенос состояния
     * батареи в дропнутый предмет делается кодом — как у ящиков
     * ({@link com.hbm_m.blockentity.crates.BaseCrateBlockEntity#saveToItem}).
     * Формат совпадает с чтением в {@code MachineBatteryBlock#setPlacedBy}:
     * CUSTOM_DATA → {"BlockEntityTag": {...}}.
     */
    //? if >= 1.21.1 {
    /*public void saveToItemStack(net.minecraft.world.item.ItemStack stack) {
        CompoundTag beTag = new CompoundTag();
        this.writeNBT(beTag);
        if (!beTag.isEmpty()) {
            CompoundTag root = new CompoundTag();
            root.put("BlockEntityTag", beTag);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                    net.minecraft.world.item.component.CustomData.of(root));
        }
    }
    *///?}

    //? if forge {
    /** Original {@code ISidedInventory}: unten {0, 1}, oben {0}, Seiten {1}; Akkus hinein, leere aus 0 und volle aus 1 heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return side == net.minecraft.core.Direction.DOWN ? new int[] { 0, 1 } : side == net.minecraft.core.Direction.UP ? new int[] { 0 } : new int[] { 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { if (com.hbm_m.blockentity.SidedItemAccess.charge(stack) < 0) return false;
                    if (slot == 0 && com.hbm_m.blockentity.SidedItemAccess.isEmptyBattery(stack)) return true;
                    return slot == 1 && com.hbm_m.blockentity.SidedItemAccess.isFullBattery(stack); }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}

    // ── Redstone-over-Radio (1:1 TileEntityMachineBattery) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "fill",
                PREFIX_VALUE + "fillpercent",
                PREFIX_VALUE + "delta",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode (0-3)",
                PREFIX_FUNCTION + "setredmode" + NAME_SEPARATOR + "mode" + PARAM_SEPARATOR + "fallback (0-3)",
                PREFIX_FUNCTION + "setpriority" + NAME_SEPARATOR + "priority (0-2)",
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "fill").equals(name))        return "" + getEnergyStored();
        // Original getPowerRemainingScaled(100) = power * 100 / getMaxPower()
        if ((PREFIX_VALUE + "fillpercent").equals(name)) return "" + (getEnergyStored() * 100 / getMaxEnergyStored());
        if ((PREFIX_VALUE + "delta").equals(name))       return "" + averagedEnergyDelta;
        return null;
    }

    /**
     * Original-Zaehlung der Modi (0 = Eingang, 1 = Puffer, 2 = Ausgang, 3 = aus) - der Port zaehlt
     * 0 = Puffer, 1 = Eingang; die Funkbefehle bleiben bei der Original-Zaehlung.
     */
    private static int swapModeROR(int mode) {
        return mode == 0 ? 1 : mode == 1 ? 0 : mode;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // redLow = Modus ohne Redstone, redHigh = Modus mit Redstone
        if ((PREFIX_FUNCTION + "setmode").equals(name) && params.length > 0) {
            int mode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != swapModeROR(this.modeOnNoSignal)) {
                this.modeOnNoSignal = swapModeROR(mode);
                this.markChangedROR();
                return null;
            } else if (params.length > 1) {
                int altmode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[1], 0, 3);
                this.modeOnNoSignal = swapModeROR(altmode);
                this.markChangedROR();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setredmode").equals(name) && params.length > 0) {
            int mode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 3);
            if (mode != swapModeROR(this.modeOnSignal)) {
                this.modeOnSignal = swapModeROR(mode);
                this.markChangedROR();
                return null;
            } else if (params.length > 1) {
                int altmode = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[1], 0, 3);
                this.modeOnSignal = swapModeROR(altmode);
                this.markChangedROR();
                return null;
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setpriority").equals(name) && params.length > 0) {
            // Original: parseInt(0..2) + 1 -> LOW, NORMAL, HIGH
            int priority = com.hbm_m.api.redstoneoverradio.IRORInteractive.parseInt(params[0], 0, 2) + 1;
            this.priority = Priority.values()[priority];
            this.markChangedROR();
            return null;
        }
        return null;
    }

    /** Original {@code markChanged()}. */
    private void markChangedROR() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
}
