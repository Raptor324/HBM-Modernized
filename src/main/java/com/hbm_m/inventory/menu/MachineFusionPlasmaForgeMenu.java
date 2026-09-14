package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.fusion.FusionPlasmaForgeBlockEntity;
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
 * 1:1-Port von {@code ContainerMachinePlasmaForge} (1.7.10): Batterie (152/82), Blaupause (35/81),
 * Booster (98/116), 3x4 Eingaben ab (8/18), Ausgabe (116/36), Spielerinventar ab (8/162).
 */
public class MachineFusionPlasmaForgeMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = FusionPlasmaForgeBlockEntity.INVENTORY_SIZE;

    private final FusionPlasmaForgeBlockEntity blockEntity;
    private final Level level;

    public MachineFusionPlasmaForgeMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineFusionPlasmaForgeMenu(int id, Inventory inv, FusionPlasmaForgeBlockEntity blockEntity) {
        super(ModMenuTypes.FUSION_PLASMA_FORGE_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.level = inv.player.level();

        ModItemStackHandlerContainer machineInventory =
                new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(machineInventory, FusionPlasmaForgeBlockEntity.SLOT_BATTERY, 152, 82));
        this.addSlot(new Slot(machineInventory, FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT, 35, 81));
        this.addSlot(new Slot(machineInventory, FusionPlasmaForgeBlockEntity.SLOT_BOOSTER, 98, 116));

        // Original: addSlots(assembler, 3, 8, 18, 3, 4) - drei Reihen zu vier Slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                this.addSlot(new Slot(machineInventory,
                        FusionPlasmaForgeBlockEntity.SLOT_INPUT_FIRST + col + row * 4,
                        8 + col * 18, 18 + row * 18));
            }
        }

        this.addSlot(new Slot(machineInventory, FusionPlasmaForgeBlockEntity.SLOT_OUTPUT, 116, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 162 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 220));
        }
    }

    public static MachineFusionPlasmaForgeMenu create(int id, Inventory inv, FusionPlasmaForgeBlockEntity be) {
        return new MachineFusionPlasmaForgeMenu(id, inv, be);
    }

    private static FusionPlasmaForgeBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockEntity be = inv.player.level().getBlockEntity(data.readBlockPos());
        if (be instanceof FusionPlasmaForgeBlockEntity forge) return forge;
        throw new IllegalStateException("BlockEntity is not a plasma forge");
    }

    public FusionPlasmaForgeBlockEntity getBlockEntity() {
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
            // Original: Blaupause -> Slot 1, Batterie -> Slot 0, alles andere in Booster/Eingaben.
            if (copy.is(ModItems.BLUEPRINT_FOLDER.get())) {
                if (!moveItemStackTo(stack, FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT,
                        FusionPlasmaForgeBlockEntity.SLOT_BLUEPRINT + 1, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, FusionPlasmaForgeBlockEntity.SLOT_BOOSTER,
                    FusionPlasmaForgeBlockEntity.SLOT_OUTPUT, false)) {
                if (!moveItemStackTo(stack, FusionPlasmaForgeBlockEntity.SLOT_BATTERY,
                        FusionPlasmaForgeBlockEntity.SLOT_BATTERY + 1, false)) return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, ModBlocks.PLASMA_FORGE.get());
    }
}
