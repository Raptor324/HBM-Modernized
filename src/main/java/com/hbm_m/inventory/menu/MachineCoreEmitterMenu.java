package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCoreEmitterBlockEntity;
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
 * Port von {@code ContainerCoreEmitter} (1.7.10). Das Original besitzt ausschliesslich
 * Spieler-Inventar-Slots (kein Maschinen-Slot) - {@code transferStackInSlot} ist im Original
 * faktisch ein No-Op (gibt immer {@code null}/nichts zurueck). Die modernisierte
 * {@link MachineCoreEmitterBlockEntity} besitzt ebenfalls keine nutzbaren Item-Slots
 * ({@code isItemValidForSlot} liefert immer {@code false}), daher 1:1-Port ohne Maschinen-Slots.
 */
public class MachineCoreEmitterMenu extends AbstractContainerMenu {

    private final MachineCoreEmitterBlockEntity blockEntity;

    public MachineCoreEmitterMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCoreEmitterMenu(int id, Inventory inventory, MachineCoreEmitterBlockEntity blockEntity) {
        super(ModMenuTypes.CORE_EMITTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        // Original: 3x9 Spieler-Hauptinventar bei y=84, dann Hotbar bei y=142 (kein Maschinen-Slot).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineCoreEmitterMenu create(int id, Inventory inventory, MachineCoreEmitterBlockEntity blockEntity) {
        return new MachineCoreEmitterMenu(id, inventory, blockEntity);
    }

    private static MachineCoreEmitterBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCoreEmitterBlockEntity coreEmitterBlockEntity) {
            return coreEmitterBlockEntity;
        }
        throw new IllegalStateException("No MachineCoreEmitterBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":core_emitter_menu");
    }

    public MachineCoreEmitterBlockEntity getBlockEntity() {
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
        // Original transferStackInSlot ist ein No-Op (kein Maschinen-Slot zum Verschieben).
        return ItemStack.EMPTY;
    }
}
