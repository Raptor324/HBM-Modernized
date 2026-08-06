package com.hbm_m.blockentity.machines;

import java.util.Map;

import com.hbm_m.api.fluids.IFluidConnectorMK2;
import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSolidifierMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.recipe.SolidificationRecipes;
import com.hbm_m.recipe.SolidificationRecipes.Recipe;

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
 * Solidifier - Port von {@code TileEntityMachineSolidifier} (1.7.10 Original). Reiner Fluid-
 * Empfaenger (kein Versand-Tank), Rezepte aus {@link SolidificationRecipes} (Fluid -> Item, 1:1).
 * Upgrade-Slots (Speed/Power) via {@link UpgradeManager}, analog zu {@code OilDrillBaseBlockEntity}.
 */
public class MachineSolidifierBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_OUTPUT    = 0;
    public static final int SLOT_BATTERY   = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;
    public static final int SLOT_FLUID_ID  = 4;
    public static final int INVENTORY_SIZE = 5;

    private static final long MAX_POWER      = 100_000L;
    private static final int  USAGE_BASE     = 250;
    private static final int  PROCESS_TIME_BASE = 100;
    private static final int  TANK_CAPACITY  = 24_000;

    private final FluidTank tank = new FluidTank(TANK_CAPACITY);
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private int usage = USAGE_BASE;
    private int progress;
    private int processTime = PROCESS_TIME_BASE;

    public MachineSolidifierBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLIDIFIER_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, MAX_POWER, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSolidifierBlockEntity be) {
        if (!level.isClientSide) {
            be.serverTick(level, pos);
        }
    }

    private void serverTick(Level level, BlockPos pos) {
        ensureNetworkInitialized();
        chargeFromBatterySlot(SLOT_BATTERY);

        ItemStack[] slots = inventorySlotArray();
        if (tank.setType(SLOT_FLUID_ID, slots)) applySlotsArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (!(neighborBe instanceof IFluidConnectorMK2)) continue;
                trySubscribe(tank.getTankType(), level, neighborPos, dir);
            }
        }

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, getValidUpgrades());
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int power = upgradeManager.getLevel(UpgradeType.POWER);

        processTime = PROCESS_TIME_BASE - (PROCESS_TIME_BASE / 4) * speed;
        usage = (USAGE_BASE + (USAGE_BASE * speed)) / (power + 1);

        if (canProcess()) {
            process();
        } else {
            progress = 0;
        }

        setChanged();
        sendUpdateToClient();
    }

    private boolean canProcess() {
        if (getEnergyStored() < usage) return false;

        Recipe recipe = SolidificationRecipes.get(tank.getTankType());
        if (recipe == null) return false;
        if (recipe.fillMb() > tank.getFluidAmountMb()) return false;

        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (!current.isEmpty()) {
            if (!ItemStack.isSameItemSameTags(current, recipe.output())) return false;
            if (current.getCount() + recipe.output().getCount() > current.getMaxStackSize()) return false;
        }

        return true;
    }

    private void process() {
        setEnergyStored(Math.max(0L, getEnergyStored() - usage));
        progress++;

        if (progress >= processTime) {
            Recipe recipe = SolidificationRecipes.get(tank.getTankType());
            if (recipe != null) {
                tank.drainMb(recipe.fillMb());

                ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
                if (current.isEmpty()) {
                    inventory.setStackInSlot(SLOT_OUTPUT, recipe.output().copy());
                } else {
                    current.grow(recipe.output().getCount());
                }
            }
            progress = 0;
        }
    }

    private Map<UpgradeType, Integer> getValidUpgrades() {
        return Map.of(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);
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

    @Override public FluidTank[] getAllTanks()      { return new FluidTank[]{ tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[]{ tank }; }
    @Override public FluidTank[] getSendingTanks()   { return FluidTank.EMPTY_ARRAY; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null && fluid != null && fluid != Fluids.EMPTY;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public FluidTank getTank()     { return tank; }
    public int getProgress()       { return progress; }
    public int getProcessTime()    { return processTime; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
        tag.putInt("usage", usage);
        tag.putInt("process_time", processTime);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
        usage = tag.getInt("usage");
        processTime = tag.getInt("process_time");
        if (processTime <= 0) processTime = PROCESS_TIME_BASE;
        tank.readFromNBT(tag, "tank");
    }

    // ── Slot validation ──────────────────────────────────────────────────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_OUTPUT -> false;
            case SLOT_BATTERY -> isEnergyProviderItem(stack) || isEnergyReceiverItem(stack)
                                  || stack.getItem() instanceof com.hbm_m.item.fekal_electric.ItemCreativeBattery;
            case SLOT_UPGRADE_1, SLOT_UPGRADE_2 -> stack.getItem() instanceof ItemMachineUpgrade;
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    // ── Menu ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.solidifier");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineSolidifierMenu.create(id, inventory, this);
    }
}
