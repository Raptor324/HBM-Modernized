package com.hbm_m.block.entity.machine;

import com.hbm_m.api.energy.IEnergyReceiver;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machine.MachineBatteryBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.menu.MachineBatteryMenu;
import com.hbm_m.util.LongDataPacker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Энергохранилище с настраиваемыми режимами работы.
 * Режимы: 0 = BOTH, 1 = INPUT, 2 = OUTPUT, 3 = DISABLED
 */
public class MachineBatteryBlockEntity extends BaseMachineBlockEntity {

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;
    private static final int SLOT_COUNT = 2;
    private static final long FALLBACK_CAPACITY = 9_000_000_000_000_000_000L;
    private static final long DEFAULT_TRANSFER_RATE = 100_000_000_000L;

    public int modeOnNoSignal = 0;
    public int modeOnSignal = 0;
    private IEnergyReceiver.Priority priority = IEnergyReceiver.Priority.LOW;

    private final ContainerData data;

    public MachineBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_BATTERY_BE.get(),
                pos,
                state,
                SLOT_COUNT,
                resolveCapacity(state),
                DEFAULT_TRANSFER_RATE,
                DEFAULT_TRANSFER_RATE);
        this.data = createDataAccessor();
    }

    private static long resolveCapacity(BlockState state) {
        return state.getBlock() instanceof MachineBatteryBlock block
                ? block.getCapacity()
                : FALLBACK_CAPACITY;
    }

    private ContainerData createDataAccessor() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long energy = MachineBatteryBlockEntity.this.getEnergyStored();
                long capacity = MachineBatteryBlockEntity.this.getMaxEnergyStored();
                long delta = MachineBatteryBlockEntity.this.getEnergyDelta();
                return switch (index) {
                    case 0 -> LongDataPacker.packHigh(energy);
                    case 1 -> LongDataPacker.packLow(energy);
                    case 2 -> LongDataPacker.packHigh(capacity);
                    case 3 -> LongDataPacker.packLow(capacity);
                    case 4 -> (int) Math.max(Math.min(delta, Integer.MAX_VALUE), Integer.MIN_VALUE);
                    case 5 -> modeOnNoSignal;
                    case 6 -> modeOnSignal;
                    case 7 -> priority.ordinal();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 5 -> modeOnNoSignal = Math.floorMod(value, 4);
                    case 6 -> modeOnSignal = Math.floorMod(value, 4);
                    case 7 -> {
                        int clamped = Math.max(0, Math.min(value, IEnergyReceiver.Priority.values().length - 1));
                        priority = IEnergyReceiver.Priority.values()[clamped];
                    }
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBatteryBlockEntity battery) {
        if (level.isClientSide) {
            return;
        }

        battery.ensureNetworkInitialized();

        if (level.getGameTime() % 10L == 0L) {
            battery.updateEnergyDelta(battery.getEnergyStored());
        }

        battery.chargeFromItem();
        battery.dischargeToItem();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new MachineBatteryMenu(windowId, playerInventory, this, this.data);
    }

    public ContainerData getContainerData() {
        return this.data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case INPUT_SLOT -> canStackProvideEnergy(stack);
            case OUTPUT_SLOT -> canStackReceiveEnergy(stack);
            default -> false;
        };
    }

    private boolean canStackProvideEnergy(ItemStack stack) {
        boolean hasHbm = stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                .map(provider -> provider.canExtract())
                .orElse(false);
        if (hasHbm) {
            return true;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::canExtract)
                .orElse(false);
    }

    private boolean canStackReceiveEnergy(ItemStack stack) {
        boolean hasHbm = stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER)
                .map(receiver -> receiver.canReceive())
                .orElse(false);
        if (hasHbm) {
            return true;
        }
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(IEnergyStorage::canReceive)
                .orElse(false);
    }

    public int getCurrentMode() {
        if (this.level == null) {
            return modeOnNoSignal;
        }
        return this.level.hasNeighborSignal(this.worldPosition) ? modeOnSignal : modeOnNoSignal;
    }

    @Override
    public IEnergyReceiver.Priority getPriority() {
        return this.priority;
    }

    public void setPriority(IEnergyReceiver.Priority priority) {
        this.priority = priority;
        setChanged();
    }

    @Override
    public boolean canReceive() {
        int mode = getCurrentMode();
        return (mode == 0 || mode == 1) && super.canReceive();
    }

    @Override
    public boolean canExtract() {
        int mode = getCurrentMode();
        return (mode == 0 || mode == 2) && super.canExtract();
    }

    private void chargeFromItem() {
        if (!canReceive()) {
            return;
        }

        ItemStack sourceStack = this.inventory.getStackInSlot(INPUT_SLOT);
        if (sourceStack.isEmpty()) {
            return;
        }

        long energyNeeded = this.getMaxEnergyStored() - this.getEnergyStored();
        if (energyNeeded <= 0) {
            return;
        }

        long maxTransfer = Math.min(energyNeeded, this.getReceiveSpeed());
        if (maxTransfer <= 0) {
            return;
        }

        if (transferFromHbmProvider(sourceStack, maxTransfer)) {
            return;
        }

        transferFromForgeProvider(sourceStack, maxTransfer);
    }

    private void dischargeToItem() {
        if (!canExtract()) {
            return;
        }

        ItemStack targetStack = this.inventory.getStackInSlot(OUTPUT_SLOT);
        if (targetStack.isEmpty()) {
            return;
        }

        long available = this.getEnergyStored();
        if (available <= 0) {
            return;
        }

        long maxTransfer = Math.min(available, this.getProvideSpeed());
        if (maxTransfer <= 0) {
            return;
        }

        if (transferToHbmReceiver(targetStack, maxTransfer)) {
            return;
        }

        transferToForgeReceiver(targetStack, maxTransfer);
    }

    private boolean transferFromHbmProvider(ItemStack stack, long maxTransfer) {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_PROVIDER)
                .map(provider -> {
                    if (!provider.canExtract()) {
                        return false;
                    }
                    long extracted = provider.extractEnergy(maxTransfer, false);
                    if (extracted > 0) {
                        this.setEnergyStored(this.getEnergyStored() + extracted);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean transferFromForgeProvider(ItemStack stack, long maxTransfer) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> {
                    if (!storage.canExtract()) {
                        return false;
                    }
                    int transferInt = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
                    if (transferInt <= 0) {
                        return false;
                    }
                    int extracted = storage.extractEnergy(transferInt, false);
                    if (extracted > 0) {
                        this.setEnergyStored(this.getEnergyStored() + extracted);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean transferToHbmReceiver(ItemStack stack, long maxTransfer) {
        return stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER)
                .map(receiver -> {
                    if (!receiver.canReceive()) {
                        return false;
                    }
                    long accepted = receiver.receiveEnergy(maxTransfer, false);
                    if (accepted > 0) {
                        this.setEnergyStored(this.getEnergyStored() - accepted);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    private boolean transferToForgeReceiver(ItemStack stack, long maxTransfer) {
        return stack.getCapability(ForgeCapabilities.ENERGY)
                .map(storage -> {
                    if (!storage.canReceive()) {
                        return false;
                    }
                    int transferInt = (int) Math.min(Integer.MAX_VALUE, maxTransfer);
                    if (transferInt <= 0) {
                        return false;
                    }
                    int accepted = storage.receiveEnergy(transferInt, false);
                    if (accepted > 0) {
                        this.setEnergyStored(this.getEnergyStored() - accepted);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    public void drops() {
        if (this.level == null) {
            return;
        }
        SimpleContainer container = new SimpleContainer(this.inventory.getSlots());
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            container.setItem(i, this.inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    public void handleButtonPress(int buttonId) {
        switch (buttonId) {
            case 0 -> this.data.set(5, (this.modeOnNoSignal + 1) % 4);
            case 1 -> this.data.set(6, (this.modeOnSignal + 1) % 4);
            case 2 -> {
                int nextIndex = (this.priority.ordinal() + 1) % IEnergyReceiver.Priority.values().length;
                this.data.set(7, nextIndex);
            }
            default -> {
                return;
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("modeOnNoSignal", modeOnNoSignal);
        tag.putInt("modeOnSignal", modeOnSignal);
        tag.putInt("priority", priority.ordinal());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        modeOnNoSignal = tag.getInt("modeOnNoSignal");
        modeOnSignal = tag.getInt("modeOnSignal");
        if (tag.contains("priority")) {
            int idx = Math.max(0, Math.min(tag.getInt("priority"), IEnergyReceiver.Priority.values().length - 1));
            priority = IEnergyReceiver.Priority.values()[idx];
        }
    }
}

