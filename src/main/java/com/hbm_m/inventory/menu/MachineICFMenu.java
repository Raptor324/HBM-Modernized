package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.icf.MachineICFBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerICF} (1.7.10). Oben die fuenf Vorratsplaetze bei (80, 18), darunter
 * die Kammer bei (116, 54), unten die fuenf Ausgabeplaetze bei (80, 90) und links davon der Platz
 * fuer die Fluessigkeitskennung bei (44, 90). Das Spielerinventar sitzt bei (44, 140).
 */
public class MachineICFMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = MachineICFBlockEntity.INVENTORY_SIZE;

    private final MachineICFBlockEntity blockEntity;
    private final ContainerData data;

    public MachineICFMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public MachineICFMenu(int id, Inventory inv, MachineICFBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_ICF_MENU.get(), id);
        this.blockEntity = blockEntity;
        this.data = blockEntity.getContainerData();

        checkContainerDataCount(data, 5);
        addDataSlots(data);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < 5; i++) {
            this.addSlot(new Slot(container, MachineICFBlockEntity.SLOT_INPUT_START + i, 80 + i * 18, 18));
        }
        this.addSlot(new Slot(container, MachineICFBlockEntity.SLOT_CHAMBER, 116, 54));
        for (int i = 0; i < 5; i++) {
            // Ausgabeplaetze: nur entnehmen, nicht befuellen.
            this.addSlot(new Slot(container, MachineICFBlockEntity.SLOT_OUTPUT_START + i, 80 + i * 18, 90) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        this.addSlot(new Slot(container, MachineICFBlockEntity.SLOT_FLUID_ID, 44, 90));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 44 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 44 + col * 18, 198));
        }
    }

    private static MachineICFBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof MachineICFBlockEntity icf) return icf;
        throw new IllegalStateException("No MachineICFBlockEntity at " + pos);
    }

    public MachineICFBlockEntity getBlockEntity() { return blockEntity; }

    /** Laserleistung in Promille des Hoechstwerts - die Rohwerte sprengen den int-Bereich. */
    public int getLaserPermille() { return data.get(0); }
    public int getHeatPermille()  { return data.get(1); }
    public int getConsumption()   { return data.get(2); }
    public int getOutput()        { return data.get(3); }
    public int getFlux()          { return data.get(4); }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            // Kapseln wandern in den Vorrat, alles andere bleibt liegen.
            if (!moveItemStackTo(stack, MachineICFBlockEntity.SLOT_INPUT_START,
                    MachineICFBlockEntity.SLOT_INPUT_END + 1, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 256.0D;
    }
}
