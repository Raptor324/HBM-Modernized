package com.hbm_m.inventory.menu.radio;

import com.hbm_m.blockentity.network.radio.RadioTorchCounterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.inventory.menu.ModMenuTypes;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerCounterTorch}: drei Musterplaetze (Geisterplaetze, 138/18+44*i) - ein Klick kopiert den Stapel in
 * der Hand, ein Rechtsklick auf einen belegten Platz schaltet den Vergleichsmodus weiter. Kein Shift-Klick.
 */
public class RadioTorchCounterMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = RadioTorchCounterBlockEntity.SLOT_COUNT;

    private final RadioTorchCounterBlockEntity blockEntity;

    public RadioTorchCounterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public RadioTorchCounterMenu(int id, Inventory inventory, RadioTorchCounterBlockEntity blockEntity) {
        super(ModMenuTypes.RADIO_TORCH_COUNTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < MACHINE_SLOT_COUNT; i++) {
            this.addSlot(new Slot(container, i, 138, 18 + 44 * i));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 12 + col * 18, 156 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 12 + col * 18, 214));
        }
    }

    public static RadioTorchCounterMenu create(int id, Inventory inventory, RadioTorchCounterBlockEntity blockEntity) {
        return new RadioTorchCounterMenu(id, inventory, blockEntity);
    }

    private static RadioTorchCounterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof RadioTorchCounterBlockEntity counterBlockEntity) {
            return counterBlockEntity;
        }
        throw new IllegalStateException("No RadioTorchCounterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":radio_torch_counter_menu");
    }

    public RadioTorchCounterBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index > 2) {
            super.clicked(index, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(index);

        if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) {
            if (!player.level().isClientSide) {
                blockEntity.nextFilterMode(index);
                syncToClient();
            }
            return;
        }

        slot.set(this.getCarried().copy());
        blockEntity.getMatcher().initPattern(index, slot.getItem());
        if (!player.level().isClientSide) syncToClient();
    }

    private void syncToClient() {
        if (blockEntity.getLevel() != null) {
            blockEntity.getLevel().sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }
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
        return ItemStack.EMPTY;
    }
}
