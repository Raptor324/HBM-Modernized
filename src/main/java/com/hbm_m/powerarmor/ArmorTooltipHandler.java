package com.hbm_m.powerarmor;

import java.util.List;

import com.hbm_m.armormod.item.ItemArmorMod;
import com.hbm_m.armormod.util.ArmorModificationHelper;
import com.hbm_m.handler.ArmorRegistry;
import com.hbm_m.handler.HazardClass;
import com.hbm_m.handler.HazmatRegistry;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/**
 * Original {@code ModEventHandlerClient.drawTooltip} (Ruestungsteil): Schadensresistenz, Gefahrenschutz,
 * Strahlungsresistenz (inkl. Verkleidung) und installierte Ruestungsmods - in dieser Reihenfolge.
 */
public class ArmorTooltipHandler {

    private ArmorTooltipHandler() {}

    public static void drawTooltip(ItemStack stack, List<Component> list, boolean armorTableOpen) {

        /// DAMAGE RESISTANCE ///
        DamageResistanceHandler.addInfo(stack, list);

        /// HAZMAT INFO ///
        var hazInfo = ArmorRegistry.getProtection(stack.getItem());

        if (!hazInfo.isEmpty()) {

            if (Screen.hasShiftDown()) {
                list.add(Component.translatable("hazard.prot").withStyle(ChatFormatting.GOLD));
                for (HazardClass clazz : hazInfo) {
                    list.add(Component.literal("  ").append(Component.translatable(clazz.translationKey)).withStyle(ChatFormatting.YELLOW));
                }
            } else {
                list.add(hold("to display protection info"));
            }
        }

        /// CLADDING (LEGACY) ///
        double rad = HazmatRegistry.getResistance(stack);
        rad = ((int) (rad * 1000)) / 1000D;
        if (rad > 0) list.add(Component.translatable("trait.radResistance", rad).withStyle(ChatFormatting.YELLOW));

        /// ARMOR MODS ///
        if (stack.getItem() instanceof ArmorItem && ArmorModificationHelper.hasMods(stack)) {

            if (!Screen.hasShiftDown() && !armorTableOpen) {

                list.add(hold("to display installed armor mods"));

            } else {

                list.add(Component.literal("Mods:").withStyle(ChatFormatting.YELLOW));

                ItemStack[] mods = ArmorModificationHelper.pryMods(stack);

                for (int i = 0; i < 8; i++) {

                    if (mods[i] != null && !mods[i].isEmpty() && mods[i].getItem() instanceof ItemArmorMod mod) {

                        mod.addDesc(list, mods[i], stack);
                    }
                }
            }
        }
    }

    private static Component hold(String what) {
        return Component.literal("Hold <").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
                .append(Component.literal("LSHIFT").withStyle(ChatFormatting.YELLOW, ChatFormatting.ITALIC))
                .append(Component.literal("> " + what).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
