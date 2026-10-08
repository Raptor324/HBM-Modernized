package com.hbm_m.blockentity.machines;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.blockentity.BaseMachineBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineCokerMenu;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CokerRecipe;

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
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineCoker}: Verkokungsturm ohne Strom, beheizt von einer {@link IHeatSource} direkt unter
 * dem Kern (Diffusion 0,25, max. 100000 TU). Fortschritt {@code heat / 100} pro Tick bis 20000; das Rezept haengt am
 * Fluidtyp von Tank 0 ({@link CokerRecipe}), das Nebenprodukt fliesst in Tank 1. Acht Anschluesse um den Sockel,
 * Russ in die Umwelt, Rauchfahne oben am Turm (22 hoch).
 */
public class MachineCokerBlockEntity extends BaseMachineBlockEntity implements IFluidStandardTransceiverMK2 {

    public static final int SLOT_FLUID_ID = 0;
    public static final int SLOT_OUTPUT   = 1;
    public static final int INVENTORY_SIZE = 2;

    public static int processTime = 20_000;
    public static int maxHeat = 100_000;
    public static double diffusion = 0.25D;

    private final FluidTank tank0 = new FluidTank(ModFluids.HEAVYOIL.getSource(), 16_000);
    private final FluidTank tank1 = new FluidTank(ModFluids.OIL_COKER.getSource(), 8_000);

    public boolean wasOn;
    public int progress;
    public int heat;

