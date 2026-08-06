package com.hbm_m.recipe;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item-based breeder recipe registry - Java-hardcoded-list port of the 1.7.10
 * {@code com.hbm.inventory.recipes.BreederRecipes} (see {@code TileEntityMachineReactorBreeding}
 * for the original single-block-per-tick application logic; that class is a {@code BlockDummyable}
 * multiblock and its item-transmutation logic is what got ported here 1:1, minus the
 * {@code ItemBreedingRod}/{@code BreedingRodType} meta-item system which does not exist in this
 * port).
 *
 * <p>MATERIAL SUBSTITUTIONS (the original recipes operate on {@code ItemBreedingRod} meta-variants
 * - single/dual/quad "fuel rod" items - which were never ported to this codebase; there is no
 * {@code ItemBreedingRod} class here). Each recipe below is remapped onto the closest existing raw
 * material item from {@link ModIngots} / {@link ModItems}, since those already represent the same
 * elements/isotopes in solid form:</p>
 * <ul>
 *   <li>CO -&gt; CO60: {@code ModIngots.COBALT} -&gt; {@code ModIngots.CO60} (exact isotope match)</li>
 *   <li>RA226 -&gt; AC227: {@code ModIngots.RA226} -&gt; {@code ModIngots.ACTINIUM} (Ac-227, per its
 *       translation comment "Слиток актиния-227")</li>
 *   <li>TH232 -&gt; THF: {@code ModIngots.THORIUM232} -&gt; {@code ModIngots.THORIUM} (no thorium
 *       fluoride intermediate exists in this port; approximated as processed thorium)</li>
 *   <li>U235 -&gt; NP237: {@code ModIngots.URANIUM235} -&gt; {@code ModIngots.NEPTUNIUM} (Np-237 is
 *       this mod's default neptunium isotope)</li>
 *   <li>NP237 -&gt; PU238: {@code ModIngots.NEPTUNIUM} -&gt; {@code ModIngots.PLUTONIUM238}</li>
 *   <li>PU238 -&gt; PU239: {@code ModIngots.PLUTONIUM238} -&gt; {@code ModIngots.PLUTONIUM239}</li>
 *   <li>U238 -&gt; RGP, URANIUM -&gt; RGP: {@code ModIngots.URANIUM238} / {@code ModIngots.URANIUM}
 *       -&gt; {@code ModIngots.PU_MIX} ("Plutonium Mix" is this port's closest analog to the
 *       original's "Reactor Grade Plutonium")</li>
 *   <li>RGP -&gt; WASTE: {@code ModIngots.PU_MIX} -&gt; {@code ModItems.NUCLEAR_WASTE}</li>
 * </ul>
 * <p>NOT PORTED: LITHIUM -&gt; TRITIUM (tritium only exists as a gas/fluid in this mod, there is no
 * solid tritium item to breed into) and the {@code meteorite_sword_etched -> meteorite_sword_bred}
 * easter-egg recipe (neither item exists in this port). The {@code rod}/{@code rod_dual}/
 * {@code rod_quad} 2x/3x flux multiplier variants from the original are likewise dropped along with
 * the rod system itself - each element below is a single recipe.</p>
 */
public class BreederRecipes {

    public static final class BreederRecipe {
        public final ItemStack output;
        /** Energy (FE) drawn per tick while this recipe is active - reuses the original's "flux" balance number 1:1. */
        public final int energyPerTick;

        public BreederRecipe(ItemStack output, int energyPerTick) {
            this.output = output;
            this.energyPerTick = energyPerTick;
        }
    }

    private static final Map<Item, BreederRecipe> RECIPES = new HashMap<>();

    public static void registerRecipes() {
        RECIPES.clear();

        register(ModIngots.COBALT, ModIngots.CO60, 100);
        register(ModIngots.RA226, ModIngots.ACTINIUM, 300);
        register(ModIngots.THORIUM232, ModIngots.THORIUM, 500);
        register(ModIngots.URANIUM235, ModIngots.NEPTUNIUM, 300);
        register(ModIngots.NEPTUNIUM, ModIngots.PLUTONIUM238, 200);
        register(ModIngots.PLUTONIUM238, ModIngots.PLUTONIUM239, 1000);
        register(ModIngots.URANIUM238, ModIngots.PU_MIX, 300);
        register(ModIngots.URANIUM, ModIngots.PU_MIX, 200);

        Item puMix = ModItems.getIngot(ModIngots.PU_MIX).get();
        Item waste = ModItems.NUCLEAR_WASTE.get();
        RECIPES.put(puMix, new BreederRecipe(new ItemStack(waste), 200));
    }

    private static void register(ModIngots input, ModIngots output, int energyPerTick) {
        Item inItem = ModItems.getIngot(input).get();
        Item outItem = ModItems.getIngot(output).get();
        RECIPES.put(inItem, new BreederRecipe(new ItemStack(outItem), energyPerTick));
    }

    public static BreederRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return RECIPES.get(stack.getItem());
    }

    public static Map<Item, BreederRecipe> getAllRecipes() {
        return RECIPES;
    }
}
