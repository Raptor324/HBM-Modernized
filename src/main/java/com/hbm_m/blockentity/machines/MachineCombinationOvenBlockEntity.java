package com.hbm_m.blockentity.machines;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.MachinePollutingBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.handler.pollution.PollutionHandler;
import com.hbm_m.interfaces.IHeatSource;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.fluid.trait.PollutionType;
import com.hbm_m.inventory.menu.MachineCombinationOvenMenu;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CombinationOvenRecipe;
import com.hbm_m.sound.HbmSoundsNT;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityFurnaceCombination}: Koksofen ohne Strom, beheizt von einer {@link IHeatSource} direkt unter dem
 * Kern (Diffusion 0,25, max. 100000 TU, Abkuehlung {@code max(heat/1000, 1)}). Pro Tick {@code heat / 100} Fortschritt
 * bis 20000; je Vorgang ein Eingangsitem zu Item und/oder Fluid ({@link CombinationOvenRecipe}), das Fluid landet im
 * 24000-mB-Tank, dessen Typ sich nach dem Rezept richtet. Alle 20 Ticks werden Fluid und Rauch an allen 3x2 Seiten-
 * und 3x3 Deckzellen angeboten. Waehrend er brennt: Wesen ueber dem Ofen fangen Feuer, Flammenwerfergeraeusch, Russ.
 */
public class MachineCombinationOvenBlockEntity extends MachinePollutingBlockEntity {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_CONTAINER_IN = 2;
    public static final int SLOT_CONTAINER_OUT = 3;
    public static final int INVENTORY_SIZE = 4;

    public static int processTime = 20_000;
    public static int maxHeat = 100_000;
    public static double diffusion = 0.25D;

    public boolean wasOn;
    public int progress;
    public int heat;

    private final FluidTank tank = new FluidTank(ModFluids.NONE.getSource(), 24_000);

