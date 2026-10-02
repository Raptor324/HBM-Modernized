package com.hbm_m.item.tool;

import com.hbm_m.inventory.menu.LemegetonMenu;

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

/** 1:1 {@code com.hbm.items.tool.ItemBookLemegeton}: Rechtsklick oeffnet die Materialaufwertung. */
public class ItemBookLemegeton extends Item {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player instanceof ServerPlayer sp) {
            MenuRegistry.openExtendedMenu(sp, new MenuProvider() {
                @Override public Component getDisplayName() { return stack.getHoverName(); }
                @Override public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) { return new LemegetonMenu(id, inv); }
            }, buf -> {});
        }
        return InteractionResultHolder.success(stack);
    }
}
