package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PneumoStorageMonoMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * 1:1-Port von {@code TileEntityPneumoStorageMono} (1.7.10): das Massenlager.
 *
 * <p>Drei Faecher, aber jedes fasst {@value #CAPACITY} Stueck. Der Gegenstand eines Fachs wird als
 * <b>Vorlage</b> in den Platz gelegt und bleibt dort; der eigentliche Bestand steht daneben als
 * blosse Zahl. Deshalb laesst dieses Lager auch keine Typfestlegung ueber das Terminal zu - was
 * hineinsoll, muss man selbst bestimmen.</p>
 */
public class PneumoStorageMonoBlockEntity extends PneumaticStorageBlockEntity {

    /** Original: {@code CAPACITY = 100_000} je Fach. */
    public static final int CAPACITY = 100_000;
    /** Original: {@code super(3)}. */
    public static final int INVENTORY_SIZE = 3;

    private int[] amounts = new int[INVENTORY_SIZE];

    public PneumoStorageMonoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_STORAGE_MONO_BE.get(), pos, state, INVENTORY_SIZE);
    }

    public int[] getAmounts() { return amounts; }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoStorageMonoBlockEntity be) {
        if (level.isClientSide()) return;
        be.tickStorage(level, pos);
        be.setChanged();
        be.sendUpdateToClient();
    }

    @Override
    public long getAmountAt(int index) {
        return index >= 0 && index < amounts.length ? amounts[index] : 0L;
    }

    /** Original: das Massenlager legt keine Typen selbst fest. */
    @Override
    public boolean allowTypeSetting() {
        return false;
    }

    @Override
    public long useUpItem(int index, long amount) {
        if (amounts[index] <= 0) return amount;

        int toRemove = (int) Math.min(amount, amounts[index]);
        amounts[index] -= toRemove;
        setChanged();
        return amount - toRemove;
    }

    @Override
    public long addItem(int index, long amount) {
        int capacity = CAPACITY - amounts[index];
        if (capacity <= 0) return amount;

        int toAdd = (int) Math.min(amount, capacity);
        amounts[index] += toAdd;
        setChanged();
        return amount - toAdd;
    }

    @Override
    public long setupType(int index, ItemStack zeroStack, long amount) {
        return amount;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putIntArray("amounts", amounts);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int[] stored = tag.getIntArray("amounts");
        amounts = stored.length == INVENTORY_SIZE ? stored : new int[INVENTORY_SIZE];
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_storage_mono");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoStorageMonoMenu(id, inv, this);
    }
}
