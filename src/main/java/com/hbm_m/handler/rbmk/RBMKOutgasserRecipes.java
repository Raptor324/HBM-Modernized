package com.hbm_m.handler.rbmk;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;

/**
 * 1:1 port of CE's {@code OutgasserRecipes} - the neutron-activation table the RBMK outgasser
 * (irradiation channel) runs on.
 *
 * <p>The port previously had no such table at all: its outgasser was an invented xenon scrubber
 * that consumed flux to strip poison out of a fuel rod, which is not something CE's outgasser ever
 * did. What it actually does is bombard an ordinary item until it transmutes - lithium into
 * tritium, gold into gold-198, thorium-232 into thorium fuel, mushrooms into glowing ones.</p>
 *
 * <p>The coal/tar chain is included: coal (gem, dust or block) cracks into coal tar plus syngas,
 * coal tar gasses off into coal oil, and chlorinated tar wax into radiosolvent.</p>
 */
public class RBMKOutgasserRecipes {

    /**
     * One activation result: a solid output, a fluid output, or both. Either may be absent.
     * {@code fusionOnly} is 1:1 {@code OutgasserRecipe.fusionOnly}: such inputs are rejected by the
     * outgasser and belong to the fusion breeder instead (the default table has none of them; they
     * only appear in user-supplied {@code hbmIrradiation.json} recipes on 1.7.10).
     */
    public record OutgasserRecipe(ItemStack solidOutput, Fluid fluidType, int fluidAmount, boolean fusionOnly) {

        public OutgasserRecipe(ItemStack solidOutput, Fluid fluidType, int fluidAmount) {
            this(solidOutput, fluidType, fluidAmount, false);
        }

        public boolean hasFluid() { return fluidType != null && fluidAmount > 0; }
        public boolean hasSolid() { return solidOutput != null && !solidOutput.isEmpty(); }
    }

    private static final Map<Item, OutgasserRecipe> RECIPES = new LinkedHashMap<>();

    /**
     * 1:1 {@code OutgasserRecipes.getOutput} dict fallback: the same defaults keyed on the port's
     * 1:1 OreDictionary tags ({@code hbm_m:oredict/...} from {@code OreDictTagData}), so any
     * third-party item carrying the same dict tag (any mod's gold ingot, any coal gem) activates
     * too. Consulted only when the concrete-Item lookup misses, mirroring the original's
     * exact-stack pass before the dict-key pass.
     */
    private static final Map<TagKey<Item>, OutgasserRecipe> TAG_RECIPES = new LinkedHashMap<>();
    private static boolean initialised = false;

    private static void put(Item input, ItemStack solid, Fluid fluid, int amount) {
        if (input == null || input == Items.AIR) return;
        RECIPES.put(input, new OutgasserRecipe(solid, fluid, amount));
    }

