package com.hbm_m.blockentity.network;

import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCraneUnboxerMenu;
import com.hbm_m.platform.ModItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCraneUnboxer}: 21 Puffer, Stapel- und Auswurf-Upgrade. Gibt ohne Redstone im Takt des Auswurf-Upgrades
 * den ersten Stapel (bis zur Menge des Stapel-Upgrades) auf das Band am Eingang.
 */
public class MachineCraneUnboxerBlockEntity extends CraneBaseBlockEntity {

    public static final int BUFFER_START = 0;
    public static final int BUFFER_END = 20;
    public static final int SLOT_UPGRADE_STACK = 21;
    public static final int SLOT_UPGRADE_EJECTOR = 22;
    public static final int INVENTORY_SIZE = 23;

    public MachineCraneUnboxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_UNBOXER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    @Override
    protected ModItemStackHandler createInventoryHandler(int size) {
        return new ModItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) { setChanged(); }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) { return true; }
        };
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneUnboxerBlockEntity be) {
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        int delay = MachineCraneExtractorBlockEntity.delayFor(inventory.getStackInSlot(SLOT_UPGRADE_EJECTOR));

        if (level.getGameTime() % delay == 0 && !level.hasNeighborSignal(pos)) {
            int amount = MachineCraneExtractorBlockEntity.amountFor(inventory.getStackInSlot(SLOT_UPGRADE_STACK));

            Direction outputSide = getInputSide(); // Achtung, vertauscht!
            IConveyorBelt belt = CraneInventoryUtil.beltAt(level, pos.relative(outputSide));

            if (belt != null) {
                for (int i = 0; i < 21; i++) {
                    ItemStack stack = inventory.getStackInSlot(i);

                    if (!stack.isEmpty()) {
                        stack = stack.copy();
                        int toSend = Math.min(amount, stack.getCount());
                        inventory.extractItem(i, toSend, false);
                        stack.setCount(toSend);
                        CraneInventoryUtil.sendItem(level, pos, outputSide, belt, stack);
                        break;
                    }
                }
            }
        }
    }

    @Override protected int[] getAccessibleSlots() {
        return new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20 };
    }

    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }
    @Override protected boolean canExtractItem(int slot, ItemStack stack) { return true; }

    @Override protected Component getDefaultName() { return Component.translatable("container.craneUnboxer"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneUnboxerMenu.create(id, inventory, this);
    }
}
