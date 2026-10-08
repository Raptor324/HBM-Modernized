package com.hbm_m.api.tile;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.item.tool.ItemBlowtorch;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 1:1 {@code IRepairable}: beschaedigte Maschinen (z. B. explodierte Tanks), die mit dem Schweissbrenner und den
 * angegebenen Materialien repariert werden, und brennende Maschinen, die sich loeschen lassen.
 */
public interface IRepairable {

    /** Gegenstueck zu {@code AStack}: Zutat und Menge. */
    record RepairStack(Ingredient ingredient, int count) {
        public ItemStack display(long time) {
            ItemStack[] items = ingredient.getItems();
            if (items.length == 0) return ItemStack.EMPTY;
            return items[(int) (time / 20 % items.length)].copyWithCount(count);
        }
    }

    boolean isDamaged();
    List<RepairStack> getRepairMaterials();
    void repair();

    void tryExtinguish(Level world, BlockPos pos, EnumExtinguishType type);

    enum EnumExtinguishType {
        WATER,
        FOAM,
        SAND,
        CO2
    }

    @Nullable
    static List<RepairStack> getRepairMaterials(Level world, BlockPos core, Player player) {

        ItemStack held = player.getMainHandItem();

        if (held.isEmpty() || !(held.getItem() instanceof ItemBlowtorch)) return null;

        BlockEntity be = world.getBlockEntity(core);
        if (!(be instanceof IRepairable repairable)) return null;

        if (!repairable.isDamaged()) return null;
        return repairable.getRepairMaterials();
    }

    /** {@code tryRepairMultiblock}: mit allen Materialien im Inventar (die dabei verbraucht werden) wird repariert. */
    static boolean tryRepairMultiblock(Level world, BlockPos core, Player player) {

        BlockEntity be = world.getBlockEntity(core);
        if (!(be instanceof IRepairable repairable)) return false;

        if (!repairable.isDamaged()) return false;

        List<RepairStack> list = repairable.getRepairMaterials();
        if (list == null || list.isEmpty() || doesPlayerHaveStacks(player, list, true)) {
            if (!world.isClientSide) repairable.repair();
            return true;
        }

        return false;
    }

    /** {@code InventoryUtil.doesPlayerHaveAStacks}: erst auf einer Kopie pruefen, nur bei vollstaendigem Fund abziehen. */
    static boolean doesPlayerHaveStacks(Player player, List<RepairStack> stacks, boolean shouldRemove) {

        var original = player.getInventory().items;
        ItemStack[] inventory = new ItemStack[original.size()];
        for (int i = 0; i < inventory.length; i++) inventory[i] = original.get(i).copy();

        for (RepairStack stack : stacks) {
            int needed = stack.count();

            for (int j = 0; j < inventory.length && needed > 0; j++) {
                ItemStack inv = inventory[j];
                if (!inv.isEmpty() && stack.ingredient().test(inv)) {
                    int take = Math.min(needed, inv.getCount());
                    inv.shrink(take);
                    needed -= take;
                }
            }

            if (needed > 0) return false;
        }

        if (shouldRemove) {
            for (int i = 0; i < inventory.length; i++) {
                if (!ItemStack.matches(inventory[i], original.get(i))) {
                    original.set(i, inventory[i].isEmpty() ? ItemStack.EMPTY : inventory[i]);
                }
            }
            player.inventoryMenu.broadcastChanges();
        }

        return true;
    }

    /** {@code addGenericOverlay}: "Repair with:" und die Materialliste. */
    static void addGenericOverlay(net.minecraft.client.gui.GuiGraphics g, Level world, BlockPos core, Component title) {

        Player me = net.minecraft.client.Minecraft.getInstance().player;
        if (me == null) return;
        List<RepairStack> materials = getRepairMaterials(world, core, me);

        if (materials == null) return;

        List<Component> text = new ArrayList<>();
        text.add(Component.literal("Repair with:").withStyle(ChatFormatting.GOLD));

        for (RepairStack stack : materials) {
            ItemStack display = stack.display(world.getGameTime());
            if (display.isEmpty()) {
                text.add(Component.literal("- ERROR").withStyle(ChatFormatting.RED));
            } else {
                text.add(Component.literal("- ").append(display.getHoverName()).append(" x" + display.getCount()));
            }
        }

        com.hbm_m.interfaces.ILookOverlay.printGeneric(g, title, 0xffff00, 0x404000, text);
    }
}
