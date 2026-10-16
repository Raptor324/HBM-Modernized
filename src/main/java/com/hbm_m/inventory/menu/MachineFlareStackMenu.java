package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineFlareStackBlockEntity;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Порт {@code ContainerMachineGasFlare} (1.7.10): Battery (143,71), Fluid in (17,17),
 * Fluid out = take-only (17,53), Fluid-ID (35,71), Upgrades (80,71)/(98,71);
 * инвентарь игрока на 121/179.
 */
public class MachineFlareStackMenu extends AbstractContainerMenu {

    public static final int MACHINE_SLOT_COUNT = MachineFlareStackBlockEntity.INVENTORY_SIZE;

    private static final int SLOT_BATTERY = MachineFlareStackBlockEntity.SLOT_BATTERY;
    private static final int SLOT_FLUID_IN = MachineFlareStackBlockEntity.SLOT_FLUID_IN;
    private static final int SLOT_FLUID_OUT = MachineFlareStackBlockEntity.SLOT_FLUID_OUT;
    private static final int SLOT_FLUID_ID = MachineFlareStackBlockEntity.SLOT_FLUID_ID;
    private static final int SLOT_UPGRADE_1 = MachineFlareStackBlockEntity.SLOT_UPGRADE_1;
    private static final int SLOT_UPGRADE_2 = MachineFlareStackBlockEntity.SLOT_UPGRADE_2;

    private final MachineFlareStackBlockEntity blockEntity;

    public MachineFlareStackMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineFlareStackMenu(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        super(ModMenuTypes.FLARE_STACK_MENU.get(), id);
        this.blockEntity = blockEntity;

        // null-BE возможен только в реплее Flashback — рендерим заглушку без машины.
        var handler = blockEntity != null
                ? blockEntity.getInventory()
                : new com.hbm_m.platform.DummyItemStackHandler(MACHINE_SLOT_COUNT);
        var container = new com.hbm_m.inventory.ModItemStackHandlerContainer(handler,
                blockEntity != null ? blockEntity::setChanged : () -> {});

        // Battery
        this.addSlot(new Slot(container, SLOT_BATTERY, 143, 71));
        // Fluid in
        this.addSlot(new Slot(container, SLOT_FLUID_IN, 17, 17));
        // Fluid out — SlotTakeOnly: класть нельзя, забирать можно
        this.addSlot(new SlotTakeOnly(container, SLOT_FLUID_OUT, 17, 53));
        // Fluid ID
        this.addSlot(new Slot(container, SLOT_FLUID_ID, 35, 71));
        // Upgrades
        this.addSlot(new Slot(container, SLOT_UPGRADE_1, 80, 71));
        this.addSlot(new Slot(container, SLOT_UPGRADE_2, 98, 71));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 179));
        }
    }

    public static MachineFlareStackMenu create(int id, Inventory inventory, MachineFlareStackBlockEntity blockEntity) {
        return new MachineFlareStackMenu(id, inventory, blockEntity);
    }

    private static MachineFlareStackBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineFlareStackBlockEntity flareStackBlockEntity) {
            return flareStackBlockEntity;
        }
        // На клиенте тайл может отсутствовать (реплей Flashback) — не крашим пакет, возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inventory.player.level().isClientSide) {
            return null;
        }
        throw new IllegalStateException("No MachineFlareStackBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":flare_stack_menu");
    }

    public MachineFlareStackBlockEntity getBlockEntity() {
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

    /**
     * Порт {@code ContainerMachineGasFlare.transferStackInSlot}: слоты машины → инвентарь;
     * из инвентаря: fluid identifier → слот 3, батарея → слот 0, апгрейд → слоты 4/5,
     * остальное → канистры (слоты 1/2).
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (slotStack.getItem() instanceof FluidIdentifierItem) {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_ID, SLOT_FLUID_ID + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (blockEntity != null && blockEntity.acceptsBattery(slotStack)) {
                    if (!this.moveItemStackTo(slotStack, SLOT_BATTERY, SLOT_BATTERY + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slotStack.getItem() instanceof ItemMachineUpgrade) {
                    if (!this.moveItemStackTo(slotStack, SLOT_UPGRADE_1, SLOT_UPGRADE_2 + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(slotStack, SLOT_FLUID_IN, SLOT_FLUID_OUT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
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

    /** Порт {@code SlotTakeOnly}: mayPlace = false, извлечение разрешено. */
    private static class SlotTakeOnly extends Slot {
        public SlotTakeOnly(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
