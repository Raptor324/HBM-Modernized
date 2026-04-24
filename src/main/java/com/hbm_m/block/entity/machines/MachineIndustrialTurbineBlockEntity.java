package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.ModFluids;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * Industrial Turbine BlockEntity - converts steam to energy (HE).
 *
 * Stats:
 * - Steam tank: 64,000 mB (input)
 * - Spent steam tank: 64,000 mB (output)
 * - Energy output: up to 500 HE/t depending on steam type
 * - Steam consumption: 50 mB/t
 */
public class MachineIndustrialTurbineBlockEntity extends BaseMachineBlockEntity {

    // Slot definitions
    public static final int SLOT_STEAM_IN = 0;      // Steam container input
    public static final int SLOT_STEAM_OUT = 1;      // Empty container output
    public static final int SLOT_SPENT_IN = 2;       // Empty container for spent steam
    public static final int SLOT_SPENT_OUT = 3;      // Filled spent steam container
    public static final int INVENTORY_SIZE = 4;

    // Capacity constants
    private static final long ENERGY_CAPACITY = 500_000L;
    private static final long ENERGY_EXTRACT_RATE = 10_000L;
    private static final int STEAM_CAPACITY = 64_000;
    private static final int SPENT_STEAM_CAPACITY = 64_000;

    // Conversion constants
    private static final int STEAM_CONSUMPTION_RATE = 50;   // mB of steam consumed per tick
    private static final long ENERGY_PER_MB_STEAM = 100;    // HE per mB of steam (regular)
    private static final long ENERGY_PER_MB_HOT = 200;      // HE per mB of hot steam
    private static final long ENERGY_PER_MB_SUPERHOT = 400;  // HE per mB of super hot steam
    private static final long ENERGY_PER_MB_ULTRAHOT = 800;  // HE per mB of ultra hot steam

    // Fluid tanks
    private final FluidTank steamTank;
    private final FluidTank spentSteamTank;

    private final LazyOptional<IFluidHandler> lazySteamHandler;
    private final LazyOptional<IFluidHandler> lazySpentHandler;

    private boolean isActive = false;
    private float anim = 0.0F;
    private float prevAnim = 0.0F;

    protected final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> steamTank.getFill();
                case 1 -> steamTank.getMaxFill();
                case 2 -> spentSteamTank.getFill();
                case 3 -> spentSteamTank.getMaxFill();
                case 4 -> isActive ? 1 : 0;
                case 5 -> (int) (energy & 0xFFFFFFFF);
                case 6 -> (int) ((energy >> 32) & 0xFFFFFFFF);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {}

