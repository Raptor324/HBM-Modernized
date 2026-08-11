package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.machines.MachineSolarBoilerBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

/**
 * Solar Boiler BlockEntity - converts water to steam using sunlight.
 *
 * Mirrors the original HBM 1.7.10 "Solar Boiler" multiblock: it requires direct sky
 * access and daytime to operate, and its conversion rate scales up with the number of
 * nearby "Solar Mirror" blocks that themselves have sky access.
 *
 * No RF energy consumption - purely solar driven.
 */
public class MachineSolarBoilerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    // Slot definitions (item-based fluid container loading/unloading)
    public static final int SLOT_WATER_IN  = 0;
    public static final int SLOT_WATER_OUT = 1;
    public static final int SLOT_STEAM_IN  = 2;
    public static final int SLOT_STEAM_OUT = 3;
    public static final int INVENTORY_SIZE = 4;

    // Tank indices
    public static final int TANK_WATER = 0;
    public static final int TANK_STEAM = 1;

    // Capacity constants
    private static final int WATER_CAPACITY = 16_000;        // 16,000 mB
    private static final int STEAM_CAPACITY = 1_600_000;     // 1,600,000 mB

    // Conversion constants
    private static final int STEAM_PER_WATER = 100;          // 1 mB water -> 100 mB steam
    private static final int BASE_WATER_PER_TICK = 2;        // base mB water/tick with sun and zero mirrors
    private static final int WATER_PER_MIRROR = 1;           // additional mB water/tick per active nearby mirror

    // Solar mirror scan: horizontal radius around the boiler, scanned at the boiler's
    // controller Y level (and one level below, to catch mirrors placed on the ground
    // next to a boiler raised up on a pedestal).
    private static final int MIRROR_SCAN_RADIUS = 7;
    private static final int MAX_EFFECTIVE_MIRRORS = 32;

    // Fluid tanks
    private final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.WATER.getSource(), WATER_CAPACITY),
            new FluidTank(ModFluids.STEAM.getSource(), STEAM_CAPACITY)
    };

    // Cached solar brightness (0-15 sky light at the block above the structure)
    private int solarBrightness = 0;
    // Cached count of nearby mirrors with sky access (for GUI / debugging)
    private int activeMirrorCount = 0;

    // GUI data
    protected final ContainerData data;

    public MachineSolarBoilerBlockEntity(BlockPos pos, BlockState state) {
        // No RF energy capacity/receive rate - solar powered
        super(ModBlockEntities.SOLAR_BOILER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getWaterAmount();
                    case 1 -> getWaterCapacity();
                    case 2 -> getSteamAmount();
                    case 3 -> getSteamCapacity();
                    case 4 -> solarBrightness;
                    case 5 -> isActive() ? 1 : 0;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 4) solarBrightness = value;
            }

            @Override
            public int getCount() { return 6; }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolarBoilerBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick(level, pos);
            return;
        }

        boolean wasActive = be.isActive();

        // Update solar brightness (sky brightness above the top of the 3x3x3 structure)
        be.solarBrightness = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos.above(2));

        // Process item-based fluid containers (buckets, canisters, etc.)
        boolean changed = be.processFluidContainers();

        // Convert water to steam using solar energy + nearby mirrors
        changed |= be.processSolarBoiling(level, pos);

        // Update visual state (LIT property)
        boolean nowActive = be.isActive();
        if (wasActive != nowActive) {
            level.setBlock(pos, state.setValue(MachineSolarBoilerBlock.LIT, nowActive), 3);
            changed = true;
        }

        if (changed) {
            be.setChanged();
            be.sendUpdateToClient();
        }

        be.ensureNetworkInitialized();
    }

    /** Client-side: spawn steam particles above the boiler while it's actively producing steam. */
    private void clientTick(Level level, BlockPos pos) {
        if (!isActive()) return;
        if (level.getGameTime() % 4 != 0) return;

        double x = pos.getX() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;
        double y = pos.getY() + 3.05; // top of the 3x3x3 structure
        double z = pos.getZ() + 0.5 + (level.random.nextDouble() - 0.5) * 1.5;

        level.addParticle(ParticleTypes.CLOUD, x, y, z, 0.0, 0.05, 0.0);
    }

    private boolean processFluidContainers() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            slots[i] = inventory.getStackInSlot(i);
        }

        boolean changed = false;

        if (getTank(TANK_WATER).loadTank(SLOT_WATER_IN, SLOT_WATER_OUT, slots)) {
            changed = true;
        }
        if (getTank(TANK_STEAM).unloadTank(SLOT_STEAM_IN, SLOT_STEAM_OUT, slots)) {
            changed = true;
        }

        if (changed) {
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                inventory.setStackInSlot(i, slots[i]);
            }
        }

        return changed;
    }

    /**
     * Core conversion logic. Conversion rate formula:
     * <pre>
     *   waterPerTick = (BASE_WATER_PER_TICK + activeMirrors * WATER_PER_MIRROR) * (solarBrightness / 15)
     *   steamProduced = waterPerTick * STEAM_PER_WATER
     * </pre>
     * Gated on: direct sky access above the structure, daytime (sun above the horizon),
     * available water, and free space in the steam tank.
     */
    private boolean processSolarBoiling(Level level, BlockPos pos) {
        if (!hasSkyAccess(level, pos) || !level.isDay()) {
            if (activeMirrorCount != 0) {
                activeMirrorCount = 0;
                return true;
            }
            return false;
        }

        activeMirrorCount = countActiveMirrors(level, pos);

        if (solarBrightness <= 0) return false;

        FluidTank waterTank = getTank(TANK_WATER);
        FluidTank steamTank = getTank(TANK_STEAM);

        if (waterTank.getFill() <= 0 || steamTank.getFill() >= steamTank.getMaxFill()) return true;

        int mirrors = Math.min(activeMirrorCount, MAX_EFFECTIVE_MIRRORS);
        int baseRate = BASE_WATER_PER_TICK + mirrors * WATER_PER_MIRROR;

        int waterToProcess = (baseRate * solarBrightness) / 15;
        if (waterToProcess <= 0) return true;

        waterToProcess = Math.min(waterToProcess, waterTank.getFill());
        if (waterToProcess <= 0) return true;

        int steamProduced = waterToProcess * STEAM_PER_WATER;
        int steamSpace = steamTank.getMaxFill() - steamTank.getFill();

        if (steamSpace < steamProduced) {
            steamProduced = steamSpace;
            waterToProcess = steamProduced / STEAM_PER_WATER;
        }

        if (waterToProcess <= 0) return true;

        // Consume water, produce steam
        waterTank.drainMb(waterToProcess);
        steamTank.fillMb(ModFluids.STEAM.getSource(), steamProduced);

        return true;
    }

    /** Direct sky access check above the top of the 3x3x3 structure. */
    private boolean hasSkyAccess(Level level, BlockPos pos) {
        return level.canSeeSky(pos.above(2));
    }

    /**
     * Scans a horizontal area around the boiler for "Solar Mirror" blocks that themselves
     * have sky access. Scanned at the boiler's controller Y level and one level below it,
     * within {@link #MIRROR_SCAN_RADIUS} blocks horizontally.
     */
    private int countActiveMirrors(Level level, BlockPos pos) {
        int count = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dy = 0; dy >= -1; dy--) {
            int y = pos.getY() + dy;
            for (int dx = -MIRROR_SCAN_RADIUS; dx <= MIRROR_SCAN_RADIUS; dx++) {
                for (int dz = -MIRROR_SCAN_RADIUS; dz <= MIRROR_SCAN_RADIUS; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    cursor.set(pos.getX() + dx, y, pos.getZ() + dz);

                    if (!level.isLoaded(cursor)) continue;
                    if (!level.getBlockState(cursor).is(ModBlocks.SOLAR_MIRRORS.get())) continue;
                    if (!level.canSeeSky(cursor)) continue;

                    count++;
                    if (count >= MAX_EFFECTIVE_MIRRORS) return count;
                }
            }
        }
        return count;
    }

    public boolean isActive() {
        return solarBrightness > 0
                && level != null && level.isDay()
                && getTank(TANK_WATER).getFill() > 0
                && getTank(TANK_STEAM).getFill() < getTank(TANK_STEAM).getMaxFill();
    }

    // ═══════════════════════════ Tanks ════════════════════════════════

    public FluidTank[] getTanks() {
        return tanks;
    }

    public FluidTank getTank(int index) {
        return (index >= 0 && index < tanks.length) ? tanks[index] : tanks[TANK_WATER];
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { getTank(TANK_WATER) };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { getTank(TANK_STEAM) };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ═══════════════════════════ Getters ════════════════════════════════

    public int getWaterAmount()    { return getTank(TANK_WATER).getFill(); }
    public int getWaterCapacity()  { return getTank(TANK_WATER).getMaxFill(); }
    public int getSteamAmount()    { return getTank(TANK_STEAM).getFill(); }
    public int getSteamCapacity()  { return getTank(TANK_STEAM).getMaxFill(); }
    public int getSolarBrightness() { return solarBrightness; }
    public int getActiveMirrorCount() { return activeMirrorCount; }

    // --- NBT ---
    //? if < 1.21.1 {
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putInt("SolarBrightness", solarBrightness);
        tag.putInt("ActiveMirrorCount", activeMirrorCount);
    }
    //?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.saveAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "tank_" + i);
        }
        tag.putInt("SolarBrightness", solarBrightness);
        tag.putInt("ActiveMirrorCount", activeMirrorCount);
    
    }
    *///?}

    //? if < 1.21.1 {
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        solarBrightness = tag.getInt("SolarBrightness");
        activeMirrorCount = tag.getInt("ActiveMirrorCount");
    }
    //?} else {
    /*@Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {

        super.loadAdditional(tag, registries);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "tank_" + i);
        }
        solarBrightness = tag.getInt("SolarBrightness");
        activeMirrorCount = tag.getInt("ActiveMirrorCount");
    
    }
    *///?}

    // --- Capabilities ---
    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new SolarBoilerFluidHandler(this));
    }
    //?}

    // --- GUI ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.hbm_m.solar_boiler");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        // No GUI for the Solar Boiler - it operates passively.
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.solar_boiler");
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_WATER_IN  -> com.hbm_m.api.fluids.FluidItemAccess.hasFluidHandler(stack);
            case SLOT_STEAM_IN  -> com.hbm_m.api.fluids.FluidItemAccess.hasFluidHandler(stack);
            case SLOT_WATER_OUT, SLOT_STEAM_OUT -> false;
            default -> false;
        };
    }

    public void drops() {
        if (level != null) {
            SimpleContainer container = new SimpleContainer(inventory.getSlots());
            for (int i = 0; i < inventory.getSlots(); i++) {
                container.setItem(i, inventory.getStackInSlot(i));
            }
            Containers.dropContents(level, worldPosition, container);
        }
    }
}
