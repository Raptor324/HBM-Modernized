package com.hbm_m.menu;

import com.hbm_m.block.entity.custom.machines.MachineOreAcidizerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class OreAcidizerMenu extends AbstractContainerMenu {
    private final MachineOreAcidizerBlockEntity blockEntity;
    private final ContainerData data;

    public OreAcidizerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(1));
    }

    public OreAcidizerMenu(int id, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ORE_ACIDIZER_MENU.get(), id);
        this.blockEntity = (MachineOreAcidizerBlockEntity) entity;
        this.data = data;
        IItemHandler handler = this.blockEntity.getInventory();

        // Slot layout visually matched to gui_crystallizer.png
        // Input (top left)
        this.addSlot(new SlotItemHandler(handler, 0, 27, 19));
        // Input (bottom left)
        this.addSlot(new SlotItemHandler(handler, 1, 27, 55));
        // Battery (bottom left, canister)
        this.addSlot(new SlotItemHandler(handler, 2, 27, 91));
        // Output (center right)
        this.addSlot(new SlotItemHandler(handler, 3, 98, 37));
        // Upgrade 1 (top right gear)
        this.addSlot(new SlotItemHandler(handler, 4, 116, 19));
        // Upgrade 2 (bottom right gear)
        this.addSlot(new SlotItemHandler(handler, 5, 134, 19));
        // Fluid ID (small slot, right of progress bar)
        this.addSlot(new SlotItemHandler(handler, 6, 80, 19));
        // Extra slot (rightmost, output arrow)
        this.addSlot(new SlotItemHandler(handler, 7, 134, 55));

        // Player inventory
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 122 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 180));
        }
        addDataSlots(data);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // TODO: Implement shift-click logic analog zum Beispiel
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity != null && blockEntity.stillValid(player);
    }
}
