package com.hbm_m.block.entity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.entity.BaseMachineBlockEntity;
import com.hbm_m.block.entity.ModBlockEntities;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineZirnoxMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ZirnoxRodItem;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.tags_and_tiers.ModTags;

import dev.architectury.fluid.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
//?}

@SuppressWarnings("UnstableApiUsage")
public class MachineZirnoxBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int INVENTORY_SIZE   = 28;
    public static final int ROD_SLOT_COUNT   = 24;
    public static final int SLOT_CO2_IN      = 24;
    public static final int SLOT_WATER_IN    = 25;
    public static final int SLOT_CO2_OUT     = 26;
    public static final int SLOT_WATER_OUT   = 27;

    public static final long MAX_HEAT      = 100_000;
    public static final long MAX_PRESSURE  = 100_000;
    public static final int  STEAM_MAX     = 32_000;
    public static final long CO2_MAX       = 16_000;
    public static final int  WATER_MAX     = 64_000;
    public static final long TANK_MAX      = WATER_MAX;

    private static final int[] NEIGHBOR_MASK_0  = {1, 7};
    private static final int[] NEIGHBOR_MASK_1  = {0, 2, 8};
    private static final int[] NEIGHBOR_MASK_2  = {1, 9};
    private static final int[] NEIGHBOR_MASK_3  = {4, 10};
    private static final int[] NEIGHBOR_MASK_4  = {3, 5, 11};
    private static final int[] NEIGHBOR_MASK_5  = {4, 6, 12};
    private static final int[] NEIGHBOR_MASK_6  = {5, 13};
    private static final int[] NEIGHBOR_MASK_7  = {0, 8, 14};
    private static final int[] NEIGHBOR_MASK_8  = {1, 7, 9, 15};
    private static final int[] NEIGHBOR_MASK_9  = {2, 8, 16};
    private static final int[] NEIGHBOR_MASK_10 = {3, 11, 17};
    private static final int[] NEIGHBOR_MASK_11 = {4, 10, 12, 18};
    private static final int[] NEIGHBOR_MASK_12 = {5, 11, 13, 19};
    private static final int[] NEIGHBOR_MASK_13 = {6, 12, 20};
    private static final int[] NEIGHBOR_MASK_14 = {7, 15, 21};
    private static final int[] NEIGHBOR_MASK_15 = {8, 14, 16, 22};
    private static final int[] NEIGHBOR_MASK_16 = {9, 15, 23};
    private static final int[] NEIGHBOR_MASK_17 = {10, 18};
    private static final int[] NEIGHBOR_MASK_18 = {11, 17, 19};
    private static final int[] NEIGHBOR_MASK_19 = {12, 18, 20};
    private static final int[] NEIGHBOR_MASK_20 = {13, 19};
    private static final int[] NEIGHBOR_MASK_21 = {14, 22};
    private static final int[] NEIGHBOR_MASK_22 = {15, 21, 23};
    private static final int[] NEIGHBOR_MASK_23 = {16, 22};

    public final FluidTank waterTank;
    public final FluidTank steamTank;

    public long carbonDioxide = 0;
    public long heat          = 0;
    public long pressure      = 0;
    public boolean isOn            = false;
    public boolean redstonePowered = false;
    private int output = 0;

    //? if forge {
    private final LazyOptional<IFluidHandler> lazyWaterHandler;
    private final LazyOptional<IFluidHandler> lazySteamHandler;
    //?}

    public MachineZirnoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);
        this.waterTank = new FluidTank(Fluids.WATER, WATER_MAX);
        this.steamTank = new FluidTank(ModFluids.SUPERHOTSTEAM.getSource(), STEAM_MAX);
        //? if forge {
        this.lazyWaterHandler = LazyOptional.of(() -> new WaterFluidHandler(this));
        this.lazySteamHandler = LazyOptional.of(() -> new SteamFluidHandler(this));
        //?}
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineZirnoxBlockEntity be) {
        if (!level.isClientSide) {
            be.update(level, pos);
        }
    }

    private void update(Level level, BlockPos pos) {
        long oldCo2      = carbonDioxide;
        int  oldWater    = waterTank.getFill();
        int  oldSteam    = steamTank.getFill();
        long oldHeat     = heat;
        long oldPressure = pressure;
        boolean oldOn    = isOn;

        processFluidInputs();

        if (redstonePowered) isOn = true;

        if (isOn) {
            for (int slot = 0; slot < ROD_SLOT_COUNT; slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (!stack.isEmpty() && stack.getItem() instanceof ZirnoxRodItem) decay(slot);
            }
        }

        pressure = computePressure();
        output = 0;

        if (heat > 0 && heat < MAX_HEAT) {
            if (waterTank.getFill() > 0 && carbonDioxide > 0 && steamTank.getFill() < STEAM_MAX) {
                generateSteam();
                long cooling = Math.max(1L, (heat * pressure) / 1_000_000L);
                heat = Math.max(0L, heat - cooling);
            } else {
                heat = Math.max(0L, heat - 10L);
            }
        }

        // Push steam out / pull water in via pipes
        for (Direction dir : Direction.values()) {
            BlockPos pipePos = pos.relative(dir);
            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            if (steamTank.getFill() > 0) {
                tryProvide(steamTank, level, pipePos, dir);
            }
            if (waterTank.getFill() < waterTank.getMaxFill()) {
                trySubscribe(Fluids.WATER, level, pipePos, dir);
            }
        }

        checkIfMeltdown();

        if (oldSteam != steamTank.getFill() || oldCo2 != carbonDioxide || oldWater != waterTank.getFill()
                || oldHeat != heat || oldPressure != pressure || oldOn != isOn) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ── IFluidStandardTransceiverMK2 ──────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ waterTank, steamTank }; }
    @Override public FluidTank[] getReceivingTanks(){ return new FluidTank[]{ waterTank }; }
    @Override public FluidTank[] getSendingTanks()  {
        return steamTank.getFill() > 0 ? new FluidTank[]{ steamTank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        if (VanillaFluidEquivalence.sameSubstance(fluid, Fluids.WATER)
                || VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WATER.getSource())) {
            return waterTank.getFill() < waterTank.getMaxFill();
        }
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.SUPERHOTSTEAM.getSource())) {
            return steamTank.getFill() > 0;
        }
        return false;
    }

    // ── Button handling ───────────────────────────────────────────────────

    public void handleButtonPress(int action) {
        switch (action) {
            case 0 -> { if (!redstonePowered) isOn = !isOn; }
            case 1 -> { carbonDioxide = Math.max(0L, carbonDioxide - 1_000L); pressure = computePressure(); }
            default -> { return; }
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── Gauge scaling (for GUI) ───────────────────────────────────────────

    public int getGaugeScaled(int scale, int type) {
        return switch (type) {
            case 0 -> (int) Math.min((long) steamTank.getFill() * scale / STEAM_MAX, scale);
            case 1 -> (int) (carbonDioxide * scale / CO2_MAX);
            case 2 -> (int) Math.min((long) waterTank.getFill() * scale / WATER_MAX, scale);
            case 3 -> (int) Math.min(heat * scale / MAX_HEAT, scale);
            case 4 -> (int) Math.min(pressure * scale / MAX_PRESSURE, scale);
            default -> 0;
        };
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        CompoundTag wt = new CompoundTag(); waterTank.writeToNBT(wt, "water"); tag.put("WaterTank", wt);
        CompoundTag st = new CompoundTag(); steamTank.writeToNBT(st, "steam"); tag.put("SteamTank", st);
        tag.putLong("co2", carbonDioxide);
        tag.putLong("heat", heat);
        tag.putLong("pressure", pressure);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("redstonePowered", redstonePowered);
        tag.putInt("output", output);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("WaterTank")) waterTank.readFromNBT(tag.getCompound("WaterTank"), "water");
        if (tag.contains("SteamTank")) steamTank.readFromNBT(tag.getCompound("SteamTank"), "steam");
        // Legacy migration: read old long-based values
        if (tag.contains("water") && !tag.contains("WaterTank")) waterTank.fillMb(Fluids.WATER, (int) tag.getLong("water"));
        carbonDioxide  = tag.getLong("co2");
        heat           = tag.getLong("heat");
        pressure       = tag.getLong("pressure");
        isOn           = tag.getBoolean("isOn");
        redstonePowered= tag.getBoolean("redstonePowered");
        output         = tag.getInt("output");
    }

    // ── Redstone ──────────────────────────────────────────────────────────

    public void setRedstonePowered(boolean powered) {
        if (!powered && redstonePowered) isOn = false;
        redstonePowered = powered;
        setChanged();
        sendUpdateToClient();
    }

    // ── Internal logic ────────────────────────────────────────────────────

    private long computePressure() {
        long byTank = carbonDioxide * 2L;
        long byHeat = (long) (heat * ((double) carbonDioxide / Math.max(1.0, CO2_MAX)));
        return Math.max(0L, byTank + byHeat);
    }

    private void generateSteam() {
        if (heat <= 10_256L) return;

        double efficiency = Math.min((double) carbonDioxide / 14_000.0D, 1.0D);
        int cycle = (int) ((((double) heat - 10_256.0D) / (double) MAX_HEAT) * efficiency * 25.0D * 5.0D);
        if (cycle <= 0) return;

        int space = STEAM_MAX - steamTank.getFill();
        int water = waterTank.getFill();
        int transfer = Math.min(cycle, Math.min(water, space));
        if (transfer <= 0) return;

        output = transfer;
        waterTank.drainMb(transfer);
        steamTank.fillMb(ModFluids.SUPERHOTSTEAM.getSource(), transfer);
    }

    private void processFluidInputs() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = inventory.getStackInSlot(i);

        boolean changed = false;
        if (waterTank.loadTank(SLOT_WATER_IN, SLOT_WATER_OUT, slots)) changed = true;
        if (tryLoadCO2FromBarrel(slots))                               changed = true;

        if (changed) for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
    }

    private boolean tryLoadCO2FromBarrel(ItemStack[] slots) {
        ItemStack input = slots[SLOT_CO2_IN];
        if (input.isEmpty() || !input.is(ModItems.FLUID_BARREL.get())) return false;

        long available = CO2_MAX - carbonDioxide;
        if (available <= 0) return false;

        ItemStack single = input.copy(); single.setCount(1);
        FluidStack barrelFluid = FluidBarrelItem.getFluid(single);
        if (barrelFluid.isEmpty() || !VanillaFluidEquivalence.sameSubstance(barrelFluid.getFluid(), ModFluids.CARBONDIOXIDE.getSource()))
            return false;

        long amount = barrelFluid.getAmount();
        if (amount <= 0 || available < amount) return false;

        ItemStack output = slots[SLOT_CO2_OUT];
        ItemStack emptied = single.copy();
        FluidBarrelItem.setFluid(emptied, FluidStack.empty());
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(output, emptied) || output.getCount() >= output.getMaxStackSize())
                return false;
        }

        input.shrink(1);
        slots[SLOT_CO2_IN] = input;
        if (output.isEmpty()) slots[SLOT_CO2_OUT] = emptied.copy();
        else { output.grow(1); slots[SLOT_CO2_OUT] = output; }
        carbonDioxide = Math.min(CO2_MAX, carbonDioxide + amount);
        return true;
    }

    private void checkIfMeltdown() {
        if (pressure > MAX_PRESSURE || heat > MAX_HEAT) meltdown();
    }

    private void meltdown() {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        isOn = false;
        waterTank.drainMb(waterTank.getFill());
        steamTank.drainMb(steamTank.getFill());
        carbonDioxide = 0;
        pressure = 0;
        heat = Math.max(heat, MAX_HEAT);

        if (level != null && !level.isClientSide) {
            BlockState current = getBlockState();
            BlockState destroyed = ModBlocks.ZIRNOX_DESTROYED.get().defaultBlockState();
            for (var property : current.getProperties()) {
                if (destroyed.hasProperty(property)) destroyed = copyPropertyUnsafe(destroyed, current, property);
            }
            level.setBlock(worldPosition, destroyed, 3);
            spawnDebrisBurst((ServerLevel) level);
            ExplosionNukeGeneric.incrementRad(level,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D, 35F);
            level.explode(null,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 1.0D, worldPosition.getZ() + 0.5D,
                8.0F, Level.ExplosionInteraction.BLOCK);
        }
    }

    private void spawnDebrisBurst(ServerLevel level) {
        double x = worldPosition.getX() + 0.5D;
        double y = worldPosition.getY() + 2.0D;
        double z = worldPosition.getZ() + 0.5D;

        level.sendParticles(ParticleTypes.EXPLOSION,   x, y, z, 10,  1.2D, 0.8D, 1.2D, 0.02D);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 80,  2.5D, 1.2D, 2.5D, 0.03D);
        level.sendParticles(ParticleTypes.FLAME,       x, y, z, 60,  2.0D, 1.0D, 2.0D, 0.05D);
        level.sendParticles(ParticleTypes.ASH,         x, y, z, 100, 2.5D, 1.5D, 2.5D, 0.01D);

        com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType[] types = {
            com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.BLANK,
            com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.ELEMENT,
            com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.SHRAPNEL,
            com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.CONCRETE,
            com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.EXCHANGER
        };
        int[] counts = {4, 3, 5, 2, 2};
        for (int t = 0; t < types.length; t++) {
            for (int i = 0; i < counts[t]; i++) {
                var debris = com.hbm_m.entity.projectile.ZirnoxDebrisEntity.create(level, x, y, z, types[t]);
                double speed = 0.3D + level.random.nextDouble() * 0.5D;
                double angle = level.random.nextDouble() * Math.PI * 2;
                debris.setDeltaMovement(Math.cos(angle) * speed, 0.4D + level.random.nextDouble() * 0.6D, Math.sin(angle) * speed);
                level.addFreshEntity(debris);
            }
        }
    }

    // ── Rod/decay helpers ─────────────────────────────────────────────────

    private boolean hasFuelRod(int slot) {
        RodSpec spec = getRodSpec(inventory.getStackInSlot(slot));
        return spec != null && !spec.breeding();
    }

    private int getNeighbourCount(int slot) {
        int count = 0;
        for (int n : getNeighbouringSlots(slot)) if (hasFuelRod(n)) count++;
        return count;
    }

    private void decay(int slot) {
        ItemStack stack = inventory.getStackInSlot(slot);
        RodSpec spec = getRodSpec(stack);
        if (spec == null) return;

        int pulses = getNeighbourCount(slot);
        if (!spec.breeding()) pulses++;

        for (int i = 0; i < pulses; i++) {
            heat += spec.heat();
            ZirnoxRodItem.incrementLifeTime(stack);
            if (ZirnoxRodItem.getLifeTime(stack) > spec.maxLife()) {
                inventory.setStackInSlot(slot, spec.depletedOrProduct().copy());
                break;
            }
        }
    }

    private RodSpec getRodSpec(ItemStack stack) {
        if (stack.isEmpty()) return null;
        if (stack.is(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get()))   return new RodSpec(250_000, 30,  false, new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_URANIUM_FUEL.get()))           return new RodSpec(200_000, 50,  false, new ItemStack(ModItems.ROD_ZIRNOX_URANIUM_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_TH232.get()))                  return new RodSpec(20_000,  0,   true,  new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_THORIUM_FUEL.get()))           return new RodSpec(200_000, 40,  false, new ItemStack(ModItems.ROD_ZIRNOX_THORIUM_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_MOX_FUEL.get()))               return new RodSpec(165_000, 75,  false, new ItemStack(ModItems.ROD_ZIRNOX_MOX_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL.get()))         return new RodSpec(175_000, 65,  false, new ItemStack(ModItems.ROD_ZIRNOX_PLUTONIUM_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_U233_FUEL.get()))              return new RodSpec(150_000, 100, false, new ItemStack(ModItems.ROD_ZIRNOX_U233_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_U235_FUEL.get()))              return new RodSpec(165_000, 85,  false, new ItemStack(ModItems.ROD_ZIRNOX_U235_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_LES_FUEL.get()))               return new RodSpec(150_000, 150, false, new ItemStack(ModItems.ROD_ZIRNOX_LES_FUEL_DEPLETED.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_LITHIUM.get()))                return new RodSpec(20_000,  0,   true,  new ItemStack(ModItems.ROD_ZIRNOX_TRITIUM.get()));
        if (stack.is(ModItems.ROD_ZIRNOX_ZFB_MOX.get()))               return new RodSpec(50_000,  35,  false, new ItemStack(ModItems.ROD_ZIRNOX_ZFB_MOX_DEPLETED.get()));
        return null;
    }

    private int[] getNeighbouringSlots(int slot) {
        return switch (slot) {
            case 0  -> NEIGHBOR_MASK_0;  case 1  -> NEIGHBOR_MASK_1;  case 2  -> NEIGHBOR_MASK_2;
            case 3  -> NEIGHBOR_MASK_3;  case 4  -> NEIGHBOR_MASK_4;  case 5  -> NEIGHBOR_MASK_5;
            case 6  -> NEIGHBOR_MASK_6;  case 7  -> NEIGHBOR_MASK_7;  case 8  -> NEIGHBOR_MASK_8;
            case 9  -> NEIGHBOR_MASK_9;  case 10 -> NEIGHBOR_MASK_10; case 11 -> NEIGHBOR_MASK_11;
            case 12 -> NEIGHBOR_MASK_12; case 13 -> NEIGHBOR_MASK_13; case 14 -> NEIGHBOR_MASK_14;
            case 15 -> NEIGHBOR_MASK_15; case 16 -> NEIGHBOR_MASK_16; case 17 -> NEIGHBOR_MASK_17;
            case 18 -> NEIGHBOR_MASK_18; case 19 -> NEIGHBOR_MASK_19; case 20 -> NEIGHBOR_MASK_20;
            case 21 -> NEIGHBOR_MASK_21; case 22 -> NEIGHBOR_MASK_22; case 23 -> NEIGHBOR_MASK_23;
            default -> new int[0];
        };
    }

    // ── Item-slot validation ──────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (slot >= 0 && slot < ROD_SLOT_COUNT)
            return stack.is(ModTags.Items.ZIRNOX_RODS);
        if (slot == SLOT_CO2_IN)
            return holdsFluid(stack, ModFluids.CARBONDIOXIDE.getSource());
        if (slot == SLOT_WATER_IN)
            return holdsFluid(stack, ModFluids.WATER.getSource()) || stack.is(Items.WATER_BUCKET);
        return false;
    }

    private static boolean holdsFluid(ItemStack stack, Fluid targetFluid) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.WATER_BUCKET))
            return VanillaFluidEquivalence.sameSubstance(targetFluid, Fluids.WATER);
        if (!stack.is(ModItems.FLUID_BARREL.get())) return false;
        FluidStack fluid = FluidBarrelItem.getFluid(stack);
        return !fluid.isEmpty() && VanillaFluidEquivalence.sameSubstance(fluid.getFluid(), targetFluid);
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyPropertyUnsafe(BlockState target, BlockState source,
            net.minecraft.world.level.block.state.properties.Property property) {
        return target.setValue(property, source.getValue(property));
    }

    private record RodSpec(int maxLife, int heat, boolean breeding, ItemStack depletedOrProduct) {}

    @Override
    protected Component getDefaultName() { return Component.translatable("block.hbm_m.zirnox"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineZirnoxMenu.create(id, inv, this);
    }

    // ── Forge fluid capabilities ──────────────────────────────────────────
    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            return lazySteamHandler.cast(); // any side: steam out preferred for pipes pulling
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyWaterHandler.invalidate();
        lazySteamHandler.invalidate();
    }

    private static class WaterFluidHandler implements IFluidHandler {
        private final MachineZirnoxBlockEntity be;
        WaterFluidHandler(MachineZirnoxBlockEntity be) { this.be = be; }
        @Override public int getTanks() { return 1; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int t) {
            return new net.minecraftforge.fluids.FluidStack(be.waterTank.getTankType(), be.waterTank.getFill()); }
        @Override public int getTankCapacity(int t) { return be.waterTank.getMaxFill(); }
        @Override public boolean isFluidValid(int t, @NotNull net.minecraftforge.fluids.FluidStack s) {
            return VanillaFluidEquivalence.sameSubstance(s.getFluid(), Fluids.WATER); }
        @Override public int fill(net.minecraftforge.fluids.FluidStack res, FluidAction a) {
            if (!VanillaFluidEquivalence.sameSubstance(res.getFluid(), Fluids.WATER)) return 0;
            int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
            int fill = Math.min(space, res.getAmount());
            if (a.execute()) be.waterTank.fillMb(Fluids.WATER, fill);
            return fill; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack r, FluidAction a) { return net.minecraftforge.fluids.FluidStack.EMPTY; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack drain(int max, FluidAction a) { return net.minecraftforge.fluids.FluidStack.EMPTY; }
    }

    private static class SteamFluidHandler implements IFluidHandler {
        private final MachineZirnoxBlockEntity be;
        SteamFluidHandler(MachineZirnoxBlockEntity be) { this.be = be; }
        @Override public int getTanks() { return 1; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int t) {
            return new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), be.steamTank.getFill()); }
        @Override public int getTankCapacity(int t) { return be.steamTank.getMaxFill(); }
        @Override public boolean isFluidValid(int t, @NotNull net.minecraftforge.fluids.FluidStack s) { return false; }
        @Override public int fill(net.minecraftforge.fluids.FluidStack r, FluidAction a) { return 0; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack res, FluidAction a) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int d = Math.min(be.steamTank.getFill(), res.getAmount());
            net.minecraftforge.fluids.FluidStack out = new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), d);
            if (a.execute()) be.steamTank.drainMb(d);
            return out; }
        @Override public @NotNull net.minecraftforge.fluids.FluidStack drain(int max, FluidAction a) {
            if (be.steamTank.getFill() <= 0) return net.minecraftforge.fluids.FluidStack.EMPTY;
            int d = Math.min(be.steamTank.getFill(), max);
            net.minecraftforge.fluids.FluidStack out = new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), d);
            if (a.execute()) be.steamTank.drainMb(d);
            return out; }
    }
    //?}
}
