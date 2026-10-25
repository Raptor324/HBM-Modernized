package com.hbm_m.item.tool;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.handler.BobmazonOfferFactory;
import com.hbm_m.inventory.gui.GUIScreenBobmazon;
import com.hbm_m.item.ITooltipProvider;
import com.hbm_m.item.ModItems;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** 1:1 {@code ItemCatalog}: Bobmazon und der versteckte Katalog oeffnen clientseitig die Bestellseite. */
public class ItemCatalog extends Item implements ITooltipProvider {

    public ItemCatalog(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (world.isClientSide && hand == InteractionHand.MAIN_HAND) {
            EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientOnly.open(player));
        }
        return InteractionResultHolder.sidedSuccess(stack, world.isClientSide);
    }

    @Override
    public void appendHbmTooltip(ItemStack stack, @Nullable Level level, List<Component> list, TooltipFlag flag) {

        if (this == ModItems.BOBMAZON_HIDDEN.get()) {
            list.add(Component.literal("For a guide on how to obtain this, visit https://bit.ly/2TPgcqT"));
            list.add(Component.literal("No tricks this time, i promise."));
        }
    }

    private static final class ClientOnly {
        static void open(Player player) {
            List<BobmazonOfferFactory.Offer> offers = BobmazonOfferFactory.getOffers(player.getMainHandItem());
            if (offers != null) net.minecraft.client.Minecraft.getInstance().setScreen(new GUIScreenBobmazon(player, offers));
        }
    }
}
