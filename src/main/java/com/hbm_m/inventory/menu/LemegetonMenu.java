package com.hbm_m.inventory.menu;

import com.hbm_m.inventory.recipes.LemegetonRecipes;
import com.hbm_m.item.ModItems;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** 1:1 {@code ContainerLemegeton}: 1x1-"Werkbank" mit {@link LemegetonRecipes}. */
public class LemegetonMenu extends AbstractContainerMenu {

    public final SimpleContainer craftMatrix = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            LemegetonMenu.this.slotsChanged(this);
        }
    };
    public final ResultContainer craftResult = new ResultContainer();
    private final Player player;

    public LemegetonMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id, inv);
    }

    public LemegetonMenu(int id, Inventory inventory) {
        super(ModMenuTypes.LEMEGETON_MENU.get(), id);
        this.player = inventory.player;

        // SlotCrafting: Entnehmen verbraucht eine Zutat
        this.addSlot(new Slot(this.craftResult, 0, 107, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }

            @Override
            public void onTake(Player p, ItemStack stack) {
                craftMatrix.removeItem(0, 1);
                super.onTake(p, stack);
            }
        });
        this.addSlot(new Slot(this.craftMatrix, 0, 49, 35));

        for (int l = 0; l < 3; ++l) {
            for (int i1 = 0; i1 < 9; ++i1) {
                this.addSlot(new Slot(inventory, i1 + l * 9 + 9, 8 + i1 * 18, 84 + l * 18));
            }
        }

        for (int l = 0; l < 9; ++l) {
            this.addSlot(new Slot(inventory, l, 8 + l * 18, 142));
        }

        this.slotsChanged(this.craftMatrix);
    }

    @Override
    public void slotsChanged(Container inventory) {
        this.craftResult.setItem(0, LemegetonRecipes.getRecipe(this.craftMatrix.getItem(0)));
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide) {
            ItemStack itemstack = this.craftMatrix.removeItemNoUpdate(0);
            if (!itemstack.isEmpty()) p.drop(itemstack, false);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player p, int slotNo) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotNo);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (slotNo <= 1) {
                if (!this.moveItemStackTo(itemstack1, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 1, 2, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(p, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player p) {
        return p.getInventory().contains(new ItemStack(ModItems.BOOK_LEMEGETON.get()));
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.craftResult && super.canTakeItemForPickAll(stack, slot);
    }
}
