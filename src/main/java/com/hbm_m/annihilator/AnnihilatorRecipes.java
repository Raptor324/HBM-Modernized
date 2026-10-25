package com.hbm_m.annihilator;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.hbm_m.config.GeneralConfig;
import com.hbm_m.item.industrial.BlueprintPools;
import com.hbm_m.item.industrial.ItemBlueprints;

import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code AnnihilatorRecipes}: Meilensteine je Pool-Schluessel. Erreicht der Zaehler eines Schluessels eine Stufe,
 * zahlt der Annihilator den hoechsten neu erreichten Meilenstein aus. Standardmaessig (wie im Original) nur im
 * 528-Modus belegt: Blaupausen fuer die 528-Rezeptpools.
 *
 * <p>Schluessel wie im {@link AnnihilatorPoolManager}: {@code item:<id>}, {@code comp:<id>}, {@code fluid:<id>},
 * {@code dict:<tag>} (Oredict-Namen sind im Port die Item-Tags, siehe {@code ModulePatternMatcher.getOreDictNames}).</p>
 */
public final class AnnihilatorRecipes {

    public record Milestone(BigInteger amount, Supplier<ItemStack> payout) { }

    public static final Map<String, List<Milestone>> recipes = new HashMap<>();
    private static boolean initialized = false;

    private AnnihilatorRecipes() {}

    private static void put(String key, String amount, String pool) {
        recipes.computeIfAbsent(key, k -> new ArrayList<>())
                .add(new Milestone(new BigInteger(amount), () -> ItemBlueprints.make(BlueprintPools.POOL_PREFIX_528 + pool)));
    }

    /**
     * Original {@code registerDefaults}. Oredict-Schluessel werden auf die Forge-Tags abgebildet; die Sammelnamen
     * {@code ANY_PLASTIC}, {@code ANY_HARDPLASTIC}, {@code ANY_RESISTANTALLOY} auf die synthetischen Namen aus
     * {@link AnnihilatorPoolManager#dictNames}.
     */
    public static void registerDefaults() {
        if (initialized) return;
        initialized = true;

        if (GeneralConfig.enable528) {
            put("dict:forge:ingots/steel", "256", "steel");
            put("dict:forge:billets/silicon", "256", "chip");
            put("dict:forge:nuggets/bismuth", "128", "chip_bismoid");
            put("item:hbm_m:pellet_charged", "1024", "chip_quantum");

            put("dict:forge:billets/uranium", "256", "gascent");
            put("dict:" + AnnihilatorPoolManager.ANY_PLASTIC, "512", "plastic");
            put("dict:forge:ingots/rubber", "512", "rubber");
            put("dict:forge:ingots/ferrouranium", "1024", "ferrouranium");
            put("dict:forge:dusts/strontium", "256", "strontium");
            put("dict:" + AnnihilatorPoolManager.ANY_HARDPLASTIC, "1024", "hardplastic");
            put("dict:" + AnnihilatorPoolManager.ANY_RESISTANTALLOY, "1024", "tcalloy");
            put("item:hbm_m:powder_chlorophyte", "1024", "chlorophyte");

            put("item:hbm_m:drive_flash_flightsim", "64", "soyuz");
            put("comp:hbm_m:ammo_standard_bmg50_fmj", "256", "bmg");
            put("comp:hbm_m:ammo_arty", "128", "arty");
            put("comp:hbm_m:controller", "128", "controller");
        }
    }

    /**
     * Original {@code getHighestPayoutFromKey}: Ist {@code prevAmount} null, wird ausgezahlt, sobald der Zaehler die
     * Schwelle erreicht; sonst muss der vorherige Stand darunter gelegen haben.
     */
    public static ItemStack getHighestPayoutFromKey(String key, BigInteger prevAmount, BigInteger currentAmount) {
        registerDefaults();
        List<Milestone> recipe = recipes.get(key);
        if (recipe != null) return getHighestPayoutFromRecipe(recipe, prevAmount, currentAmount);
        return null;
    }

    public static ItemStack getHighestPayoutFromRecipe(List<Milestone> recipe, BigInteger prevAmount, BigInteger currentAmount) {

        BigInteger highestYet = BigInteger.ZERO;
        Milestone highestPayout = null;
        for (Milestone milestone : recipe) {
            if (prevAmount != null && prevAmount.compareTo(milestone.amount()) != -1) continue; // vorheriger Stand schon darueber
            if (currentAmount.compareTo(highestYet) != 1) continue;
            if (currentAmount.compareTo(milestone.amount()) != -1) {
                highestYet = milestone.amount();
                highestPayout = milestone;
            }
        }

        return highestPayout != null ? highestPayout.payout().get().copy() : null;
    }
}
