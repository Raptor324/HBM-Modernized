package com.hbm_m.inventory.menu;

import java.util.List;
import java.util.Optional;

import com.hbm_m.advancement.AchievementHandler;
import com.hbm_m.block.machines.anvils.AnvilTier;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipe.AStack;
import com.hbm_m.recipe.AnvilRecipeManager;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 1:1 {@code ContainerAnvil}: zwei Schmiede-Slots und der Ausgabeslot gehoeren nur dem geoeffneten Fenster
 * (beim Schliessen fallen die Eingaben heraus); die Konstruktion zieht direkt aus dem Spielerinventar
 * ({@link #craftConstruction}, {@code AnvilCraftPacket}).
 */
public class AnvilMenu extends AbstractContainerMenu {

    public final SimpleContainer input = new SimpleContainer(8);
    public final SimpleContainer output = new SimpleContainer(1);

    /** "because we can't trust these rascals with their packets" */
    public final AnvilTier tier;
    public final BlockPos pos;
    private final Player player;

    // Client
    public AnvilMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, extraData.readBlockPos(), AnvilTier.values()[extraData.readVarInt()]);
    }

    public AnvilMenu(int containerId, Inventory inventory, BlockPos pos, AnvilTier tier) {
        super(ModMenuTypes.ANVIL_MENU.get(), containerId);
        this.tier = tier;
        this.pos = pos;
        this.player = inventory.player;
        this.input.addListener(this::slotsChanged);

        this.addSlot(new Slot(input, 0, 17, 27));
        this.addSlot(new Slot(input, 1, 53, 27));
        this.addSlot(new Slot(output, 0, 89, 27) {

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
                super.onTake(player, stack);
                AchievementHandler.fire(player, stack);

                ItemStack left = AnvilMenu.this.input.getItem(0);
                ItemStack right = AnvilMenu.this.input.getItem(1);

                if (left.isEmpty() || right.isEmpty()) {
                    return;
                }

                Optional<AnvilRecipe> rec = AnvilRecipeManager.findSmithingAnyTier(player.level(), left, right);
                if (rec.isPresent()) {
                    AnvilMenu.this.input.removeItem(0, rec.get().amountConsumed(0));
                    AnvilMenu.this.input.removeItem(1, rec.get().amountConsumed(1));
                    AnvilMenu.this.updateSmithing();
                }
            }
        });

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + 56));
            }
        }

        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inventory, i, 8 + i * 18, 142 + 56));
        }

        this.slotsChanged(this.input);
    }

    @Override
    public void slotsChanged(@NotNull Container container) {
        super.slotsChanged(container);
        if (container == this.input) updateSmithing();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack var3 = ItemStack.EMPTY;
        Slot var4 = this.slots.get(index);

        if (var4.hasItem()) {
            ItemStack var5 = var4.getItem();
            var3 = var5.copy();

            if (index == 2) {
                if (!this.moveItemStackTo(var5, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
                var4.onQuickCraft(var5, var3);
            } else if (index <= 1) {
                if (!this.moveItemStackTo(var5, 3, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(var5, 0, 2, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (var5.isEmpty()) {
                var4.setByPlayer(ItemStack.EMPTY);
            } else {
                var4.setChanged();
            }

            var4.onTake(player, var5);
        }

        return var3;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            for (int i = 0; i < this.input.getContainerSize(); ++i) {
                ItemStack itemstack = this.input.removeItemNoUpdate(i);

                if (!itemstack.isEmpty()) {
                    player.drop(itemstack, false);
                }
            }
        }
    }

    private void updateSmithing() {

        ItemStack left = this.input.getItem(0);
        ItemStack right = this.input.getItem(1);

        if (left.isEmpty() || right.isEmpty()) {
            this.output.setItem(0, ItemStack.EMPTY);
            return;
        }

        Optional<AnvilRecipe> rec = AnvilRecipeManager.findSmithing(this.player.level(), left, right, this.tier);
        this.output.setItem(0, rec.map(r -> r.getSmithingOutput(left, right)).orElse(ItemStack.EMPTY));
    }

    // ═══════════════════════════ AnvilCraftPacket ═══════════════════════════

    /** {@code AnvilCraftPacket.Handler.onMessage}: mode 1 = so oft wie moeglich (bis ein Stapel voll ist). */
    public void craftConstruction(Player p, AnvilRecipe recipe, int mode) {
        if (!recipe.isConstruction()) return;
        if (!recipe.canCraftOn(this.tier)) return; //player is using the wrong type of anvil -> bad

        List<AnvilRecipe.ResultEntry> out = recipe.getOutputs();
        int count = mode == 1 ? (out.size() > 1 ? 64 : (out.get(0).stack().getMaxStackSize() / out.get(0).stack().getCount())) : 1;

        for (int i = 0; i < count; i++) {
            if (doesPlayerHaveAStacks(p, recipe.getInputs(), true)) {
                giveChanceStacksToPlayer(p, out);
                AchievementHandler.fire(p, out.get(0).stack());
            } else {
                break;
            }
        }

        p.inventoryMenu.broadcastChanges();
        this.broadcastChanges();
    }

    /** {@code InventoryUtil.doesPlayerHaveAStacks}: erst auf einer Kopie pruefen, dann (optional) abziehen. */
    public static boolean doesPlayerHaveAStacks(Player player, List<AStack> stacks, boolean shouldRemove) {
        List<ItemStack> original = player.getInventory().items;
        ItemStack[] inventory = new ItemStack[original.size()];
        boolean[] modified = new boolean[original.size()];
        int[] remaining = new int[stacks.size()];

        for (int i = 0; i < remaining.length; i++) remaining[i] = stacks.get(i).count();
        for (int i = 0; i < original.size(); i++) inventory[i] = original.get(i).copy();

        for (int i = 0; i < remaining.length; i++) {
            AStack stack = stacks.get(i);
            for (int j = 0; j < inventory.length; j++) {
                ItemStack inv = inventory[j];
                if (stack.matchesIgnoreSize(inv)) {
                    int size = Math.min(remaining[i], inv.getCount());
                    remaining[i] -= size;
                    inv.shrink(size);
                    modified[j] = true;
                    if (remaining[i] <= 0) break;
                }
            }
        }

        for (int r : remaining) {
            if (r > 0) return false;
        }

        if (shouldRemove) {
            for (int i = 0; i < original.size(); i++) {
                if (modified[i]) original.set(i, inventory[i].isEmpty() ? ItemStack.EMPTY : inventory[i]);
            }
            player.getInventory().setChanged();
        }

        return true;
    }

    /** {@code InventoryUtil.giveChanceStacksToPlayer} */
    public static void giveChanceStacksToPlayer(Player player, List<AnvilRecipe.ResultEntry> stacks) {
        for (AnvilRecipe.ResultEntry out : stacks) {
            if (out.chance() == 1.0F || player.getRandom().nextFloat() < out.chance()) {
                ItemStack give = out.stack().copy();
                if (!player.getInventory().add(give)) {
                    player.drop(give, false); // Rest statt voller Kopie (Original dupliziert hier)
                }
            }
        }
    }
}
