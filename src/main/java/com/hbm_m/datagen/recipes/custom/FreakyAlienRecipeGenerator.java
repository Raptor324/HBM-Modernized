package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.recipe.AnvilRecipe.OverlayType;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Joke recycling of the freaky_alien_block (no 1.7.10 analog - user-requested content):
 * centrifuge absurd cocktail, anvil dismantling with glyphid/singularity chances, shredder pancake
 * (blade shreds itself separately in {@code MachineShredderBlockEntity}), chemical plant dissolves it
 * in sulfuric acid into the "Unstable Sanity" fluid.
 */
public final class FreakyAlienRecipeGenerator {

    private FreakyAlienRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        Item alien = item("hbm_m:freaky_alien_block");

        // Centrifuge: absurd but partly useful cocktail
        CentrifugeRecipeBuilder.itemRecipe(alien,
                stack("hbm_m:nugget_schrabidium", 1),
                new ItemStack(Items.ROTTEN_FLESH, 2),
                stack("hbm_m:lead_ingot", 1),
                new ItemStack(Items.DEAD_BUSH, 1))
                .save(writer, "centrifuge/freaky_alien");

        // Anvil dismantling: glyphid meat, glyphid egg, tiny singularity chance
        AnvilRecipeBuilder.construction(1, 0, OverlayType.RECYCLING)
                .input(Ingredient.of(alien), 1, true)
                .addOutput(stack("hbm_m:glyphid_meat", 2))
                .addOutput(stack("hbm_m:egg_glyphid", 1), 0.5F)
                .addOutput(stack("hbm_m:singularity", 1), 0.01F)
                .save(writer, "anvil/freaky_alien");

        // Shredder: pancakes made from scrap metal, nails and gem dust (the blades complain on their own)
        ShredderRecipeBuilder.shredderRecipe(Ingredient.of(alien), stack("hbm_m:pancake", 1))
                .save(writer, "shredder/freaky_alien");

        // Chemical plant: dissolve in sulfuric acid -> Concentrate REN-TV (Unstable Sanity)
        ChemicalPlantRecipeBuilder.chemicalPlantRecipe(200, 100)
                .addItemInput(Ingredient.of(alien), 1)
                .addFluidInput(ModFluids.SULFURIC_ACID.getSource(), 4000)
                .addFluidOutput(ModFluids.UNSTABLE_SANITY.getSource(), 2000)
                .withIconItem(alien)
                .save(writer, "chemplant/freaky_alien");
    }

    private static Item item(String id) {
        Item it = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hbm_m", id.substring("hbm_m:".length())));
        if (it == Items.AIR) throw new IllegalStateException("FreakyAlienRecipeGenerator: unknown item " + id);
        return it;
    }

    private static ItemStack stack(String id, int count) {
        return new ItemStack(item(id), count);
    }
}
//?}
