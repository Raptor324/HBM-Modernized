package com.hbm_m.block.entity.machine;

import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.block.machine.MachineWoodBurnerBlock;
import com.hbm_m.capability.ModCapabilities;
import com.hbm_m.item.ModItems;
import com.hbm_m.menu.MachineWoodBurnerMenu;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * Дровяной генератор энергии. Сжигает топливо и заряжает предметы/сеть.
 */
public class MachineWoodBurnerBlockEntity extends BaseMachineBlockEntity {

    private static final int SLOT_COUNT = 3;
    private static final int FUEL_SLOT = 0;
    private static final int ASH_SLOT = 1;
    private static final int CHARGE_SLOT = 2;

    private static final long CAPACITY = 100_000L;
    private static final long GENERATION_RATE = 50L;
    private static final long OUTPUT_RATE = GENERATION_RATE * 2;

    private int burnTime = 0;
    private int maxBurnTime = 0;
    private boolean enabled = true;

    private final ContainerData data;

    public MachineWoodBurnerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WOOD_BURNER_BE.get(), pos, state, SLOT_COUNT, CAPACITY, 0L, OUTPUT_RATE);
        this.data = createDataAccessor();
    }

    private ContainerData createDataAccessor() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                long energy = MachineWoodBurnerBlockEntity.this.getEnergyStored();
                return switch (index) {
                    case 0 -> (int) (energy & 0xFFFFFFFFL);
                    case 1 -> (int) (energy >> 32);
                    case 2 -> (int) (CAPACITY & 0xFFFFFFFFL);
                    case 3 -> (int) (CAPACITY >> 32);
                    case 4 -> burnTime;
                    case 5 -> maxBurnTime;
                    case 6 -> isBurning() ? 1 : 0;
                    case 7 -> enabled ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 7) {
                    enabled = value != 0;
                    setChanged();
                }
            }

            @Override
            public int getCount() {
                return 8;
            }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineWoodBurnerBlockEntity burner) {
        if (level.isClientSide()) {
            return;
        }

        burner.ensureNetworkInitialized();

        boolean wasBurning = burner.isBurning();
        boolean canBurnNow = burner.canBurn();

        if (burner.enabled && burner.burnTime <= 0 && canBurnNow) {
            burner.startBurning();
        }

        if (burner.enabled && burner.isBurning()) {
            burner.burnTime--;
            long newEnergy = Math.min(burner.getMaxEnergyStored(), burner.getEnergyStored() + GENERATION_RATE);
            if (newEnergy != burner.getEnergyStored()) {
                burner.setEnergyStored(newEnergy);
            }
            burner.chargeItem();

            if (burner.burnTime == 0) {
                burner.maxBurnTime = 0;
                burner.tryProduceAsh();
            }
        } else if (burner.isBurning()) {
            burner.burnTime = 0;
            burner.maxBurnTime = 0;
        }

        if (wasBurning != burner.isBurning()) {
            level.setBlock(pos, state.setValue(MachineWoodBurnerBlock.LIT, burner.isBurning()), 3);
        }

        setChanged(level, pos, state);
    }

    private boolean canBurn() {
        return !this.inventory.getStackInSlot(FUEL_SLOT).isEmpty() && this.getEnergyStored() < this.getMaxEnergyStored();
    }

    private void startBurning() {
        ItemStack fuelStack = this.inventory.getStackInSlot(FUEL_SLOT);
        int burnTicks = ForgeHooks.getBurnTime(fuelStack, null);
        if (burnTicks <= 0) {
            return;
        }

        this.maxBurnTime = burnTicks;
        this.burnTime = burnTicks;

        if (fuelStack.getItem() == Items.LAVA_BUCKET) {
            this.inventory.setStackInSlot(FUEL_SLOT, new ItemStack(Items.BUCKET));
        } else {
            fuelStack.shrink(1);
        }
        setChanged();
    }

    private void tryProduceAsh() {
        if (this.level == null) {
            return;
        }
        if (this.level.random.nextFloat() >= 0.5f) {
            return;
        }
        ItemStack ashStack = this.inventory.getStackInSlot(ASH_SLOT);
        if (ashStack.isEmpty()) {
            this.inventory.setStackInSlot(ASH_SLOT, new ItemStack(ModItems.WOOD_ASH_POWDER.get()));
        } else if (ashStack.is(ModItems.WOOD_ASH_POWDER.get()) && ashStack.getCount() < ashStack.getMaxStackSize()) {
            ashStack.grow(1);
        }
    }

    private void chargeItem() {
        if (this.getEnergyStored() <= 0) {
            return;
        }

        ItemStack toCharge = this.inventory.getStackInSlot(CHARGE_SLOT);
        if (toCharge.isEmpty()) {
            return;
        }

        long maxTransfer = Math.min(this.getProvideSpeed(), this.getEnergyStored());
        if (maxTransfer <= 0) {
            return;
        }

        if (transferToHbmReceiver(toCharge, maxTransfer)) {
            return;
        }

        transferToForgeReceiver(toCharge, maxTransfer);
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
                }).orElse(false);
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
                }).orElse(false);
    }

    public boolean isBurning() {
        return this.burnTime > 0;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.wood_burner");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineWoodBurnerMenu(containerId, playerInventory, this, this.data);
    }

    public ContainerData getContainerData() {
        return this.data;
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case FUEL_SLOT -> ForgeHooks.getBurnTime(stack, null) > 0;
            case ASH_SLOT -> false;
            case CHARGE_SLOT -> stack.getCapability(ModCapabilities.HBM_ENERGY_RECEIVER).isPresent()
                    || stack.getCapability(ForgeCapabilities.ENERGY)
                    .map(IEnergyStorage::canReceive)
                    .orElse(false);
            default -> false;
        };
    }

    public void drops() {
        if (this.level == null) return;
        SimpleContainer container = new SimpleContainer(this.inventory.getSlots());
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            container.setItem(i, this.inventory.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, container);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putBoolean("enabled", enabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        enabled = tag.getBoolean("enabled");
    }
}
 
