package com.hbm_m.blockentity.network;

import com.hbm_m.api.tile.IControlReceiver;
import com.hbm_m.block.network.IConveyorBelt;
import com.hbm_m.blockentity.ModBlockEntities;
import com.hbm_m.inventory.menu.MachineCraneBoxerMenu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 1:1 {@code TileEntityCraneBoxer}: packt alle zwei Ticks je nach Modus 4/8/16 volle Stapel zu einem Paket, im
 * Redstone-Modus bei steigender Flanke alles, was da ist, und legt es auf das Band am Ausgang.
 */
public class MachineCraneBoxerBlockEntity extends CraneBaseBlockEntity implements IControlReceiver {

    public static final int INVENTORY_SIZE = 7 * 3;

    public static final byte MODE_4 = 0;
    public static final byte MODE_8 = 1;
    public static final byte MODE_16 = 2;
    public static final byte MODE_REDSTONE = 3;

    public byte mode = 0;
    private boolean lastRedstone = false;

    public MachineCraneBoxerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRANE_BOXER_BE.get(), pos, state, INVENTORY_SIZE);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineCraneBoxerBlockEntity be) {
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        boolean redstone = level.hasNeighborSignal(pos);

        if (mode == MODE_REDSTONE && redstone && !lastRedstone) {
            Direction outputSide = getOutputSide();
            IConveyorBelt belt = CraneInventoryUtil.beltAt(level, pos.relative(outputSide));

            int pack = 0;
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                if (!inventory.getStackInSlot(i).isEmpty()) pack++;
            }

            if (belt != null && pack > 0) {
                ItemStack[] box = new ItemStack[pack];
                for (int i = 0; i < INVENTORY_SIZE && pack > 0; i++) {
                    if (!inventory.getStackInSlot(i).isEmpty()) {
                        pack--;
                        box[pack] = inventory.getStackInSlot(i).copy();
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                CraneInventoryUtil.sendPackage(level, pos, outputSide, belt, box);
            }
        }

        this.lastRedstone = redstone;

        if (mode != MODE_REDSTONE && level.getGameTime() % 2 == 0) {
            int pack = 1;
            switch (mode) {
                case MODE_4: pack = 4; break;
                case MODE_8: pack = 8; break;
                case MODE_16: pack = 16; break;
            }

            int fullStacks = 0;
            for (int i = 0; i < INVENTORY_SIZE; i++) {
                ItemStack s = inventory.getStackInSlot(i);
                if (!s.isEmpty() && s.getCount() == s.getMaxStackSize()) fullStacks++;
            }

            Direction outputSide = getOutputSide();
            IConveyorBelt belt = CraneInventoryUtil.beltAt(level, pos.relative(outputSide));

            if (belt != null && fullStacks >= pack) {
                ItemStack[] box = new ItemStack[pack];
                for (int i = 0; i < INVENTORY_SIZE && pack > 0; i++) {
                    ItemStack s = inventory.getStackInSlot(i);
                    if (!s.isEmpty() && s.getCount() == s.getMaxStackSize()) {
                        pack--;
                        box[pack] = s.copy();
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    }
                }
                CraneInventoryUtil.sendPackage(level, pos, outputSide, belt, box);
            }
        }

        sendUpdateToClient();
    }

    public byte getMode() { return mode; }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("toggle")) this.mode = (byte) ((this.mode + 1) % 4);
        setChanged();
        sendUpdateToClient();
    }

    @Override
    protected void writeNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.writeNbtData(tag, registries);
        tag.putByte("mode", mode);
        tag.putBoolean("lastRedstone", lastRedstone);
    }

    @Override
    protected void readNbtData(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.readNbtData(tag, registries);
        mode = tag.getByte("mode");
        lastRedstone = tag.getBoolean("lastRedstone");
    }

    /** Original: Einfuegen auf allen 21 Plaetzen erlaubt, Entnahme nicht ({@code TileEntityMachineBase}). */
    @Override protected boolean isItemValidForSlot(int slot, ItemStack stack) { return true; }

    @Override protected Component getDefaultName() { return Component.translatable("container.craneBoxer"); }
    @Override public Component getDisplayName() { return getDefaultName(); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return MachineCraneBoxerMenu.create(id, inventory, this);
    }
}
