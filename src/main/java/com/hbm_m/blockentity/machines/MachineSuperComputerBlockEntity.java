package com.hbm_m.blockentity.machines;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSuperComputerMenu;
import com.hbm_m.module.machine.MachineModuleSuperComputer;
import com.hbm_m.recipe.SuperComputerRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineSuperComputer}: 8 Slots (Batterie, Blueprint-Ordner, drei Eingaenge, drei Ausgaenge),
 * je ein Ein- und Ausgangstank (4.000 mB, waechst mit dem Rezept), Rezept per Rezeptwaehler. Der Energiespeicher
 * waechst mit dem Rezept ({@code power * 100}, mindestens 100.000). Fuenf Anschluesse am vorderen Ausleger.
 */
public class MachineSuperComputerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_BATTERY = 0;
    public static final int SLOT_BLUEPRINT = 1;
    public static final int ITEM_INPUT_START = 2;
    public static final int ITEM_OUTPUT_START = 5;
    public static final int SLOT_COUNT = 8;

    public final FluidTank[] inputTanks = new FluidTank[1];
    public final FluidTank[] outputTanks = new FluidTank[1];

    public boolean didProcess = false;

    public final MachineModuleSuperComputer computerModule;

    public MachineSuperComputerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUPERCOMPUTER_BE.get(), pos, state, SLOT_COUNT, 100_000L, 100_000L);
        inputTanks[0] = new FluidTank(ModFluids.NONE.getSource(), 4_000);
        outputTanks[0] = new FluidTank(ModFluids.NONE.getSource(), 4_000);

        this.computerModule = new MachineModuleSuperComputer(this, inventory,
                new int[] { 2, 3, 4 }, new int[] { 5, 6, 7 },
                inputTanks, outputTanks, null);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSuperComputerBlockEntity be) {
        be.computerModule.setLevel(level);
        if (level instanceof ServerLevel serverLevel) be.serverTick(serverLevel);
    }

    private record DirPos(BlockPos pos, Direction dir) { }

    /**
     * Original {@code getPorts}: vorne am Ausleger ({@code dir * 8}) sowie seitlich bei {@code dir * 7} und
     * {@code dir * 5}; verbunden wird jeweils mit dem Nachbarblock ausserhalb der Anlage.
     */
    private DirPos[] getConPos() {
        Direction dir = getBlockState().hasProperty(DummyableMachineBlock.FACING)
                ? getBlockState().getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        BlockPos front = p.relative(dir, 8);
        BlockPos side7 = p.relative(dir, 7);
        BlockPos side5 = p.relative(dir, 5);
        return new DirPos[] {
                new DirPos(front.relative(dir), dir),
                new DirPos(side7.relative(rot, 2), rot),
                new DirPos(side7.relative(rot.getOpposite(), 2), rot.getOpposite()),
                new DirPos(side5.relative(rot, 2), rot),
                new DirPos(side5.relative(rot.getOpposite(), 2), rot.getOpposite()),
        };
    }

    private void serverTick(ServerLevel world) {

        SuperComputerRecipe recipe = computerModule.peekRecipe();
        long maxPower = 100_000L;
        if (recipe != null) maxPower = (long) recipe.getPowerConsumption() * 100L;
        maxPower = Math.max(Math.max(energy, maxPower), 100_000L);
        if (maxPower != getMaxEnergyStored()) setEnergyCapacity(maxPower);

        chargeFromBatterySlot(SLOT_BATTERY);

        for (DirPos con : getConPos()) {
            this.trySubscribe(world, con.pos.getX(), con.pos.getY(), con.pos.getZ(), con.dir);
            for (FluidTank tank : inputTanks) if (tank.getTankType() != ModFluids.NONE.getSource()) this.trySubscribe(tank.getTankType(), world, con.pos, con.dir);
            for (FluidTank tank : outputTanks) if (tank.getFill() > 0) this.tryProvide(tank, world, con.pos, con.dir);
        }

        boolean dirty = this.computerModule.updateAndGetDirty(1D, 1D, true, inventory.getStackInSlot(SLOT_BLUEPRINT));
        this.didProcess = this.computerModule.getDidProcess();
        if (dirty) setChanged();

        sendUpdateToClient();
    }

    // ==================== Rezeptwahl ====================

    @Nullable
    public ResourceLocation getSelectedRecipeId() {
        return computerModule.getSelectedRecipeId();
    }

    @Nullable
    public SuperComputerRecipe getSelectedRecipe() {
        return computerModule.peekRecipe();
    }

    public void setSelectedRecipe(@Nullable ResourceLocation recipeId) {
        computerModule.setSelectedRecipe(recipeId);
        if (level != null && !level.isClientSide) computerModule.syncTankConfigurationToRecipe(level);
        setChanged();
        if (level != null && !level.isClientSide) sendUpdateToClient();
    }

    public ItemStack getBlueprintFolder() {
        return inventory.getStackInSlot(SLOT_BLUEPRINT);
    }

    /** Original {@code GUIScreenRecipeSelector}: alle Rezepte, deren Pool frei oder im Ordner installiert ist. */
    public List<SuperComputerRecipe> getAvailableRecipes() {
        if (level == null) return List.of();
        String installedPool = com.hbm_m.item.industrial.ItemBlueprints.getBlueprintPool(getBlueprintFolder());
        return com.hbm_m.recipe.index.ModRecipeIndex.of(level.getRecipeManager()).getAll(SuperComputerRecipe.Type.INSTANCE).stream().filter(r -> {
            String pool = r.getBlueprintPool();
            if (pool == null || pool.isEmpty()) return true;
            return installedPool != null && !installedPool.isEmpty() && installedPool.equals(pool);
        }).toList();
    }

    public double getProgressFraction() {
        return computerModule.getProgressPercent();
    }

    // ==================== Slots ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return true;
        if (slot == SLOT_BLUEPRINT && stack.getItem() instanceof com.hbm_m.item.industrial.ItemBlueprints) return true;
        return this.computerModule != null && this.computerModule.isItemValidForSlot(slot, stack);
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getReceivingTanks() { return inputTanks; }
    @Override public FluidTank[] getSendingTanks() { return outputTanks; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { inputTanks[0], outputTanks[0] }; }

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
        inputTanks[0].writeToNBT(tag, "i");
        outputTanks[0].writeToNBT(tag, "o");
        tag.putLong("power", energy);
        tag.putLong("maxPower", getMaxEnergyStored());
        tag.putBoolean("didProcess", didProcess);
        computerModule.writeNBT(tag);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        inputTanks[0].readFromNBT(tag, "i");
        outputTanks[0].readFromNBT(tag, "o");
        if (tag.contains("maxPower")) setEnergyCapacity(Math.max(1, tag.getLong("maxPower")));
        if (tag.contains("power")) energy = tag.getLong("power");
        computerModule.readNBT(tag);
        didProcess = tag.getBoolean("didProcess");
        computerModule.didProcess = didProcess;
    }

    // ==================== Sonstiges ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.machine_supercomputer");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineSuperComputerMenu(containerId, playerInventory, this);
    }

    /** Original: 17x9x17 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 8, worldPosition.getY(), worldPosition.getZ() - 8,
                worldPosition.getX() + 9, worldPosition.getY() + 9, worldPosition.getZ() + 9);
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slots 2-7; Zutaten nach Rezept hinein, Ausgaben 5-7 und verstopfte Eingaenge heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return com.hbm_m.blockentity.SidedItemAccess.range(2, 7); }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return isItemValidForSlot(slot, stack); }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return slot >= 5 || computerModule.isSlotClogged(slot); }
            });

    @Override
    public @org.jetbrains.annotations.NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@org.jetbrains.annotations.NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sidedItems.invalidate();
    }
    //?}
}
