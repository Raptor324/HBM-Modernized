package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.fusion.FusionBreederBlockEntity;
import com.hbm_m.interfaces.IItemFluidIdentifier;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

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
 * 1:1-Port von {@code ContainerFusionBreeder} (1.7.10): Fluid-Identifier (26/72),
 * Eingabe (48/45), Ausgabe (112/45); Spielerinventar ab (8/118).
 */
public class MachineFusionBreederMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = FusionBreederBlockEntity.INVENTORY_SIZE;

    private final FusionBreederBlockEntity blockEntity;
    private final Level level;

    public MachineFusionBreederMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineFusionBreederMenu(int id, Inventory inv, FusionBreederBlockEntity blockEntity) {
        super(ModMenuTypes.FUSION_BREEDER_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineInventory, FusionBreederBlockEntity.SLOT_FLUID_ID, 26, 72));
        this.addSlot(new Slot(machineInventory, FusionBreederBlockEntity.SLOT_INPUT, 48, 45));
        this.addSlot(new Slot(machineInventory, FusionBreederBlockEntity.SLOT_OUTPUT, 112, 45) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 118 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 176));
        }
    }

    public static MachineFusionBreederMenu create(int id, Inventory inv, FusionBreederBlockEntity be) {
        return new MachineFusionBreederMenu(id, inv, be);
    }

    private static FusionBreederBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof FusionBreederBlockEntity breeder) return breeder;
        throw new IllegalStateException("BlockEntity is not a fusion breeder");
    }

    public FusionBreederBlockEntity getBlockEntity() {
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
            // Original: Fluid-Identifier in Slot 0, alles andere in den Eingabeslot.
            if (copy.getItem() instanceof IItemFluidIdentifier) {
                if (!moveItemStackTo(stack, FusionBreederBlockEntity.SLOT_FLUID_ID,
                        FusionBreederBlockEntity.SLOT_FLUID_ID + 1, false)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stack, FusionBreederBlockEntity.SLOT_INPUT,
                        FusionBreederBlockEntity.SLOT_INPUT + 1, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.BREEDER_FUSION.get());
    }
}
