package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.api.fluids.VanillaFluidEquivalence;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.FluidType;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.FT_Heatable;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingStep;
import com.hbm_m.inventory.fluid.trait.FT_Heatable.HeatingType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * Hephaestus - Port von {@code TileEntityMachineHephaestus} (1.7.10 Original). Passiver
 * geothermischer Waermetauscher (kein Inventar, kein GUI, keine Elektrizitaet) - scannt zyklisch
 * (10 Ticks Rotationsfenster, ein Layer pro Tick, 15x15-Flaeche) den Untergrund auf Lava/Vulkan-
 * Bloecke und nutzt die Summe als Waerme fuer {@code FT_Heatable} (analog zu {@code
 * MachineBoilerBlockEntity}, aber mit direktem Block-Scan statt {@code IHeatSource}-Pull, 1:1 aus
 * dem Original).
 * <p>
 * SCOPE-Entscheidung: {@code volcanic_lava_block} (Waerme-Gewicht 150) und {@code ore_volcano}
 * (Gewicht 300 + 3-facher "Fissure"-Bonus fuer 20 Ticks) existieren in diesem Port nicht - nur
 * gewoehnliche Lava (Gewicht 5, 1:1 aus dem Original) wird als Waermequelle erkannt.
 */
public class MachineHephaestusBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    private static final int TANK_CAPACITY = 24_000;
    private static final int SCAN_RANGE = 7;
    private static final int SCAN_WINDOW = 10;
    private static final int LAVA_HEAT = 5;

    private final FluidTank inputTank = new FluidTank(ModFluids.CRUDE_OIL.getSource(), TANK_CAPACITY);
    private final FluidTank outputTank = new FluidTank(ModFluids.HOTOIL.getSource(), TANK_CAPACITY);

    private final int[] heatLayers = new int[SCAN_WINDOW];

    public MachineHephaestusBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEPHAESTUS_BE.get(), pos, state, 0, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineHephaestusBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        setupTanks();

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
                if (!FluidTank.isFluidTypeExplicitlySet(inputTank.getTankType())) continue;
                trySubscribe(inputTank.getTankType(), level, neighborPos, dir);
            }
        }

        int layer = (int) (level.getGameTime() % SCAN_WINDOW);
        int y = pos.getY() - 1 - layer;
        int heat = 0;

        if (y >= level.getMinBuildHeight()) {
            for (int x = -SCAN_RANGE; x <= SCAN_RANGE; x++) {
                for (int z = -SCAN_RANGE; z <= SCAN_RANGE; z++) {
                    heat += heatFromBlock(level, pos.getX() + x, y, pos.getZ() + z);
                }
            }
        }
        heatLayers[layer] = heat;

        heatFluid();

        if (outputTank.getFill() > 0) {
            for (Direction dir : Direction.values()) {
                if (dir == Direction.UP || dir == Direction.DOWN) continue;
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
                tryProvide(outputTank, level, neighborPos, dir);
            }
        }

        setChanged();
        sendUpdateToClient();
    }

    private int heatFromBlock(Level level, int x, int y, int z) {
        return level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.LAVA) ? LAVA_HEAT : 0;
    }

    private int getTotalHeat() {
        int total = 0;
        for (int h : heatLayers) total += h;
        return total;
    }

    private void heatFluid() {
        FT_Heatable trait = FluidType.getTrait(inputTank.getTankType(), FT_Heatable.class);
        if (trait == null) return;

        HeatingStep step = trait.getFirstStep();
        if (step == null || step.amountReq <= 0) return;

        int heat = getTotalHeat();
        int inputOps = inputTank.getFluidAmountMb() / step.amountReq;
        int outputOps = (outputTank.getCapacityMb() - outputTank.getFluidAmountMb()) / step.amountProduced;
        int heatOps = step.heatReq > 0 ? heat / step.heatReq : 0;
        int ops = Math.min(Math.min(inputOps, outputOps), heatOps);
        if (ops <= 0) return;

        inputTank.drainMb(ops * step.amountReq);
        outputTank.fillMb(step.typeProduced, ops * step.amountProduced);
    }

    private void setupTanks() {
        FT_Heatable trait = FluidType.getTrait(inputTank.getTankType(), FT_Heatable.class);
        if (trait != null && trait.getEfficiency(HeatingType.HEATEXCHANGER) > 0) {
            HeatingStep step = trait.getFirstStep();
            if (step != null) {
                outputTank.setTankType(step.typeProduced);
                return;
            }
        }
        outputTank.setTankType(ModFluids.NONE.getSource());
    }

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ inputTank, outputTank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ inputTank }; }
    @Override public FluidTank[] getSendingTanks() {
        return outputTank.getFill() > 0 ? new FluidTank[]{ outputTank } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        if (fromDir == null || fromDir == Direction.UP || fromDir == Direction.DOWN) return false;
        if (fluid == null || fluid == Fluids.EMPTY) return false;
        return VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.CRUDE_OIL.getSource());
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank getInputTank()  { return inputTank; }
    public FluidTank getOutputTank() { return outputTank; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        inputTank.writeToNBT(tag, "input");
        outputTank.writeToNBT(tag, "output");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        inputTank.readFromNBT(tag, "input");
        outputTank.readFromNBT(tag, "output");
    }

    // ── Slot validation / Menu ─────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false; // Kein Inventar - siehe Klassenkommentar.
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.hephaestus");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null; // Kein GUI im Original.
    }
}
