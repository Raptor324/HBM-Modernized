package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineOreSlopperBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.industrial.ItemMachineUpgrade;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.DummyItemStackHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code ContainerOreSlopper}: Batterie (8,72), Fluid-ID (26,72), Eingang (71,27), sechs Ausgaenge 2x3 ab (134,18),
 * Upgrades (62,72)/(80,72), Spielerinventar ab y 122.
 */
public class MachineOreSlopperMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = MachineOreSlopperBlockEntity.INVENTORY_SIZE;

    private final MachineOreSlopperBlockEntity blockEntity;

    public MachineOreSlopperMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineOreSlopperMenu(int id, Inventory inventory, MachineOreSlopperBlockEntity blockEntity) {
        super(ModMenuTypes.ORE_SLOPPER_MENU.get(), id);
        this.blockEntity = blockEntity;

        // На клиенте тайл может отсутствовать (реплей Flashback) — подставляем пустую заглушку
        var container = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        this.addSlot(new Slot(container, 0, 8, 72));
        this.addSlot(new Slot(container, 1, 26, 72));
        this.addSlot(new Slot(container, 2, 71, 27));
        int[][] out = { {134, 18}, {152, 18}, {134, 36}, {152, 36}, {134, 54}, {152, 54} };
        for (int i = 0; i < 6; i++) {
            this.addSlot(new Slot(container, 3 + i, out[i][0], out[i][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        this.addSlot(new Slot(container, 9, 62, 72));
        this.addSlot(new Slot(container, 10, 80, 72));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 180));
        }
    }

    private static MachineOreSlopperBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineOreSlopperBlockEntity slopper) return slopper;
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineOreSlopperBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":ore_slopper_menu");
    }

    public MachineOreSlopperBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // audit13: Original isUseableByPlayer (<= 128 zur Kernmitte) oder Huelle <= 64; Vanilla 64 schloss die GUI an grossen Maschinen
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    /** Original {@code transferStackInSlot}: Erz, Upgrades, Fluid-IDs und Batterien in ihre Plaetze. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack result = stack.copy();

        if (index <= 10) {
            if (!this.moveItemStackTo(stack, 11, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (result.getItem() == ModItems.BEDROCK_ORE_BASE.get()) {
            if (!this.moveItemStackTo(stack, 2, 3, false)) return ItemStack.EMPTY;
        } else if (result.getItem() instanceof ItemMachineUpgrade) {
            if (!this.moveItemStackTo(stack, 9, 11, false)) return ItemStack.EMPTY;
        } else if (result.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
            if (!this.moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
        } else if (com.hbm_m.api.energy.ItemEnergyAccess.getHbmProvider(result).isPresent()
                || com.hbm_m.api.energy.ItemEnergyAccess.getHbmReceiver(result).isPresent()) {
            if (!this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }
}