    public MachineCombinationOvenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COMBINATION_OVEN_BE.get(), pos, state, INVENTORY_SIZE, 0L, 0L, 0L, 50);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCombinationOvenBlockEntity be) {
        if (!level.isClientSide()) be.serverTick(level, pos);
        else be.clientTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {

        this.tryPullHeat();

        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                Direction rot = dir.getClockWise();

                for (int y = pos.getY(); y <= pos.getY() + 1; y++) {
                    for (int j = -1; j <= 1; j++) {
                        BlockPos p = new BlockPos(pos.getX() + dir.getStepX() * 2 + rot.getStepX() * j, y, pos.getZ() + dir.getStepZ() * 2 + rot.getStepZ() * j);
                        if (tank.getFill() > 0) this.tryProvide(tank, level, p, dir);
                        this.sendSmoke(level, p, dir);
                    }
                }
            }

            for (int x = pos.getX() - 1; x <= pos.getX() + 1; x++) {
                for (int z = pos.getZ() - 1; z <= pos.getZ() + 1; z++) {
                    BlockPos p = new BlockPos(x, pos.getY() + 2, z);
                    if (tank.getFill() > 0) this.tryProvide(tank, level, p, Direction.UP);
                    this.sendSmoke(level, p, Direction.UP);
                }
            }
        }

        this.wasOn = false;

        ItemStack[] slots = slotArray();
        if (tank.unloadTank(SLOT_CONTAINER_IN, SLOT_CONTAINER_OUT, slots)) applySlotArray(slots);

        CombinationOvenRecipe recipe = findRecipe();

        if (recipe != null && canSmelt(recipe)) {
            int burn = heat / 100;

            if (burn > 0) {
                this.wasOn = true;
                this.progress += burn;
                this.heat -= burn;

                if (progress >= processTime) {
                    this.setChanged();
                    progress -= processTime;

                    ItemStack out = recipe.getOutput();
                    if (!out.isEmpty()) {
                        ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
                        if (current.isEmpty()) {
                            inventory.setStackInSlot(SLOT_OUTPUT, out.copy());
                        } else {
                            current.grow(out.getCount());
                            inventory.setStackInSlot(SLOT_OUTPUT, current);
                        }
                    }

                    if (recipe.getFluidOutput() != null) {
                        Fluid type = recipe.getOutputFluid();
                        if (tank.getTankType() != type) tank.setTankType(type);
                        tank.setFill(tank.getFill() + recipe.getOutputFluidAmount());
                    }

                    ItemStack in = inventory.getStackInSlot(SLOT_INPUT).copy();
                    in.shrink(1);
                    inventory.setStackInSlot(SLOT_INPUT, in.isEmpty() ? ItemStack.EMPTY : in);
                }

                List<Entity> entities = level.getEntitiesOfClass(Entity.class,
                        new AABB(pos.getX() - 0.5, pos.getY() + 2, pos.getZ() - 0.5, pos.getX() + 1.5, pos.getY() + 4, pos.getZ() + 1.5));
                for (Entity e : entities) e.setSecondsOnFire(5);

                if (level.getGameTime() % 10 == 0)
                    level.playSound(null, pos.getX(), pos.getY() + 1, pos.getZ(), HbmSoundsNT.get("hbm:weapon.flamethrowerShoot"), SoundSource.BLOCKS, 0.25F, 0.5F);
                if (level.getGameTime() % 20 == 0) this.pollute(PollutionType.SOOT, PollutionHandler.SOOT_PER_SECOND * 3);
            }
        } else {
            this.progress = 0;
        }

        setChanged();
        sendUpdateToClient();
    }

    private void clientTick(Level level, BlockPos pos) {
        if (this.wasOn && level.random.nextInt(15) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5 + level.random.nextGaussian() * 0.5, pos.getY() + 2,
                    pos.getZ() + 0.5 + level.random.nextGaussian() * 0.5, 0, 0, 0);
        }
    }

    /** Original {@code CombinationRecipes.getOutput(slots[0])}. */
    @Nullable
    private CombinationOvenRecipe findRecipe() {
        return findRecipe(level, inventory.getStackInSlot(SLOT_INPUT));
    }

    @Nullable
    public static CombinationOvenRecipe findRecipe(@Nullable Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        for (CombinationOvenRecipe recipe : RecipeHooks.getAllRecipes(level, CombinationOvenRecipe.Type.INSTANCE)) {
            if (recipe.matchesInput(stack)) return recipe;
        }
        return null;
    }

    public boolean canSmelt(CombinationOvenRecipe recipe) {
        ItemStack out = recipe.getOutput();

        if (!out.isEmpty()) {
            ItemStack current = inventory.getStackInSlot(SLOT_OUTPUT);
            if (!current.isEmpty()) {
                if (!PlatformHooks.isSameItemSameTags(out, current)) return false;
                if (out.getCount() + current.getCount() > current.getMaxStackSize()) return false;
            }
        }

        if (recipe.getFluidOutput() != null) {
            Fluid type = recipe.getOutputFluid();
            if (tank.getTankType() != type && tank.getFill() > 0) return false;
            if (tank.getTankType() == type && tank.getFill() + recipe.getOutputFluidAmount() > tank.getMaxFill()) return false;
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

    // ── Inventar-Hilfen ──────────────────────────────────────────────────────

    private ItemStack[] slotArray() {
        ItemStack[] arr = new ItemStack[INVENTORY_SIZE];
        for (int i = 0; i < INVENTORY_SIZE; i++) arr[i] = inventory.getStackInSlot(i);
        return arr;
    }

    private void applySlotArray(ItemStack[] arr) {
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            inventory.setStackInSlot(i, arr[i] == null ? ItemStack.EMPTY : arr[i]);
        }
        setChanged();
    }

    /** Original {@code isItemValidForSlot}: nur Slot 0 mit gueltigem Rezept (Slot 2 fuer die GUI zusaetzlich offen). */
    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_INPUT) return findRecipe(level, stack) != null;
        return slot == SLOT_CONTAINER_IN;
    }

    //? if forge {
    private final Map<Direction, net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler>> sided = new EnumMap<>(Direction.class);

    /** Original {@code getAccessibleSlotsFromSide {0, 1}}: nur Slot 0 einfuegbar, nur Slot 1 entnehmbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && side != null) {
            return sided.computeIfAbsent(side, d -> net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                @Override public int getSlots() { return 2; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot != SLOT_INPUT || !isItemValidForSlot(slot, stack)) return stack;
                    return inventory.insertItem(slot, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot != SLOT_OUTPUT) return ItemStack.EMPTY;
                    return inventory.extractItem(slot, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == SLOT_INPUT && isItemValidForSlot(slot, stack); }
            })).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.values().forEach(net.minecraftforge.common.util.LazyOptional::invalidate);
        sided.clear();
    }
    //?}

    // ── Fluid ────────────────────────────────────────────────────────────────

    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { tank }; }
    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { tank, smoke, smokeLeaded, smokePoison }; }

    // ── Zugriff ──────────────────────────────────────────────────────────────

    public FluidTank getTank() { return tank; }
    public int getHeat()        { return heat; }
    public int getProgress()    { return progress; }

    // ── NBT ─────────────────────────────────────────────────────────────────

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tank.writeToNBT(tag, "tank");
        tag.putInt("prog", progress);
        tag.putInt("heat", heat);
        tag.putBoolean("wasOn", wasOn);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        tank.readFromNBT(tag, "tank");
        progress = tag.getInt("prog");
        heat = tag.getInt("heat");
        wasOn = tag.getBoolean("wasOn");
    }

    // ── Menue ────────────────────────────────────────────────────────────────

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.hbm_m.combination_oven");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineCombinationOvenMenu(id, inventory, this);
    }

    private AABB bb = null;

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        if (bb == null) bb = new AABB(worldPosition.getX() - 1, worldPosition.getY(), worldPosition.getZ() - 1,
                worldPosition.getX() + 2, worldPosition.getY() + 2.125, worldPosition.getZ() + 2);
        return bb;
    }
}
