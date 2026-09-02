package com.hbm_m.util;

import java.util.Locale;
import java.util.Map;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeItem;

/**
 * Порт ColorUtil (1.7.10): цвет красителя для окраски кабелей ЛЭП.
 * Значения — та же таблица nameToColor из оригинала.
 */
public final class ColorUtil {

    public static final Map<String, Integer> NAME_TO_COLOR = Map.ofEntries(
            Map.entry("white", 15790320),
            Map.entry("orange", 15435844),
            Map.entry("magenta", 12801229),
            Map.entry("lightblue", 6719955),
            Map.entry("yellow", 14602026),
            Map.entry("lime", 4312372),
            Map.entry("pink", 14188952),
            Map.entry("gray", 4408131),
            Map.entry("lightgray", 11250603),
            Map.entry("silver", 11250603),
            Map.entry("cyan", 2651799),
            Map.entry("purple", 8073150),
            Map.entry("blue", 2437522),
            Map.entry("brown", 5320730),
            Map.entry("green", 3887386),
            Map.entry("red", 11743532),
            Map.entry("black", 1973019)
    );

    private ColorUtil() {}

    public static int getColorFromDye(ItemStack stack) {
        if (stack.getItem() instanceof DyeItem dye) {
            String name = dye.getDyeColor().getName().toLowerCase(Locale.US);
            Integer color = NAME_TO_COLOR.get(name);
            return color != null ? color : 0;
        }
        return 0;
    }
}
