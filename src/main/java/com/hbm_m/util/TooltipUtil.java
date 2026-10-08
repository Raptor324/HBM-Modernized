package com.hbm_m.util;

import com.hbm_m.lib.RefStrings;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class TooltipUtil {
    public static void addNuclearTooltip(List<Component> tooltip, Level level,
                                         ItemStack stack, BlockState state, TooltipFlag flag) {
        tooltip.add(Component.literal("⚠️ ОПАСНО! ЯДЕРНЫЙ УРОН")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000)))); // Тёмно-красный

        tooltip.add(Component.literal("Урон: 150+ сердец в радиусе 25 бл.")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000)))); // Красный

        tooltip.add(Component.literal("Вызывает радиацию и тошноту")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080)))); // Серый
    }

    // --- 1.7.10-Tooltipfarben ---
    // Original GuiScreen.drawHoveringText: jede Zeile ausser der ersten bekommt "§7" vorangestellt,
    // addInformation-Zeilen ohne eigenen Farbcode sind also GRAU. In 1.20 sind ungestylte Zeilen WEISS.

    /** Ungestylte Zeile wie im Original (grau). */
    public static MutableComponent gray(String text) {
        return Component.literal(text).withStyle(ChatFormatting.GRAY);
    }

    /** Uebersetzte Zeile wie im Original (grau). */
    public static MutableComponent grayT(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    /** Explizit weisse Zeile (Original §f / RESET am Zeilenanfang) - bleibt von {@link #applyLegacyGray} unberuehrt. */
    public static MutableComponent white(String text) {
        return Component.literal(text).withStyle(ChatFormatting.WHITE);
    }

    /**
     * Bildet das "§7"-Praefix des Originals nach: alle Zeilen ab Index 1 ohne eigene Grundfarbe werden grau.
     * Kindkomponenten mit eigener Farbe behalten sie (wie spaetere Farbcodes in der Originalzeile).
     */
    public static void applyLegacyGray(List<Component> lines) {
        applyLegacyGray(lines, 1);
    }

    /** Wie {@link #applyLegacyGray(List)}, aber erst ab {@code from} (fuer Zeilen, die der Mod an fremde Gegenstaende haengt). */
    public static void applyLegacyGray(List<Component> lines, int from) {
        for (int i = Math.max(1, from); i < lines.size(); i++) {
            Component line = lines.get(i);
            if (line == null || line.getStyle().getColor() != null) continue;
            // Zeile beginnt mit eigenem Legacy-Farbcode (§x/§r im Text): nicht umfaerben, sonst setzte ein spaeteres §r
            // auf Grau statt (wie im Original) auf Weiss zurueck.
            String raw = line.getString();
            if (raw.length() > 1 && raw.charAt(0) == '§' && "0123456789abcdefrABCDEFR".indexOf(raw.charAt(1)) >= 0) continue;
            lines.set(i, line.copy().withStyle(line.getStyle().withColor(ChatFormatting.GRAY)));
        }
    }

    /** Nur Gegenstaende dieses Mods (Item-/Block-Item-ID im Namespace hbm_m). */
    public static boolean isHbmStack(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id != null && RefStrings.MODID.equals(id.getNamespace());
    }
}
