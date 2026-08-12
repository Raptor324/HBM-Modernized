package com.hbm_m.blockentity.machines;

import com.hbm_m.platform.PlatformHooks;

import java.util.Map;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCyclotronMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CyclotronRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

public class MachineCyclotronBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final long MAX_POWER = 100_000_000L;
    public static final int BASE_CONSUMPTION = 1_000_000;
    public static final int DURATION = 690;

    private static final int WATER_PER_TICK = 500;
    private static final int TANK_WATER = 0;
    private static final int TANK_SPENT_STEAM = 1;
    private static final int TANK_AMAT = 2;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(
            UpgradeType.SPEED, 3,
            UpgradeType.POWER, 3,
            UpgradeType.EFFECT, 3
    );

    public static final int SLOT_INPUT_START = 0;
    public static final int SLOT_INPUT_END_EXCLUSIVE = 3;
    public static final int SLOT_TARGET_START = 3;
    public static final int SLOT_TARGET_END_EXCLUSIVE = 6;
    public static final int SLOT_OUTPUT_START = 6;
    public static final int SLOT_OUTPUT_END_EXCLUSIVE = 9;
    public static final int SLOT_BATTERY = 9;
    public static final int SLOT_UPGRADE_START = 10;
    public static final int SLOT_UPGRADE_END_EXCLUSIVE = 12;
    public static final int INVENTORY_SIZE = 12;

    private final FluidTank[] tanks;
    private final UpgradeManager upgradeManager = new UpgradeManager();

    private int progress;

    public MachineCyclotronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CYCLOTRON_BE.get(), pos, state, INVENTORY_SIZE, MAX_POWER, 5_000_000L);

        this.tanks = new FluidTank[3];
        this.tanks[TANK_WATER] = new FluidTank(ModFluids.WATER.getSource(), 32_000);
        this.tanks[TANK_SPENT_STEAM] = new FluidTank(ModFluids.SPENTSTEAM.getSource(), 32_000);
        this.tanks[TANK_AMAT] = new FluidTank(ModFluids.AMAT.getSource(), 8_000);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCyclotronBlockEntity blockEntity) {
        if (level.isClientSide) {
            return;
        }

        blockEntity.ensureNetworkInitialized();
        blockEntity.chargeFromBattery();
        blockEntity.upgradeManager.checkSlots(blockEntity.inventory, SLOT_UPGRADE_START, SLOT_UPGRADE_END_EXCLUSIVE - 1, VALID_UPGRADES);

        boolean changed = false;

        if (blockEntity.canProcess()) {
            blockEntity.progress += blockEntity.getSpeed();
            blockEntity.setEnergyStored(blockEntity.getEnergyStored() - blockEntity.getConsumption());

            int coolantUse = blockEntity.getCoolantConsumption();
            blockEntity.tanks[TANK_WATER].setFill(blockEntity.tanks[TANK_WATER].getFill() - coolantUse);
            blockEntity.tanks[TANK_SPENT_STEAM].setFill(blockEntity.tanks[TANK_SPENT_STEAM].getFill() + coolantUse);
            changed = true;

            if (blockEntity.progress >= DURATION) {
                blockEntity.process();
                blockEntity.progress = 0;
            }
        } else if (blockEntity.progress != 0) {
            blockEntity.progress = 0;
            changed = true;
        }

        if (changed) {
            blockEntity.setChanged();
        }

        if (changed || level.getGameTime() % 20L == 0L) {
            blockEntity.sendUpdateToClient();
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.cyclotron");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot >= SLOT_INPUT_START && slot < SLOT_INPUT_END_EXCLUSIVE) {
            // Рецепты data-driven — валидация по всем CyclotronRecipe через RecipeManager (RecipeHooks).
            if (stack == null || stack.isEmpty() || level == null) return false;
            for (CyclotronRecipe r : RecipeHooks.getAllRecipes(level, CyclotronRecipe.Type.INSTANCE)) {
                if (r.getInput().test(stack)) return true;
            }
            return false;
        }
        if (slot >= SLOT_TARGET_START && slot < SLOT_TARGET_END_EXCLUSIVE) {
            if (stack == null || stack.isEmpty() || level == null) return false;
            for (CyclotronRecipe r : RecipeHooks.getAllRecipes(level, CyclotronRecipe.Type.INSTANCE)) {
                if (r.getTarget().test(stack)) return true;
            }
            return false;
        }
        if (slot == SLOT_BATTERY) {
            return isCyclotronBatteryCandidate(stack);
        }
        if (slot >= SLOT_UPGRADE_START && slot < SLOT_UPGRADE_END_EXCLUSIVE) {
            return stack.getItem() instanceof ItemMachineUpgrade;
        }
        return false;
    }

    public static boolean isCyclotronBatteryCandidate(ItemStack stack) {
        return isEnergyProviderItem(stack);
    }

    public int getProgress() {
        return this.progress;
    }

    public int getProgressScaled(int scale) {
        return (int) ((long) this.progress * (long) scale / (long) DURATION);
    }

    public int getSpeed() {
        return upgradeManager.getLevel(UpgradeType.SPEED) + 1;
    }

    public int getConsumption() {
        int efficiency = upgradeManager.getLevel(UpgradeType.POWER);
        return BASE_CONSUMPTION - (100_000 * efficiency);
    }

    public int getCoolantConsumption() {
        int effect = upgradeManager.getLevel(UpgradeType.EFFECT);
        return (WATER_PER_TICK / (effect + 1)) * getSpeed();
    }

    private boolean canProcess() {
        if (getEnergyStored() < getConsumption()) {
            return false;
        }

        for (int lane = 0; lane < 3; lane++) {
            ItemStack input = inventory.getStackInSlot(SLOT_INPUT_START + lane);
            ItemStack target = inventory.getStackInSlot(SLOT_TARGET_START + lane);
            // Рецепты data-driven — поиск по двум стекам через RecipeManager (RecipeHooks).
            CyclotronRecipe recipe = findCyclotronRecipe(target, input);
            if (recipe == null) {
                continue;
            }

            ItemStack out = recipe.getOutput();
            if (out.isEmpty()) {
                continue;
            }

            ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT_START + lane);
            if (existing.isEmpty()) {
                return true;
            }

            if (!PlatformHooks.isSameItemSameTags(existing, out)) {
                continue;
            }

            int max = Math.min(existing.getMaxStackSize(), inventory.getSlotLimit(SLOT_OUTPUT_START + lane));
            if (existing.getCount() + out.getCount() <= max) {
                return true;
            }
        }

        return false;
    }

    private void process() {
        for (int lane = 0; lane < 3; lane++) {
            int inputSlot = SLOT_INPUT_START + lane;
            int targetSlot = SLOT_TARGET_START + lane;
            int outputSlot = SLOT_OUTPUT_START + lane;

            ItemStack input = inventory.getStackInSlot(inputSlot);
            ItemStack target = inventory.getStackInSlot(targetSlot);
            // Рецепты data-driven — поиск по двум стекам через RecipeManager (RecipeHooks).
            CyclotronRecipe recipe = findCyclotronRecipe(target, input);
            if (recipe == null) {
                continue;
            }

            ItemStack out = recipe.getOutput();
            if (out.isEmpty()) {
                continue;
            }

            ItemStack existing = inventory.getStackInSlot(outputSlot);
            if (existing.isEmpty()) {
                inventory.setStackInSlot(outputSlot, out);
            } else {
                if (!PlatformHooks.isSameItemSameTags(existing, out)) {
                    continue;
                }
                int max = Math.min(existing.getMaxStackSize(), inventory.getSlotLimit(outputSlot));
                if (existing.getCount() + out.getCount() > max) {
                    continue;
                }
                existing.grow(out.getCount());
                inventory.setStackInSlot(outputSlot, existing);
            }

            inventory.extractItem(inputSlot, 1, false);
            inventory.extractItem(targetSlot, 1, false);
            tanks[TANK_AMAT].setFill(tanks[TANK_AMAT].getFill() + recipe.getAmatProduced());
        }

        if (tanks[TANK_AMAT].getFill() > tanks[TANK_AMAT].getMaxFill()) {
            tanks[TANK_AMAT].setFill(tanks[TANK_AMAT].getMaxFill());
        }
    }

    /**
     * Линейный поиск {@link CyclotronRecipe} по паре target+input. Замена удалённому
     * {@code CyclotronRecipes.getOutput(target, input)} — рецепты теперь data-driven (JSON),
     * источник правды {@code RecipeManager}, доступ кросс-версионный через {@link RecipeHooks}.
     */
    private CyclotronRecipe findCyclotronRecipe(ItemStack target, ItemStack input) {
        if (target == null || input == null || target.isEmpty() || input.isEmpty() || level == null) {
            return null;
        }
        for (CyclotronRecipe r : RecipeHooks.getAllRecipes(level, CyclotronRecipe.Type.INSTANCE)) {
            if (r.matches(target, input)) {
                return r;
            }
        }
        return null;
    }

    private void chargeFromBattery() {
        ItemStack battery = inventory.getStackInSlot(SLOT_BATTERY);
        if (battery.isEmpty()) {
            return;
        }
        chargeFromBatterySlot(SLOT_BATTERY);
    }

    @Override
    public FluidTank[] getReceivingTanks() {
        return new FluidTank[] { tanks[TANK_WATER] };
    }

    @Override
    public FluidTank[] getSendingTanks() {
        return new FluidTank[] { tanks[TANK_SPENT_STEAM], tanks[TANK_AMAT] };
    }

    @Override
    public FluidTank[] getAllTanks() {
        return tanks;
    }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, net.minecraft.core.Direction fromDir) {
        return fromDir != null;
    }

    @Override
    protected void writeNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putInt("progress", this.progress);
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].writeToNBT(tag, "t" + i);
        }
    }

    @Override
    protected void readNbtData(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        this.progress = tag.getInt("progress");
        for (int i = 0; i < tanks.length; i++) {
            tanks[i].readFromNBT(tag, "t" + i);
        }
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCyclotronMenu.create(id, inventory, this);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof com.hbm_m.block.machines.MachineCyclotronBlock block)) {
            BlockPos p1 = worldPosition.offset(-2, 0, -2);
            BlockPos p2 = worldPosition.offset(3, 4, 3);
            return new net.minecraft.world.phys.AABB(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ());
        }
        net.minecraft.core.Direction facing = state.getValue(com.hbm_m.block.machines.MachineCyclotronBlock.FACING);
        return block.getStructureHelper().getRenderBoundingBox(worldPosition, facing, 0.0);
    }
}
