package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineSilexMenu;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.recipe.SilexRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
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
 * 1:1 {@code TileEntitySILEX}: Peroxidtank (16.000 mB, Typ per Fluidkennung, Kanister ueber Platz 2/3), Ladeleiste bis
 * 16.000 mB fuer genau ein Material ({@code current}). Items werden alle 21 Ticks mit ihrer Ladung ({@code fluid_produced})
 * an Peroxid eingeladen, passende Fluide (UF6, PUF6, Todesfluessigkeit ...) direkt mit 50 mB/Tick. Gearbeitet wird
 * nur, solange ein FEL die noetige Wellenlaenge liefert ({@code mode}, jeden Tick zurueckgesetzt); mehr Wellenlaenge =
 * doppelt so schnell. Die gewichtete Ausgabe rotiert ueber eine Primzahl, statt zu wuerfeln. Ausgabe ueber Platz 4 in die
 * Warteschlange 5-10.
 */
public class MachineSilexBlockEntity extends BaseMachineBlockEntity implements IFluidStandardReceiverMK2,
        com.hbm_m.api.tile.IControlReceiver {

    public static final int maxFill = 16000;
    public static final int processTime = 100;
    public static final int PRIME = 137;

    public EnumWavelengths mode = EnumWavelengths.NULL;
    public final FluidTank tank = new FluidTank(ModFluids.PEROXIDE.getSource(), 16000);

    /** Original {@code current}: eingeladenes Item (Menge 1) ... */
    public ItemStack currentItem = ItemStack.EMPTY;
    /** ... oder eingeladenes Fluid. */
    @Nullable public Fluid currentFluid = null;
    public int currentFill;
    public int progress;
    public int recipeIndex = 0;
    int loadDelay;

    public MachineSilexBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SILEX_BE.get(), pos, state, 11, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineSilexBlockEntity be) {
        if (level instanceof ServerLevel world) be.serverTick(world, pos);
    }

    private ItemStack[] slotsArray() {
        ItemStack[] arr = new ItemStack[inventory.getSlots()];
        for (int i = 0; i < arr.length; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlots(ItemStack[] arr) {
        for (int i = 0; i < arr.length; i++) inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
    }

    public boolean hasCurrent() {
        return !currentItem.isEmpty() || currentFluid != null;
    }

    private void serverTick(ServerLevel world, BlockPos pos) {

        ItemStack[] slots = slotsArray();
        boolean changed = tank.setType(1, 1, slots);
        changed |= tank.loadTank(2, 3, slots);
        if (changed) applySlots(slots);

        Direction rot = getBlockState().getValue(DummyableMachineBlock.FACING).getClockWise();
        this.trySubscribe(tank.getTankType(), world, pos.relative(rot, 2).above(), rot);
        this.trySubscribe(tank.getTankType(), world, pos.relative(rot, -2).above(), rot.getOpposite());

        loadFluid(world);

        if (!process(world)) {
            this.progress = 0;
        }

        dequeue();

        if (currentFill <= 0) {
            currentItem = ItemStack.EMPTY;
            currentFluid = null;
        }

        setChanged();
        sendUpdateToClient();

        this.mode = EnumWavelengths.NULL;
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("void")) voidContents();
    }

    /** Original {@code handleButtonPacket}: Inhalt der Ladeleiste verwerfen. */
    public void voidContents() {
        this.currentFill = 0;
        this.currentItem = ItemStack.EMPTY;
        this.currentFluid = null;
        setChanged();
    }

    public int getProgressScaled(int i) {
        return (progress * i) / processTime;
    }

    public int getFluidScaled(int i) {
        return (tank.getFill() * i) / tank.getMaxFill();
    }

    public int getFillScaled(int i) {
        return (currentFill * i) / maxFill;
    }

    private static boolean sameSingular(ItemStack a, ItemStack b) {
        return !a.isEmpty() && !b.isEmpty() && a.getItem() == b.getItem() && ItemStack.isSameItemSameTags(a.copyWithCount(1), b.copyWithCount(1));
    }

    public void loadFluid(Level world) {

        Fluid type = tank.getTankType();
        SilexRecipe fluidRecipe = SilexRecipe.forFluid(world, type);

        if (fluidRecipe != null) {

            if (currentFill == 0) {
                currentItem = ItemStack.EMPTY;
                currentFluid = type;
            }

            if (currentFluid == type) {
                int toFill = Math.min(50, Math.min(maxFill - currentFill, tank.getFill()));
                currentFill += toFill;
                tank.setFill(tank.getFill() - toFill);
            }
        }

        loadDelay++;

        if (loadDelay > 20)
            loadDelay = 0;

        ItemStack in = inventory.getStackInSlot(0);
        if (loadDelay == 0 && !in.isEmpty() && tank.getTankType() == ModFluids.PEROXIDE.getSource()
                && (!hasCurrent() || (currentFluid == null && sameSingular(currentItem, in)))) {
            SilexRecipe recipe = SilexRecipe.forItem(world, in);

            if (recipe == null)
                return;

            int load = recipe.getFluidProduced();

            if (load <= maxFill - this.currentFill && load <= tank.getFill()) {
                this.currentFill += load;
                this.currentItem = in.copyWithCount(1);
                this.currentFluid = null;
                tank.setFill(tank.getFill() - load);
                ItemStack rest = in.copy();
                rest.shrink(1);
                inventory.setStackInSlot(0, rest);
            }
        }
    }

    @Nullable
    private SilexRecipe currentRecipe(Level world) {
        if (currentFluid != null) return SilexRecipe.forFluid(world, currentFluid);
        if (!currentItem.isEmpty()) return SilexRecipe.forItem(world, currentItem);
        return null;
    }

    private boolean process(Level world) {

        if (!hasCurrent() || currentFill <= 0)
            return false;

        SilexRecipe recipe = currentRecipe(world);

        if (recipe == null)
            return false;

        if (recipe.getLaser() > this.mode.ordinal())
            return false;

        if (currentFill < recipe.getFluidConsumed())
            return false;

        if (!inventory.getStackInSlot(4).isEmpty())
            return false;

        int progressSpeed = (int) Math.pow(2, this.mode.ordinal() - recipe.getLaser() + 1) / 2;

        progress += progressSpeed;

        if (progress >= processTime) {

            currentFill -= recipe.getFluidConsumed();

            int totalWeight = recipe.getTotalWeight();
            this.recipeIndex %= Math.max(totalWeight, 1);

            int weight = 0;

            for (SilexRecipe.WeightedOutput weighted : recipe.getOutputs()) {
                weight += weighted.weight();

                if (this.recipeIndex < weight) {
                    inventory.setStackInSlot(4, weighted.stack().copy());
                    break;
                }
            }

            progress = 0;
            this.setChanged();

            this.recipeIndex += PRIME;
        }

        return true;
    }

    private void dequeue() {

        ItemStack out = inventory.getStackInSlot(4);
        if (!out.isEmpty()) {

            for (int i = 5; i < 11; i++) {
                ItemStack q = inventory.getStackInSlot(i);
                if (!q.isEmpty() && q.getCount() < q.getMaxStackSize() && ItemStack.isSameItemSameTags(out, q)) {
                    ItemStack grown = q.copy();
                    grown.grow(1);
                    inventory.setStackInSlot(i, grown);
                    ItemStack rest = out.copy();
                    rest.shrink(1);
                    inventory.setStackInSlot(4, rest);
                    return;
                }
            }

            for (int i = 5; i < 11; i++) {
                if (inventory.getStackInSlot(i).isEmpty()) {
                    inventory.setStackInSlot(i, out.copy());
                    inventory.setStackInSlot(4, ItemStack.EMPTY);
                    return;
                }
            }
        }
    }

    /** Fuer GUI/Tooltip: Name des geladenen Materials. */
    public Component getCurrentName() {
        if (currentFluid != null) return com.hbm_m.inventory.fluid.FluidType.forFluid(currentFluid).getLocalizedName();
        return currentItem.isEmpty() ? Component.empty() : currentItem.getHoverName();
    }

    // ==================== Slots ====================

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == 0) return level == null || SilexRecipe.forItem(level, stack) != null;
        if (slot == 1 || slot == 2) return true;
        return false;
    }

    // ==================== Fluid ====================

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return fromDir != null;
    }

    public FluidTank getTank() { return tank; }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putInt("fill", currentFill);
        tag.putInt("recipeIndex", recipeIndex);
        tag.putString("mode", mode.toString());
        tag.putInt("progress", progress);
        if (currentFluid != null) {
            tag.putString("currentFluid", BuiltInRegistries.FLUID.getKey(currentFluid).toString());
        } else if (!currentItem.isEmpty()) {
            tag.put("currentItem", com.hbm_m.platform.PlatformHooks.safeItemSave(currentItem, registries));
        }
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
        currentFill = tag.getInt("fill");
        recipeIndex = tag.getInt("recipeIndex");
        try { mode = EnumWavelengths.valueOf(tag.getString("mode")); } catch (IllegalArgumentException e) { mode = EnumWavelengths.NULL; }
        progress = tag.getInt("progress");
        currentFluid = null;
        currentItem = ItemStack.EMPTY;
        if (currentFill > 0) {
            if (tag.contains("currentFluid")) {
                ResourceLocation id = ResourceLocation.tryParse(tag.getString("currentFluid"));
                if (id != null) currentFluid = BuiltInRegistries.FLUID.get(id);
            } else if (tag.contains("currentItem")) {
                currentItem = com.hbm_m.platform.PlatformHooks.itemStackOf(tag.getCompound("currentItem"), registries);
            }
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.silex");
    }

    @Override
    public @NotNull Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineSilexMenu.create(id, inventory, this);
    }

    /** Original: 3x3x3 um den Kern. */
    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 3, worldPosition.getZ() + 2);
    }
}
