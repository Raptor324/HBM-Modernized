package com.hbm_m.inventory.menu;
import com.hbm_m.blockentity.machines.MachineRockMillBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.industrial.ItemBlueprints;
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
 * 1:1 {@code ContainerMachineRockMill}: Batterie (152,91), Ordner (35,90), drei Eingaenge ab (8,27), drei
 * Ausgaenge ab (80,27), Spielerinventar ab y 138.
 */
public class MachineRockMillMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineRockMillBlockEntity.SLOT_COUNT;
    private static final int PLAYER_INV_START = MACHINE_SLOT_COUNT;
    private static final int PLAYER_INV_END = MACHINE_SLOT_COUNT + 36;

    private final MachineRockMillBlockEntity blockEntity;

    public MachineRockMillMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineRockMillMenu(int id, Inventory inventory, MachineRockMillBlockEntity blockEntity) {
        super(ModMenuTypes.ROCKMILL_MENU.get(), id);
        this.blockEntity = blockEntity;

        var handler = blockEntity.getInventory();
        var container = new ModItemStackHandlerContainer(handler, blockEntity::setChanged);

        // Batterie
        this.addSlot(new Slot(container, 0, 152, 91));
        // Blueprint-Ordner
        this.addSlot(new Slot(container, 1, 35, 90) {
            @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(1, stack); }
        });
        // Eingaenge
        for (int i = 0; i < 3; i++) {
            final int s = 2 + i;
            this.addSlot(new Slot(container, s, 8 + i * 18, 27) {
                @Override public boolean mayPlace(ItemStack stack) { return handler.isItemValid(s, stack); }
            });
        }
        // Ausgaenge
        for (int i = 0; i < 3; i++) {
            this.addSlot(new Slot(container, 5 + i, 80 + i * 18, 27) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 138 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 138 + 58));
        }
    }

    private static MachineRockMillBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineRockMillBlockEntity mill) {
            return mill;
        }
        throw new IllegalStateException("No MachineRockMillBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":rockmill_menu");
    }

    public MachineRockMillBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    /** Original {@code transferStackInSlot}: Batterie, Ordner, sonst Eingaenge. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            result = slotStack.copy();

            if (index < MACHINE_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) return ItemStack.EMPTY;
            } else {
                boolean battery = com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(slotStack).isPresent()
                        || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(slotStack).isPresent();
                if (battery) {
                    if (!this.moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
                } else if (slotStack.getItem() instanceof ItemBlueprints) {
                    if (!this.moveItemStackTo(slotStack, 1, 2, false)) return ItemStack.EMPTY;
                } else {
                    if (!this.moveItemStackTo(slotStack, 2, 5, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();

            if (slotStack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, slotStack);
        }
        return result;
    }
}
