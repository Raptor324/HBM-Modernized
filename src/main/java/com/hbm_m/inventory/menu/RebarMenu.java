package com.hbm_m.inventory.menu;

import com.hbm_m.inventory.HeldItemInventory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code ItemRebarPlacer.ContainerRebar}: ein Musterplatz bei (53,36) fuer die Betonsorte (Klick legt eine Kopie
 * des gehaltenen Stapels mit Anzahl 1 hinein, leere Hand leert ihn), Spielerinventar ab y 100. Umschalt-Klick tut
 * nichts, der geoeffnete Setzer ist gesperrt.
 */
public class RebarMenu extends AbstractContainerMenu {

    public final HeldItemInventory rebar;
    private final int selected;

    public RebarMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readEnum(InteractionHand.class));
    }

    public RebarMenu(int id, Inventory inv, InteractionHand hand) {
        super(ModMenuTypes.REBAR_MENU.get(), id);
        this.selected = inv.selected;
        this.rebar = new HeldItemInventory(inv.player, inv.player.getItemInHand(hand), 1, 1, (s, st) -> true, false, false);

        this.addSlot(new Slot(rebar, 0, 53, 36) {
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public int getMaxStackSize() { return 1; }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 100 + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 158));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int index, int button, ClickType mode, Player player) {
        // prevents the player from moving around the currently open box
        if (mode == ClickType.SWAP && button == selected) return;
        if (index == selected + 28) return;

        if (index != 0) {
            super.clicked(index, button, mode, player);
            return;
        }

        Slot slot = this.getSlot(index);
        ItemStack held = this.getCarried();

        // SlotPattern.putStack: Kopie mit Anzahl 1
        ItemStack put = held.isEmpty() ? ItemStack.EMPTY : held.copyWithCount(1);
        slot.set(put);
        rebar.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
