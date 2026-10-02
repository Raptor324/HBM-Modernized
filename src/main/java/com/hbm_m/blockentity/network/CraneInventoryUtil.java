package com.hbm_m.blockentity.network;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.block.network.IEnterableBlock;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.entity.conveyor.MovingConveyorPackageEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
//?}

/** Gemeinsame Helfer der Krane: {@code CraneInserter.addToInventory}, {@code InventoryUtil.masquerade}, Band-Ausgabe. */
public final class CraneInventoryUtil {

    private CraneInventoryUtil() {}

    //? if forge {
    /**
     * Zielinventar an {@code pos}, von Seite {@code side} aus betrachtet. Original {@code InventoryUtil.masquerade}: Oefen
     * nehmen von jeder Seite Brennstoff (1) und Eingabe (0) an.
     */
    @Nullable
    public static IItemHandler inventoryAt(Level level, BlockPos pos, Direction side) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te == null) return null;
        if (te instanceof AbstractFurnaceBlockEntity furnace) return new SlotView(new InvWrapper(furnace), new int[] { 1, 0 });
        return te.getCapability(ForgeCapabilities.ITEM_HANDLER, side).orElse(null);
    }

    /**
     * 1:1 {@code CraneInserter.addToInventory}: erst auf gleiche Stapel auffuellen, dann leere Plaetze belegen.
     * Veraendert {@code toAdd}; gibt den Rest zurueck ({@link ItemStack#EMPTY} = alles untergebracht).
     */
    public static ItemStack addToInventory(IItemHandler inv, ItemStack toAdd) {
        for (int i = 0; i < inv.getSlots() && !toAdd.isEmpty(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameTags(stack, toAdd)
                    && stack.getCount() < Math.min(stack.getMaxStackSize(), inv.getSlotLimit(i))) {
                ItemStack rest = inv.insertItem(i, toAdd.copy(), false);
                toAdd.setCount(rest.getCount());
            }
        }
        for (int i = 0; i < inv.getSlots() && !toAdd.isEmpty(); i++) {
            if (inv.getStackInSlot(i).isEmpty()) {
                ItemStack rest = inv.insertItem(i, toAdd.copy(), false);
                toAdd.setCount(rest.getCount());
            }
        }
        return toAdd.isEmpty() ? ItemStack.EMPTY : toAdd;
    }

    /** Sicht auf ausgewaehlte Plaetze eines Inventars (Original: {@code access}-Array). */
    public static class SlotView implements IItemHandler {
        private final IItemHandler inv;
        private final int[] access;
        public SlotView(IItemHandler inv, int[] access) { this.inv = inv; this.access = access; }
        @Override public int getSlots() { return access.length; }
        @Override public ItemStack getStackInSlot(int slot) { return inv.getStackInSlot(access[slot]); }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inv.insertItem(access[slot], stack, simulate); }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return inv.extractItem(access[slot], amount, simulate); }
        @Override public int getSlotLimit(int slot) { return inv.getSlotLimit(access[slot]); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return inv.isItemValid(access[slot], stack); }
    }

    /** Vanilla {@code Container.calcRedstoneFromInventory} fuer einen Item-Handler. */
    public static int comparator(IItemHandler inv) {
        int nonEmpty = 0;
        float f = 0.0F;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                f += (float) stack.getCount() / (float) Math.min(inv.getSlotLimit(i), stack.getMaxStackSize());
                nonEmpty++;
            }
        }
        f /= (float) inv.getSlots();
        return Mth.floor(f * 14.0F) + (nonEmpty > 0 ? 1 : 0);
    }
    //?}

    /** Original: {@code getClosestSnappingPosition} fuer ein aus {@code from} in Richtung {@code dir} ausgegebenes Objekt. */
    public static Vec3 snap(Level level, BlockPos from, Direction dir, IConveyorBelt belt) {
        Vec3 pos = new Vec3(from.getX() + 0.5 + dir.getStepX() * 0.55, from.getY() + 0.5 + dir.getStepY() * 0.55, from.getZ() + 0.5 + dir.getStepZ() * 0.55);
        return belt.getClosestSnappingPosition(level, from.relative(dir), pos);
    }

    @Nullable
    public static IConveyorBelt beltAt(Level level, BlockPos pos) {
        Block b = level.getBlockState(pos).getBlock();
        return b instanceof IConveyorBelt belt ? belt : null;
    }

    /** Legt ein Teil auf das Band vor {@code from} (Original: {@code EntityMovingItem} an der Einrastposition). */
    public static MovingConveyorItemEntity sendItem(Level level, BlockPos from, Direction dir, IConveyorBelt belt, ItemStack stack) {
        Vec3 snap = snap(level, from, dir, belt);
        MovingConveyorItemEntity moving = MovingConveyorItemEntity.create(level, snap.x, snap.y, snap.z, stack);
        level.addFreshEntity(moving);
        return moving;
    }

    /** Wie {@link #sendItem}, aber mit sofortigem Eintritt in einen {@link IEnterableBlock} (Original: Extraktor). */
    public static void sendItemAndEnter(Level level, BlockPos from, Direction dir, IConveyorBelt belt, ItemStack stack) {
        MovingConveyorItemEntity moving = sendItem(level, from, dir, belt, stack);
        if (belt instanceof IEnterableBlock enterable) {
            BlockPos target = from.relative(dir);
            if (enterable.canItemEnter(level, target, dir.getOpposite(), moving)) {
                enterable.onItemEnter(level, target, moving);
                moving.discard();
            }
        }
    }

    public static void sendPackage(Level level, BlockPos from, Direction dir, IConveyorBelt belt, ItemStack[] box) {
        Vec3 snap = snap(level, from, dir, belt);
        level.addFreshEntity(MovingConveyorPackageEntity.create(level, snap.x, snap.y, snap.z, box));
    }
}
