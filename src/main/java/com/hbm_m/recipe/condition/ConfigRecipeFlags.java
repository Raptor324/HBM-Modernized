package com.hbm_m.recipe.condition;

import java.util.Locale;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.hbm_m.config.GeneralConfig;

/**
 * Konfig-Schalter, von denen im Original Rezepte abhaengen ({@code GeneralConfig.enableLBSM} + Unterschalter,
 * {@code enable528}, {@code enable528PressurizedRecipes}, {@code enableExpensiveMode}). Das Original liest sie beim
 * Registrieren der Rezepte; im Port werden sie beim Laden der Rezept-JSONs ausgewertet (Neustart noetig, wie
 * im Original).
 *
 * <p>Zwei Wege, beide mit denselben Schalternamen:</p>
 * <ul>
 *   <li>Ob ein Rezept existiert: Forge-Bedingung {@code {"type":"hbm_m:config","flag":"..."}} unter
 *       {@code "conditions"} (siehe {@code ConfigRecipeCondition}).</li>
 *   <li>Welche Fassung gilt (gleiche Rezept-ID, z. B. {@code inputItemsEx}): {@code "hbm_variants":[{"flags":[...],
 *       "recipe":{...}}]} - die erste Fassung, deren Schalter alle an sind, ersetzt den Rezeptinhalt
 *       ({@link #resolveVariants}).</li>
 * </ul>
 * Ein vorangestelltes {@code !} verneint einen Schalter.
 */
public final class ConfigRecipeFlags {

    public static final String VARIANTS_KEY = "hbm_variants";

    private ConfigRecipeFlags() {}

    /** Wert eines Schalters (mit optionalem {@code !}). Unbekannte Namen sind ein Fehler. */
    public static boolean test(String flag) {
        String f = flag.trim();
        if (f.startsWith("!")) return !test(f.substring(1));
        return switch (f.toLowerCase(Locale.ROOT)) {
            case "528" -> GeneralConfig.enable528;
            // Original: enable528PressurizedRecipes ist ohne 528 nach loadFromConfig aus
            case "528_pressurized" -> GeneralConfig.pressurizedRecipes528();
            case "expensive" -> GeneralConfig.enableExpensiveMode;
            // Original: if(enable528) enableLBSM = false
            case "lbsm" -> GeneralConfig.lbsm();
            case "lbsm_armor" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleArmorRecipes;
            case "lbsm_tool" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleToolRecipes;
            case "lbsm_alloy" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleAlloy;
            case "lbsm_chemistry" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleChemsitry;
            case "lbsm_centrifuge" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleCentrifuge;
            case "lbsm_anvil" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMUnlockAnvil;
            case "lbsm_crafting" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleCrafting;
            case "lbsm_medicine" -> GeneralConfig.lbsm() && GeneralConfig.enableLBSMSimpleMedicineRecipes;
            default -> throw new IllegalArgumentException("Unbekannter Rezept-Konfigschalter: " + flag);
        };
    }

    /** Alle Schalter muessen an sein. */
    public static boolean testAll(Iterable<String> flags) {
        for (String f : flags) if (!test(f)) return false;
        return true;
    }

    /**
     * Ersetzt den Rezeptinhalt durch die erste passende Fassung aus {@value #VARIANTS_KEY}. Der Rezepttyp bleibt;
     * ohne passende Fassung bleibt das Rezept, wie es ist. Das Original nimmt im Expensive-Modus
     * {@code inputItemsEx} statt {@code inputItems} usw.
     */
    public static JsonObject resolveVariants(JsonObject json) {
        if (json == null || !json.has(VARIANTS_KEY)) return json;
        JsonArray variants = json.getAsJsonArray(VARIANTS_KEY);
        for (JsonElement e : variants) {
            JsonObject v = e.getAsJsonObject();
            boolean ok = true;
            for (JsonElement f : v.getAsJsonArray("flags")) {
                if (!test(f.getAsString())) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            JsonObject out = v.getAsJsonObject("recipe").deepCopy();
            if (json.has("type")) out.add("type", json.get("type"));
            return out;
        }
        JsonObject out = json.deepCopy();
        out.remove(VARIANTS_KEY);
        return out;
    }
}
