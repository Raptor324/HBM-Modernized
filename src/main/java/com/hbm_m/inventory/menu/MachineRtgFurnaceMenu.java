package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineRtgFurnaceBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerRtgFurnace}: Eingang (56,17), Pellets (38/56/74,53), Ausgang (116,35), Spielerinventar ab y 84.
 * {@code dualCookTime} wird als Fortschrittsbalken 0 synchronisiert.
 */
public class MachineRtgFurnaceMenu extends AbstractContainerMenu {

    private final MachineRtgFurnaceBlockEntity diFurnace;

    public MachineRtgFurnaceMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRtgFurnaceMenu(int id, Inventory inventory, MachineRtgFurnaceBlockEntity tedf) {
        super(ModMenuTypes.MACHINE_RTG_FURNACE_MENU.get(), id);
        this.diFurnace = tedf;

        var container = new ModItemStackHandlerContainer(tedf.getInventory(), tedf::setChanged);

        this.addSlot(new Slot(container, 0, 56, 17));
        this.addSlot(new Slot(container, 1, 38, 53));
        this.addSlot(new Slot(container, 2, 56, 53));
        this.addSlot(new Slot(container, 3, 74, 53));
        // Original SlotCraftingOutput
        this.addSlot(new Slot(container, 4, 116, 35) { @Override public boolean mayPlace(ItemStack s) { return false; } });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        }

        this.addDataSlots(new ContainerData() {
            @Override public int get(int index) { return index == 0 ? diFurnace.dualCookTime : 0; }
            @Override public void set(int index, int value) { if (index == 0) diFurnace.dualCookTime = value; }
            @Override public int getCount() { return 1; }
        });
    }

    public static MachineRtgFurnaceMenu create(int id, Inventory inventory, MachineRtgFurnaceBlockEntity blockEntity) {
        return new MachineRtgFurnaceMenu(id, inventory, blockEntity);
    }

    private static MachineRtgFurnaceBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRtgFurnaceBlockEntity furnace) {
            return furnace;
        }
        throw new IllegalStateException("No MachineRtgFurnaceBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":machine_rtg_furnace_menu");
    }

    public MachineRtgFurnaceBlockEntity getBlockEntity() {
        return diFurnace;
    }

    @Override
    public boolean stillValid(Player player) {
        if (diFurnace == null || diFurnace.isRemoved() || diFurnace.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = diFurnace.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Original {@code transferStackInSlot}: Maschine -> Spieler, Spieler -> Slots 0-3. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 4) {
                if (!this.moveItemStackTo(var5, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(var5, 0, 4, false)) {
                return ItemStack.EMPTY;
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
