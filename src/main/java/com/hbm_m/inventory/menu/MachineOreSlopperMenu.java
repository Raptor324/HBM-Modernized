package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MachineOreSlopperMenu extends AbstractContainerMenu {

    private final MachineOreSlopperBlockEntity blockEntity;

    private static final int SLOT_INPUT = MachineOreSlopperBlockEntity.SLOT_INPUT;
    private static final int OUTPUT_START = 1;
    private static final int MACHINE_SLOT_COUNT = 8; // 1 Eingang + 6 Ausgaenge + 1 Batterie
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineOreSlopperMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOreSlopperMenu(int id, Inventory inventory, MachineOreSlopperBlockEntity blockEntity) {
        super(ModMenuTypes.ORE_SLOPPER_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку,
        // чтобы конструктор дошёл до конца и пакет открытия меню не уронил клиент
        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        // Eingangsslot (bedrock_ore_base) - links im Panel, ueber dem Wassertank-Bereich.
        this.addSlot(new Slot(container, SLOT_INPUT, 44, 37));

        // 6 Ausgabeslots (LIGHT, HEAVY, RARE, ACTINIDE, NONMETAL, CRYSTAL) - 3x2-Raster rechts.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 3; col++) {
                int index = OUTPUT_START + row * 3 + col;
                this.addSlot(new Slot(container, index, 98 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Nur Entnahme - wird von der Maschine befuellt.
                    }
                });
            }
        }

        // Batterie-Slot - unterhalb der Ausgabe-Slots.
        this.addSlot(new Slot(container, MachineOreSlopperBlockEntity.SLOT_BATTERY, 152, 37) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ItemEnergyAccess.isEnergySource(stack);
            }
        });

        int playerInvX = 8;
        int playerInvY = 124;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, playerInvX + col * 18, playerInvY + row * 18));
            }
        }
        int hotbarY = playerInvY + 58;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, playerInvX + col * 18, hotbarY));
        }
    }

    private static MachineOreSlopperBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOreSlopperBlockEntity oreSlopper) {
            return oreSlopper;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineOreSlopperBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":ore_slopper_menu");
    }

    public MachineOreSlopperBlockEntity getBlockEntity() {
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
                boolean isEnergySource = ItemEnergyAccess.isEnergySource(slotStack);
                if (isEnergySource) {
                    if (!this.moveItemStackTo(slotStack, MachineOreSlopperBlockEntity.SLOT_BATTERY, MachineOreSlopperBlockEntity.SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_INPUT, SLOT_INPUT + 1, false)) {
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