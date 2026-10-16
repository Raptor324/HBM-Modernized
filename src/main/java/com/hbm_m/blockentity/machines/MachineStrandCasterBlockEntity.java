package com.hbm_m.blockentity.machines;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.block.ICrucibleAcceptor;
import com.hbm_m.api.fluids.IFluidStandardReceiverMK2;
import com.hbm_m.api.fluids.IFluidStandardSenderMK2;
import com.hbm_m.blockentity.BaseHbmBlockEntity;
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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
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
 * Порт {@code TileEntityMachineStrandCaster} (1.7.10) — машина непрерывного
 * литья: буфер расплава ({@code type}/{@code amount}) с изложницей в слоте 0
 * и шестью слотами выхода; партия литься ({@code maxProcessable}) расходует
 * воду и выдаёт отработанный пар. Налив расплава — только сверху, на 2×2
 * верх башни (порт {@code getMetalPourPos}).
 */
public class MachineStrandCasterBlockEntity extends BaseHbmBlockEntity implements MenuProvider, ICrucibleAcceptor, IFluidStandardReceiverMK2, IFluidStandardSenderMK2 {

    public static final int SLOT_MOLD = 0;
    public static final int SLOT_OUTPUT_START = 1;
    public static final int SLOT_COUNT = 7;
    private static final int OUTPUT_SLOTS = 6;

    public static final int TANK_CAPACITY = 64_000;

    @Nullable public MaterialType type = null;
    public int amount = 0;
    private long lastProgressTick = 0;

    private final FluidTank water = new FluidTank(ModFluids.WATER.getSource(), TANK_CAPACITY);
    private final FluidTank steam = new FluidTank(ModFluids.SPENTSTEAM.getSource(), TANK_CAPACITY);

