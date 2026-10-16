package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineOilburnerBlockEntity;
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
 * Port of {@code ContainerOilburner} (1.7.10 Original).
 * <p>
 * SCOPE-Vereinfachung: Das Original hatte 3 Item-Slots (Fluessig-Container rein/raus bei
 * (26,17)/(26,53) + Fluid-ID-Neuzuweisung bei (44,71)). {@link MachineOilburnerBlockEntity}
 * hat - wie im Original-Kommentar dort beschrieben - KEIN Inventar (0 Slots, siehe
 * {@code BaseMachineBlockEntity}-Konstruktoraufruf mit {@code inventorySize = 0}); Befuellen
 * laeuft ausschliesslich ueber die Fluid-Capability (Eimer/Rohr). Daher enthaelt dieses Menu
 * nur die Spielerinventar-Slots, keine Machine-Slots - analog zu {@link MachineFlareStackMenu},
 * dessen BlockEntity ebenfalls kein Inventar hat.
 */
public class MachineOilburnerMenu extends AbstractContainerMenu {

    private final MachineOilburnerBlockEntity blockEntity;

    public MachineOilburnerMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOilburnerMenu(int id, Inventory inventory, MachineOilburnerBlockEntity blockEntity) {
        super(ModMenuTypes.OILBURNER_MENU.get(), id);
        this.blockEntity = blockEntity;

        // Player inventory, ported 1:1 from ContainerOilburner (offset = 37).
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 121 + row * 18));
            }
        }

        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 179));
        }
    }

    public static MachineOilburnerMenu create(int id, Inventory inventory, MachineOilburnerBlockEntity blockEntity) {
        return new MachineOilburnerMenu(id, inventory, blockEntity);
    }

    private static MachineOilburnerBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOilburnerBlockEntity oilburnerBlockEntity) {
            return oilburnerBlockEntity;
        }
        throw new IllegalStateException("No MachineOilburnerBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":oilburner_menu");
    }

    public MachineOilburnerBlockEntity getBlockEntity() {
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

    // No machine slots exist (see class javadoc), so there is nothing to shift-click into/out of.
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