        @Override
        public int getCount() {
            return 7;
        }
    };

    public MachineIndustrialTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INDUSTRIAL_TURBINE_BE.get(), pos, state,
              INVENTORY_SIZE, ENERGY_CAPACITY, 0L, ENERGY_EXTRACT_RATE);

        this.steamTank = new FluidTank(STEAM_CAPACITY);
        this.spentSteamTank = new FluidTank(ModFluids.SPENTSTEAM.getSource(), SPENT_STEAM_CAPACITY);

        this.lazySteamHandler = steamTank.getCapability();
        this.lazySpentHandler = spentSteamTank.getCapability();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineIndustrialTurbineBlockEntity be) {
        be.prevAnim = be.anim;

        if (level.isClientSide()) {
            if (be.isActive) {
                be.anim += 0.15F;
                if (be.anim > (float) (Math.PI * 2.0)) {
                    be.anim -= (float) (Math.PI * 2.0);
                }
            }
            return;
        }

        be.ensureNetworkInitialized();

        // Process fluid containers
        be.processFluidContainers();

        boolean wasActive = be.isActive;

        // Convert steam to energy
        be.processTurbine();

        // Push energy to network
        if (be.energy > 0 && level.getGameTime() % 10L == 0L) {
            be.updateEnergyDelta(be.getEnergyStored());
        }

        if (wasActive != be.isActive) {
            be.setChanged();
            be.sendUpdateToClient();
        }
    }

    private void processFluidContainers() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }

        // Load steam from containers
        if (steamTank.loadTank(SLOT_STEAM_IN, SLOT_STEAM_OUT, slots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                inventory.setStackInSlot(i, slots[i]);
            }
        }

        // Unload spent steam to containers
        if (spentSteamTank.unloadTank(SLOT_SPENT_IN, SLOT_SPENT_OUT, slots)) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                inventory.setStackInSlot(i, slots[i]);
            }
        }
    }

    private void processTurbine() {
        if (steamTank.getFill() <= 0 || steamTank.getTankType() == null) {
            isActive = false;
            return;
        }

        // Calculate energy per mB based on steam type
        long energyPerMb = getEnergyPerMb();
        if (energyPerMb <= 0) {
            isActive = false;
            return;
        }

        int steamAvailable = steamTank.getFill();
        int steamToConsume = Math.min(STEAM_CONSUMPTION_RATE, steamAvailable);

        // Check energy capacity
        long energyToGenerate = steamToConsume * energyPerMb;
        long energySpace = getMaxEnergyStored() - getEnergyStored();
        if (energySpace <= 0) {
            isActive = false;
            return;
        }
        if (energyToGenerate > energySpace) {
            steamToConsume = (int) (energySpace / energyPerMb);
            energyToGenerate = steamToConsume * energyPerMb;
        }

        // Check spent steam capacity
        int spentSpace = spentSteamTank.getMaxFill() - spentSteamTank.getFill();
        if (spentSpace < steamToConsume) {
            steamToConsume = spentSpace;
            energyToGenerate = steamToConsume * energyPerMb;
        }

        if (steamToConsume <= 0) {
            isActive = false;
            return;
        }

        // Consume steam
        steamTank.fill(steamTank.getFill() - steamToConsume);

        // Produce spent steam
        spentSteamTank.setTankType(ModFluids.SPENTSTEAM.getSource());
        spentSteamTank.fill(spentSteamTank.getFill() + steamToConsume);

        // Generate energy
        setEnergyStored(getEnergyStored() + energyToGenerate);

        isActive = true;
        setChanged();
        sendUpdateToClient();
    }

    private long getEnergyPerMb() {
        if (steamTank.getTankType() == ModFluids.ULTRAHOTSTEAM.getSource()) return ENERGY_PER_MB_ULTRAHOT;
        if (steamTank.getTankType() == ModFluids.SUPERHOTSTEAM.getSource()) return ENERGY_PER_MB_SUPERHOT;
        if (steamTank.getTankType() == ModFluids.HOTSTEAM.getSource()) return ENERGY_PER_MB_HOT;
        if (steamTank.getTankType() == ModFluids.STEAM.getSource()) return ENERGY_PER_MB_STEAM;
        return 0;
    }

    public void drops() {
        if (level == null) return;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.industrial_turbine");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_STEAM_IN || slot == SLOT_SPENT_IN) {
            return stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).isPresent();
        }
        return false;
    }

    @Override
    protected void setupFluidCapability() {
        // Handled via custom FluidTank wrappers
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: Implement menu/GUI
        return null;
    }

    // --- Accessors ---

    public FluidTank getSteamTank() {
        return steamTank;
    }

    public FluidTank getSpentSteamTank() {
        return spentSteamTank;
    }

    public boolean isActive() {
        return isActive;
    }

    public float getAnim(float partialTicks) {
        return prevAnim + (anim - prevAnim) * partialTicks;
    }

    // --- NBT ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        steamTank.writeToNBT(tag, "steam");
        spentSteamTank.writeToNBT(tag, "spent");
        tag.putBoolean("active", isActive);
        tag.putFloat("anim", anim);
        tag.putFloat("prevAnim", prevAnim);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        steamTank.readFromNBT(tag, "steam");
        spentSteamTank.readFromNBT(tag, "spent");
        isActive = tag.getBoolean("active");
        anim = tag.getFloat("anim");
        prevAnim = tag.getFloat("prevAnim");
    }

    // --- Capabilities ---

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            // Input steam from the front/sides, extract spent from back
            if (side == null) return lazySteamHandler.cast();
            // Default to steam input
            return lazySteamHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(
                worldPosition.offset(-2, -1, -4),
                worldPosition.offset(3, 4, 8)
        );
    }
}
