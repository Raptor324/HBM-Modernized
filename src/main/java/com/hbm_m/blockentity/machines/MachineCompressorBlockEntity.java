package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCompressorMenu;
import com.hbm_m.recipe.CompressorRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Compressor: Direktport der Kernlogik aus {@code TileEntityMachineCompressorBase}/
 * {@code TileEntityMachineCompressor} (1.7.10 Original) - komprimiert ein Eingangs-Fluid auf
 * einen um 1 hoeheren Druck ({@link FluidTank#getPressure()}), sofern kein spezielles Rezept
 * ({@link CompressorRecipe}) greift, das ein anderes Ausgangs-Fluid definiert.
 * <p>
 * Vereinfachungen: kein Item-Upgrade-System (SPEED/POWER/OVERDRIVE-Slots entfallen, feste
 * Basiswerte), kein manueller Ziel-Druck-Regler (das Original erlaubt per Steuer-Paket die Wahl
 * einer Ziel-Kompressionsstufe 0-3 im GUI - hier komprimiert die Maschine immer stur schrittweise
 * vom aktuellen Eingangsdruck aus, ohne Sprung-Auswahl).
 */
public class MachineCompressorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_BATTERY = 1;
    private static final int SLOT_COUNT = 2;

    private static final long MAX_POWER = 100_000L;
    private static final int PROCESS_TIME_BASE = 100;
    private static final int POWER_REQUIREMENT_BASE = 2_500;

    private final FluidTank[] tanks = new FluidTank[2];

    public boolean isOn = false;
    public int progress = 0;

    public MachineCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        tanks[0] = new FluidTank(16_000);
        tanks[1] = new FluidTank(16_000).withPressure(1);
    }

    public FluidTank[] getTanks() { return tanks; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCompressorBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0].getTankType(), level, pos.relative(dir), dir);
                be.tryProvide(be.tanks[1], level, pos.relative(dir), dir);
            }
        }

        be.setupOutputTank();

        CompressorRecipe recipe = CompressorRecipe.getRecipe(level, be.tanks[0].getTankType(), be.tanks[0].getPressure());
        int powerRequirement = POWER_REQUIREMENT_BASE;

        if (be.canProcess(recipe, powerRequirement)) {
            be.progress++;
            be.isOn = true;
            be.energy = Math.max(0, be.energy - powerRequirement);

            if (be.progress >= PROCESS_TIME_BASE) {
                be.progress = 0;
                be.process(recipe);
            }
        } else {
            be.progress = 0;
            be.isOn = false;
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private void setupOutputTank() {
        CompressorRecipe recipe = CompressorRecipe.getRecipe(getLevel(), tanks[0].getTankType(), tanks[0].getPressure());
        if (recipe == null) {
            if (tanks[1].getFill() <= 0) {
                tanks[1].withPressure(tanks[0].getPressure() + 1);
            }
        } else {
            if (tanks[1].getFill() <= 0) {
                tanks[1].withPressure(recipe.getOutputPressure());
            }
        }
    }

    private boolean canProcess(@Nullable CompressorRecipe recipe, int powerRequirement) {
        if (energy <= powerRequirement) return false;

        if (recipe == null) {
            return tanks[0].getFill() >= 1000 && tanks[1].getFill() + 1000 <= tanks[1].getMaxFill();
        }
        return tanks[0].getFill() >= recipe.getInputMb() && tanks[1].getFill() + recipe.getOutputMb() <= tanks[1].getMaxFill();
    }

    private void process(@Nullable CompressorRecipe recipe) {
        if (recipe == null) {
            tanks[0].drainMb(1000);
            tanks[1].fillMb(tanks[1].getTankType(), 1000);
        } else {
            tanks[0].drainMb(recipe.getInputMb());
            tanks[1].fillMb(recipe.getOutputFluid(), recipe.getOutputMb());
        }
    }

    // ==================== IFluidUserMK2 / MK2-Netz ====================

    @Override
    public FluidTank[] getAllTanks() { return tanks; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== NBT ====================

    //? if < 1.21.1 {
    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.putInt("progress", progress);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
    }
    //?} else {
    /*@Override
    public void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
    tag.putInt("progress", progress);
    tanks[0].writeToNBT(tag, "tank0");
    tanks[1].writeToNBT(tag, "tank1");
    }
    *///?}

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        progress = tag.getInt("progress");
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
    }

    public int getProgressScaled(int scale) {
        return progress * scale / PROCESS_TIME_BASE;
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.compressor");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_ID -> true;
            case SLOT_BATTERY -> isEnergyProviderItem(stack);
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineCompressorMenu.create(id, inv, this);
    }
}
