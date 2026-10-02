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
 * Gemeinsames Menue der Gegenstaende mit eigenem Inventar: {@code ContainerLeadBox}, {@code ContainerPlasticBag},
 * {@code ContainerCasingBag} (1:1 Slotlagen). Wie im Original pruefen die Plaetze die Gueltigkeit nicht
 * ({@code SlotNonRetarded}/{@code Slot} der 1.7.10) und der geoeffnete Gegenstand selbst ist gesperrt.
 */
public class HeldItemMenu extends AbstractContainerMenu {

    public enum Layout {
        /** ContainerLeadBox: 4x5 ab (43,18), Spieler ab y 104, GUI 176x186. */
        LEAD_BOX(20, 1, 5, 43, 18, 104, 186, true, true),
        /** ContainerPlasticBag: 1 Platz bei (80,65), Spieler ab y 134, GUI 176x216. */
        PLASTIC_BAG(1, 1, 1, 80, 65, 134, 216, false, false),
        /** ContainerCasingBag: 3x5 ab (44,18), Spieler ab y 100, GUI 176x186. */
        CASING_BAG(15, 64, 5, 44, 18, 100, 186, false, false),
        /** ContainerToolBox: 3x8 ab (17,49), Spieler ab y 129, GUI 176x211; Plaetze pruefen (SlotNonRetarded). */
        TOOLBOX(24, 64, 8, 17, 49, 129, 211, true, true);

        public final int size, stackLimit, cols, x, y, playerY, ySize;
        public final boolean crateSounds, checkSize;

        Layout(int size, int stackLimit, int cols, int x, int y, int playerY, int ySize, boolean crateSounds, boolean checkSize) {
            this.size = size;
            this.stackLimit = stackLimit;
            this.cols = cols;
            this.x = x;
            this.y = y;
            this.playerY = playerY;
            this.ySize = ySize;
            this.crateSounds = crateSounds;
            this.checkSize = checkSize;
        }
    }

    public final Layout layout;
    public final HeldItemInventory box;
    private final int selected;

    /** Client: Hand und Layout kommen aus dem Puffer. */
    public HeldItemMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv, buf.readEnum(Layout.class), buf.readEnum(InteractionHand.class));
    }

    public HeldItemMenu(int id, Inventory inv, Layout layout, InteractionHand hand) {
        super(ModMenuTypes.HELD_ITEM_MENU.get(), id);
        this.layout = layout;
        this.selected = inv.selected;
        ItemStack held = inv.player.getItemInHand(hand);
        this.box = new HeldItemInventory(inv.player, held, layout.size, layout.stackLimit,
                layout == Layout.TOOLBOX ? (s, st) -> !(st.getItem() instanceof com.hbm_m.item.tool.ItemToolBox) : (s, st) -> true,
                layout.crateSounds, layout.checkSize);
        this.box.startOpen(inv.player);

        for (int i = 0; i < layout.size; i++) {
            if (layout == Layout.TOOLBOX) {
                this.addSlot(new Slot(box, i, layout.x + (i % layout.cols) * 18, layout.y + (i / layout.cols) * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return container.canPlaceItem(getContainerSlot(), stack); }
                });
            } else {
                this.addSlot(new Slot(box, i, layout.x + (i % layout.cols) * 18, layout.y + (i / layout.cols) * 18));
            }
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, layout.playerY + i * 18));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, layout.playerY + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack ret = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            ret = stack.copy();

            if (index <= box.getContainerSize() - 1) {
                if (!this.moveItemStackTo(stack, box.getContainerSize(), this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 0, box.getContainerSize(), false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            slot.onTake(player, stack);
        }

        return ret;
    }

    @Override
    public void clicked(int index, int button, ClickType type, Player player) {
        // prevents the player from moving around the currently open box
        if (type == ClickType.SWAP && button == selected) return;
        if (index == selected + 27 + box.getContainerSize()) return;
        super.clicked(index, button, type, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.box.stopOpen(player);
        if (layout == Layout.TOOLBOX && !player.level().isClientSide) com.hbm_m.item.tool.ItemToolBox.onClose(player, box.target);
    }
}
