package com.hbm_m.blockentity.network.pneumatic;

import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.PneumoStorageImporterMenu;

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
 * 1:1-Port von {@code TileEntityPneumoStorageImporter} (1.7.10): die Eingabe ins Lagernetz.
 *
 * <p>Neun Plaetze, in die man - von Hand oder ueber ein Rohr - alles hineinschiebt, was ins Lager
 * soll. Jeden Tick versucht das Geraet, den Inhalt an das Netz abzugeben.</p>
 *
 * <p>Passt gerade nichts hinein, legt sich der betroffene Platz fuer hundert Ticks schlafen, statt
 * es jeden Tick erneut zu probieren. Wird von Hand etwas eingelegt, wacht er sofort wieder auf -
 * darum setzt das Einlegen die Sperre auf einen einzigen Tick zurueck.</p>
 */
public class PneumoStorageImporterBlockEntity extends PneumaticMachineBlockEntity {

    public static final int INVENTORY_SIZE = 9;
    /** Original: {@code this.delay[i] = 100} nach einem gescheiterten Versuch. */
    private static final int RETRY_DELAY = 100;

    private final int[] delay = new int[INVENTORY_SIZE];

    public PneumoStorageImporterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PNEUMO_STORAGE_IMPORTER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    /** Original: {@code setInventorySlotContents} weckt den Platz sofort wieder auf. */
    public void wakeSlot(int index) {
        if (index >= 0 && index < delay.length) delay[index] = Math.max(delay[index], 1);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PneumoStorageImporterBlockEntity be) {
        if (level.isClientSide()) return;

        be.tickNetwork(level, pos);

        if (be.cache == null || be.cache.hasExpired) return;

        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (be.delay[i] > 0) {
                be.delay[i]--;
                continue;
            }

            ItemStack stack = be.getInventory().getStackInSlot(i);
            if (stack.isEmpty()) continue;

            int leftover = (int) be.cache.addItemsAndReturnQuantity(stack, stack.getCount());

            if (leftover == stack.getCount()) {
                be.delay[i] = RETRY_DELAY;
            } else {
                stack.shrink(stack.getCount() - leftover);
                be.setChanged();
            }
        }
    }

    @Override
    protected boolean isItemValidForSlot(int slot, ItemStack stack) {
        return true;
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putIntArray("delay", delay);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        int[] stored = tag.getIntArray("delay");
        if (stored.length == INVENTORY_SIZE) System.arraycopy(stored, 0, delay, 0, INVENTORY_SIZE);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.hbm_m.pneumatic_storage_importer");
    }

    @Override
    public Component getDisplayName() {
        return getDefaultName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new PneumoStorageImporterMenu(id, inv, this);
    }
}
