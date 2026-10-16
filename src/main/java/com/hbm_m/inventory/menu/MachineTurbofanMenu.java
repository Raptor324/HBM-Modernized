package com.hbm_m.inventory.menu;

import com.hbm_m.api.energy.ItemEnergyAccess;
import com.hbm_m.blockentity.machines.MachineTurbofanBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
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

/**
 * 1:1-Port der Slot-Koordinaten aus {@code ContainerMachineTurbofan} (1.7.10-Original). Die
 * Upgrade- und Identifier-Slots behalten ihre Original-Koordinaten; die Indizes verschoben
 * (Upgrade = Original-Slot 2 -> hier Slot 4), um alte gespeicherte Inventare nicht zu brechen:
 * <pre>
 * Original-Slot 0 (Treibstoff-Behaelter)     -> hier Slot 0, (17, 17)
 * Original-Slot 1 (Leer-Behaelter, TakeOnly) -> hier Slot 1, (17, 53)
 * Original-Slot 2 (Upgrade AFTERBURN/flame_pony) -> hier Slot 4, (98, 71)
 * Original-Slot 3 (Batterie)                 -> hier Slot 2, (143, 71)
 * Original-Slot 4 (Fluid-Identifier)         -> hier Slot 3, (44, 71)
 * </pre>
 */
public class MachineTurbofanMenu extends AbstractContainerMenu {

    private final MachineTurbofanBlockEntity blockEntity;

    private static final int SLOT_FUEL_CONTAINER = MachineTurbofanBlockEntity.SLOT_FUEL_CONTAINER;
    private static final int SLOT_EMPTY_CONTAINER = MachineTurbofanBlockEntity.SLOT_EMPTY_CONTAINER;
    private static final int SLOT_BATTERY = MachineTurbofanBlockEntity.SLOT_BATTERY;
    private static final int SLOT_FLUID_IDENTIFIER = MachineTurbofanBlockEntity.SLOT_FLUID_IDENTIFIER;
    private static final int SLOT_UPGRADE = MachineTurbofanBlockEntity.SLOT_UPGRADE;
    private static final int MACHINE_SLOT_COUNT = 5;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    public MachineTurbofanMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineTurbofanMenu(int id, Inventory inventory, MachineTurbofanBlockEntity blockEntity) {
        super(ModMenuTypes.TURBOFAN_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, SLOT_FUEL_CONTAINER, 17, 17));
        this.addSlot(new Slot(container, SLOT_EMPTY_CONTAINER, 17, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        this.addSlot(new Slot(container, SLOT_BATTERY, 143, 71));
        this.addSlot(new Slot(container, SLOT_FLUID_IDENTIFIER, 44, 71));
        // Original (ContainerMachineTurbofan): Aufwertungsslot bei 98, 71.
        this.addSlot(new Slot(container, SLOT_UPGRADE, 98, 71));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 179));
        }
    }

    private static MachineTurbofanBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineTurbofanBlockEntity turbofan) {
            return turbofan;
        }
        throw new MenuBlockEntityMissingException("No MachineTurbofanBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":turbofan_menu");
    }

    public MachineTurbofanBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MenuReach.stillValid(player, blockEntity);
    }

    /** Оригинал: ItemMachineUpgrade и flame_pony идут в слот форсажа. */
    private static boolean isAfterburnerUpgrade(ItemStack stack) {
        return stack.getItem() instanceof com.hbm_m.item.industrial.ItemMachineUpgrade
                || stack.is(com.hbm_m.item.ModItems.FLAME_PONY.get());
    }

    private static boolean isEnergySource(ItemStack stack) {
        return ItemEnergyAccess.isEnergySource(stack);
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
                if (isEnergySource(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (isAfterburnerUpgrade(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_UPGRADE, SLOT_UPGRADE + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_IDENTIFIER, SLOT_FLUID_IDENTIFIER + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(slotStack, SLOT_FUEL_CONTAINER, SLOT_FUEL_CONTAINER + 1, false)) {
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
