package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineHydrotreaterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Slot-Layout 1:1 aus {@code ContainerMachineHydrotreater} (1.7.10 Original): 11 Maschinenslots in Original-Reihenfolge,
 *  Ein-/Ausgabe nur entnehmbar ueber die Slotpruefung des BE. */
public class MachineHydrotreaterMenu extends AbstractContainerMenu {

    private final MachineHydrotreaterBlockEntity blockEntity;
    private static final int SLOT_BATTERY = MachineHydrotreaterBlockEntity.SLOT_BATTERY;
    private static final int SLOT_CATALYST = MachineHydrotreaterBlockEntity.SLOT_CATALYST;
    private static final int SLOT_FLUID_ID = MachineHydrotreaterBlockEntity.SLOT_FLUID_ID;
    private static final int MACHINE_SLOT_COUNT = MachineHydrotreaterBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineHydrotreaterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineHydrotreaterMenu(int id, Inventory inventory, MachineHydrotreaterBlockEntity blockEntity) {
        super(ModMenuTypes.HYDROTREATER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);
        this.addSlot(new Slot(container, SLOT_BATTERY, 17, 90));
        this.addSlot(new Slot(container, 1, 35, 90));   // Kanister Eingang
        this.addSlot(new Slot(container, 2, 35, 108));  // Kanister Ausgang (nur entnehmbar)
        this.addSlot(new DeprecatedSlot(container, 3, 53, 90));  // Wasserstoff ein (stillgelegt, braucht Druck)
        this.addSlot(new DeprecatedSlot(container, 4, 53, 108)); // Wasserstoff aus (ebenso)
        this.addSlot(new Slot(container, 5, 125, 90));  // entschwefeltes Oel Eingang
        this.addSlot(new Slot(container, 6, 125, 108)); // entschwefeltes Oel Ausgang
        this.addSlot(new Slot(container, 7, 143, 90));  // Sauergas Eingang
        this.addSlot(new Slot(container, 8, 143, 108)); // Sauergas Ausgang
        this.addSlot(new Slot(container, SLOT_FLUID_ID, 17, 108));
        this.addSlot(new Slot(container, SLOT_CATALYST, 89, 36));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 156 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 214));
        }
    }

    public static MachineHydrotreaterMenu create(int id, Inventory inventory, MachineHydrotreaterBlockEntity blockEntity) {
        return new MachineHydrotreaterMenu(id, inventory, blockEntity);
    }

    private static MachineHydrotreaterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineHydrotreaterBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineHydrotreaterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":hydrotreater_menu");
    }

    public MachineHydrotreaterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    /** Original {@code SlotDeprecated}: haelt Altbestand, sonst weder sichtbar noch bedienbar. */
    private static final class DeprecatedSlot extends Slot {
        DeprecatedSlot(net.minecraft.world.Container c, int id, int x, int y) { super(c, id, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return false; }
        @Override public boolean isActive() { return false; }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Original transferStackInSlot: Batterie -> 0, Fluidkennung -> 9, Katalysator -> 10, sonst 1/3/5/7
                if (com.hbm_m.api.energy.ItemEnergyAccess.isEnergySource(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
                } else if (slotStack.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) return ItemStack.EMPTY;
                } else if (slotStack.is(com.hbm_m.item.ModItems.CATALYTIC_CONVERTER.get())) {
                    if (!this.moveItemStackTo(slotStack, SLOT_CATALYST, SLOT_CATALYST + 1, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false))
                        if (!this.moveItemStackTo(slotStack, 3, 4, false))
                            if (!this.moveItemStackTo(slotStack, 5, 6, false))
                                if (!this.moveItemStackTo(slotStack, 7, 8, false))
                                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            if (slotStack.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
