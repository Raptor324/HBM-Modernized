package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModTags;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Generates all press recipes (plates, wires, circuits).
 *
 * <p>Использует {@code save(writer, "id")} из {@link BaseRecipeBuilder} —
 * Stonecutter-блоки с {@code ResourceLocation} больше не нужны: хелпер сам
 * строит id через {@code RefStrings.MODID} с учётом версии MC.</p>
 */
public final class PressRecipeGenerator {

    private PressRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        generateFlat(writer);
        generatePlates(writer);
        generateWires(writer);
        generateCircuits(writer);
    }

    private static void generateFlat(Consumer<FinishedRecipe> writer) {
        // Ported from 1.7.10 PressRecipes.java (StampType.FLAT).
        // Only recipes whose input AND output items both exist in this port were ported;
        // see task summary for the recipes that were skipped (dusts, briquettes, pages, etc. don't exist here).

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.BIOMASS_COMPRESSED.get()))
                .stamp(ModTags.Items.STAMPS_FLAT)
                .material(ModItems.BIOMASS.get())
                .save(writer, "biomass_compressed");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.BALL_RESIN.get()))
                .stamp(ModTags.Items.STAMPS_FLAT)
                .material(net.minecraft.world.level.block.Blocks.JUNGLE_LOG)
                .save(writer, "ball_resin");
    }

    private static void generatePlates(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_IRON.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.IRON_INGOT)
                .save(writer, "plate_iron");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COPPER.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.COPPER_INGOT)
                .save(writer, "plate_copper");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GOLD.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.GOLD_INGOT)
                .save(writer, "plate_gold");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_STEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.STEEL).get())
                .save(writer, "plate_steel");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_LEAD.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                .save(writer, "plate_lead");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ADVANCED_ALLOY.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                .save(writer, "plate_advanced_alloy");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SATURNITE.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SATURNITE).get())
                .save(writer, "plate_saturnite");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SCHRABIDIUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                .save(writer, "plate_schrabidium");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_TITANIUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.TITANIUM).get())
                .save(writer, "plate_titanium");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ALUMINUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                .save(writer, "plate_aluminium");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNSTEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNSTEEL).get())
                .save(writer, "plate_gunsteel");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COMBINE_STEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.COMBINE_STEEL).get())
                .save(writer, "plate_combine_steel");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNMETAL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNMETAL).get())
                .save(writer, "plate_gunmetal");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_DURA_STEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.DURA_STEEL).get())
                .save(writer, "plate_dura_steel");
    }

    private static void generateWires(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_COPPER.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.COPPER_INGOT)
                .save(writer, "wire_copper");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_GOLD.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.GOLD_INGOT)
                .save(writer, "wire_gold");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                .save(writer, "wire_advanced_alloy");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                .save(writer, "wire_aluminium");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_CARBON.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                .save(writer, "wire_carbon");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_FINE.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.IRON_INGOT)
                .save(writer, "wire_fine");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.MAGNETIZED_TUNGSTEN).get())
                .save(writer, "wire_magnetized_tungsten");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_RED_COPPER.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.RED_COPPER).get())
                .save(writer, "wire_red_copper");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_SCHRABIDIUM.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                .save(writer, "wire_schrabidium");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_TUNGSTEN.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .save(writer, "wire_tungsten");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.SILICON_CIRCUIT.get()))
                .stamp(ModTags.Items.STAMPS_CIRCUIT)
                .material(ModItems.BILLET_SILICON.get())
                .save(writer, "silicon_circuit");
    }

    private static void generateCircuits(Consumer<FinishedRecipe> writer) {
    }
}
//?}
