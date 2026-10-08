package com.hbm_m.inventory.menu;

import com.hbm_m.blockentity.machines.MachineCombustionEngineBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;
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

/** 1:1 {@code ContainerCombustionEngine}: Fluid rein/raus, Kolbensatz, Batterie, Fluidkennung. */
public class MachineCombustionEngineMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOT_COUNT = 5;

    private final MachineCombustionEngineBlockEntity blockEntity;

    public MachineCombustionEngineMenu(int id, Inventory inventory, FriendlyByteBuf extraData) {
        this(id, inventory, getBlockEntity(inventory, extraData));
    }

    public MachineCombustionEngineMenu(int id, Inventory inventory, MachineCombustionEngineBlockEntity blockEntity) {
        super(ModMenuTypes.COMBUSTION_ENGINE_MENU.get(), id);
        this.blockEntity = blockEntity;
        if (blockEntity != null) blockEntity.openInventory();

        var tedf = new ModItemStackHandlerContainer(
                blockEntity != null ? blockEntity.getInventory() : new DummyItemStackHandler(MACHINE_SLOT_COUNT),
                blockEntity != null ? blockEntity::setChanged : null);

        this.addSlot(new Slot(tedf, 0, 17, 17));
        this.addSlot(new Slot(tedf, 1, 17, 53) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        this.addSlot(new Slot(tedf, 2, 88, 71));
        this.addSlot(new Slot(tedf, 3, 143, 71));
        this.addSlot(new Slot(tedf, 4, 35, 71));

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 121 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 179));
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (blockEntity != null) blockEntity.closeInventory();
    }

    private static MachineCombustionEngineBlockEntity getBlockEntity(Inventory inventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof MachineCombustionEngineBlockEntity engine) {
            return engine;
        }
        if (inventory.player.level().isClientSide) return null;
        throw new IllegalStateException("No MachineCombustionEngineBlockEntity found at " + pos + " for menu " + RefStrings.MODID + ":combustion_engine_menu");
    }

    public MachineCombustionEngineBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public boolean stillValid(Player player) {
        // w16b: Original isUseableByPlayer (TileEntityMachineBase) = 128 vom Kern, dazu Huelle der Maschine (MultiblockMenuReach)
        return MultiblockMenuReach.stillValidCore(blockEntity, player, 128.0D);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4 != null && var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index <= 4) {
                if (!this.moveItemStackTo(var5, MACHINE_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {

                if (var3.getItem() instanceof com.hbm_m.api.item.IBatteryItem) {
                    if (!this.moveItemStackTo(var5, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (var3.getItem() instanceof com.hbm_m.interfaces.IItemFluidIdentifier) {
                    if (!this.moveItemStackTo(var5, 4, 5, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (MachineCombustionEngineBlockEntity.pistonType(var3.getItem()) >= 0) {
                    if (!this.moveItemStackTo(var5, 2, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
                    if (!this.moveItemStackTo(var5, 0, 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (var5.isEmpty()) {
                var4.set(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }
        }

        return var3;
    }
}
