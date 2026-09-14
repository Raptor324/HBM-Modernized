package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.pneumatic.PneumoStorageExporterBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1-Port von {@code ContainerPneumoStorageExporter} (1.7.10): links die neun
 * Anforderungsvorlagen als 3x3 bei (17, 17), rechts die neun Ausgabeplaetze bei (80, 17), das
 * Spielerinventar auf Hoehe 103.
 *
 * <p>Die Vorlagen behalten ihre <b>Stapelgroesse</b> - sie ist die geforderte Menge. Ein Klick
 * legt die Vorlage samt Menge fest, der Gegenstand bleibt in der Hand.</p>
 */
public class PneumoStorageExporterMenu extends AbstractContainerMenu {

    /** Zwei Schalterzustaende, der Funkbetrieb und neun Filter zu je drei Zahlen. */
    private static final int DATA_COUNT = 3 + PneumoStorageExporterBlockEntity.REQUEST_SLOTS * 3;

    private static final int REQUEST_SLOTS = PneumoStorageExporterBlockEntity.REQUEST_SLOTS;
    private static final int MACHINE_SLOTS = PneumoStorageExporterBlockEntity.INVENTORY_SIZE;

    private final PneumoStorageExporterBlockEntity blockEntity;
    private final ContainerData data;

    public PneumoStorageExporterMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public PneumoStorageExporterMenu(int id, Inventory inv, PneumoStorageExporterBlockEntity blockEntity) {
        super(ModMenuTypes.PNEUMO_STORAGE_EXPORTER_MENU.get(), id);
        this.blockEntity = blockEntity;

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> blockEntity.isContinuousRequest() ? 1 : 0;
                    case 1 -> blockEntity.getRequestMode();
                    case 2 -> blockEntity.isRorConfiguredMode() ? 1 : 0;
                    // Ab 3 die neun Funkfilter zu je drei Zahlen - die Oberflaeche zeigt sie an.
                    default -> index < DATA_COUNT
                            ? blockEntity.getRorFilter((index - 3) / 3)[(index - 3) % 3] : 0;
                };
            }

            @Override
            public void set(int index, int value) { }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        // Anforderungsvorlagen
        for (int i = 0; i < REQUEST_SLOTS; i++) {
            this.addSlot(new Slot(container, i, 17 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        // Ausgabeplaetze - nur entnehmen
        for (int i = 0; i < REQUEST_SLOTS; i++) {
            this.addSlot(new Slot(container, REQUEST_SLOTS + i, 80 + (i % 3) * 18, 17 + (i / 3) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 103 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 161));
        }
    }

    private static PneumoStorageExporterBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PneumoStorageExporterBlockEntity exporter) return exporter;
        throw new IllegalStateException("No PneumoStorageExporterBlockEntity at " + pos);
    }

    public PneumoStorageExporterBlockEntity getBlockEntity() { return blockEntity; }

    public boolean isContinuous() { return data.get(0) != 0; }
    public int getRequestMode()   { return data.get(1); }
    public boolean isRorMode()    { return data.get(2) != 0; }

    /** Gegenstandsnummer, Metadatenzahl und Menge des Funkfilters auf diesem Platz. */
    public int getRorFilter(int slot, int field) { return data.get(3 + slot * 3 + field); }

    /** 1:1-Port von {@code slotClick}: die Vorlage uebernimmt Gegenstand <b>und</b> Menge. */
    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index >= REQUEST_SLOTS) {
            super.clicked(index, button, clickType, player);
            return;
        }

        ItemStack held = getCarried();
        slots.get(index).set(held.isEmpty() ? ItemStack.EMPTY : held.copy());
        blockEntity.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Vorlagen lassen sich nicht umlagern.
        if (index < REQUEST_SLOTS) return ItemStack.EMPTY;

        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
