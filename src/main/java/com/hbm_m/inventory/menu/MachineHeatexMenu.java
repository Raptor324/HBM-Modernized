package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineHeatexBlockEntity;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Port of {@code ContainerHeaterHeatex} (1.7.10 Original).
 * <p>
 * SCOPE-Vereinfachung: Das Original besitzt einen Item-Slot (Index 0, bei 80/72) zur Neuzuweisung des
 * heissen Fluid-Typs per Item ({@code IFluidCopiable}). {@link MachineHeatexBlockEntity} wurde ohne
 * Item-Inventar portiert (0 Slots, Tanks sind fest auf coolant_hot/coolant typisiert) - daher enthaelt
 * dieses Menu nur die Spielerinventar-Slots, 1:1 in den Original-Koordinaten uebernommen.
 */
public class MachineHeatexMenu extends AbstractContainerMenu {

    /** Слоты 0..26 — основной инвентарь, 27..35 — хотбар; машинных слотов у меню нет. */
    private static final int PLAYER_INVENTORY_SLOTS = 27;

    private final MachineHeatexBlockEntity blockEntity;

    public MachineHeatexMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineHeatexMenu(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        super(ModMenuTypes.HEATEX_MENU.get(), id);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    public static MachineHeatexMenu create(int id, Inventory inventory, MachineHeatexBlockEntity blockEntity) {
        return new MachineHeatexMenu(id, inventory, blockEntity);
    }

    private static MachineHeatexBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineHeatexBlockEntity heatexBlockEntity) {
            return heatexBlockEntity;
        }
        throw new IllegalStateException("No MachineHeatexBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":heatex_menu");
    }

    public MachineHeatexBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Keine Maschinen-Slots vorhanden (siehe Klassenkommentar) - Shift-Klick verhaelt sich wie im
        // Original fuer alle Nicht-Slot-0-Faelle: nur innerhalb des Spielerinventars zusammenfuehren.
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            // Диапазон обязан исключать сам слот-источник: у этого меню нет машинных слотов, и
            // [0, slots.size()) включало его самого. При слиянии moveItemStackTo сравнивает стек
            // сам с собой, делает setCount(0) и следом setCount(count * 2) — стек удваивался.
            if (index < PLAYER_INVENTORY_SLOTS) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INVENTORY_SLOTS, this.slots.size(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, false)) {
                return ItemStack.EMPTY;
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
