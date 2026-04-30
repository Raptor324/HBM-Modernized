package com.hbm_m.datagen.recipes.custom;
//? if forge {
/*import java.util.function.Consumer;

import com.hbm_m.datagen.assets.ModItemTagProvider;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/^*
 * Generates all press recipes (plates, wires, circuits).
 ^/
public final class PressRecipeGenerator {

    private PressRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        generatePlates(writer);
        generateWires(writer);
        generateCircuits(writer);
    }

    private static void generatePlates(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_IRON.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.IRON_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_iron"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_iron"));
                //?}


        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COPPER.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.COPPER_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_copper"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_copper"));
                //?}


        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GOLD.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(Items.GOLD_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_gold"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gold"));
                //?}


        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_STEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.STEEL).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_steel"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_steel"));
                //?}


        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_LEAD.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_lead"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_lead"));
                //?}

                
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ADVANCED_ALLOY.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_advanced_alloy"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_advanced_alloy"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SATURNITE.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SATURNITE).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_saturnite"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_saturnite"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_SCHRABIDIUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_schrabidium"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_schrabidium"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_TITANIUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.TITANIUM).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_titanium"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_titanium"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_ALUMINUM.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_aluminium"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_aluminium"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNSTEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNSTEEL).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_gunsteel"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gunsteel"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_COMBINE_STEEL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.COMBINE_STEEL).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_combine_steel"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_combine_steel"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.PLATE_GUNMETAL.get()))
                .stamp(ModTags.Items.STAMPS_PLATE)
                .material(ModItems.getIngot(ModIngots.GUNMETAL).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "plate_gunmetal"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "plate_gunmetal"));
                //?}

    }

    private static void generateWires(Consumer<FinishedRecipe> writer) {
        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_COPPER.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.COPPER_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_copper"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_copper"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_GOLD.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.GOLD_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_gold"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_gold"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ADVANCED_ALLOY.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_advanced_alloy"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_advanced_alloy"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_ALUMINIUM.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.ALUMINUM).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_aluminium"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_aluminium"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_CARBON.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.LEAD).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_carbon"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_carbon"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_FINE.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(Items.IRON_INGOT)
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_fine"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_fine"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.MAGNETIZED_TUNGSTEN).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_magnetized_tungsten"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_magnetized_tungsten"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_RED_COPPER.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.RED_COPPER).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_red_copper"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_red_copper"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_SCHRABIDIUM.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.SCHRABIDIUM).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_schrabidium"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_schrabidium"));
                //?}

        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.WIRE_TUNGSTEN.get(), 8))
                .stamp(ModTags.Items.STAMPS_WIRE)
                .material(ModItems.getIngot(ModIngots.TUNGSTEN).get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "wire_tungsten"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "wire_tungsten"));
                //?}


        PressRecipeBuilder.pressRecipe(new ItemStack(ModItems.SILICON_CIRCUIT.get()))
                .stamp(ModTags.Items.STAMPS_CIRCUIT)
                .material(ModItems.BILLET_SILICON.get())
                //? if fabric && < 1.21.1 {
                /^.save(writer, new ResourceLocation(RefStrings.MODID, "silicon_circuit"));
                ^///?} else {
                                .save(writer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "silicon_circuit"));
                //?}

    }
    private static void generateCircuits(Consumer<FinishedRecipe> writer) {
    }
}
*///?}
