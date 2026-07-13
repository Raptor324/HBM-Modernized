package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MissileAssemblyBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class MissileAssemblyMenu extends AbstractContainerMenu {

    public final MissileAssemblyBlockEntity blockEntity;

    private static final int PLAYER_INV_START = 6;
    private static final int PLAYER_INV_END = 42;

    public MissileAssemblyMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (MissileAssemblyBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public MissileAssemblyMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), id);

        this.blockEntity = (MissileAssemblyBlockEntity) entity;

        var container = new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged);

        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_CHIP, 8, 36));
        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_WARHEAD, 26, 36));
        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_FUSELAGE, 44, 36));
        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_FINS, 62, 36));
        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_THRUSTER, 80, 36));
        this.addSlot(new Slot(container, MissileAssemblyBlockEntity.SLOT_OUTPUT, 152, 36) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public boolean canBuild() {
        return blockEntity.canBuild();
    }

    public int chipState() { return blockEntity.chipState(); }
    public int fuselageState() { return blockEntity.fuselageState(); }
    public int stabilityState() { return blockEntity.stabilityState(); }
    public int thrusterState() { return blockEntity.thrusterState(); }
    public int warheadState() { return blockEntity.warheadState(); }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (pIndex < PLAYER_INV_START) {
                if (!this.moveItemStackTo(slotStack, PLAYER_INV_START, PLAYER_INV_END, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemstack);
            } else {
                if (!this.moveItemStackTo(slotStack, 0, MissileAssemblyBlockEntity.SLOT_OUTPUT, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), pPlayer, ModBlocks.MACHINE_MISSILE_ASSEMBLY.get());
    }

    private void addPlayerInventory(Inventory i) {
        for (int y = 0; y < 3; ++y) {
            for (int x = 0; x < 9; ++x) {
                this.addSlot(new Slot(i, x + y * 9 + 9, 8 + x * 18, 84 + y * 18 + 56));
            }
        }
    }

    private void addPlayerHotbar(Inventory i) {
        for (int x = 0; x < 9; ++x) {
            this.addSlot(new Slot(i, x, 8 + x * 18, 142 + 56));
        }
    }
}
