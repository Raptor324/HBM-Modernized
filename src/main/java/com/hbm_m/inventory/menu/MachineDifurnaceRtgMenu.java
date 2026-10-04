package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerMachineDiFurnaceRTG}: Eingaenge (80,18)/(80,54), Ausgang (134,36), Pellets 2x3 ab (22,18),
 * Spielerinventar ab y 84. Rechtsklick mit leerer Hand auf einen leeren Eingang dreht dessen Eingabeseite weiter.
 */
public class MachineDifurnaceRtgMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineDifurnaceRtgBlockEntity.INVENTORY_SIZE;
    private static final int PLAYER_SLOT_START = MACHINE_SLOT_COUNT;

    private final MachineDifurnaceRtgBlockEntity blockEntity;

    public MachineDifurnaceRtgMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineDifurnaceRtgMenu(int id, Inventory inventory, MachineDifurnaceRtgBlockEntity blockEntity) {
        super(ModMenuTypes.MACHINE_DIFURNACE_RTG_MENU.get(), id);
        this.blockEntity = blockEntity;

        var container = new ModItemStackHandlerContainer(blockEntity.getInventory(), blockEntity::setChanged);

        this.addSlot(new Slot(container, 0, 80, 18));
        this.addSlot(new Slot(container, 1, 80, 54));
        this.addSlot(new Slot(container, 2, 134, 36) { @Override public boolean mayPlace(ItemStack s) { return false; } });
        int[][] pellets = { {22, 18}, {40, 18}, {22, 36}, {40, 36}, {22, 54}, {40, 54} };
        for (int i = 0; i < 6; i++) this.addSlot(new Slot(container, 3 + i, pellets[i][0], pellets[i][1]));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static MachineDifurnaceRtgMenu create(int id, Inventory inventory, MachineDifurnaceRtgBlockEntity blockEntity) {
        return new MachineDifurnaceRtgMenu(id, inventory, blockEntity);
    }

    private static MachineDifurnaceRtgBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineDifurnaceRtgBlockEntity difurnace) {
            return difurnace;
        }
        throw new IllegalStateException("No MachineDifurnaceRtgBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":machine_difurnace_rtg_menu");
    }

    public MachineDifurnaceRtgBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public void clicked(int index, int button, ClickType clickType, Player player) {
        if (index >= 0 && index < 2 && button == 1 && clickType == ClickType.PICKUP) {
            Slot slot = this.getSlot(index);
            if (!slot.hasItem() && getCarried().isEmpty()) {
                if (!player.level().isClientSide) blockEntity.cycleSide(index);
                return;
            }
        }
        super.clicked(index, button, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (blockEntity == null || blockEntity.getLevel() != player.level()) {
            return false;
        }
        BlockPos pos = blockEntity.getBlockPos();
        return player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= 64.0D;
    }

    /** Original {@code transferStackInSlot}: Pellets in die Pelletplaetze, alles andere in die Eingaenge. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        if (index <= 8) {
            if (!this.moveItemStackTo(stack, PLAYER_SLOT_START, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (stack.getItem() instanceof ItemRTGPellet) {
            if (!this.moveItemStackTo(stack, 3, 9, false)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, 3, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, stack);
        return result;
    }
}
