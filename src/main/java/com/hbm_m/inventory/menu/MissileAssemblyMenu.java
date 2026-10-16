package com.hbm_m.inventory.menu;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.blockentity.machines.MissileAssemblyBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
import com.hbm_m.platform.DummyItemStackHandler;

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
        this(id, inv, getBlockEntity(inv, extraData));
    }

    private static MissileAssemblyBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf extraData) {
        BlockEntity blockEntity = inv.player.level().getBlockEntity(extraData.readBlockPos());
        if (blockEntity instanceof MissileAssemblyBlockEntity tile) return tile;
        // На клиенте тайл может отсутствовать (реплей Flashback) — возвращаем null.
        // На сервере отсутствие тайла — реальный баг, поэтому там падаем как раньше.
        if (inv.player.level().isClientSide) return null;
        throw new IllegalStateException("BlockEntity is not a MissileAssemblyBlockEntity");
    }

    public MissileAssemblyMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.MISSILE_ASSEMBLY_MENU.get(), id);

        this.blockEntity = entity instanceof MissileAssemblyBlockEntity missile ? missile : null;

        // тайл может отсутствовать на клиенте (реплей Flashback) — подставляем пустую заглушку
        var container = this.blockEntity != null
                ? new ModItemStackHandlerContainer(this.blockEntity.getInventory(), this.blockEntity::setChanged)
                : new ModItemStackHandlerContainer(new DummyItemStackHandler(MissileAssemblyBlockEntity.SLOT_OUTPUT + 1), () -> {});

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
        // тайл может отсутствовать на клиенте (реплей Flashback)
        return blockEntity != null && blockEntity.canBuild();
    }

    public int chipState() { return blockEntity != null ? blockEntity.chipState() : 0; }
    public int fuselageState() { return blockEntity != null ? blockEntity.fuselageState() : 0; }
    public int stabilityState() { return blockEntity != null ? blockEntity.stabilityState() : 0; }
    public int thrusterState() { return blockEntity != null ? blockEntity.thrusterState() : 0; }
    public int warheadState() { return blockEntity != null ? blockEntity.warheadState() : 0; }

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
        // тайл может отсутствовать на клиенте (реплей Flashback)
        if (blockEntity == null) {
            return false;
        }
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
