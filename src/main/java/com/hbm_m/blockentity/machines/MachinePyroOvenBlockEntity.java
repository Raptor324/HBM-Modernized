package com.hbm_m.blockentity.machines;

import java.util.Map;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachinePyroOvenMenu;
import com.hbm_m.item.fekal_electric.ItemCreativeBattery;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.recipe.PyroOvenRecipes;
import com.hbm_m.recipe.PyroOvenRecipes.Recipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
 * Pyro Oven - Port von {@code TileEntityMachinePyroOven} (1.7.10 Original). Rezepte kommen aus
 * {@link PyroOvenRecipes} (erstes passendes Rezept gewinnt, 1:1 wie im Original). Upgrade-Slots
 * (Speed/Power/Overdrive) via {@link UpgradeManager}, analog zu {@code OilDrillBaseBlockEntity}.
 * <p>
 * SCOPE-Entscheidung: Pollution (SOOT beim Laufen, {@code TileEntityMachinePolluting}) entfaellt
 * ersatzlos - fehlende Infrastruktur, wie bei allen anderen Maschinen dieser Session dokumentiert.
 */
public class MachinePyroOvenBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY  = 0;
    public static final int SLOT_ITEM_IN  = 1;
    public static final int SLOT_ITEM_OUT = 2;
    public static final int SLOT_FLUID_ID = 3;
    public static final int SLOT_UPGRADE_1 = 4;
    public static final int SLOT_UPGRADE_2 = 5;
    public static final int INVENTORY_SIZE = 6;

    private static final long MAX_POWER   = 10_000_000L;
    private static final int  TANK_CAPACITY = 24_000;
    private static final int  BASE_CONSUMPTION = 10_000;

    private final FluidTank tank0 = new FluidTank(TANK_CAPACITY);
    private final FluidTank tank1 = new FluidTank(TANK_CAPACITY);

    private final UpgradeManager upgradeManager = new UpgradeManager();
    private int speedLevel;
    private int powerSavingLevel;
    private int overdriveLevel;

    private float progress;
    private boolean isProgressing;

    public MachinePyroOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PYROOVEN_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachinePyroOvenBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();

        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (!battery.isEmpty() && battery.getItem() instanceof ItemCreativeBattery) {
            setEnergyStored(getMaxEnergyStored());
        } else {
            chargeFromBatterySlot(SLOT_BATTERY);
        }

        ItemStack[] slots = inventorySlotArray();
        if (tank0.setType(SLOT_FLUID_ID, slots)) applySlotsArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
                trySubscribe(tank0.getTankType(), level, neighborPos, dir);
                if (tank1.getFill() > 0) {
                    tryProvide(tank1, level, neighborPos, dir);
                }
            }
        }

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, getValidUpgrades());
        speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        powerSavingLevel = upgradeManager.getLevel(UpgradeType.POWER);
        overdriveLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        isProgressing = false;

        Recipe recipe = canProcess();
        if (recipe != null) {
            int overdriveSpeed = speedLevel + overdriveLevel * 2;
            progress += 1F / Math.max((recipe.duration() - speedLevel * (recipe.duration() / 4)) / (overdriveLevel * 2 + 1), 1);
            isProgressing = true;
            setEnergyStored(Math.max(0L, getEnergyStored() - getConsumption(overdriveSpeed, powerSavingLevel)));

            if (progress >= 1F) {
                progress = 0F;
                finishRecipe(recipe);
                setChanged();
            }
        } else {
            progress = 0F;
        }

        setChanged();
        sendUpdateToClient();
    }

    private static int getConsumption(int speed, int powerSaving) {
        return (int) (BASE_CONSUMPTION * Math.pow(speed + 1, 2)) / (powerSaving + 1);
    }

    private Recipe lastValidRecipe;

    private Recipe getMatchingRecipe() {
        if (lastValidRecipe != null && doesRecipeMatch(lastValidRecipe)) return lastValidRecipe;
        for (Recipe rec : PyroOvenRecipes.getAll()) {
            if (doesRecipeMatch(rec)) {
                lastValidRecipe = rec;
                return rec;
            }
        }
        return null;
    }

    private boolean doesRecipeMatch(Recipe recipe) {
        if (recipe.inputFluid() != null && !com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank0.getTankType(), recipe.inputFluid())) return false;

        ItemStack itemIn = inventory.getStackInSlot(SLOT_ITEM_IN);
        if (recipe.inputItem() != null) {
            if (itemIn.isEmpty()) return false;
            if (itemIn.getItem() != recipe.inputItem()) return false;
        } else if (!itemIn.isEmpty()) {
            return false;
        }
        return true;
    }

    private Recipe canProcess() {
        int consumption = getConsumption(speedLevel, powerSavingLevel);
        if (getEnergyStored() < consumption) return null;

        Recipe recipe = getMatchingRecipe();
        if (recipe == null) return null;

        if (recipe.inputFluid() != null && tank0.getFluidAmountMb() < recipe.inputFluidMb()) return null;
        ItemStack itemIn = inventory.getStackInSlot(SLOT_ITEM_IN);
        if (recipe.inputItem() != null && itemIn.getCount() < recipe.inputItemCount()) return null;

        if (recipe.outputFluid() != null) {
            boolean sameType = com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(tank1.getTankType(), recipe.outputFluid())
                    || tank1.getFill() <= 0;
            if (sameType && recipe.outputFluidMb() + tank1.getFluidAmountMb() > tank1.getCapacityMb()) return null;
        }

        if (recipe.outputItem() != null && !recipe.outputItem().isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_ITEM_OUT);
            if (!current.isEmpty()) {
                if (!ItemStack.isSameItemSameTags(current, recipe.outputItem())) return null;
                if (current.getCount() + recipe.outputItem().getCount() > recipe.outputItem().getMaxStackSize()) return null;
            }
        }

        return recipe;
    }

    private void finishRecipe(Recipe recipe) {
        if (recipe.outputItem() != null && !recipe.outputItem().isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_ITEM_OUT);
            if (current.isEmpty()) {
                inventory.setStackInSlot(SLOT_ITEM_OUT, recipe.outputItem().copy());
            } else {
                current.grow(recipe.outputItem().getCount());
            }
        }
        if (recipe.outputFluid() != null) {
            tank1.fillMb(recipe.outputFluid(), recipe.outputFluidMb());
        }
        if (recipe.inputItem() != null) {
            ItemStack itemIn = inventory.getStackInSlot(SLOT_ITEM_IN);
            itemIn.shrink(recipe.inputItemCount());
            if (itemIn.isEmpty()) inventory.setStackInSlot(SLOT_ITEM_IN, ItemStack.EMPTY);
        }
        if (recipe.inputFluid() != null) {
            tank0.drainMb(recipe.inputFluidMb());
        }
    }

    private Map<UpgradeType, Integer> getValidUpgrades() {
        return Map.of(UpgradeType.SPEED, 3, UpgradeType.POWER, 3, UpgradeType.OVERDRIVE, 3);
    }

    // ── Inventory helpers ────────────────────────────────────────────────────

    private ItemStack[] inventorySlotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotsArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    // ── IFluidStandardTransceiverMK2 ─────────────────────────────────────────

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ tank0, tank1 }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ tank0 }; }
    @Override public FluidTank[] getSendingTanks() {
        return tank1.getFill() > 0 ? new FluidTank[]{ tank1 } : FluidTank.EMPTY_ARRAY;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fluid != null && fluid != Fluids.EMPTY;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank getTank0() { return tank0; }
    public FluidTank getTank1() { return tank1; }
    public float getProgress()  { return progress; }
    public boolean isProgressing() { return isProgressing; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putFloat("progress", progress);
        tank0.writeToNBT(tag, "tank0");
        tank1.writeToNBT(tag, "tank1");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getFloat("progress");
        tank0.readFromNBT(tag, "tank0");
        tank1.readFromNBT(tag, "tank1");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_BATTERY -> stack.getItem() instanceof ItemCreativeBattery
                                  || isEnergyProviderItem(stack)
                                  || isEnergyReceiverItem(stack);
            case SLOT_ITEM_IN -> true;
            case SLOT_ITEM_OUT -> false;
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2 -> stack.getItem() instanceof ItemMachineUpgrade;
            default -> false;
        };
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.pyrooven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachinePyroOvenMenu.create(id, inventory, this);
    }
}
