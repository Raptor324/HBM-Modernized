package com.hbm_m.inventory.menu;

import com.hbm_m.inventory.recipes.MagicRecipes;
import com.hbm_m.item.ModItems;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;

/**
 * Контейнер книги Вагонов ({@code book_of_}) — порт {@code ContainerBook} из 1.7.10.
 *
 * <p>Сетка 2×2 («расширенный крафт на 4 слота»), результат вычисляется статически
 * через {@link MagicRecipes} — ванильный {@code RecipeManager} и {@link CraftingRecipe}
 * не используются. Закрывается, если у игрока больше нет книги в инвентаре
 * (как в оригинале — {@code canInteractWith}).</p>
 */
public class BookMenu extends AbstractContainerMenu {

    public final TransientCraftingContainer craftMatrix = new TransientCraftingContainer(this, 2, 2);
    public final Container craftResult = new BookResultContainer();

    private static final int RESULT_SLOT = 0;
    private static final int GRID_SLOTS = 4;
    private static final int INV_SLOTS = 36;

    public BookMenu(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    /** Фабрика для {@code MenuRegistry.ofExtended} — extraData не используется. */
    public BookMenu(int id, Inventory inventory, net.minecraft.network.FriendlyByteBuf extraData) {
        super(ModMenuTypes.BOOK_MENU.get(), id);

        // Позиции слотов 1:1 с оригинальным GUIBook (сетка с шагом 36, «рамка» вокруг 2×2)
        this.addSlot(new Slot(this.craftResult, 0, 124, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return !this.container.getItem(0).isEmpty();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                // В оригинале использовался ванильный SlotCrafting: по одному предмету
                // из каждого непустого слота сетки + остаточные предметы (ведро и т.п.)
                for(int i = 0; i < craftMatrix.getContainerSize(); i++) {
                    ItemStack ingredient = craftMatrix.getItem(i);
                    if(!ingredient.isEmpty()) {
                        ItemStack remainder = ingredient.getCraftingRemainingItem();
                        ingredient.shrink(1);
                        if(!remainder.isEmpty() && player != null) {
                            if(!player.getInventory().add(remainder)) {
                                player.drop(remainder, false);
                            }
                        }
                    }
                }
                craftMatrix.setChanged();
            }
        });

        for(int row = 0; row < 2; ++row) {
            for(int col = 0; col < 2; ++col) {
                this.addSlot(new Slot(this.craftMatrix, col + row * 2, 30 + col * 36, 17 + row * 36));
            }
        }

        for(int row = 0; row < 3; ++row) {
            for(int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        for(int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }

        this.slotsChanged(this.craftMatrix);
    }

    @Override
    public void slotsChanged(Container container) {
        this.craftResult.setItem(0, MagicRecipes.getRecipe(this.craftMatrix));
    }

    @Override
    public boolean stillValid(Player player) {
        // Книга должна оставаться в инвентаре, иначе контейнер закрывается
        for(int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if(player.getInventory().getItem(i).is(ModItems.BOOK_OF_.get()))
                return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if(slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if(index == RESULT_SLOT) {
                if(!this.moveItemStackTo(itemstack1, 1 + GRID_SLOTS, 1 + GRID_SLOTS + INV_SLOTS, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if(index >= 1 && index < 1 + GRID_SLOTS) {
                if(!this.moveItemStackTo(itemstack1, 1 + GRID_SLOTS, 1 + GRID_SLOTS + INV_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(index >= 1 + GRID_SLOTS && index < 1 + GRID_SLOTS + 27) {
                if(!this.moveItemStackTo(itemstack1, 1 + GRID_SLOTS + 27, 1 + GRID_SLOTS + INV_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if(index >= 1 + GRID_SLOTS + 27 && index < 1 + GRID_SLOTS + INV_SLOTS) {
                if(!this.moveItemStackTo(itemstack1, 1 + GRID_SLOTS, 1 + GRID_SLOTS + 27, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }

            if(itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if(itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // В оригинале содержимое сетки выбрасывалось под игрока при закрытии;
        // здесь то же самое, только с возвратом в инвентарь, когда он вмещает
        if(!player.level().isClientSide) {
            for(int i = 0; i < this.craftMatrix.getContainerSize(); i++) {
                ItemStack stack = this.craftMatrix.removeItemNoUpdate(i);
                if(!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }

    /** Однослотовый контейнер результата, аналог InventoryCraftResult. */
    private static class BookResultContainer implements Container {

        private ItemStack result = ItemStack.EMPTY;

        @Override
        public int getContainerSize() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return this.result.isEmpty();
        }

        @Override
        public ItemStack getItem(int slot) {
            return this.result;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            ItemStack taken = this.result.split(amount);
            this.setChanged();
            return taken;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            ItemStack old = this.result;
            this.result = ItemStack.EMPTY;
            return old;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            this.result = stack;
            this.setChanged();
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            this.result = ItemStack.EMPTY;
        }
    }
}
