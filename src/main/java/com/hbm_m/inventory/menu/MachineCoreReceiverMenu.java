package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCoreReceiverBlockEntity;
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
 * Port von {@code ContainerCoreReceiver} (1.7.10 Original). Das Original besitzt keinerlei
 * maschinen-eigene Slots (der Core Receiver hat keine ItemHandler-Interaktion, nur einen Laser-
 * Energieempfang und einen Cryogel-Tank) - nur Spielerinventar + Hotbar werden registriert, mit
 * exakt den Original-Koordinaten (Inventar bei y=84, Hotbar bei y=142).
 * <p>
 * Der modernisierte {@link MachineCoreReceiverBlockEntity} besitzt zwar (geerbt von
 * {@code BaseMachineBlockEntity}) einen 4-Slot-ItemHandler, dieser wird aber ueberall als ungueltig
 * markiert ({@code isItemValidForSlot} liefert immer {@code false}) und im Original existiert kein
 * Aequivalent dazu - es werden daher bewusst keine Slots dafuer angelegt, um 1:1 zum Original zu bleiben.
 */
public class MachineCoreReceiverMenu extends AbstractContainerMenu {

    private final MachineCoreReceiverBlockEntity blockEntity;

    public MachineCoreReceiverMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCoreReceiverMenu(int id, Inventory inventory, MachineCoreReceiverBlockEntity blockEntity) {
        super(ModMenuTypes.CORE_RECEIVER_MENU.get(), id);
        this.blockEntity = blockEntity;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineCoreReceiverMenu create(int id, Inventory inventory, MachineCoreReceiverBlockEntity blockEntity) {
        return new MachineCoreReceiverMenu(id, inventory, blockEntity);
    }

    private static MachineCoreReceiverBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCoreReceiverBlockEntity coreReceiverBlockEntity) {
            return coreReceiverBlockEntity;
        }
        throw new IllegalStateException("No MachineCoreReceiverBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":core_receiver_menu");
    }

    public MachineCoreReceiverBlockEntity getBlockEntity() {
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
        // Original: transferStackInSlot liefert immer null - es gibt keine maschinen-eigenen Slots,
        // in/aus denen verschoben werden koennte.
        return ItemStack.EMPTY;
    }
}
