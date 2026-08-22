package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.machines.MachineElectricFurnaceBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineElectricFurnaceMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Electric Furnace: Direktport der Kernlogik aus {@code TileEntityMachineElectricFurnace}
 * (1.7.10 Original). Bereits im Original ein einzelner Block (kein Multiblock).
 * <p>
 * Vereinfachung: Der Upgrade-Slot (SPEED/POWER, Slot 3) des Originals entfaellt, da dieser
 * Port kein Item-Upgrade-System besitzt (kein ItemMachineUpgrade/UpgradeManagerNT gefunden) -
 * stattdessen feste {@code consumption = 50}/Tick und {@code maxProgress = 100} Ticks wie im
 * Original ohne Upgrades. Der Pollution-Aufruf (SOOT) des Originals entfaellt ebenfalls, da
 * dieser Port kein PollutionHandler-Aequivalent fuer diese Maschine mitbringt.
 * <p>
 * 100% Vanilla-Schmelzrezepte, Energie via {@link BaseMachineBlockEntity#chargeFromBatterySlot(int)}
 * aus Slot 0 (Batterie-Item), maxPower = 100'000.
 */
public class MachineElectricFurnaceBlockEntity extends BaseMachineBlockEntity {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_INPUT = 1;
    public static final int SLOT_OUTPUT = 2;
    /** ContainerElectricFurnace puts a SlotUpgrade at index 3 (111, 34); this port had no upgrade
     *  slot at all, so speed/power upgrades could never be fitted to an electric furnace. */
    public static final int SLOT_UPGRADE = 3;
    private static final int SLOT_COUNT = 4;

    private static final java.util.Map<UpgradeType, Integer> VALID_UPGRADES = java.util.Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3);

    private final com.hbm_m.inventory.UpgradeManager upgradeManager = new com.hbm_m.inventory.UpgradeManager();

    private static final long MAX_POWER = 100_000L;
    private static final long CONSUMPTION = 50L;
    private static final int MAX_PROGRESS = 100;

    private int progress = 0;

    private static final int DATA_PROGRESS = 0;
    private static final int DATA_MAX_PROGRESS = 1;
    private static final int DATA_HAS_POWER = 2;
    private static final int DATA_COUNT = 3;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> MAX_PROGRESS;
                case DATA_HAS_POWER -> energy >= CONSUMPTION ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == DATA_PROGRESS) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    private final SimpleContainer recipeInput = new SimpleContainer(1);

    public MachineElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE_BE.get(), pos, state, SLOT_COUNT, MAX_POWER, MAX_POWER);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineElectricFurnaceBlockEntity be) {
        if (level.isClientSide()) return;

        be.chargeFromBatterySlot(SLOT_BATTERY);
        be.upgradeManager.checkSlots(be.inventory, SLOT_UPGRADE, SLOT_UPGRADE, VALID_UPGRADES);

        // Same scaling the chemical factory uses: speed shortens the cycle and costs more power,
        // power upgrades take a quarter off the draw per level.
        int speedLevel = Math.min(be.upgradeManager.getLevel(UpgradeType.SPEED), 3);
        int powerLevel = Math.min(be.upgradeManager.getLevel(UpgradeType.POWER), 3);
        int step = 1 + speedLevel;
        long draw = Math.max(1L, (long) (CONSUMPTION * (1.0 + speedLevel) * (1.0 - 0.25 * powerLevel)));

        boolean wasLit = be.progress > 0;
        boolean hasPower = be.energy >= draw;

        if (hasPower && be.canSmelt(level)) {
            be.progress += step;
            be.setEnergyStored(be.energy - draw);

            if (be.progress >= MAX_PROGRESS) {
                be.progress = 0;
                be.craftItem(level);
            }
        } else {
            be.progress = 0;
        }

        boolean isLit = be.progress > 0;
        if (wasLit != isLit) {
            level.setBlock(pos, state.setValue(MachineElectricFurnaceBlock.LIT, isLit), 3);
        }

        be.setChanged();
        be.sendUpdateToClient();
    }

    private boolean canSmelt(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().getResultItem(level.registryAccess());
        if (result.isEmpty()) return false;

        return canAcceptResult(result);
    }

    private boolean canAcceptResult(ItemStack result) {
        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) return true;
        if (!ItemStack.isSameItemSameTags(current, result)) return false;
        return current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void craftItem(Level level) {
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        var recipe = getRecipe(level, input);
        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();

        input.shrink(1);

        ItemStack output = inventory.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            inventory.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
            inventory.setStackInSlot(SLOT_OUTPUT, output);
        }
    }

    private java.util.Optional<SmeltingRecipe> getRecipe(Level level, ItemStack input) {
        recipeInput.setItem(0, input);
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, recipeInput, level);
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("progress", progress);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        progress = tag.getInt("progress");
    }

    // ==================== GUI ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.electric_furnace");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_OUTPUT) return false;
        return slot == SLOT_INPUT;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineElectricFurnaceMenu(id, inv, this, data);
    }
}
