package com.hbm_m.datagen.recipes.custom;
//? if forge {
import java.util.function.Consumer;

import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
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
        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.IRON, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.IRON_INGOT)
                .save(writer, "plate_iron");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.COPPER, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.COPPER_INGOT)
                .save(writer, "plate_copper");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.GOLD, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.GOLD_INGOT)
                .save(writer, "plate_gold");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.STEEL, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .save(writer, "plate_steel");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.LEAD, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT))
                .save(writer, "plate_lead");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.ADVANCED_ALLOY, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.INGOT))
                .save(writer, "plate_advanced_alloy");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.SATURNITE, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.INGOT))
                .save(writer, "plate_saturnite");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.SCHRABIDIUM, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT))
                .save(writer, "plate_schrabidium");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.TITANIUM, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT))
                .save(writer, "plate_titanium");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.ALUMINUM, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT))
                .save(writer, "plate_aluminium");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.GUNSTEEL, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.GUNSTEEL, MaterialShape.INGOT))
                .save(writer, "plate_gunsteel");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.COMBINE_STEEL, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.INGOT))
                .save(writer, "plate_combine_steel");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.GUNMETAL, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.INGOT))
                .save(writer, "plate_gunmetal");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.DURA_STEEL, MaterialShape.PLATE, 1))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.INGOT))
                .save(writer, "plate_dura_steel");
    }

    private static void generateWires(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.COPPER, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.COPPER_INGOT)
                .save(writer, "wire_copper");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.GOLD, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.GOLD_INGOT)
                .save(writer, "wire_gold");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.ADVANCED_ALLOY, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.INGOT))
                .save(writer, "wire_advanced_alloy");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.ALUMINIUM, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT))
                .save(writer, "wire_aluminium");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.CARBON, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT))
                .save(writer, "wire_carbon");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_FINE.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.IRON_INGOT)
                .save(writer, "wire_fine");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.INGOT))
                .save(writer, "wire_magnetized_tungsten");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.RED_COPPER, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT))
                .save(writer, "wire_red_copper");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.INGOT))
                .save(writer, "wire_schrabidium");

        PressRecipeBuilder.pressRecipe(ModMaterialItems.stack(ModMaterials.TUNGSTEN, MaterialShape.WIRE, 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT))
                .save(writer, "wire_tungsten");

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.SILICON_CIRCUIT.get()))
                .stamp(ModTags.Items.STAMPS_CIRCUIT)
                .material(ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.BILLET))
                .save(writer, "silicon_circuit");
    }

    private static void generateCircuits(Consumer<FinishedRecipe> writer) {
    }
}
//?}