    /** {@code new OreDictStack(KEY)} of the original, keyed on the port's {@code hbm_m:oredict} tag. */
    private static void putTag(String oredictPath, ItemStack solid, Fluid fluid, int amount) {
        if (oredictPath == null) return;
        TAG_RECIPES.put(TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, oredictPath)),
                new OutgasserRecipe(solid, fluid, amount));
    }

    private static ItemStack one(net.minecraft.core.Registry<Item> ignored, Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static synchronized void init() {
        if (initialised) return;
        initialised = true;

        Fluid tritium = ModFluids.TRITIUM.getSource();

        // --- lithium to tritium ---
        put(ModBlocksItem(com.hbm_m.block.ModBlocks.BLOCK_LITHIUM.get()), ItemStack.EMPTY, tritium, 10_000);
        put(com.hbm_m.item.ModItems.LITHIUM.get(), ItemStack.EMPTY, tritium,  1_000);
        put(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER), ItemStack.EMPTY, tritium,  1_000);
        put(ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.POWDER_TINY), ItemStack.EMPTY, tritium,    100);

        // --- gold to gold-198 ---
        put(Items.GOLD_INGOT,  one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.INGOT)),  null, 0);
        put(Items.GOLD_NUGGET, one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)), null, 0);
        put(ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.POWDER),
                one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.POWDER)),                null, 0);

        // E8 (Fork, nur mit Space-Addon): Space OutgasserRecipes - Kobalt -> Kobalt-60, Bismut -> Polonium-210
        if (com.hbm_m.api.space.SpaceApi.forkActive()) {
            for (MaterialShape shape : new MaterialShape[] { MaterialShape.INGOT, MaterialShape.NUGGET, MaterialShape.POWDER }) {
                put(ModMaterialItems.item(ModMaterials.COBALT, shape), one(null, ModMaterialItems.item(ModMaterials.CO60, shape)), null, 0);
            }
            for (MaterialShape shape : new MaterialShape[] { MaterialShape.INGOT, MaterialShape.NUGGET, MaterialShape.POWDER }) {
                put(ModMaterialItems.item(ModMaterials.BISMUTH, shape), one(null, ModMaterialItems.item(ModMaterials.POLONIUM, shape)), null, 0);
            }
        }

        // --- thorium-232 to thorium fuel ---
        put(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.INGOT),
                one(null, ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.INGOT)),          null, 0);
        put(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.NUGGET),
                one(null, ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)),         null, 0);
        put(ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.BILLET),
                one(null, ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET)),         null, 0);

        // --- mushrooms to glowing mushrooms ---
        put(Blocks.BROWN_MUSHROOM.asItem(), one(null, com.hbm_m.block.ModBlocks.MUSH.get().asItem()), null, 0);
        put(Blocks.RED_MUSHROOM.asItem(),   one(null, com.hbm_m.block.ModBlocks.MUSH.get().asItem()), null, 0);
        put(Items.MUSHROOM_STEW,            one(null, ModItems.GLOWING_STEW.get()),                   null, 0);

        // --- coal cracks into tar and syngas; a coal block yields nine times as much ---
        Fluid syngas = ModFluids.SYNGAS.getSource();
        ItemStack coalTar = new ItemStack(ModItems.OIL_TAR_COAL.get());

        put(Items.COAL,                                  coalTar.copy(),                     syngas,  50);
        put(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER), coalTar.copy(),             syngas,  50);
        put(Blocks.COAL_BLOCK.asItem(),
                new ItemStack(ModItems.OIL_TAR_COAL.get(), 9),                               syngas, 500);

        // Original OutgasserRecipes l.57: PVC -> C4 + Kolloid
        put(ModMaterialItems.item(ModMaterials.PVC, MaterialShape.INGOT),
                new ItemStack(ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT)), ModFluids.COLLOID.getSource(), 250);

        // --- the tars themselves gas off further ---
        put(ModItems.OIL_TAR_COAL.get(), ItemStack.EMPTY, ModFluids.COALOIL.getSource(),      100);
        put(ModItems.OIL_TAR_WAX.get(),  ItemStack.EMPTY, ModFluids.RADIOSOLVENT.getSource(), 100);

        // --- the original table's OreDictStack defaults (OutgasserRecipes.registerDefaults), keyed
        // on the port's 1:1 dict tags so third-party items carrying the same tags activate too.
        // blockLithium, billetThorium232 and ingotPVC have no hbm_m:oredict tag in OreDictTagData -
        // their canon items stay covered by the concrete table above.
        putTag("oredict/ingot/lithium",     ItemStack.EMPTY, tritium,  1_000);
        putTag("oredict/dust/lithium",      ItemStack.EMPTY, tritium,  1_000);
        putTag("oredict/dust_tiny/lithium", ItemStack.EMPTY, tritium,    100);
        putTag("oredict/ingot/gold",  one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.INGOT)),  null, 0);
        putTag("oredict/nugget/gold", one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.NUGGET)), null, 0);
        putTag("oredict/dust/gold",   one(null, ModMaterialItems.item(ModMaterials.AU198, MaterialShape.POWDER)), null, 0);
        putTag("oredict/ingot/thorium232",  one(null, ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.INGOT)),  null, 0);
        putTag("oredict/nugget/thorium232", one(null, ModMaterialItems.item(ModMaterials.THORIUM_FUEL, MaterialShape.NUGGET)), null, 0);
        putTag("oredict/gem/coal",  coalTar.copy(), syngas,  50);
        putTag("oredict/dust/coal", coalTar.copy(), syngas,  50);
        putTag("oredict/block/coal", new ItemStack(ModItems.OIL_TAR_COAL.get(), 9), syngas, 500);
    }

    private static Item ModBlocksItem(net.minecraft.world.level.block.Block block) {
        return block == null ? null : block.asItem();
    }

    public static OutgasserRecipe getRecipe(ItemStack input) {
        if (input == null || input.isEmpty()) return null;
        init();
        // 1:1 OutgasserRecipes.getOutput: the exact stack first, then the item's dict keys.
        OutgasserRecipe exact = RECIPES.get(input.getItem());
        if (exact != null) return exact;
        for (Map.Entry<TagKey<Item>, OutgasserRecipe> e : TAG_RECIPES.entrySet()) {
            if (input.is(e.getKey())) return e.getValue();
        }
        return null;
    }

    /** Every registered activation, for JEI and the outgasser's own item filter. */
    public static Map<Item, OutgasserRecipe> getRecipes() {
        init();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
