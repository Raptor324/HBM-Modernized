package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.inventory.UpgradeManager;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineCompressorMenu;
import com.hbm_m.item.industrial.ItemMachineUpgrade.UpgradeType;
import com.hbm_m.recipe.CompressorRecipe;

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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityMachineCompressorBase}: verdichtet das Eingangsfluid um eine Druckstufe (oder nach
 * {@link CompressorRecipe} in ein anderes Fluid). Die Eingangsdruckstufe (0-4) waehlt man in der GUI; Upgrades
 * Geschwindigkeit (ohne Rezept 60/20/10 Ticks, mit Rezept Dauer/(n+1)), Energiesparen und Overdrive.
 */
public abstract class MachineCompressorBaseBlockEntity extends BaseMachineBlockEntity
        implements IFluidStandardTransceiverMK2, com.hbm_m.api.tile.IControlReceiver, com.hbm_m.interfaces.IUpgradeInfoProvider {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_BATTERY = 1;
    public static final int SLOT_UPGRADE_1 = 2;
    public static final int SLOT_UPGRADE_2 = 3;

    public static final long maxPower = 100_000;
    public static final int processTimeBase = 100;
    public static final int powerRequirementBase = 2_500;

    private static final Map<UpgradeType, Integer> VALID_UPGRADES = new EnumMap<>(UpgradeType.class);
    static {
        VALID_UPGRADES.put(UpgradeType.SPEED, 3);
        VALID_UPGRADES.put(UpgradeType.POWER, 3);
        VALID_UPGRADES.put(UpgradeType.OVERDRIVE, 9);
    }

    public final FluidTank[] tanks = new FluidTank[] {
            new FluidTank(ModFluids.NONE.getSource(), 16_000),
            new FluidTank(ModFluids.NONE.getSource(), 16_000).withPressure(1)
    };
    public boolean isOn;
    public int progress;
    public int processTime = 100;
    public int powerRequirement;

    public final UpgradeManager upgradeManager = new UpgradeManager();

    protected MachineCompressorBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, maxPower, maxPower, 0L);
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    public record DirPos(BlockPos pos, Direction dir) { }

    public abstract DirPos[] getConPos();

    /** Original {@code ForgeDirection.getOrientation(meta - offset)} - im Port die FACING-Richtung. */
    protected Direction dir() {
        BlockState state = getBlockState();
        return state.hasProperty(com.hbm_m.block.machines.DummyableMachineBlock.FACING)
                ? state.getValue(com.hbm_m.block.machines.DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCompressorBaseBlockEntity be) {
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
        else be.clientTick(level);
    }

    protected void clientTick(Level level) { }

    @Nullable
    private CompressorRecipe recipe() {
        return CompressorRecipe.getRecipe(level, tanks[0].getTankType(), tanks[0].getPressure());
    }

    private void serverTick(ServerLevel world) {

        if (world.getGameTime() % 20 == 0) {
            this.updateConnections(world);
        }

        // Original: Library.chargeTEFromItems(slots, 1, power, maxPower)
        chargeFromBatterySlot(SLOT_BATTERY);
        ItemStack[] slots = slotsArray();
        if (this.tanks[0].setType(SLOT_FLUID_ID, slots)) applySlots(slots);
        this.setupTanks();

        upgradeManager.checkSlots(inventory, SLOT_UPGRADE_1, SLOT_UPGRADE_2, VALID_UPGRADES);

        int speedLevel = upgradeManager.getLevel(UpgradeType.SPEED);
        int powerLevel = upgradeManager.getLevel(UpgradeType.POWER);
        int overLevel = upgradeManager.getLevel(UpgradeType.OVERDRIVE);

        CompressorRecipe rec = recipe();
        int timeBase = processTimeBase;
        if (rec != null) timeBase = rec.getDuration();

        if (rec == null) this.processTime = speedLevel == 3 ? 10 : speedLevel == 2 ? 20 : speedLevel == 1 ? 60 : timeBase;
        else this.processTime = timeBase / (speedLevel + 1);
        this.powerRequirement = powerRequirementBase / (powerLevel + 1);
        this.processTime = this.processTime / (overLevel + 1);
        this.powerRequirement = this.powerRequirement * ((overLevel * 2) + 1);

        if (processTime <= 0) processTime = 1;

        if (canProcess()) {
            this.progress++;
            this.isOn = true;
            this.energy -= powerRequirement;

            if (progress >= this.processTime) {
                progress = 0;
                this.process();
                this.setChanged();
            }

        } else {
            this.progress = 0;
            this.isOn = false;
        }

        for (DirPos con : getConPos()) {
            this.tryProvide(tanks[1], world, con.pos(), con.dir());
        }

        setChanged();
        sendUpdateToClient();
    }

    protected void updateConnections(ServerLevel world) {
        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos().getX(), con.pos().getY(), con.pos().getZ(), con.dir());
            this.trySubscribe(tanks[0].getTankType(), world, con.pos(), con.dir());
        }
    }

    public boolean canProcess() {

        if (this.energy <= powerRequirement) return false;

        CompressorRecipe recipe = recipe();

        if (recipe == null) {
            return tanks[0].getFill() >= 1000 && tanks[1].getFill() + 1000 <= tanks[1].getMaxFill();
        }

        return tanks[0].getFill() >= recipe.getInputMb() && tanks[1].getFill() + recipe.getOutputMb() <= tanks[1].getMaxFill();
    }

    public void process() {

        CompressorRecipe recipe = recipe();

        if (recipe == null) {
            tanks[0].setFill(tanks[0].getFill() - 1_000);
            tanks[1].setFill(tanks[1].getFill() + 1_000);
        } else {
            tanks[0].setFill(tanks[0].getFill() - recipe.getInputMb());
            tanks[1].setFill(tanks[1].getFill() + recipe.getOutputMb());
        }
    }

    protected void setupTanks() {

        CompressorRecipe recipe = recipe();

        if (recipe == null) {
            tanks[1].withPressure(tanks[0].getPressure() + 1).setTankType(tanks[0].getTankType());
        } else {
            tanks[1].withPressure(recipe.getOutputPressure()).setTankType(recipe.getOutputFluid());
        }
    }

    // ==================== Steuerung ====================

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        int compression = data.getInt("compression");

        if (compression != tanks[0].getPressure()) {
            tanks[0].withPressure(compression);

            CompressorRecipe recipe = CompressorRecipe.getRecipe(level, tanks[0].getTankType(), compression);

            if (recipe == null) {
                tanks[1].withPressure(compression + 1);
            } else {
                tanks[1].withPressure(recipe.getOutputPressure()).setTankType(recipe.getOutputFluid());
            }

            this.setChanged();
        }
    }

    // ==================== Inventar ====================

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[4];
        for (int i = 0; i < 4; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < 4; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return tanks; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tanks[1] }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tanks[0] }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    public FluidTank[] getTanks() { return tanks; }

    // ==================== Upgrades ====================

    @Override
    public boolean canProvideInfo(UpgradeType type, int level, boolean extendedInfo) {
        return type == UpgradeType.SPEED || type == UpgradeType.POWER || type == UpgradeType.OVERDRIVE;
    }

    @Override
    public void provideInfo(UpgradeType type, int level, List<Component> info, boolean extendedInfo) {
        info.add(com.hbm_m.interfaces.IUpgradeInfoProvider.getStandardLabel(com.hbm_m.block.ModBlocks.COMPRESSOR.get()));
        if (type == UpgradeType.SPEED) {
            info.add(Component.literal("Generic compression: ").append(Component.translatable(KEY_DELAY, "-" + (level == 3 ? 90 : level == 2 ? 80 : level == 1 ? 40 : 0) + "%")).withStyle(ChatFormatting.GREEN));
            info.add(Component.literal("Recipe: ").append(Component.translatable(KEY_DELAY, "-" + (100 - 100 / (level + 1)) + "%")).withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.POWER) {
            info.add(Component.translatable(KEY_CONSUMPTION, "-" + (100 - 100 / (level + 1)) + "%").withStyle(ChatFormatting.GREEN));
        }
        if (type == UpgradeType.OVERDRIVE) {
            info.add(Component.literal("YES").withStyle((System.currentTimeMillis() / 500) % 2 == 0 ? ChatFormatting.RED : ChatFormatting.DARK_GRAY));
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
        tag.putInt("progress", progress);
        tag.putInt("processTime", processTime);
        tag.putInt("powerRequirement", powerRequirement);
        tag.putBoolean("isOn", isOn);
        tanks[0].writeToNBT(tag, "0");
        tanks[1].writeToNBT(tag, "1");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        progress = tag.getInt("progress");
        processTime = tag.contains("processTime") ? tag.getInt("processTime") : processTimeBase;
        powerRequirement = tag.getInt("powerRequirement");
        isOn = tag.getBoolean("isOn");
        tanks[0].readFromNBT(tag, "0");
        tanks[1].readFromNBT(tag, "1");
    }

    // ==================== Menue ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCompressor");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCompressorMenu.create(id, inventory, this);
    }
}
