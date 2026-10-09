package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.interfaces.IMultiblockSidedIO;
import com.hbm_m.explosion.ExplosionNukeGeneric;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineZirnoxMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ZirnoxRodItem;
import com.hbm_m.item.tags_and_tiers.ModTags;

import dev.architectury.fluid.FluidStack;
import java.util.EnumSet;
import java.util.Set;

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
public class MachineZirnoxBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2, IMultiblockSidedIO,
        com.hbm_m.api.redstoneoverradio.IRORValueProvider, com.hbm_m.api.redstoneoverradio.IRORInteractive {

    public static final int INVENTORY_SIZE   = 28;
    public static final int ROD_SLOT_COUNT   = 24;
    public static final int SLOT_CO2_IN      = 24;
    public static final int SLOT_WATER_IN    = 25;
    public static final int SLOT_CO2_OUT     = 26;
    public static final int SLOT_WATER_OUT   = 27;

    public static final long MAX_HEAT      = 100_000;
    public static final long MAX_PRESSURE  = 100_000;
    /** Original: steam 8000, carbonDioxide 16000, water 32000. */
    public static final int  STEAM_MAX     = 8_000;
    public static final int  CO2_MAX       = 16_000;
    public static final int  WATER_MAX     = 32_000;

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
    public final FluidTank co2Tank;
    public final FluidTank steamTank;
    public long heat          = 0;
    public long pressure      = 0;
    public boolean isOn            = false;
    public boolean redstonePowered = false;
    private int output = 0;

    private Set<Direction> allowedFluidSides = EnumSet.noneOf(Direction.class);
    private boolean fluidSidesFromMultiblockStructure = false;

    public MachineZirnoxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZIRNOX_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L);
        this.waterTank = new FluidTank(Fluids.WATER, WATER_MAX);
        this.co2Tank = new FluidTank(ModFluids.CARBONDIOXIDE.getSource(), CO2_MAX);
        this.steamTank = new FluidTank(ModFluids.SUPERHOTSTEAM.getSource(), STEAM_MAX);
    }

    @Override public int getFloorCount() { return 3 * 3; }
    @Override public BlockPos getFloorPosFromIndex(int index) { return this.standardFloor5x5(index); }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineZirnoxBlockEntity be) {
        if (!level.isClientSide) {
            be.update(level, pos);
        }
    }

    private void update(Level level, BlockPos pos) {
        this.checkTilt(TiltType.CONFIG, true);
        int  oldCo2      = co2Tank.getFill();
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
                else if (stack.is(ModItems.METEORITE_SWORD_BRED.get()))
                    inventory.setStackInSlot(slot, new ItemStack(ModItems.METEORITE_SWORD_IRRADIATED.get()));
            }
        }

        pressure = computePressure();
        output = 0;

        if (heat > 0 && heat < MAX_HEAT) {
            if (waterTank.getFill() > 0 && co2Tank.getFill() > 0 && steamTank.getFill() < STEAM_MAX) {
                generateSteam();
                // Original: heat -= (int) (heat * pressure / 1000000F)
                heat -= (int) ((float) heat * (float) pressure / 1000000F);
            } else {
                heat -= 10;
            }

            if (level != null && level.getGameTime() % 100 == 0) {
                com.hbm_m.satellite.RayScanEvents.reportEvent(level, worldPosition, com.hbm_m.satellite.RayScanEvents.INFO_NUCLEAR, 200);
            }
        }

        // Push steam out / pull water in via pipes (Original: gekippt keine Anschluesse)
        if (!this.tilted) for (Direction dir : Direction.values()) {
            if (fluidSidesFromMultiblockStructure) {
                if (!allowedFluidSides.contains(dir)) continue;
            } else if (!allowedFluidSides.isEmpty() && !allowedFluidSides.contains(dir)) {
                continue;
            }
            BlockPos pipePos = pos.relative(dir);
            BlockEntity pipeBe = level.getBlockEntity(pipePos);
            if (!(pipeBe instanceof com.hbm_m.api.fluids.IFluidConnectorMK2)) continue;

            if (steamTank.getFill() > 0) {
                tryProvide(steamTank, level, pipePos, dir);
            }
            if (waterTank.getFill() < waterTank.getMaxFill()) {
                trySubscribe(Fluids.WATER, level, pipePos, dir);
            }
            if (co2Tank.getFill() < co2Tank.getMaxFill()) {
                trySubscribe(ModFluids.CARBONDIOXIDE.getSource(), level, pipePos, dir);
            }
        }

        checkIfMeltdown();

        if (oldSteam != steamTank.getFill() || oldCo2 != co2Tank.getFill() || oldWater != waterTank.getFill()
                || oldHeat != heat || oldPressure != pressure || oldOn != isOn) {
            setChanged();
            sendUpdateToClient();
        }
    }

    // ── IFluidStandardTransceiverMK2 ──────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ waterTank, steamTank, co2Tank }; }
    @Override public FluidTank[] getReceivingTanks(){ return new FluidTank[]{ waterTank, co2Tank }; }
    @Override public FluidTank[] getSendingTanks()  {
        return steamTank.getFill() > 0 ? new FluidTank[]{ steamTank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir != null) {
            if (fluidSidesFromMultiblockStructure && !allowedFluidSides.contains(fromDir)) return false;
            if (!fluidSidesFromMultiblockStructure && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(fromDir)) {
                return false;
            }
        }
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        if (VanillaFluidEquivalence.sameSubstance(fluid, Fluids.WATER)
                || VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WATER.getSource())) {
            return waterTank.getFill() < waterTank.getMaxFill();
        }
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.SUPERHOTSTEAM.getSource())) {
            return steamTank.getFill() > 0;
        }
        if (VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.CARBONDIOXIDE.getSource())) {
            return co2Tank.getFill() < co2Tank.getMaxFill();
        }
        return false;
    }

    // ── Button handling ───────────────────────────────────────────────────

    public void handleButtonPress(int action) {
        switch (action) {
            case 0 -> { if (!redstonePowered) isOn = !isOn; }
            case 1 -> { co2Tank.drainMb(Math.min(1_000, co2Tank.getFill())); pressure = computePressure(); }
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
            case 1 -> (int) Math.min((long) co2Tank.getFill() * scale / CO2_MAX, scale);
            case 2 -> (int) Math.min((long) waterTank.getFill() * scale / WATER_MAX, scale);
            case 3 -> (int) Math.min(heat * scale / MAX_HEAT, scale);
            case 4 -> (int) Math.min(pressure * scale / MAX_PRESSURE, scale);
            default -> 0;
        };
    }

    // ── NBT ───────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        CompoundTag wt = new CompoundTag(); waterTank.writeToNBT(wt, "water"); tag.put("WaterTank", wt);
        CompoundTag ct = new CompoundTag(); co2Tank.writeToNBT(ct, "co2"); tag.put("Co2Tank", ct);
        CompoundTag st = new CompoundTag(); steamTank.writeToNBT(st, "steam"); tag.put("SteamTank", st);
        tag.putLong("heat", heat);
        tag.putLong("pressure", pressure);
        tag.putBoolean("isOn", isOn);
        tag.putBoolean("redstonePowered", redstonePowered);
        tag.putInt("output", output);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("WaterTank")) waterTank.readFromNBT(tag.getCompound("WaterTank"), "water");
        if (tag.contains("Co2Tank")) co2Tank.readFromNBT(tag.getCompound("Co2Tank"), "co2");
        if (tag.contains("SteamTank")) steamTank.readFromNBT(tag.getCompound("SteamTank"), "steam");
        // Legacy migration: read old long-based values
        if (tag.contains("water") && !tag.contains("WaterTank")) waterTank.fillMb(Fluids.WATER, (int) tag.getLong("water"));
        if (tag.contains("co2") && !tag.contains("Co2Tank")) co2Tank.fillMb(ModFluids.CARBONDIOXIDE.getSource(), (int) tag.getLong("co2"));
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
        int co2 = co2Tank.getFill();
        long byTank = co2 * 2L;
        long byHeat = (long) (heat * ((double) co2 / Math.max(1.0, CO2_MAX)));
        return Math.max(0L, byTank + byHeat);
    }

    /** 1:1 {@code generateSteam}: Wasser und Dampf werden unabhaengig voneinander begrenzt (wie im Original). */
    private void generateSteam() {
        if (this.heat > 10256) {
            float mult = 7.5F; // was 5 originally
            int cycle = (int) ((((float) heat - 10256F) / (float) MAX_HEAT) * Math.min(((float) co2Tank.getFill() / 14000F), 1F) * 25F * mult);
            this.output = cycle;

            waterTank.setFill(waterTank.getFill() - cycle);
            steamTank.setFill(steamTank.getFill() + cycle);

            if (waterTank.getFill() < 0)
                waterTank.setFill(0);

            if (steamTank.getFill() > steamTank.getMaxFill())
                steamTank.setFill(steamTank.getMaxFill());
        }
    }

    private void processFluidInputs() {
        ItemStack[] slots = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) slots[i] = inventory.getStackInSlot(i);

        boolean changed = false;
        if (waterTank.loadTank(SLOT_WATER_IN, SLOT_WATER_OUT, slots)) changed = true;
        if (co2Tank.loadTank(SLOT_CO2_IN, SLOT_CO2_OUT, slots))       changed = true;

        if (changed) for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, slots[i]);
    }

    private void checkIfMeltdown() {
        if (pressure > MAX_PRESSURE || heat > MAX_HEAT) meltdown();
    }

    private void meltdown() {
        for (int i = 0; i < INVENTORY_SIZE; i++) inventory.setStackInSlot(i, ItemStack.EMPTY);
        isOn = false;
        waterTank.drainMb(waterTank.getFill());
        co2Tank.drainMb(co2Tank.getFill());
        steamTank.drainMb(steamTank.getFill());
        pressure = 0;
        heat = Math.max(heat, MAX_HEAT);

        if (level != null && !level.isClientSide) {
            BlockState current = getBlockState();
            BlockState destroyed = ModBlocks.ZIRNOX_DESTROYED.get().defaultBlockState();
            for (var property : current.getProperties()) {
                if (destroyed.hasProperty(property)) destroyed = copyPropertyUnsafe(destroyed, current, property);
            }
            level.setBlock(worldPosition, destroyed, 3);
            level.playSound(null, worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(),
                com.hbm_m.sound.HbmSoundsNT.get("hbm:block.rbmk_explosion"), net.minecraft.sounds.SoundSource.BLOCKS, 10.0F, 1.0F);
            level.explode(null, worldPosition.getX(), worldPosition.getY() + 3, worldPosition.getZ(),
                12.0F, Level.ExplosionInteraction.BLOCK);
            zirnoxDebris();
            ExplosionNukeGeneric.waste(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), 35);

            // Everyone within 100 blocks gets credited, exactly as the original does - there is
            // no single player to blame for a runaway reactor.
            com.hbm_m.advancement.ModAdvancements.grantNearby(level,
                worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D, worldPosition.getZ() + 0.5D,
                100D, com.hbm_m.advancement.ModAdvancements.ZIRNOX_BOOM);

            // Original: radMark fuer die Strahlungs-Elementare
            if (com.hbm_m.config.MobConfig.enableElementals()) {
                for (net.minecraft.world.entity.player.Player p : level.getEntitiesOfClass(net.minecraft.world.entity.player.Player.class,
                        new net.minecraft.world.phys.AABB(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5,
                                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5).inflate(100, 100, 100))) {
                    if (p instanceof net.minecraft.server.level.ServerPlayer sp) com.hbm_m.handler.BossSpawnHandler.markForRadBeasts(sp);
                }
            }
        }
    }

    /** 1:1 {@code spawnDebris}: Start 4 ueber dem Kern, Beton steiler, Waermetauscher flach und seitlich. */
    private void spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType type) {
        var debris = com.hbm_m.entity.projectile.ZirnoxDebrisEntity.create(level, worldPosition.getX() + 0.5D, worldPosition.getY() + 4D, worldPosition.getZ() + 0.5D, type);
        double mx = level.random.nextGaussian() * 0.75D;
        double mz = level.random.nextGaussian() * 0.75D;
        double my = 0.01D + level.random.nextDouble() * 1.25D;

        if (type == com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.CONCRETE) {
            mx *= 0.25D;
            my += level.random.nextDouble();
            mz *= 0.25D;
        }

        if (type == com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.EXCHANGER) {
            mx += 0.5D;
            my *= 0.1D;
            mz += 0.5D;
        }

        debris.setDeltaMovement(mx, my, mz);
        level.addFreshEntity(debris);
    }

    /** 1:1 {@code zirnoxDebris}. */
    private void zirnoxDebris() {
        for (int i = 0; i < 2; i++) {
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.EXCHANGER);
        }

        for (int i = 0; i < 20; i++) {
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.CONCRETE);
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.BLANK);
        }

        for (int i = 0; i < 10; i++) {
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.ELEMENT);
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.GRAPHITE);
            spawnDebris(com.hbm_m.entity.projectile.ZirnoxDebrisEntity.DebrisType.SHRAPNEL);
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
        if (stack.is(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL.get()))   return new RodSpec(250_000, 30,  false, new ItemStack(ModItems.ROD_ZIRNOX_NATURAL_URANIUM_FUEL_DEPLETED.get()));
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

    /** Original {@code isItemValidForSlot}: nur Zirnox-Staebe in die 24 Stabplaetze (Behaelter nur per Hand im GUI). */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        return slot >= 0 && slot < ROD_SLOT_COUNT && stack.getItem() instanceof ZirnoxRodItem;
    }

    private static boolean holdsFluid(ItemStack stack, Fluid targetFluid) {
        if (stack.isEmpty()) return false;
        if (stack.is(Items.WATER_BUCKET))
            return VanillaFluidEquivalence.sameSubstance(targetFluid, Fluids.WATER);
        // Original: alles, was die FluidContainerRegistry als Behaelter dieser Fluessigkeit kennt
        return com.hbm_m.inventory.FluidContainerRegistry.getFluidContent(stack, targetFluid) > 0;
    }

    // ── Misc ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static BlockState copyPropertyUnsafe(BlockState target, BlockState source,
            net.minecraft.world.level.block.state.properties.Property property) {
        return target.setValue(property, source.getValue(property));
    }

    private record RodSpec(int maxLife, int heat, boolean breeding, ItemStack depletedOrProduct) {}

    @Override
    protected Component getDefaultName() { return Component.translatable("container.zirnox"); }
    @Override
    public Component getDisplayName()    { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineZirnoxMenu.create(id, inv, this);
    }

    @Override
    public void setAllowedFluidSidesFromMultiblockStructure(Set<Direction> sides) {
        this.allowedFluidSides = safeCopyDirectionSet(sides);
        this.fluidSidesFromMultiblockStructure = true;
        setChanged();
        sendUpdateToClient();
    }

    @Override
    public void setAllowedFluidSides(Set<Direction> sides) {
        this.allowedFluidSides = safeCopyDirectionSet(sides);
        this.fluidSidesFromMultiblockStructure = false;
        setChanged();
        sendUpdateToClient();
        //? if neoforge {
        /*invalidateCapabilities(); // NeoForge-Capability-Cache: Seiten/Rolle geaendert
        *///?}
    }

    /**
     * Безопасная defensive-копия Set<Direction> в EnumSet. EnumSet.copyOf(Collection)
     * бросает IllegalArgumentException на пустой коллекции; здесь всегда возвращаем
     * валидный EnumSet (пустой ли, нет) и принимаем null как пустое множество.
     */
    private static EnumSet<Direction> safeCopyDirectionSet(Set<Direction> sides) {
        EnumSet<Direction> out = EnumSet.noneOf(Direction.class);
        if (sides != null && !sides.isEmpty()) {
            out.addAll(sides);
        }
        return out;
    }

    @Override
    public Set<Direction> getAllowedFluidSides() {
        return this.allowedFluidSides;
    }

    // ── Forge fluid capabilities ──────────────────────────────────────────
    //? if forge {
    @Override
    protected void setupFluidCapability() {
        setFluidHandler(new UnifiedFluidHandler(this));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast(); // Original ISidedInventory
        if (cap == ForgeCapabilities.FLUID_HANDLER && side != null) {
            if (fluidSidesFromMultiblockStructure && !allowedFluidSides.contains(side)) {
                return LazyOptional.empty();
            }
            if (!fluidSidesFromMultiblockStructure && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                return LazyOptional.empty();
            }
        }
        return super.getCapability(cap, side);
    }

    private static class UnifiedFluidHandler implements IFluidHandler {
        private final MachineZirnoxBlockEntity be;

        UnifiedFluidHandler(MachineZirnoxBlockEntity be) {
            this.be = be;
        }

        @Override public int getTanks() { return 3; }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> new net.minecraftforge.fluids.FluidStack(be.waterTank.getTankType(), be.waterTank.getFill());
                case 1 -> new net.minecraftforge.fluids.FluidStack(be.co2Tank.getTankType(), be.co2Tank.getFill());
                case 2 -> new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), be.steamTank.getFill());
                default -> net.minecraftforge.fluids.FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> be.waterTank.getMaxFill();
                case 1 -> be.co2Tank.getMaxFill();
                case 2 -> be.steamTank.getMaxFill();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.minecraftforge.fluids.FluidStack stack) {
            if (tank == 0) return VanillaFluidEquivalence.sameSubstance(stack.getFluid(), Fluids.WATER);
            if (tank == 1) return VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.CARBONDIOXIDE.getSource());
            return false;
        }

        @Override
        public int fill(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), Fluids.WATER)) {
                int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
                int toFill = Math.min(space, resource.getAmount());
                if (toFill <= 0) return 0;
                if (action.execute()) be.waterTank.fillMb(Fluids.WATER, toFill);
                return toFill;
            }
            if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.CARBONDIOXIDE.getSource())) {
                int space = be.co2Tank.getMaxFill() - be.co2Tank.getFill();
                int toFill = Math.min(space, resource.getAmount());
                if (toFill <= 0) return 0;
                if (action.execute()) be.co2Tank.fillMb(ModFluids.CARBONDIOXIDE.getSource(), toFill);
                return toFill;
            }
            return 0;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.steamTank.getFill() <= 0) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), be.steamTank.getTankType())) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(resource.getAmount(), be.steamTank.getFill());
            net.minecraftforge.fluids.FluidStack drained =
                    new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            if (action.execute()) be.steamTank.drainMb(toDrain);
            return drained;
        }

        @Override
        public @NotNull net.minecraftforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.steamTank.getFill() <= 0) {
                return net.minecraftforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(maxDrain, be.steamTank.getFill());
            net.minecraftforge.fluids.FluidStack drained =
                    new net.minecraftforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            if (action.execute()) be.steamTank.drainMb(toDrain);
            return drained;
        }
    }
    //?} elif neoforge {
    /*@Override
    protected void setupFluidCapability() {
        setFluidHandler(new UnifiedFluidHandler(this));
    }

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast(); // Original ISidedInventory
        if (cap == com.hbm_m.platform.HbmCap.FLUID_HANDLER && side != null) {
            if (fluidSidesFromMultiblockStructure && !allowedFluidSides.contains(side)) {
                return com.hbm_m.platform.LazyCap.empty();
            }
            if (!fluidSidesFromMultiblockStructure && !allowedFluidSides.isEmpty() && !allowedFluidSides.contains(side)) {
                return com.hbm_m.platform.LazyCap.empty();
            }
        }
        return super.getHbmCapability(cap, side);
    }

    private static class UnifiedFluidHandler implements net.neoforged.neoforge.fluids.capability.IFluidHandler {
        private final MachineZirnoxBlockEntity be;

        UnifiedFluidHandler(MachineZirnoxBlockEntity be) {
            this.be = be;
        }

        @Override public int getTanks() { return 3; }

        @Override
        public @NotNull net.neoforged.neoforge.fluids.FluidStack getFluidInTank(int tank) {
            return switch (tank) {
                case 0 -> new net.neoforged.neoforge.fluids.FluidStack(be.waterTank.getTankType(), be.waterTank.getFill());
                case 1 -> new net.neoforged.neoforge.fluids.FluidStack(be.co2Tank.getTankType(), be.co2Tank.getFill());
                case 2 -> new net.neoforged.neoforge.fluids.FluidStack(be.steamTank.getTankType(), be.steamTank.getFill());
                default -> net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            };
        }

        @Override
        public int getTankCapacity(int tank) {
            return switch (tank) {
                case 0 -> be.waterTank.getMaxFill();
                case 1 -> be.co2Tank.getMaxFill();
                case 2 -> be.steamTank.getMaxFill();
                default -> 0;
            };
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull net.neoforged.neoforge.fluids.FluidStack stack) {
            if (tank == 0) return VanillaFluidEquivalence.sameSubstance(stack.getFluid(), Fluids.WATER);
            if (tank == 1) return VanillaFluidEquivalence.sameSubstance(stack.getFluid(), ModFluids.CARBONDIOXIDE.getSource());
            return false;
        }

        @Override
        public int fill(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) return 0;
            if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), Fluids.WATER)) {
                int space = be.waterTank.getMaxFill() - be.waterTank.getFill();
                int toFill = Math.min(space, resource.getAmount());
                if (toFill <= 0) return 0;
                if (action.execute()) be.waterTank.fillMb(Fluids.WATER, toFill);
                return toFill;
            }
            if (VanillaFluidEquivalence.sameSubstance(resource.getFluid(), ModFluids.CARBONDIOXIDE.getSource())) {
                int space = be.co2Tank.getMaxFill() - be.co2Tank.getFill();
                int toFill = Math.min(space, resource.getAmount());
                if (toFill <= 0) return 0;
                if (action.execute()) be.co2Tank.fillMb(ModFluids.CARBONDIOXIDE.getSource(), toFill);
                return toFill;
            }
            return 0;
        }

        @Override
        public @NotNull net.neoforged.neoforge.fluids.FluidStack drain(net.neoforged.neoforge.fluids.FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || be.steamTank.getFill() <= 0) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
            if (!VanillaFluidEquivalence.sameSubstance(resource.getFluid(), be.steamTank.getTankType())) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(resource.getAmount(), be.steamTank.getFill());
            net.neoforged.neoforge.fluids.FluidStack drained =
                    new net.neoforged.neoforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            if (action.execute()) be.steamTank.drainMb(toDrain);
            return drained;
        }

        @Override
        public @NotNull net.neoforged.neoforge.fluids.FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0 || be.steamTank.getFill() <= 0) {
                return net.neoforged.neoforge.fluids.FluidStack.EMPTY;
            }
            int toDrain = Math.min(maxDrain, be.steamTank.getFill());
            net.neoforged.neoforge.fluids.FluidStack drained =
                    new net.neoforged.neoforge.fluids.FluidStack(be.steamTank.getTankType(), toDrain);
            if (action.execute()) be.steamTank.drainMb(toDrain);
            return drained;
        }
    }
    *///?}

    //? if forge {
    /** Original {@code ISidedInventory}: Plaetze 0-23; nur Zirnox-Staebe hinein, nur verbrauchte (keine Zirnox-Staebe mehr) heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 23); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 24 && !(stack.getItem() instanceof com.hbm_m.item.industrial.ZirnoxRodItem); }
            });

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Plaetze 0-23; nur Zirnox-Staebe hinein, nur verbrauchte (keine Zirnox-Staebe mehr) heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(0, 23); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot < 24 && !(stack.getItem() instanceof com.hbm_m.item.industrial.ZirnoxRodItem); }
            });

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}

    // ── Redstone-over-Radio (1:1 TileEntityReactorZirnox) ──

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
                PREFIX_VALUE + "heat",
                PREFIX_VALUE + "pressure",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_VALUE + "co2",
                PREFIX_VALUE + "state",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "active (0 or 1)",
                PREFIX_FUNCTION + "ventco2"
        };
    }

    @Override
    public String provideRORValue(String name) {
        if ((PREFIX_VALUE + "heat").equals(name))     return "" + (int) Math.round(heat * 1.0E-5D * 780.0D + 20.0D);
        if ((PREFIX_VALUE + "pressure").equals(name)) return "" + (int) Math.round(pressure * 1.0E-5D * 30.0D);
        if ((PREFIX_VALUE + "water").equals(name))    return "" + waterTank.getFill();
        if ((PREFIX_VALUE + "steam").equals(name))    return "" + steamTank.getFill();
        if ((PREFIX_VALUE + "co2").equals(name))      return "" + co2Tank.getFill();
        if ((PREFIX_VALUE + "state").equals(name))    return "" + (isOn ? 1 : 0);
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            if (redstonePowered) return null;
            try {
                int val = Integer.parseInt(params[0]);
                this.isOn = (val == 1);
                this.setChanged();
            } catch (NumberFormatException e) {}
            return null;
        }
        if ((PREFIX_FUNCTION + "ventco2").equals(name)) {
            // Original: carbonDioxide.setFill(Math.max(fill - 1000, 0))
            co2Tank.drainMb(Math.min(1_000, co2Tank.getFill()));
            this.setChanged();
            return null;
        }
        return null;
    }
}
