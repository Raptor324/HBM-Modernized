package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.api.fluids.IFluidStandardTransceiverMK2;
import com.hbm_m.block.machines.DummyableMachineBlock;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.inventory.menu.MachineStrandCasterMenu;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.item.material.ItemMold;
import com.hbm_m.item.material.ItemScraps;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 1:1 {@code TileEntityMachineStrandCaster}: Stranggiessanlage. Nimmt fluessiges Metall an den vier oberen Einguessen
 * auf (bis 9 Formfuellungen), giesst bis zu 9 Formen auf einmal - spaetestens nach 10 Sekunden Ruhe - in die sechs
 * Ausgabeplaetze und kuehlt dabei je Guss {@code 5 * Formkosten} mB Wasser zu Abdampf.
 */
public class MachineStrandCasterBlockEntity extends MachineFoundryCastingBaseBlockEntity implements IFluidStandardTransceiverMK2, MenuProvider {

    public final FluidTank water = new FluidTank(ModFluids.WATER.getSource(), 64_000);
    public final FluidTank steam = new FluidTank(ModFluids.SPENTSTEAM.getSource(), 64_000);
    private long lastProgressTick = 0;

    public MachineStrandCasterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STRAND_CASTER_BE.get(), pos, state, 7);
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(DummyableMachineBlock.FACING) ? state.getValue(DummyableMachineBlock.FACING) : Direction.NORTH;
    }

    @Override
    public void updateEntity() {

        if (level instanceof ServerLevel world) {

            if (this.lastType != this.type || this.lastAmount != this.amount) {
                markForUpdate();
                this.lastType = this.type;
                this.lastAmount = this.amount;
            }

            // In case of overfill problems, spit out the excess as scrap
            if (amount > getCapacity()) {
                ItemStack scrap = ItemScraps.create(new MaterialStack(type, Math.max(amount - getCapacity(), 0)));
                world.addFreshEntity(new ItemEntity(world, worldPosition.getX() + 0.5, worldPosition.getY() + 2, worldPosition.getZ() + 0.5, scrap));
                this.amount = this.getCapacity();
            }

            if (this.amount == 0) {
                this.type = null;
            }

            this.updateConnections(world);

            int moldsToCast = maxProcessable();

            // Makes it flush the buffers after 10 seconds of inactivity, or when they're full
            if (moldsToCast > 0 && (moldsToCast >= 9 || world.getDayTime() >= lastProgressTick + 200)) {

                ItemMold.Mold mold = this.getInstalledMold();

                this.amount -= moldsToCast * mold.getCost();

                ItemStack out = mold.getOutput(type);
                int remaining = out.getCount() * moldsToCast;
                final int maxStackSize = out.getMaxStackSize();

                for (int i = 1; i < 7; i++) {
                    if (remaining <= 0) {
                        break;
                    }

                    if (slots[i].isEmpty()) {
                        slots[i] = out.copyWithCount(0);
                    }

                    if (slots[i].isEmpty() || PlatformHooks.isSameItemSameTags(slots[i], out)) {
                        int current = slots[i].isEmpty() ? 0 : slots[i].getCount();
                        int toDeposit = Math.min(remaining, maxStackSize - current);
                        slots[i] = out.copyWithCount(current + toDeposit);
                        remaining -= toDeposit;
                    }
                }

                setChanged();

                water.setFill(water.getFill() - getWaterRequired() * moldsToCast);
                steam.setFill(steam.getFill() + getWaterRequired() * moldsToCast);

                lastProgressTick = world.getDayTime();
            }

            sendUpdateToClient();
        }
    }

    private void sendUpdateToClient() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private int maxProcessable() {
        ItemMold.Mold mold = this.getInstalledMold();
        if (type == null || mold == null || mold.getOutput(type) == null) {
            return 0;
        }

        int freeSlots = 0;
        final int stackLimit = mold.getOutput(type).getMaxStackSize();

        for (int i = 1; i < 7; i++) {
            if (slots[i].isEmpty()) {
                freeSlots += stackLimit;
            } else if (PlatformHooks.isSameItemSameTags(slots[i], mold.getOutput(type))) {
                freeSlots += stackLimit - slots[i].getCount();
            }
        }

        int moldsToCast = amount / mold.getCost();
        moldsToCast = Math.min(moldsToCast, freeSlots / mold.getOutput(type).getCount());
        moldsToCast = Math.min(moldsToCast, water.getFill() / getWaterRequired());
        moldsToCast = Math.min(moldsToCast, (steam.getMaxFill() - steam.getFill()) / getWaterRequired());

        return moldsToCast;
    }

    /** Anschluss samt Richtung (Original {@code DirPos}). */
    private record DirPos(BlockPos pos, Direction dir) { }

    public DirPos[] getFluidConPos() {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition;
        return new DirPos[] {
                new DirPos(p.relative(rot, 2).relative(dir, -1), rot),
                new DirPos(p.relative(rot, -1).relative(dir, -1), rot.getOpposite()),
                new DirPos(p.relative(rot, 2).relative(dir, -5), rot),
                new DirPos(p.relative(rot, -1).relative(dir, -5), rot.getOpposite())
        };
    }

    public BlockPos[] getMetalPourPos() {
        Direction dir = facing();
        Direction rot = dir.getClockWise();
        BlockPos p = worldPosition.above(2);
        return new BlockPos[] {
                p.relative(rot).relative(dir, -1),
                p.relative(dir, -1),
                p.relative(rot),
                p
        };
    }

    /** Original: jede Formgroesse passt, die Form bestimmt die Groesse. */
    @Override
    @Nullable
    public ItemMold.Mold getInstalledMold() {
        if (slots[0].isEmpty()) return null;
        return ItemMold.getMold(slots[0]);
    }

    @Override
    public int getMoldSize() {
        ItemMold.Mold mold = getInstalledMold();
        return mold == null ? 0 : mold.size;
    }

    @Override
    public boolean canAcceptPartialPour(Level world, BlockPos pos, double dX, double dY, double dZ, Direction side, MaterialStack stack) {
        if (side != Direction.UP) return false;
        for (BlockPos p : getMetalPourPos()) {
            if (p.equals(pos)) {
                return this.standardCheck(world, pos, side, stack);
            }
        }
        return false;
    }

    @Override public boolean canAcceptPartialFlow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return false; }
    @Override @Nullable public MaterialStack flow(Level world, BlockPos pos, Direction side, MaterialStack stack) { return null; }

    @Override
    public boolean standardCheck(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        if (this.type != null && this.type != stack.material) return false;
        int limit = this.getInstalledMold() != null ? this.getInstalledMold().getCost() * 9 : this.getCapacity();
        return !(this.amount >= limit || getInstalledMold() == null);
    }

    @Override
    public int getCapacity() {
        ItemMold.Mold mold = this.getInstalledMold();
        return mold == null ? 50000 : mold.getCost() * 10;
    }

    private int getWaterRequired() {
        return getInstalledMold() != null ? 5 * getInstalledMold().getCost() : 50;
    }

    private void updateConnections(ServerLevel world) {
        for (DirPos pos : getFluidConPos()) {
            this.trySubscribe(water.getTankType(), world, pos.pos, pos.dir);
            if (steam.getFill() > 0) this.tryProvide(steam, world, pos.pos, pos.dir);
        }
    }

    @Override
    @Nullable
    public MaterialStack standardAdd(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        this.type = stack.material;

        int limit = this.getInstalledMold() != null ? this.getInstalledMold().getCost() * 9 : this.getCapacity();

        if (stack.amount + this.amount <= limit) {
            this.amount += stack.amount;
            return null;
        }

        int required = limit - this.amount;
        this.amount = limit;

        stack.amount -= required;

        lastProgressTick = world.getDayTime();

        return stack;
    }

    @Override public FluidTank[] getSendingTanks() { return new FluidTank[] { steam }; }
    @Override public FluidTank[] getReceivingTanks() { return new FluidTank[] { water }; }
    @Override public FluidTank[] getAllTanks() { return new FluidTank[] { water, steam }; }

    @Override
    public boolean isLoaded() {
        return level != null && !isRemoved() && level.isLoaded(worldPosition);
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        water.writeToNBT(nbt, "w");
        steam.writeToNBT(nbt, "s");
        nbt.putLong("t", lastProgressTick);
    }

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        water.readFromNBT(nbt, "w");
        steam.readFromNBT(nbt, "s");
        lastProgressTick = nbt.getLong("t");
    }

    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == 0) return stack.getItem() instanceof ItemCastMold;
        return false;
    }

    //? if forge {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> casterHandler;

    /** Original {@code getAccessibleSlotsFromSide {1..6}}: nur die Ausgaben, entnehmbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            if (casterHandler == null) {
                casterHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                    @Override public int getSlots() { return 6; }
                    @Override public @NotNull ItemStack getStackInSlot(int slot) { return slots[slot + 1]; }
                    @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
                    @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                        ItemStack s = slots[slot + 1];
                        if (s.isEmpty()) return ItemStack.EMPTY;
                        ItemStack out = s.copyWithCount(Math.min(amount, s.getCount()));
                        if (!simulate) {
                            s.shrink(out.getCount());
                            if (s.isEmpty()) slots[slot + 1] = ItemStack.EMPTY;
                            setChanged();
                        }
                        return out;
                    }
                    @Override public int getSlotLimit(int slot) { return 64; }
                    @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
                });
            }
            return casterHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (casterHandler != null) casterHandler.invalidate();
        casterHandler = null;
    }
    //?}

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.machineStrandCaster");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineStrandCasterMenu(id, inventory, this);
    }

    //? if forge {
    @Override
    //?}
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition.getX() - 7, worldPosition.getY(), worldPosition.getZ() - 7,
                worldPosition.getX() + 7, worldPosition.getY() + 3, worldPosition.getZ() + 7);
    }

    /** Fuer die GUI. */
    public String formatAmount(boolean mb) {
        return Mats.formatAmount(amount, mb);
    }
}
