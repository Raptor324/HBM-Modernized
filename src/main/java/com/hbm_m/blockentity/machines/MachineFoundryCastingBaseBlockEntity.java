package com.hbm_m.blockentity.machines;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.inventory.material.Mats.MaterialStack;
import com.hbm_m.item.material.ItemMold;
import com.hbm_m.item.material.ItemMold.Mold;
import com.hbm_m.platform.PlatformHooks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityFoundryCastingBase}: Form bzw. Becken mit Formplatz (0) und Gussplatz (1). Ist die Form voll und
 * der Gussplatz frei, kuehlt das Material 100 bzw. 200 Ticks ab und wird zum Gussstueck. Nur Platz 1 ist von aussen
 * entnehmbar.
 */
public abstract class MachineFoundryCastingBaseBlockEntity extends MachineFoundryBaseBlockEntity {

    public ItemStack[] slots;
    public int cooloff = 100;

    protected MachineFoundryCastingBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        slots = new ItemStack[slotCount];
        for (int i = 0; i < slotCount; i++) slots[i] = ItemStack.EMPTY;
    }

    @Override
    public void updateEntity() {
        super.updateEntity();

        if (level != null && !level.isClientSide) {

            if (this.amount > this.getCapacity()) {
                this.amount = this.getCapacity();
            }

            if (this.amount == 0) {
                this.type = null;
            }

            Mold mold = this.getInstalledMold();

            if (mold != null && this.amount == this.getCapacity() && slots[1].isEmpty()) {
                cooloff--;

                if (cooloff <= 0) {
                    this.amount = 0;

                    ItemStack out = mold.getOutput(type);

                    if (out != null) {
                        slots[1] = out.copy();
                    }

                    cooloff = 200;
                    this.markForUpdate();
                }

            } else {
                cooloff = 200;
            }
        }
    }

    /** Checks slot 0 to see what mold type is installed. Returns null if no mold is found or an incorrect size was used. */
    @Nullable
    public Mold getInstalledMold() {
        if (slots[0].isEmpty()) return null;

        Mold mold = ItemMold.getMold(slots[0]);
        if (mold != null && mold.size == this.getMoldSize()) return mold;

        return null;
    }

    /** Returns the amount of quanta this casting block can hold, depending on the installed mold or 0 if no mold is found. */
    @Override
    public int getCapacity() {
        Mold mold = this.getInstalledMold();
        return mold == null ? 0 : mold.getCost();
    }

    /**
     * Standard check for testing if this material stack can be added to the casting block. Checks:<br>
     * - type matching<br>
     * - amount being at max<br>
     * - whether a mold is installed<br>
     * - whether the mold can accept this type
     */
    @Override
    public boolean standardCheck(Level world, BlockPos pos, Direction side, MaterialStack stack) {
        if (!super.standardCheck(world, pos, side, stack)) return false; //reject if base conditions are not met
        if (!this.slots[1].isEmpty()) return false; //reject if a freshly casted item is still present
        Mold mold = this.getInstalledMold();
        if (mold == null) return false;

        return mold.getOutput(stack.material) != null; //no OD match -> no pouring
    }

    /** Returns an integer determining the mold size, 0 for small molds and 1 for the basin */
    public abstract int getMoldSize();

    @Override
    protected void readNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(nbt, registries);
        ListTag list = nbt.getList("items", Tag.TAG_COMPOUND);
        for (int i = 0; i < slots.length; i++) slots[i] = ItemStack.EMPTY;

        for (int i = 0; i < list.size(); i++) {
            CompoundTag nbt1 = list.getCompound(i);
            byte b0 = nbt1.getByte("slot");
            if (b0 >= 0 && b0 < slots.length) {
                slots[b0] = PlatformHooks.itemStackOf(nbt1, registries);
            }
        }
    }

    @Override
    protected void writeNbtData(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(nbt, registries);
        ListTag list = new ListTag();

        for (int i = 0; i < slots.length; i++) {
            if (!slots[i].isEmpty()) {
                CompoundTag nbt1 = new CompoundTag();
                nbt1.putByte("slot", (byte) i);
                PlatformHooks.saveItemStack(slots[i], nbt1, registries);
                list.add(nbt1);
            }
        }
        nbt.put("items", list);
    }

    //? if forge {
    private net.minecraftforge.common.util.LazyOptional<net.minecraftforge.items.IItemHandler> outputHandler;

    /** Original {@code ISidedInventory}: von allen Seiten nur Platz 1 entnehmbar, nichts einfuegbar. */
    @Override
    public @NotNull <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            if (outputHandler == null) {
                outputHandler = net.minecraftforge.common.util.LazyOptional.of(() -> new net.minecraftforge.items.IItemHandler() {
                    @Override public int getSlots() { return 1; }
                    @Override public @NotNull ItemStack getStackInSlot(int slot) { return slots[1]; }
                    @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) { return stack; }
                    @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                        if (slots[1].isEmpty()) return ItemStack.EMPTY;
                        ItemStack out = slots[1].copyWithCount(Math.min(amount, slots[1].getCount()));
                        if (!simulate) {
                            slots[1].shrink(out.getCount());
                            if (slots[1].isEmpty()) slots[1] = ItemStack.EMPTY;
                            markForUpdate();
                        }
                        return out;
                    }
                    @Override public int getSlotLimit(int slot) { return 64; }
                    @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return false; }
                });
            }
            return outputHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        if (outputHandler != null) outputHandler.invalidate();
        outputHandler = null;
    }
    //?}
}
