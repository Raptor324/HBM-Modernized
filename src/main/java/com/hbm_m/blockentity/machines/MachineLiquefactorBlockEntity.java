package com.hbm_m.blockentity.machines;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineLiquefactorMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.LiquefactorRecipe;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 {@code TileEntityMachineLiquefactor}: Eingang, Batterie, zwei Upgrades (Tempo/Strom je 3), 24.000-mB-Ausgangstank.
 * Prozesszeit {@code 100 - 25 * Tempo}, Verbrauch {@code (250 + 250 * Tempo) / (Strom + 1)}. Strom und Fluid an den
 * gleichen sechs Anschluessen wie beim Verfestiger (oben, unten, vier Seiten in Hoehe +1).
 */
public class MachineLiquefactorBlockEntity extends BaseMachineBlockEntity implements IFluidStandardSenderMK2,
        com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;
    public static final int SLOT_COUNT = 4;

    public static final long maxPower = 100000;
    public static final int usageBase = 250;
    public static final int processTimeBase = 100;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = Map.of(UpgradeType.SPEED, 3, UpgradeType.POWER, 3);

    private final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 24_000);
    private final UpgradeManager upgradeManager = new UpgradeManager();

    public int usage = usageBase;
    public int progress;
    public int processTime = processTimeBase;

    public MachineLiquefactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIQUEFACTOR_BE.get(), pos, state, SLOT_COUNT, maxPower, maxPower, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineLiquefactorBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
    }

    private BlockPos[] conPos(BlockPos pos) {
        return new BlockPos[] { pos.above(4), pos.below(), pos.offset(2, 1, 0), pos.offset(-2, 1, 0), pos.offset(0, 1, 2), pos.offset(0, 1, -2) };
    }

    private static final Direction[] CON_DIRS = { Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH };

    private void serverTick(ServerLevel world, BlockPos pos) {
        chargeFromBatterySlot(SLOT_BATTERY);

        BlockPos[] cons = conPos(pos);
        for (int i = 0; i < cons.length; i++) trySubscribe(world, cons[i].getX(), cons[i].getY(), cons[i].getZ(), CON_DIRS[i]);

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);
        int speed = upgradeManager.getLevel(UpgradeType.SPEED);
        int power = upgradeManager.getLevel(UpgradeType.POWER);

        this.processTime = processTimeBase - (processTimeBase / 4) * speed;
        this.usage = (usageBase + (usageBase * speed)) / (power + 1);

        if (canProcess()) process();
        else progress = 0;

        for (int i = 0; i < cons.length; i++) tryProvide(tank, world, cons[i], CON_DIRS[i]);

        setChanged();
        sendUpdateToClient();
    }

    private boolean canProcess() {
        if (getEnergyStored() < usage) return false;
        ItemStack input = inventory.getStackInSlot(SLOT_INPUT);
        if (input.isEmpty()) return false;

        LiquefactorRecipe recipe = findLiquefactorRecipe(input);
        if (recipe == null) return false;
        if (recipe.getOutput().getFluid() != tank.getTankType() && tank.getFill() > 0) return false;
        return recipe.getOutputAmountMb() + tank.getFill() <= tank.getMaxFill();
    }

    private void process() {
        setEnergyStored(getEnergyStored() - usage);
        progress++;

        if (progress >= processTime) {
            LiquefactorRecipe recipe = findLiquefactorRecipe(inventory.getStackInSlot(SLOT_INPUT));
            if (recipe != null) {
                tank.setTankType(recipe.getOutput().getFluid());
                tank.setFill(tank.getFill() + recipe.getOutputAmountMb());
                ItemStack in = inventory.getStackInSlot(SLOT_INPUT).copy();
                in.shrink(1);
                inventory.setStackInSlot(SLOT_INPUT, in);
            }
            progress = 0;
            setChanged();
        }
    }

    @Nullable
    private LiquefactorRecipe findLiquefactorRecipe(ItemStack input) {
        if (level == null) return null;
        for (LiquefactorRecipe recipe : RecipeHooks.getAllRecipes(level, LiquefactorRecipe.Type.INSTANCE)) {
            if (recipe.matchesInput(input)) return recipe;
        }
        return null;
    }

    // ==================== GUI ====================

    public FluidTank getTank() { return tank; }
    public int getProgress() { return progress; }
    public int getMaxProgress() { return processTime; }

    public int getProgressScaled(int scale) {
        return processTime <= 0 ? 0 : progress * scale / processTime;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(getBlockState().getBlock()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.translatable(KEY_DELAY, "-" + (level * 25) + "%").withStyle(ChatFormatting.GREEN));
            info.add(Component.translatable(KEY_CONSUMPTION, "+" + (level * 100) + "%").withStyle(ChatFormatting.RED));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
    }

    @Override
    public Map<UpgradeType, Integer> getValidUpgrades() {
        return VALID_UPGRADES;
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putInt("progress", progress);
        tag.putInt("usage", usage);
        tag.putInt("processTime", processTime);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
        progress = tag.getInt("progress");
        usage = tag.contains("usage") ? tag.getInt("usage") : usageBase;
        processTime = tag.contains("processTime") ? Math.max(1, tag.getInt("processTime")) : processTimeBase;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.liquefactor");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        // Original: nur Platz 0 mit gueltigem Rezept; Batterie/Upgrades ueber das Container-Slot-Verhalten
        if (slot == SLOT_INPUT) return level == null || findLiquefactorRecipe(stack) != null;
        if (slot == SLOT_BATTERY) return isEnergyProviderItem(stack);
        if (slot == SLOT_UPGRADE_1 || slot == SLOT_UPGRADE_2) return stack.getItem() instanceof ItemMachineUpgrade;
        return false;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineLiquefactorMenu.create(id, inventory, this);
    }

    /** Original: 3x4x3 um den Kern. */
    //? if forge {
    @Override
    //?}
    public net.minecraft.world.phys.AABB getRenderBoundingBox() {
        return new net.minecraft.world.phys.AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 4, worldPosition.getZ() + 2);
    }
}
