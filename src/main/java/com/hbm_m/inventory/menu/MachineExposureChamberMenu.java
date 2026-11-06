package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineExposureChamberBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

/** Slot-Koordinaten 1:1 aus {@code ContainerMachineExposureChamber} (1.7.10 Original): Partikel
 *  (8,18), Behaelter (8,54, nur Entnahme), Ingredient (80,36), Output (116,36, extraktionsonly),
 *  Batterie (152,54), Upgrade (44,54)/(62,54). */
public class MachineExposureChamberMenu extends AbstractContainerMenu {

    private final MachineExposureChamberBlockEntity blockEntity;
    private static final int MACHINE_SLOTS = 7;
    /** Menue-Index der Upgrade-Slots (Menue-Reihenfolge, nicht BE-Slot-Index). */
    private static final int MENU_SLOT_UPGRADE_FIRST = 5;
    private static final int MENU_SLOT_UPGRADE_LAST = 7;
    /** Menue-Index des Batterie-Slots. */
    private static final int MENU_SLOT_BATTERY = 4;

    public MachineExposureChamberMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, getBlockEntity(inv, buf));
    }

    public MachineExposureChamberMenu(int id, Inventory inv, MachineExposureChamberBlockEntity be) {
        super(ModMenuTypes.EXPOSURE_CHAMBER_MENU.get(), id);
        this.blockEntity = be;

        var handler = be.getInventory();
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_PARTICLE, 8, 18));
        // Behaelter-Kapsel: nur Entnahme, wie im Original (TakeOnly)
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_CONTAINER, 8, 54) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_INGREDIENT, 80, 36));
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_OUTPUT, 116, 36) {
            @Override public boolean mayPlace(ItemStack s) { return false; }
        });
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_BATTERY, 152, 54));
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_UPGRADE_1, 44, 54));
        addSlot(new SlotItemHandler(handler, MachineExposureChamberBlockEntity.SLOT_UPGRADE_2, 62, 54));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 162));
        }
    }

    public static MachineExposureChamberMenu create(int id, Inventory inv, MachineExposureChamberBlockEntity be) {
        return new MachineExposureChamberMenu(id, inv, be);
    }

    private static MachineExposureChamberBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineExposureChamberBlockEntity c) return c;
        throw new MenuBlockEntityMissingException("No MachineExposureChamberBlockEntity at " + pos);
    }

    public MachineExposureChamberBlockEntity getBlockEntity() { return blockEntity; }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // FFA-логика: апгрейды → слоты 5..6, батарейки → слот батареи, остальное → входы
            if (stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade) {
                if (!moveItemStackTo(stack, MENU_SLOT_UPGRADE_FIRST, MENU_SLOT_UPGRADE_LAST, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() instanceof com.hbm_m.api.item.IBatteryItem
                    || stack.getItem() instanceof com.hbm_m.item.fekal_electric.ItemCreativeBattery) {
                if (!moveItemStackTo(stack, MENU_SLOT_BATTERY, MENU_SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, 0, MENU_SLOT_BATTERY, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
