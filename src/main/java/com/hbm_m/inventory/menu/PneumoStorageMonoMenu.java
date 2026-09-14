package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
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
 * 1:1-Port von {@code ContainerPneumoStorageMono} (1.7.10): drei Vorlagenplaetze uebereinander bei
 * (8, 17), das Spielerinventar auf Hoehe 99.
 *
 * <p>Die drei Plaetze sind <b>Vorlagen</b>: ein Klick legt fest, was in dieses Fach gehoert, der
 * Gegenstand bleibt dabei in der Hand. Der eigentliche Bestand steht als Zahl daneben.</p>
 */
public class PneumoStorageMonoMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = PneumoStorageMonoBlockEntity.INVENTORY_SIZE;

    private final PneumoStorageMonoBlockEntity blockEntity;
    private final ContainerData amounts;

    public PneumoStorageMonoMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, getBlockEntity(inv, extraData));
    }

    public PneumoStorageMonoMenu(int id, Inventory inv, PneumoStorageMonoBlockEntity blockEntity) {
        super(ModMenuTypes.PNEUMO_STORAGE_MONO_MENU.get(), id);
        this.blockEntity = blockEntity;

        // Die Bestaende laufen ueber ContainerData, damit der Bildschirm sie sieht.
        this.amounts = new ContainerData() {
            @Override
            public int get(int index) {
                return blockEntity.getAmounts()[index];
            }

            @Override
            public void set(int index, int value) {
                blockEntity.getAmounts()[index] = value;
            }

            @Override
            public int getCount() {
                return MACHINE_SLOTS;
            }
        };
        addDataSlots(amounts);

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        for (int i = 0; i < MACHINE_SLOTS; i++) {
            this.addSlot(new Slot(container, i, 8, 17 + i * 18) {
                @Override
                public boolean mayPickup(Player player) {
                    return false;
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 99 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, 8 + col * 18, 157));
        }
    }

    private static PneumoStorageMonoBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PneumoStorageMonoBlockEntity storage) return storage;
        throw new IllegalStateException("No PneumoStorageMonoBlockEntity at " + pos);
    }

    public PneumoStorageMonoBlockEntity getBlockEntity() { return blockEntity; }

    public int getAmount(int index) {
        return index >= 0 && index < MACHINE_SLOTS ? amounts.get(index) : 0;
    }

    /** 1:1-Port von {@code slotClick}: der Vorlagenplatz uebernimmt, was in der Hand liegt. */
    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index < 0 || index >= MACHINE_SLOTS) {
            super.clicked(index, button, clickType, player);
            return;
        }

        ItemStack held = getCarried();
        ItemStack template = held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1);
        slots.get(index).set(template);
        blockEntity.setChanged();
    }

    /** Original: {@code transferStackInSlot} gibt null - Umlagern gibt es hier nicht. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) return false;
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }
}
