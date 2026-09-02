package com.hbm_m.handler.rbmk;

import java.util.LinkedHashMap;
import java.util.Map;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;

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

    /** One activation result: a solid output, a fluid output, or both. Either may be absent. */
    public record OutgasserRecipe(ItemStack solidOutput, Fluid fluidType, int fluidAmount) {

        public boolean hasFluid() { return fluidType != null && fluidAmount > 0; }
        public boolean hasSolid() { return solidOutput != null && !solidOutput.isEmpty(); }
    }

    private static final Map<Item, OutgasserRecipe> RECIPES = new LinkedHashMap<>();
    private static boolean initialised = false;

    private static void put(Item input, ItemStack solid, Fluid fluid, int amount) {
        if (input == null || input == Items.AIR) return;
        RECIPES.put(input, new OutgasserRecipe(solid, fluid, amount));
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

        // --- the tars themselves gas off further ---
        put(ModItems.OIL_TAR_COAL.get(), ItemStack.EMPTY, ModFluids.COALOIL.getSource(),      100);
        put(ModItems.OIL_TAR_WAX.get(),  ItemStack.EMPTY, ModFluids.RADIOSOLVENT.getSource(), 100);
    }

    private static Item ModBlocksItem(net.minecraft.world.level.block.Block block) {
        return block == null ? null : block.asItem();
    }

    public static OutgasserRecipe getRecipe(ItemStack input) {
        if (input == null || input.isEmpty()) return null;
        init();
        return RECIPES.get(input.getItem());
    }

    /** Every registered activation, for JEI and the outgasser's own item filter. */
    public static Map<Item, OutgasserRecipe> getRecipes() {
        init();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
