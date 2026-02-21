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

        // Slot layout matched to gui_crystallizer.png
        // Input 0 (top left)
        this.addSlot(new SlotItemHandler(handler, 0, 7, 16));
        // Input 1 (second row left)
        this.addSlot(new SlotItemHandler(handler, 1, 7, 34));
        // Input 2 (third row left / battery)
        this.addSlot(new SlotItemHandler(handler, 2, 7, 52));
        // Upgrade 1 (first gear slot)
        this.addSlot(new SlotItemHandler(handler, 3, 103, 16));
        // Upgrade 2 (second gear slot)
        this.addSlot(new SlotItemHandler(handler, 4, 121, 16));
        // Output (far right)
        this.addSlot(new SlotItemHandler(handler, 5, 143, 30));

        // Player inventory (standard 18px spacing, Y starts at 83)
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 7 + j * 18, 83 + i * 18));
            }
        }
        // Hotbar (at y=141)
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 7 + i * 18, 141));
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
