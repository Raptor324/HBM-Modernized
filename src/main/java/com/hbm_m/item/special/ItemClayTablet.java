package com.hbm_m.item.special;

import com.hbm_m.platform.PlatformHooks;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 1:1 {@code ItemClayTablet}: beim ersten Benutzen wird ein Zufallssamen ({@code tabletSeed}) gesetzt, die
 * Tafel zeigt dann ein teilweise aufgedecktes Sockelrezept ({@code GUIScreenClayTablet}). Meta 0/1 waehlt den
 * Rezeptsatz und die Optik; im Port {@code clay_tablet} (0) und {@code clay_tablet_1} (1).
 */
public class ItemClayTablet extends Item {

    /** Original-Metadatum (Rezeptsatz / GUI-Variante). */
    public final int tabletMeta;

    public ItemClayTablet(int tabletMeta, Properties properties) {
        // setMaxDamage(0), setMaxStackSize(1)
        super(properties.stacksTo(1));
        this.tabletMeta = tabletMeta;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && !PlatformHooks.hasItemTag(stack)) {
            PlatformHooks.putLong(stack, "tabletSeed", player.getRandom().nextLong());
        }
        if (world.isClientSide) EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ClientOnly.open(tabletMeta));
        return InteractionResultHolder.pass(stack);
    }

    private static final class ClientOnly {
        private static void open(int meta) {
            net.minecraft.client.Minecraft.getInstance().setScreen(new com.hbm_m.inventory.gui.GUIScreenClayTablet(meta));
        }
    }
}
