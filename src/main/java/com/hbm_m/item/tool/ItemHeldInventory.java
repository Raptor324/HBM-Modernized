package com.hbm_m.item.tool;

import com.hbm_m.platform.StackNbt;

import com.hbm_m.inventory.HeldItemInventory;
import com.hbm_m.inventory.menu.HeldItemMenu;

import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * {@code ItemLeadBox} ({@code containment_box}), {@code ItemPlasticBag} ({@code plastic_bag}) und
 * {@code ItemCasingBag} ({@code casing_bag}) 1:1: Rechtsklick oeffnet das Inventar im Gegenstand.
 */
public class ItemHeldInventory extends Item {

    private final HeldItemMenu.Layout layout;

    public ItemHeldInventory(HeldItemMenu.Layout layout, Properties properties) {
        super(properties.stacksTo(1));
        this.layout = layout;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player instanceof ServerPlayer sp) {
            MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                @Override public Component getDisplayName() { return stack.getHoverName(); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new HeldItemMenu(id, inv, layout, hand); }
            }, buf -> {
                buf.writeEnum(layout);
                buf.writeEnum(hand);
            });
        }
        return InteractionResultHolder.success(stack);
    }

    /**
     * Original {@code ItemCasingBag.pushCasing}: gibt true zurueck, wenn die Huelse aufgenommen wurde. Bruchteile
     * sammeln sich pro Huelsentyp im NBT, jede volle Einheit legt eine Huelse in die Tasche.
     */
    public static boolean pushCasing(ItemStack bag, ItemStack casing, float amount) {
        if (!StackNbt.has(bag)) StackNbt.set(bag, new net.minecraft.nbt.CompoundTag());
        String name = casing.getDescriptionId() + "@0";
        boolean ret = false;

        //only add if the previous number did not exceed 1 (i.e. the bag ran full, and may have been emptied, we don't know)
        if (StackNbt.read(bag).getFloat(name) < 1) {
            ret = true;
            StackNbt.tag(bag).putFloat(name, StackNbt.read(bag).getFloat(name) + amount);
        }

        if (StackNbt.read(bag).getFloat(name) >= 1) {
            HeldItemInventory inv = new HeldItemInventory(null, bag, HeldItemMenu.Layout.CASING_BAG.size, 64, (s, st) -> false, false, false);
            ItemStack toAdd = casing.copy();

            while (StackNbt.read(bag).getFloat(name) >= 1) {
                boolean didSomething = false;

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (toAdd.getCount() <= 0) break;
                    ItemStack slot = inv.getItem(i);
                    if (!slot.isEmpty() && StackNbt.sameItemSameTags(slot, toAdd)) {
                        int am = Math.min(toAdd.getCount(), slot.getMaxStackSize() - slot.getCount());
                        toAdd.shrink(am);
                        slot.grow(am);
                        if (am > 0) didSomething = true;
                    }
                }

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    if (toAdd.getCount() <= 0) break;
                    ItemStack slot = inv.getItem(i);
                    if (slot.isEmpty()) {
                        inv.setItem(i, toAdd);
                        didSomething = true;
                        break;
                    }
                }

                if (didSomething) {
                    StackNbt.tag(bag).putFloat(name, StackNbt.read(bag).getFloat(name) - 1F);
                    ret = true;
                } else {
                    break;
                }
            }
            inv.setChanged();
        }

        return ret;
    }
}