    private final ModItemStackHandler inventory = new ModItemStackHandler(SLOT_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 64; // как в оригинале getInventoryStackLimit()
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return isItemValidForSlot(slot, stack);
        }
    };

    /**
     * Обёртка инвентаря для внешней автоматизации:
     * - извлечение разрешено только из слотов выхода 1..6 (слот 0 с изложницей защищён);
     * - загрузка предметов через автоматизацию запрещена (как в 1.7.10).
     */
    private final ModItemStackHandler automationHandler = new ModItemStackHandler(0) {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == SLOT_MOLD) return ItemStack.EMPTY;
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false;
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot != SLOT_MOLD) {
                inventory.setStackInSlot(slot, stack);
            }
        }
    };

    public MachineStrandCasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRAND_CASTER_BE.get(), pos, state);
    }

    public ModItemStackHandler getInventory() { return inventory; }
    public FluidTank getWaterTank() { return water; }
    public FluidTank getSteamTank() { return steam; }

    public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
    public void setStackInSlot(int slot, ItemStack stack) { inventory.setStackInSlot(slot, stack); }

    public void drops() {
        if (level == null) return;
        net.minecraft.world.SimpleContainer container = new net.minecraft.world.SimpleContainer(inventory.getSlots());
        for (int i = 0; i < inventory.getSlots(); i++) {
            container.setItem(i, inventory.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropContents(level, worldPosition, container);
    }

    // ═══════════════════════════════ Логика ═══════════════════════════════

    private @Nullable ItemCastMold getInstalledMold() {
        ItemStack stack = inventory.getStackInSlot(SLOT_MOLD);
        return stack.getItem() instanceof ItemCastMold mold ? mold : null;
    }

    /** Стоимость изложницы (mB за отливку) для рендера; 0 если изложницы нет. */
    public int getMoldCostMb() {
        ItemCastMold mold = getInstalledMold();
        return mold == null ? 0 : mold.getMoldType().getCostMb();
    }

    /** Порт getCapacity: без изложницы буфер вмещает 50000 mB, с изложницей — 10 стоимостей. */
    public int getCapacity() {
        ItemCastMold mold = getInstalledMold();
        return mold == null ? 50_000 : mold.getMoldType().getCostMb() * 10;
    }

    private int getWaterRequired() {
        ItemCastMold mold = getInstalledMold();
        if (mold == null) return 50;
        int quanta = (int) Math.round((double) mold.getMoldType().getCostMb() * com.hbm_m.item.material.ScrapItem.QUANTA_PER_INGOT / MaterialStack.MB_PER_INGOT);
        return Math.max(1, quanta * 5);
    }

    /** Data-driven поиск результата (mold, material) — замена {@code mold.getOutput(type)}. */
    private @Nullable ItemStack getResultFor(MaterialType mat, ItemCastMold.MoldType mold) {
        if (level == null) return null;
        for (MoldCastingRecipe r : RecipeHooks.getAllRecipes(level, MoldCastingRecipe.Type.INSTANCE)) {
            if (r.matches(mold, mat)) {
                ItemStack out = r.getOutput();
                return out.isEmpty() ? null : out;
            }
        }
        return null;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineStrandCasterBlockEntity be) {
        if (level.isClientSide) return;

        boolean sync = false;
        int oldAmount = be.amount;
        MaterialType oldType = be.type;

        // Переполнение — излишек высыпается шлаком (порт overfill check)
        if (be.amount > be.getCapacity()) {
            ItemStack scrap = com.hbm_m.util.CrucibleUtil.createScrap(
                    new MaterialStack(be.type != null ? be.type : MaterialType.IRON, Math.max(be.amount - be.getCapacity(), 0)));
            if (!scrap.isEmpty()) {
                ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5, scrap);
                level.addFreshEntity(item);
            }
            be.amount = be.getCapacity();
        }

        if (be.amount == 0) {
            be.type = null;
        }

        be.updateConnections(level, pos);

        int moldsToCast = be.maxProcessable();

        // Партия сливается после 10 секунд простоя или когда слоты почти полны
        if (moldsToCast > 0 && (moldsToCast >= 9 || level.getGameTime() >= be.lastProgressTick + 200)) {
            ItemCastMold mold = be.getInstalledMold();

            be.amount -= moldsToCast * mold.getMoldType().getCostMb();

            ItemStack out = be.getResultFor(be.type, mold.getMoldType());
            if (out != null) {
                int remaining = out.getCount() * moldsToCast;
                int maxStackSize = out.getMaxStackSize();

                for (int i = SLOT_OUTPUT_START; i < SLOT_COUNT; i++) {
                    if (remaining <= 0) break;

                    ItemStack slot = be.inventory.getStackInSlot(i);
                    if (slot.isEmpty()) {
                        slot = out.copyWithCount(0);
                        be.inventory.setStackInSlot(i, slot);
                    }

                    if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(slot, out)) {
                        int toDeposit = Math.min(remaining, maxStackSize - slot.getCount());
                        if (toDeposit > 0) {
                            slot.grow(toDeposit);
                            remaining -= toDeposit;
                        }
                    }
                }
            }

            be.water.drainMb(be.getWaterRequired() * moldsToCast);
            be.steam.setFill(Math.min(be.steam.getMaxFill(),
                    be.steam.getFill() + be.getWaterRequired() * moldsToCast));

            be.lastProgressTick = level.getGameTime();
            be.setChanged();
        }

        if (oldAmount != be.amount || oldType != be.type) {
            be.syncToClient();
        }
    }

    /** Порт maxProcessable: партия ограничена металлом, местом в 6 слотах, водой и паром. */
    private int maxProcessable() {
        ItemCastMold mold = getInstalledMold();
        if (type == null || mold == null) return 0;
        ItemStack out = getResultFor(type, mold.getMoldType());
        if (out == null || out.isEmpty()) return 0;

        int cost = mold.getMoldType().getCostMb();
        if (cost <= 0) return 0;

        int freeSlots = 0;
        int stackLimit = out.getMaxStackSize();

        for (int i = SLOT_OUTPUT_START; i < SLOT_COUNT; i++) {
            ItemStack slot = inventory.getStackInSlot(i);
            if (slot.isEmpty()) {
                freeSlots += stackLimit;
            } else if (com.hbm_m.platform.PlatformHooks.isSameItemSameTags(slot, out)) {
                freeSlots += stackLimit - slot.getCount();
            }
        }

        int moldsToCast = amount / cost;
        moldsToCast = Math.min(moldsToCast, freeSlots / out.getCount());
        moldsToCast = Math.min(moldsToCast, water.getFill() / getWaterRequired());
        moldsToCast = Math.min(moldsToCast, (steam.getMaxFill() - steam.getFill()) / getWaterRequired());

        return moldsToCast;
    }

    private static class FluidPort {
        final BlockPos pos;
        final Direction dir;
        FluidPort(BlockPos pos, Direction dir) {
            this.pos = pos;
            this.dir = dir;
        }
    }

    /**
     * Порт getFluidConPos: 4 порта вдоль стола — 2 у башни (влево-назад и вправо-назад)
     * и 2 у конца стола (на 5 назад).
     */
    private List<FluidPort> getFluidConPositions(BlockPos corePos) {
        Direction dir = getFacing();
        Direction rot = dir.getCounterClockWise();
        List<FluidPort> ports = new ArrayList<>(4);

        // Port 1: rot * 2 - dir, facing rot
        ports.add(new FluidPort(corePos.offset(rot.getStepX() * 2 - dir.getStepX(), 0, rot.getStepZ() * 2 - dir.getStepZ()), rot));
        // Port 2: -rot - dir, facing rot.getOpposite()
        ports.add(new FluidPort(corePos.offset(-rot.getStepX() - dir.getStepX(), 0, -rot.getStepZ() - dir.getStepZ()), rot.getOpposite()));
        // Port 3: rot * 2 - dir * 5, facing rot
        ports.add(new FluidPort(corePos.offset(rot.getStepX() * 2 - dir.getStepX() * 5, 0, rot.getStepZ() * 2 - dir.getStepZ() * 5), rot));
        // Port 4: -rot - dir * 5, facing rot.getOpposite()
        ports.add(new FluidPort(corePos.offset(-rot.getStepX() - dir.getStepX() * 5, 0, -rot.getStepZ() - dir.getStepZ() * 5), rot.getOpposite()));

        return ports;
    }

    /** Порт updateConnections: вода и пар на 4 порта вдоль стола. */
    private void updateConnections(Level level, BlockPos pos) {
        for (FluidPort port : getFluidConPositions(pos)) {
            trySubscribe(water.getTankType(), level, port.pos, port.dir);
            if (steam.getFill() > 0) {
                tryProvide(steam, level, port.pos, port.dir);
            }
        }
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(com.hbm_m.block.machines.MachineStrandCasterBlock.FACING)) {
            return state.getValue(com.hbm_m.block.machines.MachineStrandCasterBlock.FACING);
        }
        return Direction.NORTH;
    }

    // ══════════════════ Налив расплава (ICrucibleAcceptor) ══════════════════

    /**
     * Порт canAcceptPartialPour: только сверху и только на 2×2 верх башни
     * (позиции {rot-dir, -dir, rot, 0} на y+2 от ядра).
     */
    @Override
    public boolean canAcceptPartialPour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        if (!isTowerTopPourPos(pos)) return false;
        return standardCheck(stack);
    }

    /** Проверка: позиция налива — одна из 4 клеток верха башни (порт getMetalPourPos). */
    private boolean isTowerTopPourPos(BlockPos pourPos) {
        Direction dir = getFacing();
        Direction rot = dir.getCounterClockWise();
        BlockPos rel = pourPos.subtract(worldPosition);
        if (rel.getY() != 2) return false;

        int dx = rel.getX();
        int dz = rel.getZ();
        int[][] candidates = {
                { rot.getStepX() - dir.getStepX(), rot.getStepZ() - dir.getStepZ() },
                { -dir.getStepX(), -dir.getStepZ() },
                { rot.getStepX(), rot.getStepZ() },
                { 0, 0 }
        };
        for (int[] c : candidates) {
            if (c[0] == dx && c[1] == dz) return true;
        }
        return false;
    }

    /** Порт standardCheck: совпадение типа, лимит (9 стоимостей изложницы) и наличие изложницы. */
    private boolean standardCheck(MaterialStack stack) {
        if (this.type != null && this.type != stack.type) return false;
        ItemCastMold mold = getInstalledMold();
        int limit = mold != null ? mold.getMoldType().getCostMb() * 9 : this.getCapacity();
        return !(this.amount >= limit || getInstalledMold() == null);
    }

    /** Порт standardAdd: тип ставится до проверки, излишек возвращается, прогресс сбрасывается. */
    @Override
    public @Nullable MaterialStack pour(Level level, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.type;
        ItemCastMold mold = getInstalledMold();
        int limit = mold != null ? mold.getMoldType().getCostMb() * 9 : this.getCapacity();

        if (stack.amount + this.amount <= limit) {
            this.amount += stack.amount;
            setChanged();
            syncToClient();
            return null;
        }

        int required = limit - this.amount;
        this.amount = limit;
        stack.amount -= required;

        this.lastProgressTick = level.getGameTime();
        setChanged();
        syncToClient();
        return stack;
    }

    @Override public boolean canAcceptPartialFlow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override public @Nullable MaterialStack flow(Level level, BlockPos pos, Direction side, MaterialStack stack) { return null; }

    // ═══════════════════════════ Жидкости (MK2) ═══════════════════════════

    @Override
    public FluidTank[] getAllTanks() { return new FluidTank[] { water, steam }; }

    @Override
    public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }

    @Override
    public FluidTank[] getSendingTanks() { return new FluidTank[] { steam }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    // ═══════════════════════════ Инвентарь/NBT ═══════════════════════════

    /** Порт isItemValidForSlot: только изложница в слот 0. */
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_MOLD && stack.getItem() instanceof ItemCastMold;
    }

    /** Порт getAccessibleSlotsFromSide: наружу доступны только слоты выхода 1..6. */
    public int[] getAccessibleSlots() {
        return new int[] { 1, 2, 3, 4, 5, 6 };
    }

    @Override
    public @Nullable Object getItemHandler(@Nullable Direction side) {
        return this.automationHandler;
    }

    //? if forge {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> lazyItemHandler = net.minecraftforge.common.util.LazyOptional.empty();

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = net.minecraftforge.common.util.LazyOptional.of(() -> automationHandler);
    }

    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }
    //?}

    @Override
    protected void writeNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.put("items", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, registries));
        if (type != null) tag.putString("mat_type", type.name);
        tag.putInt("mat_amount", amount);
        water.writeToNBT(tag, "w");
        steam.writeToNBT(tag, "s");
        tag.putLong("t", lastProgressTick);
    }

    @Override
    protected void readNbtData(CompoundTag tag, HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        if (tag.contains("items")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("items"), registries);
        } else if (tag.contains("inventory")) {
            com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), registries);
        }
        type = tag.contains("mat_type") ? MaterialType.byName(tag.getString("mat_type")) : null;
        amount = tag.getInt("mat_amount");
        water.readFromNBT(tag, "w");
        steam.readFromNBT(tag, "s");
        lastProgressTick = tag.getLong("t");
    }

    // ═══════════════════════════ Синхронизация/GUI ═══════════════════════════

    /** Полный NBT-sync клиенту (рендер уровня расплава). */
    public void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

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
