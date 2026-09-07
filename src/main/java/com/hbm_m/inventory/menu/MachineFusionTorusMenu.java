package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.fusion.FusionTorusBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ModItems;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerFusionTorus} (1.7.10) - Slotpositionen unveraendert uebernommen:
 * Batterie (8/82), Blaupause (71/81), Ausgabe (130/36); Spielerinventar ab (35/162).
 */
public class MachineFusionTorusMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = FusionTorusBlockEntity.INVENTORY_SIZE;

    private final FusionTorusBlockEntity blockEntity;
    private final Level level;

    public MachineFusionTorusMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineFusionTorusMenu(int id, Inventory inv, FusionTorusBlockEntity blockEntity) {
        super(ModMenuTypes.FUSION_TORUS_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineInventory, FusionTorusBlockEntity.SLOT_BATTERY, 8, 82));
        this.addSlot(new Slot(machineInventory, FusionTorusBlockEntity.SLOT_BLUEPRINT, 71, 81));
        this.addSlot(new Slot(machineInventory, FusionTorusBlockEntity.SLOT_OUTPUT, 130, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 35 + j * 18, 162 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 35 + i * 18, 220));
        }
    }

    public static MachineFusionTorusMenu create(int id, Inventory inv, FusionTorusBlockEntity be) {
        return new MachineFusionTorusMenu(id, inv, be);
    }

    private static FusionTorusBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof FusionTorusBlockEntity torus) return torus;
        throw new IllegalStateException("BlockEntity is not a fusion torus");
    }

    public FusionTorusBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Original: Blaupausen in den Blaupausenslot, Batterien in den Batterieslot, sonst nichts.
            if (copy.is(ModItems.BLUEPRINT_FOLDER.get())) {
                if (!moveItemStackTo(stack, FusionTorusBlockEntity.SLOT_BLUEPRINT,
                        FusionTorusBlockEntity.SLOT_BLUEPRINT + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, FusionTorusBlockEntity.SLOT_BATTERY,
                        FusionTorusBlockEntity.SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.TORUS.get());
    }
}
