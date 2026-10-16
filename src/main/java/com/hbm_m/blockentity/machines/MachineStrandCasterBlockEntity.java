package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.inventory.material.MaterialType;
import com.hbm_m.inventory.menu.MachineStrandCasterMenu;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.platform.ModItemStackHandler;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.MoldCastingRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

/**
 * Strand Caster: Direktport der Kernlogik aus {@code TileEntityMachineStrandCaster} (1.7.10 Original).
 * <p>
 * Wiederverwendet dieses Ports Guss-Infrastruktur ({@code ICrucibleAcceptor}/{@code MaterialStack}/
 * {@code MoldCastingRecipes}), bereits etabliert durch {@link MachineFoundryBasinBlockEntity}. Anders
 * als das Basin giesst dieser Block kontinuierlich (kein "voll -> einmal giessen"-Cooloff), sondern
 * verbraucht Wasser fuer Dampf sobald genug Material fuer mindestens einen Guss vorliegt - 1:1 wie im
 * Original ({@code maxProcessable()}/{@code getWaterRequired()}).
 * <p>
 * Vereinfachungen: einzelner Block statt Multiblock, 1 Output-Slot statt 6 (gleiche Konvention wie
 * andere Maschinen diese Session), Wasser-Tank per MK2-Netz statt eigenem Anschluss-Positions-System,
 * Ueberlauf-Schrott-Auswurf (EntityItem bei amount &gt; capacity) entfaellt.
 */
public class MachineStrandCasterBlockEntity extends com.hbm_m.blockentity.BaseHbmBlockEntity implements MenuProvider, ICrucibleAcceptor, IFluidStandardReceiverMK2 {

    public static final int SLOT_MOLD = 0;
    public static final int SLOT_OUTPUT = 1;
    private static final int SLOT_COUNT = 2;

    private final FluidTank water = new FluidTank(64_000);

