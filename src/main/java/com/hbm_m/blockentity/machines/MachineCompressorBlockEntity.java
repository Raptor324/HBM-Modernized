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
 * Vereinfachung: kein manueller Ziel-Druck-Regler (das Original erlaubt per Steuer-Paket die Wahl
 * einer Ziel-Kompressionsstufe 0-3 im GUI - hier komprimiert die Maschine immer stur schrittweise
 * vom aktuellen Eingangsdruck aus, ohne Sprung-Auswahl).
 */
public class MachineCompressorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;
    private static final int SLOT_COUNT = 4;

    private static final long MAX_POWER = 100_000L;
    private static final int PROCESS_TIME_BASE = 100;
    private static final int POWER_REQUIREMENT_BASE = 2_500;

    private final FluidTank[] tanks = new FluidTank[2];

    public boolean isOn = false;
    public int progress = 0;
    public int processTime = PROCESS_TIME_BASE;
    public int powerRequirement = POWER_REQUIREMENT_BASE;

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    private static final java.util.Map<com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER, 3,
            com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3);

    public MachineCompressorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMPRESSOR_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
        tanks[0] = new FluidTank(16_000);
        tanks[1] = new FluidTank(16_000).withPressure(1);
    }

    public FluidTank[] getTanks() { return tanks; }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCompressorBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = be.inventorySlotArray();
        if (be.tanks[0].setType(SLOT_FLUID_ID, slots)) be.applySlotsArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.tanks[0], level, pos.relative(dir), dir);
                be.tryProvide(be.tanks[1], level, pos.relative(dir), dir);
            }
        }

        be.setupOutputTank();

        CompressorRecipe recipe = CompressorRecipe.getRecipe(level, be.tanks[0].getTankType(), be.tanks[0].getPressure());
        be.applyUpgrades(recipe);

        if (be.canProcess(recipe, be.powerRequirement)) {
            be.progress++;
            be.isOn = true;
            be.energy = Math.max(0, be.energy - be.powerRequirement);

            if (be.progress >= be.processTime) {
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

    /** GIT TileEntityMachineCompressorBase.updateEntity: upgrades set the cycle time and draw. */
    private void applyUpgrades(@Nullable CompressorRecipe recipe) {
        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speed = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.SPEED);
        int power = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.POWER);
        int over = upgradeManager.getLevel(com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType.OVERDRIVE);

        if (recipe == null) {
            processTime = speed == 3 ? 10 : speed == 2 ? 20 : speed == 1 ? 60 : PROCESS_TIME_BASE;
        } else {
            processTime = recipe.getDuration() / (speed + 1);
        }
        processTime = Math.max(1, processTime / (over + 1));
        powerRequirement = POWER_REQUIREMENT_BASE / (power + 1) * (over * 2 + 1);
    }

    private void setupOutputTank() {
        CompressorRecipe recipe = CompressorRecipe.getRecipe(getLevel(), tanks[0].getTankType(), tanks[0].getPressure());
        if (tanks[1].getFill() > 0) return;

        // Upstream setupTanks: without a recipe the compressor only raises the pressure of the same
        // substance, so the output tank has to conform to the input type. An untyped tank swallows
        // fillMb silently, which voided the drained input.
        if (recipe == null) {
            tanks[1].withPressure(tanks[0].getPressure() + 1);
            tanks[1].setTankType(tanks[0].getTankType());
        } else {
            tanks[1].withPressure(recipe.getOutputPressure());
            tanks[1].setTankType(recipe.getOutputFluid());
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

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", progress);
        tag.putInt("processTime", processTime);
        tag.putInt("powerRequirement", powerRequirement);
        tanks[0].writeToNBT(tag, "tank0");
        tanks[1].writeToNBT(tag, "tank1");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        processTime = Math.max(1, tag.getInt("processTime"));
        powerRequirement = tag.getInt("powerRequirement");
        tanks[0].readFromNBT(tag, "tank0");
        tanks[1].readFromNBT(tag, "tank1");
    }

    private ItemStack[] inventorySlotArray() {
        ItemStack[] arr = new ItemStack[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < SLOT_COUNT; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    public int getProgressScaled(int scale) {
        return progress * scale / Math.max(1, processTime);
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
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2 -> stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade;
            default -> false;
        };
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return MachineCompressorMenu.create(id, inv, this);
    }
}
