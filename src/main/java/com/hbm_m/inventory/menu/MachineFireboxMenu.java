package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFireboxBaseBlockEntity;
import com.hbm_m.blockentity.machines.MachineFireboxBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerFirebox} (Feuerbuechse und Heizofen): oeffnet beim Bau die Klappe, schliesst sie beim Schliessen. */
public class MachineFireboxMenu extends AbstractContainerMenu {

    protected final MachineFireboxBaseBlockEntity firebox;
    private final ContainerData data;

    public MachineFireboxMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), id, inventory, lookup(inventory, extraData), new SimpleContainerData(6));
    }

    public static MachineFireboxMenu create(int id, Inventory inventory, MachineFireboxBlockEntity blockEntity) {
        return new MachineFireboxMenu(ModMenuTypes.MACHINE_FIREBOX_MENU.get(), id, inventory, blockEntity, blockEntity.getData());
    }

    protected MachineFireboxMenu(MenuType<?> type, int id, Inventory invPlayer, MachineFireboxBaseBlockEntity furnace, ContainerData data) {
        super(type, id);
        this.firebox = furnace;
        this.data = data;
        this.firebox.openInventory();

        var container = new ModItemStackHandlerContainer(furnace.getInventory(), furnace::setChanged);
        this.addSlot(new Slot(container, 0, 44, 27));
        this.addSlot(new Slot(container, 1, 62, 27));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(invPlayer, j + i * 9 + 9, 8 + j * 18, 86 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(invPlayer, i, 8 + i * 18, 144));
        }

        addDataSlots(data);
    }

    protected static MachineFireboxBaseBlockEntity lookup(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof MachineFireboxBaseBlockEntity be) return be;
        throw new IllegalStateException("No firebox at " + pos);
    }

    public MachineFireboxBaseBlockEntity getBlockEntity() {
        return firebox;
    }

    public int getHeatEnergy() { return data.get(0); }
    public int getMaxHeat() { return data.get(1); }
    public int getBurnHeat() { return data.get(2); }
    public int getBurnTime() { return data.get(3); }
    public int getMaxBurnTime() { return data.get(4); }
    public boolean wasOn() { return data.get(5) != 0; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack originalStack = slot.getItem();
            stack = originalStack.copy();

            if (index <= 1) {
                if (!this.moveItemStackTo(originalStack, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(originalStack, stack);

            } else if (!this.moveItemStackTo(originalStack, 0, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return stack;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(firebox, player, 128.0D);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.firebox.closeInventory();
    }
}