    @Nullable public MaterialType type = null;
    public int amount = 0;
    private int lastProgressTick = 0;
    private int ticksSinceProgress = 0;

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case SLOT_MOLD -> stack.getItem() instanceof ItemCastMold;
                case SLOT_OUTPUT -> false;
                default -> false;
            };
        }
    };

    public MachineStrandCasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRAND_CASTER_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() { return inventory; }
    public FluidTank getTank() { return water; }

    public void drops() {
        if (level == null) return;
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropContents(level, worldPosition, container);
    }

    private @Nullable ItemCastMold getInstalledMold() {
        ItemStack stack = inventory.getStackInSlot(SLOT_MOLD);
        return stack.getItem() instanceof ItemCastMold mold ? mold : null;
    }

    public int getCapacity() {
        ItemCastMold mold = getInstalledMold();
        return mold == null ? 0 : mold.getMoldType().getCostMb() * 10;
    }

    private int getWaterRequired(ItemCastMold mold) {
        return 5 * mold.getMoldType().getCostMb();
    }

    /** Data-driven поиск MoldCastingRecipe по паре (mold, material) — как в MachineFoundryBasinBlockEntity. */
    private @Nullable ItemStack getResultFor(MaterialType type, ItemCastMold.MoldType mold) {
        if (level == null) return null;
        for (MoldCastingRecipe r : RecipeHooks.getAllRecipes(level, MoldCastingRecipe.Type.INSTANCE)) {
            if (r.matches(mold, type)) {
                ItemStack out = r.getOutput();
                return out.isEmpty() ? null : out;
            }
        }
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineStrandCasterBlockEntity be) {
        if (level.isClientSide) return;

        if (level.getGameTime() % 10 == 0) {
            for (Direction dir : Direction.values()) {
                be.trySubscribe(be.water.getTankType(), level, pos.relative(dir), dir);
            }
        }

        int capacity = be.getCapacity();
        if (be.amount > capacity) be.amount = capacity;
        if (be.amount == 0) be.type = null;

        int moldsToCast = be.maxProcessable();
        be.ticksSinceProgress++;

        if (moldsToCast > 0 && (moldsToCast >= 9 || be.ticksSinceProgress >= 200)) {
            ItemCastMold mold = be.getInstalledMold();
            ItemStack out = be.getResultFor(be.type, mold.getMoldType());
            if (out != null && !out.isEmpty()) {
                be.amount -= moldsToCast * mold.getMoldType().getCostMb();
                if (be.amount < 0) be.amount = 0;

                ItemStack existing = be.inventory.getStackInSlot(SLOT_OUTPUT);
                ItemStack produced = out.copy();
                produced.setCount(out.getCount() * moldsToCast);
                if (existing.isEmpty()) {
                    be.inventory.setStackInSlot(SLOT_OUTPUT, produced);
                } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(existing, produced)) {
                    existing.grow(Math.min(produced.getCount(), existing.getMaxStackSize() - existing.getCount()));
                }

                int waterUsed = be.getWaterRequired(mold) * moldsToCast;
                be.water.drainMb(waterUsed);

                be.ticksSinceProgress = 0;
                be.setChanged();
            }
        }
    }

    private int maxProcessable() {
        ItemCastMold mold = getInstalledMold();
        if (type == null || mold == null) return 0;
        ItemStack out = getResultFor(type, mold.getMoldType());
        if (out == null || out.isEmpty()) return 0;

        int cost = mold.getMoldType().getCostMb();
        if (cost <= 0) return 0;

        ItemStack existing = inventory.getStackInSlot(SLOT_OUTPUT);
        int freeSpace;
        if (existing.isEmpty()) {
            freeSpace = out.getMaxStackSize();
        } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(existing, out)) {
            freeSpace = existing.getMaxStackSize() - existing.getCount();
        } else {
            freeSpace = 0;
        }

        int moldsToCast = amount / cost;
        moldsToCast = Math.min(moldsToCast, out.getCount() > 0 ? freeSpace / out.getCount() : 0);
        int waterReq = getWaterRequired(mold);
        if (waterReq > 0) {
            moldsToCast = Math.min(moldsToCast, water.getFill() / waterReq);
        }
        return Math.max(0, moldsToCast);
    }

    // ==================== ICrucibleAcceptor (molten metal poured in from above) ====================

    private boolean standardCheck(MaterialStack stack) {
        if (this.type != null && this.type != stack.type && this.amount > 0) return false;
        ItemCastMold mold = getInstalledMold();
        if (mold == null) return false;
        int limit = mold.getMoldType().getCostMb() * 9;
        return this.amount < limit;
    }

    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        return standardCheck(stack);
    }

    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.type;
        ItemCastMold mold = getInstalledMold();
        int limit = mold != null ? mold.getMoldType().getCostMb() * 9 : 0;

        if (stack.amount + this.amount <= limit) {
            this.amount += stack.amount;
            setChanged();
            return null;
        }

        int required = limit - this.amount;
        this.amount = limit;
        stack.amount -= required;
        setChanged();
        return stack;
    }

    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return stack; }

    // ==================== IFluidUserMK2 / MK2-Netz (Wasser-Eingang) ====================

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { water }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    public boolean canConnect(Fluid fluid, Direction fromDir) {
        return com.hbm_m.api.fluids.VanillaFluidEquivalence.sameSubstance(fluid, ModFluids.WATER.getSource());
    }

    // ==================== NBT ====================

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        tag.putInt("ticksSinceProgress", ticksSinceProgress);
        water.writeToNBT(tag, "water");
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        type = tag.contains("mat_type") ? MaterialType.byName(tag.getString("mat_type")) : null;
        amount = tag.getInt("mat_amount");
        ticksSinceProgress = tag.getInt("ticksSinceProgress");
        water.readFromNBT(tag, "water");
    }

    // ==================== GUI ====================

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("container.hbm_m.strand_caster");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MachineStrandCasterMenu(id, inv, this);
    }
}
