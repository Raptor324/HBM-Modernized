package com.hbm_m.api.redstoneoverradio;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;

/**
 * Rezeptnamen fuer Funk-Werte ({@code VAL:recipe}) und Funk-Befehle ({@code FUN:setrecipe!name}).
 * <p>
 * Im Original sind Rezeptnamen schlichte Zeichenketten ohne Doppelpunkt ({@code GenericRecipe.getInternalName}).
 * Im Port sind es Rezept-IDs ({@code hbm_m:pfad}); der Doppelpunkt ist aber auch der Parametertrenner
 * ({@link IRORInteractive#PARAM_SEPARATOR}). Daher: eigene Rezepte werden ohne Namensraum gemeldet und
 * angenommen, fremde mit - und zerlegte Parameter werden wieder zusammengesetzt.
 */
public final class RORRecipeNames {

    private static final String NAMESPACE = com.hbm_m.lib.RefStrings.MODID;

    private RORRecipeNames() { }

    /** Original {@code getRecipeName()}: ohne Rezept {@code "null"}. */
    public static String name(@Nullable ResourceLocation id) {
        if (id == null) return "null";
        return NAMESPACE.equals(id.getNamespace()) ? id.getPath() : id.toString();
    }

    /** Rezept-ID aus den Befehlsparametern; {@code null} bei leerem oder ungueltigem Namen. */
    @Nullable
    public static ResourceLocation parse(String[] params) {
        if (params == null || params.length == 0) return null;
        String joined = String.join(IRORInteractive.PARAM_SEPARATOR, params);
        if (joined.isEmpty() || "null".equals(joined)) return null;
        return joined.contains(":") ? ResourceLocation.tryParse(joined) : ResourceLocation.tryParse(NAMESPACE + ":" + joined);
    }
}
