package com.hbm_m.inventory.menu;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.custom.CustomMachineBlockEntity;
import com.hbm_m.inventory.ModItemStackHandlerContainer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ContainerMachineCustom}: Batterie, Fluidkennungen je Eingangstank, so viele Eingabe-, Filter- und
 * Ausgabeplaetze wie die Konfiguration freigibt. Filterplaetze sind Geister (Rechtsklick wechselt den Modus),
 * Umschalt-Klick tut nichts (Original {@code transferStackInSlot} gibt null zurueck).
 */
public class CustomMachineMenu extends AbstractContainerMenu {

    public final CustomMachineBlockEntity custom;

    public CustomMachineMenu(int id, Inventory inv, FriendlyByteBuf extra) {
        this(id, inv, (CustomMachineBlockEntity) inv.player.level().getBlockEntity(extra.readBlockPos()));
    }

    public CustomMachineMenu(int id, Inventory playerInv, CustomMachineBlockEntity tile) {
        super(ModMenuTypes.CUSTOM_MACHINE_MENU.get(), id);
        this.custom = tile;
        var c = new ModItemStackHandlerContainer(tile.getInventory(), tile::setChanged);

        //Input
        this.addSlot(new Slot(c, 0, 150, 72));
        //Fluid IDs
        for (int i = 0; i < tile.inputTanks.length; i++) this.addSlot(new Slot(c, 1 + i, 8 + 18 * i, 54));

        int in = tile.config.itemInCount;
        int[][] grid = { { 8, 72 }, { 26, 72 }, { 44, 72 }, { 8, 90 }, { 26, 90 }, { 44, 90 } };
        //Item inputs
        for (int i = 0; i < 6; i++) if (in > i) this.addSlot(new Slot(c, 4 + i, grid[i][0], grid[i][1]));
        //Templates
        for (int i = 0; i < 6; i++) if (in > i) this.addSlot(new PatternSlot(c, 10 + i, grid[i][0], grid[i][1] + 36));
        //Output
        int out = tile.config.itemOutCount;
        int[][] outGrid = { { 78, 72 }, { 96, 72 }, { 114, 72 }, { 78, 90 }, { 96, 90 }, { 114, 90 } };
        for (int i = 0; i < 6; i++) if (out > i) this.addSlot(new Slot(c, 16 + i, outGrid[i][0], outGrid[i][1]) {
            @Override public boolean mayPlace(@NotNull ItemStack stack) { return false; }
        });

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(playerInv, j + i * 9 + 9, 8 + j * 18, 174 + i * 18));

        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(playerInv, i, 8 + i * 18, 232));
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !custom.isRemoved() && player.distanceToSqr(custom.getBlockPos().getX() + 0.5, custom.getBlockPos().getY() + 0.5, custom.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Original {@code slotClick}: Filter = Geist, Rechtsklick auf belegten Filter schaltet den Modus. */
    @Override
    public void clicked(int index, int button, @NotNull ClickType clickType, @NotNull Player player) {
        if (index < 0 || index >= this.slots.size() || !(this.slots.get(index) instanceof PatternSlot)) {
            super.clicked(index, button, clickType, player);
            return;
        }

        Slot slot = this.getSlot(index);
        int tileIndex = slot.getContainerSlot();
        ItemStack held = getCarried();

        if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) {
            if (!player.level().isClientSide) custom.matcher.nextMode(tileIndex - 10, slot.getItem());
        } else {
            slot.set(held);
            if (!player.level().isClientSide) custom.matcher.initPatternSmart(tileIndex - 10, slot.getItem());
        }
        custom.setChanged();
    }
}
