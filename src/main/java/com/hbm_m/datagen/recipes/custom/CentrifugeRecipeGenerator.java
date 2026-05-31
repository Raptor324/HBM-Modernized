package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.lib.RefStrings;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.function.Consumer;

/**
 * Generates centrifuge ore/crystal recipes previously stored under {@code data/hbm_m/recipes/centrifuge/}.
 */
public final class CentrifugeRecipeGenerator {

    private CentrifugeRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerOreRecipes(writer);
        registerCrystalRecipes(writer);
    }

    private static void registerOreRecipes(Consumer<FinishedRecipe> writer) {
        tagRecipe(writer, "aluminum_ore", "forge:ores/aluminum",
                ingotPowder(ModIngots.ALUMINUM), ingotPowder(ModIngots.ALUMINUM),
                ingotPowder(ModIngots.TITANIUM), gravel());

        tagRecipe(writer, "asbestos_ore", "forge:ores/asbestos",
                ingotPowder(ModIngots.ASBESTOS), ingotPowder(ModIngots.ASBESTOS),
                ingotPowder(ModIngots.ASBESTOS), gravel());

        tagRecipe(writer, "beryllium_ore", "forge:ores/beryllium",
                ingotPowder(ModIngots.BERYLLIUM), ingotPowder(ModIngots.BERYLLIUM),
                new ItemStack(Items.EMERALD), gravel());

        tagRecipe(writer, "cinnabar_ore", "forge:ores/cinnabar",
                stack(ModItems.CINNABAR.get(), 2), stack(ModItems.CINNABAR.get(), 2),
                stack(ModItems.SULFUR.get(), 1), gravel());

        tagRecipe(writer, "coal_ore", "forge:ores/coal",
                modPowderCount(ModPowders.COAL, 2), modPowderCount(ModPowders.COAL, 2),
                modPowderCount(ModPowders.COAL, 2), gravel());

        tagRecipe(writer, "fluorite_ore", "forge:ores/fluorite",
                stack(ModItems.FLUORITE.get(), 3), stack(ModItems.FLUORITE.get(), 3),
                stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), gravel());

        tagRecipe(writer, "gold_ore", "forge:ores/gold",
                modPowder(ModPowders.GOLD), modPowder(ModPowders.GOLD), modPowder(ModPowders.GOLD), gravel());

        tagRecipe(writer, "iron_ore", "forge:ores/iron",
                modPowder(ModPowders.IRON), modPowder(ModPowders.IRON), modPowder(ModPowders.IRON), gravel());

        tagRecipe(writer, "lead_ore", "forge:ores/lead",
                ingotPowder(ModIngots.LEAD), ingotPowder(ModIngots.LEAD),
                modPowder(ModPowders.GOLD), gravel());

        tagRecipe(writer, "lignite_ore", "forge:ores/lignite",
                stack(ModItems.LIGNITE.get(), 2), stack(ModItems.LIGNITE.get(), 2),
                stack(ModItems.LIGNITE.get(), 2), gravel());

        tagRecipe(writer, "rareground_ore", "forge:ores/rareground",
                stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1),
                stack(ModItems.DUST.get(), 2), gravel());

        tagRecipe(writer, "sulfur_ore", "forge:ores/sulfur",
                stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4),
                stack(ModItems.SULFUR.get(), 4), gravel());

        tagRecipe(writer, "thorium_ore", "forge:ores/thorium",
                ingotPowder(ModIngots.THORIUM), ingotPowder(ModIngots.THORIUM),
                ingotPowder(ModIngots.URANIUM), gravel());

        tagRecipe(writer, "titanium_ore", "forge:ores/titanium",
                ingotPowder(ModIngots.TITANIUM), ingotPowder(ModIngots.TITANIUM),
                modPowder(ModPowders.IRON), gravel());

        tagRecipe(writer, "tungsten_ore", "forge:ores/tungsten",
                ingotPowder(ModIngots.TUNGSTEN), ingotPowder(ModIngots.TUNGSTEN),
                modPowder(ModPowders.IRON), gravel());

        tagRecipe(writer, "uranium_ore", "forge:ores/uranium",
                ingotPowder(ModIngots.URANIUM), ingotPowder(ModIngots.URANIUM),
                stack(ModItems.getIngot(ModIngots.RA226).get(), 1), gravel());
    }

    private static void registerCrystalRecipes(Consumer<FinishedRecipe> writer) {
        CentrifugeRecipeBuilder.itemRecipe(ModItems.CRYSTAL_GOLD.get(),
                        modPowderCount(ModPowders.GOLD, 2),
                        modPowderCount(ModPowders.GOLD, 2),
                        modPowderCount(ModPowders.GOLD, 2),
                        gravel())
                .save(writer, recipeId("gold_crystal"));
    }

    private static void tagRecipe(Consumer<FinishedRecipe> writer, String name, String tag, ItemStack... outputs) {
        CentrifugeRecipeBuilder.tagRecipe(tag, outputs)
                .save(writer, recipeId(name));
    }

    private static ResourceLocation recipeId(String name) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "centrifuge/" + name);
    }

    private static ItemStack gravel() {
        return new ItemStack(Items.GRAVEL);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack modPowder(ModPowders powder) {
        return new ItemStack(ModItems.getPowders(powder).get(), 1);
    }

    private static ItemStack modPowderCount(ModPowders powder, int count) {
        return new ItemStack(ModItems.getPowders(powder).get(), count);
    }

    private static ItemStack ingotPowder(ModIngots ingot) {
        return new ItemStack(ModItems.getPowder(ingot).get(), 1);
    }
}
//?}
