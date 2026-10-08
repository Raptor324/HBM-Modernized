package com.hbm_m.blockentity.network;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.hbm_m.blockentity.BaseHbmBlockEntity;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.entity.conveyor.MovingConveyorItemEntity;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.CrystallizerRecipe;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?}

/**
 * 1:1 {@code CranePartitioner.TileEntityCranePartitioner}: 45 Warteplaetze fuer Kristallisierer-Eingaben und 45
 * Ueberlaufplaetze; jeden Tick werden aus den Warteplaetzen (kleinste Stapel zuerst) genau so grosse Portionen, wie
 * der Kristallisierer je Vorgang braucht, als Foerderband-Teile ausgegeben.
 */
public class CranePartitionerBlockEntity extends BaseHbmBlockEntity {

    public static final int SLOT_COUNT = 45;

    public final com.hbm_m.platform.ModItemStackHandler inventory = new com.hbm_m.platform.ModItemStackHandler(SLOT_COUNT * 2) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };

    //? if forge {
    private final LazyOptional<IItemHandler> sided = LazyOptional.of(() -> new IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= SLOT_COUNT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; // declog
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot <= SLOT_COUNT - 1 && getAmount(level, stack) >= 1;
        }
    });
    //?} elif neoforge {
    /*private final com.hbm_m.platform.LazyCap<net.neoforged.neoforge.items.IItemHandler> sided = com.hbm_m.platform.LazyCap.of(() -> new net.neoforged.neoforge.items.IItemHandler() {
        @Override public int getSlots() { return inventory.getSlots(); }
        @Override public @NotNull ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot); }
        @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return isItemValid(slot, stack) ? inventory.insertItem(slot, stack, simulate) : stack;
        }
        @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot >= SLOT_COUNT ? inventory.extractItem(slot, amount, simulate) : ItemStack.EMPTY; // declog
        }
        @Override public int getSlotLimit(int slot) { return 64; }
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return slot <= SLOT_COUNT - 1 && getAmount(level, stack) >= 1;
        }
    });
    *///?}

    public CranePartitionerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_PARTITIONER.get(), pos, state);
    }

    /** {@code CrystallizerRecipes.getAmount}: benoetigte Eingangsmenge je Vorgang, 0 = kein Rezept. */
    public static int getAmount(@Nullable Level level, ItemStack stack) {
        if (level == null || stack.isEmpty()) return 0;
        for (CrystallizerRecipe r : RecipeHooks.getAllRecipes(level, CrystallizerRecipe.Type.INSTANCE))
            if (r.matchesInput(stack)) return r.getInputCount();
        return 0;
    }

    /** {@code InventoryUtil.tryAddItemToInventory} ohne Gueltigkeitspruefung. */
    public ItemStack tryAdd(int start, int end, ItemStack stack) {
        ItemStack rest = stack.copy();
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty() && StackNbt.sameItemSameTags(s, rest) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(rest.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                rest.shrink(move);
                inventory.setStackInSlot(i, s);
            }
        }
        for (int i = start; i <= end && !rest.isEmpty(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                inventory.setStackInSlot(i, rest.copy());
                rest = ItemStack.EMPTY;
            }
        }
        return rest;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CranePartitionerBlockEntity te) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < SLOT_COUNT; i++) if (!te.inventory.getStackInSlot(i).isEmpty()) stacks.add(te.inventory.getStackInSlot(i));
        stacks.sort((a, b) -> (int) Math.signum(a.getCount() - b.getCount()));

        boolean changed = false;
        for (ItemStack stack : stacks) {
            int amount = getAmount(level, stack);
            if (amount == 0) amount = stack.getCount(); // ungueltiges Teil: ganzen Stapel auswerfen
            while (stack.getCount() >= amount && amount > 0) {
                ItemStack entityStack = stack.copy();
                entityStack.setCount(amount);
                stack.shrink(amount);
                level.addFreshEntity(MovingConveyorItemEntity.create(level, pos.getX() + 0.5, pos.getY() + 0.25, pos.getZ() + 0.5, entityStack));
                changed = true;
            }
        }
        for (int i = 0; i < SLOT_COUNT; i++) if (te.inventory.getStackInSlot(i).getCount() <= 0) te.inventory.setStackInSlot(i, ItemStack.EMPTY);
        if (changed) te.setChanged();
    }

    //? if forge {
    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) return sided.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        sided.invalidate();
    }
    //?} elif neoforge {
    /*@Override
    public <T> com.hbm_m.platform.LazyCap<T> getHbmCapability(com.hbm_m.platform.HbmCap<T> cap, @org.jetbrains.annotations.Nullable net.minecraft.core.Direction side) {
        if (cap == com.hbm_m.platform.HbmCap.ITEM_HANDLER) return sided.cast();
        return super.getHbmCapability(cap, side);
    }

    @Override
    public void invalidateHbmCaps() {
        super.invalidateHbmCaps();
        sided.invalidate();
    }
    *///?}

    @Override
    protected void writeNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        tag.put("inventory", com.hbm_m.platform.ItemStackSerialization.serialize(inventory, com.hbm_m.platform.BlockHooks.registriesOr(registries, this)));
    }

    @Override
    protected void readNbtData(@NotNull CompoundTag tag, @Nullable HolderLookup.Provider registries) {
        if (tag.contains("inventory")) com.hbm_m.platform.ItemStackSerialization.deserialize(inventory, tag.getCompound("inventory"), com.hbm_m.platform.BlockHooks.registriesOr(registries, this));
    }
}
