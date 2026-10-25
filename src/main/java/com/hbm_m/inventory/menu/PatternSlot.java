package com.hbm_m.inventory.menu;

import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code SlotPattern}: Geister-Filterplatz - nimmt eine Kopie (Anzahl 1) auf, laesst sich nicht herausnehmen. */
public class PatternSlot extends Slot {

    public PatternSlot(Container container, int index, int x, int y) {
        super(container, index, x, y);
    }

    @Override public boolean mayPickup(Player player) { return false; }
    @Override public int getMaxStackSize() { return 1; }

    @Override
    public void set(ItemStack stack) {
        if (!stack.isEmpty()) {
            stack = stack.copy();
            stack.setCount(1);
        }
        super.set(stack);
    }

    /**
     * Original {@code slotClick} der Filter-Container: Rechtsklick auf einen belegten Filter schaltet den Modus weiter,
     * sonst wird das gehaltene Item als Geist gesetzt (bzw. geleert) und das Muster neu belegt.
     *
     * @return true, wenn der Klick hier behandelt wurde
     */
    public static boolean handle(AbstractContainerMenu menu, int slotId, int button, ClickType clickType, Player player,
                                 int start, int end, IntConsumer nextMode, BiConsumer<Integer, ItemStack> initPattern) {
        if (slotId < start || slotId > end) return false;

        Slot slot = menu.getSlot(slotId);
        ItemStack held = menu.getCarried();

        if (button == 1 && clickType == ClickType.PICKUP && slot.hasItem()) {
            if (!player.level().isClientSide) nextMode.accept(slotId);
        } else {
            slot.set(held);
            if (!player.level().isClientSide) initPattern.accept(slotId, slot.getItem());
        }
        return true;
    }
}
