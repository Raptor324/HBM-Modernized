package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCoreInjectorBlockEntity;
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
 * Port von {@code ContainerCoreInjector} (1.7.10 Original). Das Original hatte 4 Slots
 * (2x Crafting-In/Out-Paare bei 26/17, 26/53, 134/17, 134/53) fuer eine Fusionsbrennstab-
 * Craftingrezeptur (SlotCraftingOutput). Dieser Port des BlockEntity implementiert diese
 * Crafting-Logik (noch) nicht - {@code isItemValidForSlot} liefert dort unbedingt
 * {@code false}, es gibt also keine funktionalen Item-Slots zu verkabeln. Das Menu zeigt
 * daher nur die beiden Fluid-Tanks (Deuterium/Tritium, per Rohr/Pumpe befuellbar ueber
 * {@link com.hbm_m.blockentity.machines.CoreInjectorFluidHandler}) sowie das Spieler-
 * inventar - 1:1 aus dem Original uebernommene Koordinaten fuer Letzteres.
 */
public class MachineCoreInjectorMenu extends AbstractContainerMenu {

    private final MachineCoreInjectorBlockEntity blockEntity;

    public MachineCoreInjectorMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCoreInjectorMenu(int id, Inventory inventory, MachineCoreInjectorBlockEntity blockEntity) {
        super(ModMenuTypes.CORE_INJECTOR_MENU.get(), id);
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

    public static MachineCoreInjectorMenu create(int id, Inventory inventory, MachineCoreInjectorBlockEntity blockEntity) {
        return new MachineCoreInjectorMenu(id, inventory, blockEntity);
    }

    private static MachineCoreInjectorBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCoreInjectorBlockEntity be) {
            return be;
        }
        throw new IllegalStateException("No MachineCoreInjectorBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":core_injector_menu");
    }

    public MachineCoreInjectorBlockEntity getBlockEntity() {
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
        // Keine funktionalen Item-Slots vorhanden (nur Spielerinventar) - Original-Verhalten
        // fuer die Nicht-Crafting-Slots war ohnehin "return null" (kein Transfer moeglich).
        return ItemStack.EMPTY;
    }
}