    public MachineCokerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COKER_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCokerBlockEntity be) {
        if (!level.isClientSide) be.serverTick(level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {

        this.tryPullHeat();

        ItemStack[] slots = inventorySlotArray();
        if (tank0.setType(SLOT_FLUID_ID, slots)) applySlotsArray(slots);

        if (level.getGameTime() % 20 == 0) {
            for (DirPos con : getConPos()) {
                this.trySubscribe(tank0.getTankType(), level, con.pos, con.dir);
            }
        }

        this.wasOn = false;

        if (canProcess()) {
            int burn = heat / 100;

            if (burn > 0) {
                this.wasOn = true;
                this.progress += burn;
                this.heat -= burn;

                if (progress >= processTime) {
                    this.setChanged();
                    progress -= processTime;

                    CokerRecipe recipe = findCokerRecipe();
                    ItemStack output = recipe.getOutput();

                    if (output != null && !output.isEmpty()) {
                        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
                        if (current.isEmpty()) {
                            inventory.setStackInSlot(SLOT_OUTPUT, output.copy());
                        } else {
                            current.grow(output.getCount());
                            inventory.setStackInSlot(SLOT_OUTPUT, current);
                        }
                    }

                    if (recipe.getByproductFluid() != null) {
                        tank1.setFill(tank1.getFill() + recipe.getByproductMb());
                    }

                    tank0.setFill(tank0.getFill() - recipe.getInputMb());
                }
            }

            if (wasOn && level.getGameTime() % 5 == 0)
                PollutionHandler.incrementPollution(level, worldPosition, PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 5);
        }

        for (DirPos con : getConPos()) {
            if (this.tank1.getFill() > 0) this.tryProvide(tank1, level, con.pos, con.dir);
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level level, BlockPos pos) {

        if (this.wasOn) {

            if (level.getGameTime() % 2 == 0) {
                CompoundTag fx = new CompoundTag();
                fx.putString("type", "tower");
                fx.putFloat("lift", 10F);
                fx.putFloat("base", 0.75F);
                fx.putFloat("max", 3F);
                fx.putInt("life", 200 + level.random.nextInt(50));
                fx.putInt("color", 0x404040);
                fx.putDouble("posX", pos.getX() + 0.5);
                fx.putDouble("posY", pos.getY() + 22);
                fx.putDouble("posZ", pos.getZ() + 0.5);
                com.hbm_m.particle.helper.ParticleEffectClient.effectNT(fx);
            }
        }
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    private DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.offset(2, 0, 1), Direction.EAST),
                new DirPos(p.offset(2, 0, -1), Direction.EAST),
                new DirPos(p.offset(-2, 0, 1), Direction.WEST),
                new DirPos(p.offset(-2, 0, -1), Direction.WEST),
                new DirPos(p.offset(1, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(-1, 0, 2), Direction.SOUTH),
                new DirPos(p.offset(1, 0, -2), Direction.NORTH),
                new DirPos(p.offset(-1, 0, -2), Direction.NORTH)
        };
    }

    public boolean canProcess() {
        CokerRecipe recipe = findCokerRecipe();

        if (recipe == null) return false;

        int fillReq = recipe.getInputMb();
        ItemStack output = recipe.getOutput();

        if (recipe.getByproductFluid() != null) tank1.setTankType(recipe.getByproductFluid());

        if (tank0.getFill() < fillReq) return false;
        if (recipe.getByproductFluid() != null && recipe.getByproductMb() + tank1.getFill() > tank1.getMaxFill()) return false;

        if (output != null && !output.isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
            if (!current.isEmpty()) {
                if (!com.hbm_m.platform.PlatformHooks.isSameItemSameTags(current, output)) return false;
                if (output.getCount() + current.getCount() > output.getMaxStackSize()) return false;
            }
        }

        return true;
    }

    protected void tryPullHeat() {

        if (this.heat >= maxHeat) return;

        BlockEntity con = level.getBlockEntity(worldPosition.below());

        if (con instanceof IHeatSource source) {
            int diff = source.getHeatStored() - this.heat;

            if (diff == 0) {
                return;
            }

            if (diff > 0) {
                diff = (int) Math.ceil(diff * diffusion);
                source.useUpHeat(diff);
                this.heat += diff;
                if (this.heat > maxHeat)
                    this.heat = maxHeat;
                return;
            }
        }

        this.heat = Math.max(this.heat - Math.max(this.heat / 1000, 1), 0);
    }

    /** Original {@code CokerRecipes.getOutput(type)} - hier ueber die datengetriebenen {@link CokerRecipe}. */
    @org.jetbrains.annotations.Nullable
    private CokerRecipe findCokerRecipe() {
        if (level == null) return null;
        for (CokerRecipe recipe : RecipeHooks.getAllRecipes(level, CokerRecipe.Type.INSTANCE)) {
            if (recipe.matchesFluidType(tank0.getTankType())) return recipe;
        }
        return null;
    }

    // ── Inventar-Hilfen ──────────────────────────────────────────────────────

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

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank0, tank1 }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank1 }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { tank0 }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ── Zugriff ──────────────────────────────────────────────────────────────

    public FluidTank getTank0() { return tank0; }
    public FluidTank getTank1() { return tank1; }
    public int getHeat()        { return heat; }
    public int getMaxHeat()     { return maxHeat; }
    public int getProgress()    { return progress; }
    public int getMaxProgress() { return processTime; }
    public boolean isActive()   { return wasOn; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank0.writeToNBT(tag, "t0");
        tank1.writeToNBT(tag, "t1");
        tag.putInt("prog", progress);
        tag.putInt("heat", heat);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank0.readFromNBT(tag, "t0");
        tank1.readFromNBT(tag, "t1");
        progress = tag.getInt("prog");
        heat = tag.getInt("heat");
        wasOn = tag.getBoolean("wasOn");
    }

    // ── Slots (Original: nur Slot 1 zugaenglich, nichts einfuegbar) ───────────

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return false;
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineCoker");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCokerMenu.create(id, inventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 2, worldPosition.getY(), worldPosition.getZ() - 2,
                worldPosition.getX() + 3, worldPosition.getY() + 23, worldPosition.getZ() + 3);
        return bb;
    }

    //? if forge {
    /** Original {@code ISidedInventory}: Slot {1}; nichts hinein, das Ergebnis heraus. */
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
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
    //?} elif neoforge {
    /*/^* Original {@code ISidedInventory}: Slot {1}; nichts hinein, das Ergebnis heraus. ^/
    private final com.hbm_m.blockentity.SidedItemAccess sidedItems = new com.hbm_m.blockentity.SidedItemAccess(() -> inventory,
            new com.hbm_m.blockentity.SidedItemAccess.Rules() {
                @Override public int[] accessibleSlots(net.minecraft.core.Direction side) { return new int[] { 1 }; }
                @Override public boolean canInsert(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return false; }
                @Override public boolean canExtract(int slot, net.minecraft.world.item.ItemStack stack, net.minecraft.core.Direction side) { return true; }
            });

    @Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER && side != null) return sidedItems.get(side).cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sidedItems.invalidate();
    }
    *///?}
}
