package com.hbm_m.datagen.recipes.custom;
//? if forge {
import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.lib.RefStrings;

import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.function.Consumer;

public final class CentrifugeRecipeGenerator {

    private CentrifugeRecipeGenerator() {
    }

    public static void generate(Consumer<FinishedRecipe> writer) {
        registerOreRecipes(writer);
        registerCrystalRecipes(writer);
        registerBedrockRecipes(writer);
    }

    private static void registerOreRecipes(Consumer<FinishedRecipe> writer) {
        // Vanilla Ores
        tagRecipe(writer, "coal_ore", "forge:ores/coal",
                modPowderCount(ModMaterials.COAL, 2), modPowderCount(ModMaterials.COAL, 2), modPowderCount(ModMaterials.COAL, 2), gravel());
        tagRecipe(writer, "iron_ore", "forge:ores/iron",
                modPowder(ModMaterials.IRON), modPowder(ModMaterials.IRON), modPowder(ModMaterials.IRON), gravel());
        tagRecipe(writer, "gold_ore", "forge:ores/gold",
                modPowder(ModMaterials.GOLD), modPowder(ModMaterials.GOLD), modPowder(ModMaterials.GOLD), gravel());
        tagRecipe(writer, "copper_ore", "forge:ores/copper",
                modPowder(ModMaterials.COPPER), modPowder(ModMaterials.COPPER), modPowder(ModMaterials.GOLD), gravel());
        tagRecipe(writer, "diamond_ore", "forge:ores/diamond",
                modPowder(ModMaterials.DIAMOND), modPowder(ModMaterials.DIAMOND), modPowder(ModMaterials.DIAMOND), gravel());
        tagRecipe(writer, "emerald_ore", "forge:ores/emerald",
                modPowder(ModMaterials.EMERALD), modPowder(ModMaterials.EMERALD), modPowder(ModMaterials.EMERALD), gravel());
        // tagRecipe(writer, "redstone_ore", "forge:ores/redstone",
        //         stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), ingot(ModMaterials.MERCURY, 1), gravel());
        tagRecipe(writer, "lapis_ore", "forge:ores/lapis",
                modPowderCount(ModMaterials.LAPIS, 6), ingotPowderCount(ModMaterials.COBALT, 1), stack(ModItems.GEM_SODALITE.get(), 1), gravel());
        tagRecipe(writer, "quartz_ore", "forge:ores/quartz",
                modPowder(ModMaterials.QUARTZ), modPowder(ModMaterials.QUARTZ), modPowderTiny(ModMaterials.LITHIUM), stack(Items.NETHERRACK, 1));

        // HBM Ores
        tagRecipe(writer, "uranium_ore", "forge:ores/uranium",
                ingotPowder(ModMaterials.URANIUM), ingotPowder(ModMaterials.URANIUM), ingot(ModMaterials.RA226, 1), gravel());
        tagRecipe(writer, "thorium_ore", "forge:ores/thorium",
                ingotPowder(ModMaterials.THORIUM), ingotPowder(ModMaterials.THORIUM), ingotPowder(ModMaterials.URANIUM), gravel());
        tagRecipe(writer, "titanium_ore", "forge:ores/titanium",
                ingotPowder(ModMaterials.TITANIUM), ingotPowder(ModMaterials.TITANIUM), modPowder(ModMaterials.IRON), gravel());
        tagRecipe(writer, "tungsten_ore", "forge:ores/tungsten",
                ingotPowder(ModMaterials.TUNGSTEN), ingotPowder(ModMaterials.TUNGSTEN), modPowder(ModMaterials.IRON), gravel());
        tagRecipe(writer, "lead_ore", "forge:ores/lead",
                ingotPowder(ModMaterials.LEAD), ingotPowder(ModMaterials.LEAD), modPowder(ModMaterials.GOLD), gravel());
        tagRecipe(writer, "beryllium_ore", "forge:ores/beryllium",
                ingotPowder(ModMaterials.BERYLLIUM), ingotPowder(ModMaterials.BERYLLIUM), stack(Items.EMERALD, 1), gravel());
        tagRecipe(writer, "fluorite_ore", "forge:ores/fluorite",
                stack(ModItems.FLUORITE.get(), 3), stack(ModItems.FLUORITE.get(), 3), stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), gravel());
        tagRecipe(writer, "aluminum_ore", "forge:ores/aluminum",
                ingotPowder(ModMaterials.ALUMINUM), ingotPowder(ModMaterials.ALUMINUM), ingotPowder(ModMaterials.TITANIUM), gravel());
        tagRecipe(writer, "sulfur_ore", "forge:ores/sulfur",
                stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), gravel());
        tagRecipe(writer, "lignite_ore", "forge:ores/lignite",
                stack(ModItems.LIGNITE.get(), 2), stack(ModItems.LIGNITE.get(), 2), stack(ModItems.LIGNITE.get(), 2), gravel());
        tagRecipe(writer, "asbestos_ore", "forge:ores/asbestos",
                ingotPowder(ModMaterials.ASBESTOS), ingotPowder(ModMaterials.ASBESTOS), ingotPowder(ModMaterials.ASBESTOS), gravel());
        tagRecipe(writer, "cinnabar_ore", "forge:ores/cinnabar",
                stack(ModItems.CINNABAR.get(), 2), stack(ModItems.CINNABAR.get(), 2), stack(ModItems.SULFUR.get(), 1), gravel());
        tagRecipe(writer, "rareground_ore", "forge:ores/rareground",
                stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), stack(ModItems.RAREGROUND_ORE_CHUNK.get(), 1), stack(ModItems.DUST.get(), 2), gravel());
        tagRecipe(writer, "cobalt_ore", "forge:ores/cobalt",
                ingotPowderCount(ModMaterials.COBALT, 2), modPowder(ModMaterials.IRON), modPowder(ModMaterials.COPPER), gravel());
    }

    private static void registerCrystalRecipes(Consumer<FinishedRecipe> writer) {
        itemRecipe(writer, "crystal_coal", ModMaterialItems.item(ModMaterials.COAL, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.COAL, 3), modPowderCount(ModMaterials.COAL, 3), modPowderCount(ModMaterials.COAL, 3), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_iron", ModMaterialItems.item(ModMaterials.IRON, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.IRON, 2), modPowderCount(ModMaterials.IRON, 2), ingotPowder(ModMaterials.TITANIUM), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_gold", ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.GOLD, 2), modPowderCount(ModMaterials.GOLD, 2), modPowderCount(ModMaterials.GOLD, 2), gravel());
        // itemRecipe(writer, "crystal_redstone", ModItems.CRYSTAL_REDSTONE.get(),
        //         stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), stack(Items.REDSTONE, 3), ingot(ModMaterials.MERCURY, 3));
        itemRecipe(writer, "crystal_lapis", ModMaterialItems.item(ModMaterials.LAPIS, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.LAPIS, 4), modPowderCount(ModMaterials.LAPIS, 4), ingotPowder(ModMaterials.COBALT), stack(ModItems.GEM_SODALITE.get(), 2));
        itemRecipe(writer, "crystal_diamond", ModMaterialItems.item(ModMaterials.DIAMOND, MaterialShape.CRYSTAL),
                modPowder(ModMaterials.DIAMOND), modPowder(ModMaterials.DIAMOND), modPowder(ModMaterials.DIAMOND), modPowder(ModMaterials.DIAMOND));
        itemRecipe(writer, "crystal_uranium", ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.URANIUM, 2), ingotPowderCount(ModMaterials.URANIUM, 2), ingot(ModMaterials.RA226, 2), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_thorium", ModMaterialItems.item(ModMaterials.THORIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.THORIUM, 2), ingotPowderCount(ModMaterials.THORIUM, 2), ingotPowder(ModMaterials.URANIUM), ingot(ModMaterials.RA226, 1));
        itemRecipe(writer, "crystal_plutonium", ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.PLUTONIUM, 2), ingotPowderCount(ModMaterials.PLUTONIUM, 2), ingotPowder(ModMaterials.POLONIUM), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_titanium", ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.TITANIUM, 2), ingotPowderCount(ModMaterials.TITANIUM, 2), modPowder(ModMaterials.IRON), modPowderTiny(ModMaterials.LITHIUM));
        // itemRecipe(writer, "crystal_sulfur", ModItems.CRYSTAL_SULFUR.get(),
        //         stack(ModItems.SULFUR.get(), 4), stack(ModItems.SULFUR.get(), 4), modPowder(ModMaterials.IRON), ingot(ModMaterials.MERCURY, 1));
        itemRecipe(writer, "crystal_copper", ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.COPPER, 2), modPowderCount(ModMaterials.COPPER, 2), stack(ModItems.SULFUR.get(), 1), ingotPowder(ModMaterials.COBALT));
        itemRecipe(writer, "crystal_tungsten", ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.TUNGSTEN, 2), ingotPowderCount(ModMaterials.TUNGSTEN, 2), modPowder(ModMaterials.IRON), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_aluminium", ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.ALUMINUM, 2), ingotPowder(ModMaterials.TITANIUM), modPowder(ModMaterials.IRON), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_fluorite", ModMaterialItems.item(ModMaterials.FLUORITE, MaterialShape.CRYSTAL),
                stack(ModItems.FLUORITE.get(), 4), stack(ModItems.FLUORITE.get(), 4), stack(ModItems.GEM_SODALITE.get(), 2), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_beryllium", ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.BERYLLIUM, 2), ingotPowderCount(ModMaterials.BERYLLIUM, 2), modPowder(ModMaterials.QUARTZ), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_lead", ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.LEAD, 2), ingotPowderCount(ModMaterials.LEAD, 2), modPowder(ModMaterials.GOLD), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_schrabidium", ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.SCHRABIDIUM, 2), ingotPowderCount(ModMaterials.SCHRABIDIUM, 2), ingotPowder(ModMaterials.PLUTONIUM), modPowderTiny(ModMaterials.LITHIUM));
        itemRecipe(writer, "crystal_lithium", ModMaterialItems.item(ModMaterials.LITHIUM, MaterialShape.CRYSTAL),
                modPowderCount(ModMaterials.LITHIUM, 2), modPowderCount(ModMaterials.LITHIUM, 2), modPowder(ModMaterials.QUARTZ), stack(ModItems.FLUORITE.get(), 1));
        itemRecipe(writer, "crystal_cobalt", ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.CRYSTAL),
                ingotPowderCount(ModMaterials.COBALT, 2), modPowderCount(ModMaterials.IRON, 3), modPowderCount(ModMaterials.COPPER, 3), modPowderTiny(ModMaterials.LITHIUM));
    }

    private static void registerBedrockRecipes(Consumer<FinishedRecipe> writer) {
        // Light
        addBedrockPhase(writer, "light", ModItems.BEDROCK_ORE_BASE_LIGHT.get(), ModItems.BEDROCK_ORE_PRIMARY_LIGHT.get(), stack(Items.RAW_IRON, 9), stack(Items.RAW_COPPER, 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_LIGHT.get(), ModItems.BEDROCK_ORE_CRUMBS_LIGHT.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_light", ModItems.BEDROCK_ORE_SULFURIC_WASHED_LIGHT.get(), stack(ModItems.TITANIUM_RAW.get(), 6), stack(ModBlocks.RESOURCE_BAUXITE.get(), 9), stack(ModItems.CRYOLITE.get(), 3));
        itemRecipe(writer, "bedrock_solvent_washed_light", ModItems.BEDROCK_ORE_SOLVENT_WASHED_LIGHT.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 5), stack(ModItems.BEDROCK_ORE_FRAGMENT_LITHIUM.get(), 5), stack(ModItems.POWDER_SODIUM.get(), 3));
        itemRecipe(writer, "bedrock_rad_washed_light", ModItems.BEDROCK_ORE_RAD_WASHED_LIGHT.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 6), stack(ModItems.BEDROCK_ORE_FRAGMENT_LITHIUM.get(), 6), stack(ModItems.POWDER_SODIUM.get(), 6));

        // Heavy
        addBedrockPhase(writer, "heavy", ModItems.BEDROCK_ORE_BASE_HEAVY.get(), ModItems.BEDROCK_ORE_PRIMARY_HEAVY.get(), stack(ModItems.TUNGSTEN_RAW.get(), 9), stack(ModItems.LEAD_RAW.get(), 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_HEAVY.get(), ModItems.BEDROCK_ORE_CRUMBS_HEAVY.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_heavy", ModItems.BEDROCK_ORE_SULFURIC_WASHED_HEAVY.get(), stack(Items.RAW_GOLD, 2), stack(Items.RAW_GOLD, 2), stack(ModItems.BERYLLIUM_RAW.get(), 3));
        itemRecipe(writer, "bedrock_solvent_washed_heavy", ModItems.BEDROCK_ORE_SOLVENT_WASHED_HEAVY.get(), stack(ModItems.TUNGSTEN_RAW.get(), 9), stack(ModItems.LEAD_RAW.get(), 9), stack(Items.RAW_GOLD, 5));
        itemRecipe(writer, "bedrock_rad_washed_heavy", ModItems.BEDROCK_ORE_RAD_WASHED_HEAVY.get(), ingot(ModMaterials.BISMUTH, 2), ingot(ModMaterials.TANTALIUM, 2), stack(Items.RAW_GOLD, 6));

        // Rare
        addBedrockPhase(writer, "rare", ModItems.BEDROCK_ORE_BASE_RARE.get(), ModItems.BEDROCK_ORE_PRIMARY_RARE.get(), stack(ModItems.COBALT_RAW.get(), 5), stack(ModItems.RAREEARTH_RAW.get(), 5), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_RARE.get(), ModItems.BEDROCK_ORE_CRUMBS_RARE.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_rare", ModItems.BEDROCK_ORE_SULFURIC_WASHED_RARE.get(), ingot(ModMaterials.BORON, 5), ingot(ModMaterials.LANTHANIUM, 3), ingot(ModMaterials.NIOBIUM, 4));
        itemRecipe(writer, "bedrock_solvent_washed_rare", ModItems.BEDROCK_ORE_SOLVENT_WASHED_RARE.get(), ingot(ModMaterials.NEODYMIUM, 3), ingot(ModMaterials.STRONTIUM, 3), ingot(ModMaterials.ZIRCONIUM, 3));
        itemRecipe(writer, "bedrock_rad_washed_rare", ModItems.BEDROCK_ORE_RAD_WASHED_RARE.get(), ingot(ModMaterials.NIOBIUM, 5), ingot(ModMaterials.NEODYMIUM, 5), ingot(ModMaterials.STRONTIUM, 3));

        // Actinide
        addBedrockPhase(writer, "actinide", ModItems.BEDROCK_ORE_BASE_ACTINIDE.get(), ModItems.BEDROCK_ORE_PRIMARY_ACTINIDE.get(), stack(ModItems.URANIUM_RAW.get(), 4), stack(ModItems.THORIUM_RAW.get(), 4), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_ACTINIDE.get(), ModItems.BEDROCK_ORE_CRUMBS_ACTINIDE.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_actinide", ModItems.BEDROCK_ORE_SULFURIC_WASHED_ACTINIDE.get(), stack(ModItems.RADIUM_RAW.get(), 2), stack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET), 1));
        itemRecipe(writer, "bedrock_solvent_washed_actinide", ModItems.BEDROCK_ORE_SOLVENT_WASHED_ACTINIDE.get(), stack(ModItems.RADIUM_RAW.get(), 2), stack(ModMaterialItems.item(ModMaterials.POLONIUM, MaterialShape.BILLET), 1));
        itemRecipe(writer, "bedrock_rad_washed_actinide", ModItems.BEDROCK_ORE_RAD_WASHED_ACTINIDE.get(), stack(ModMaterialItems.item(ModMaterials.TECHNETIUM, MaterialShape.BILLET), 2), stack(ModMaterialItems.item(ModMaterials.URANIUM238, MaterialShape.BILLET), 1));

        // Nonmetal
        addBedrockPhase(writer, "nonmetal", ModItems.BEDROCK_ORE_BASE_NONMETAL.get(), ModItems.BEDROCK_ORE_PRIMARY_NONMETAL.get(), stack(Items.COAL, 9), stack(ModItems.SULFUR.get(), 9), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_NONMETAL.get(), ModItems.BEDROCK_ORE_CRUMBS_NONMETAL.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_nonmetal", ModItems.BEDROCK_ORE_SULFURIC_WASHED_NONMETAL.get(), stack(ModItems.LIGNITE.get(), 9), stack(ModItems.SALTPETER.get(), 6), stack(ModItems.FLUORITE.get(), 6));
        itemRecipe(writer, "bedrock_solvent_washed_nonmetal", ModItems.BEDROCK_ORE_SOLVENT_WASHED_NONMETAL.get(), stack(ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.CRYSTAL), 5), stack(ModItems.FLUORITE.get(), 6), stack(ModItems.SULFUR.get(), 6));
        itemRecipe(writer, "bedrock_rad_washed_nonmetal", ModItems.BEDROCK_ORE_RAD_WASHED_NONMETAL.get(), stack(ModItems.POWDER_CHLOROCALCITE.get(), 6), ingot(ModMaterials.SILICON, 2), ingot(ModMaterials.SILICON, 2));

        // Crystal
        addBedrockPhase(writer, "crystal", ModItems.BEDROCK_ORE_BASE_CRYSTAL.get(), ModItems.BEDROCK_ORE_PRIMARY_CRYSTAL.get(), stack(Items.REDSTONE, 9), stack(ModItems.CINNABAR.get(), 4), ModItems.BEDROCK_ORE_SULFURIC_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_SOLVENT_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_RAD_BYPRODUCT_CRYSTAL.get(), ModItems.BEDROCK_ORE_CRUMBS_CRYSTAL.get());
        itemRecipe(writer, "bedrock_sulfuric_washed_crystal", ModItems.BEDROCK_ORE_SULFURIC_WASHED_CRYSTAL.get(), stack(ModItems.GEM_SODALITE.get(), 9), ingot(ModMaterials.ASBESTOS, 6), stack(Items.DIAMOND, 3));
        itemRecipe(writer, "bedrock_solvent_washed_crystal", ModItems.BEDROCK_ORE_SOLVENT_WASHED_CRYSTAL.get(), stack(ModItems.CINNABAR.get(), 3), ingot(ModMaterials.ASBESTOS, 5), stack(Items.EMERALD, 3));
        itemRecipe(writer, "bedrock_rad_washed_crystal", ModItems.BEDROCK_ORE_RAD_WASHED_CRYSTAL.get(), stack(ModItems.BORAX.get(), 3), stack(ModItems.MOLYSITE.get(), 3), stack(ModItems.GEM_SODALITE.get(), 9));

        // Misc
        itemRecipe(writer, "blaze_rod", Items.BLAZE_ROD, stack(Items.BLAZE_POWDER, 1), stack(Items.BLAZE_POWDER, 1), stack(ModItems.FIRE_POWDER.get(), 1), stack(ModItems.FIRE_POWDER.get(), 1));
    }

    private static void addBedrockPhase(Consumer<FinishedRecipe> writer, String name, ItemLike base, ItemLike primary, ItemStack out1, ItemStack out2, ItemLike byproductS, ItemLike byproductSol, ItemLike byproductRad, ItemLike crumbs) {
        itemRecipe(writer, "bedrock_base_" + name, base, stack(primary, 1), gravel());
        itemRecipe(writer, "bedrock_primary_" + name, primary, out1, out2);
        itemRecipe(writer, "bedrock_primary_sulfuric_" + name, getSubItem(primary, "sulfuric"), stack(getSubItem(primary, "nosulfuric"), 2), stack(byproductS, 2));
        itemRecipe(writer, "bedrock_primary_solvent_" + name, getSubItem(primary, "solvent"), stack(getSubItem(primary, "nosolvent"), 2), stack(byproductS, 2), stack(byproductSol, 2));
        itemRecipe(writer, "bedrock_primary_rad_" + name, getSubItem(primary, "rad"), stack(getSubItem(primary, "norad"), 2), stack(byproductS, 2), stack(byproductSol, 2), stack(byproductRad, 2));
        
        ItemStack mOut1_18 = out1.copy(); mOut1_18.setCount(18);
        ItemStack mOut1_9 = out1.copy(); mOut1_9.setCount(9);
        ItemStack mOut2_18 = out2.copy(); mOut2_18.setCount(18);
        ItemStack mOut2_9 = out2.copy(); mOut2_9.setCount(9);
        
        itemRecipe(writer, "bedrock_primary_first_" + name, getSubItem(primary, "first"), mOut1_18, mOut2_9, stack(crumbs, 1));
        itemRecipe(writer, "bedrock_primary_second_" + name, getSubItem(primary, "second"), mOut1_9, mOut2_18, stack(crumbs, 1));
    }

    private static Item getSubItem(ItemLike parent, String sub) {
        String path = BuiltInRegistries.ITEM.getKey(parent.asItem()).getPath();
        ResourceLocation id = BaseRecipeBuilder.resLoc(path.replace("_primary_", "_primary_" + sub + "_"));
        return BuiltInRegistries.ITEM.get(id);
    }

    private static void tagRecipe(Consumer<FinishedRecipe> writer, String name, String tag, ItemStack... outputs) {
        CentrifugeRecipeBuilder.tagRecipe(tag, outputs).save(writer, recipeId(name));
    }
    
    private static void itemRecipe(Consumer<FinishedRecipe> writer, String name, ItemLike input, ItemStack... outputs) {
        CentrifugeRecipeBuilder.itemRecipe(input.asItem(), outputs).save(writer, recipeId(name));
    }

    private static String recipeId(String name) {
        return "centrifuge/" + name;
    }

    private static ItemStack gravel() {
        return new ItemStack(Items.GRAVEL);
    }

    private static ItemStack stack(ItemLike item, int count) {
        return new ItemStack(item, count);
    }

    private static ItemStack modPowder(ModMaterials powder) {
        return modPowderCount(powder, 1);
    }

    private static ItemStack modPowderCount(ModMaterials powder, int count) {
        var supplier = ModMaterialItems.get(powder, MaterialShape.POWDER);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }

    private static ItemStack modPowderTiny(ModMaterials powder) {
        return modPowderTinyCount(powder, 1);
    }

    private static ItemStack modPowderTinyCount(ModMaterials powder, int count) {
        var supplier = ModMaterialItems.get(powder, MaterialShape.POWDER_TINY);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }

    private static ItemStack ingotPowder(ModMaterials ingot) {
        return ingotPowderCount(ingot, 1);
    }

    private static ItemStack ingotPowderCount(ModMaterials ingot, int count) {
        var supplier = ModMaterialItems.get(ingot, MaterialShape.POWDER);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }

    private static ItemStack ingot(ModMaterials ingot, int count) {
        var supplier = ModMaterialItems.get(ingot, MaterialShape.INGOT);
        return (supplier != null && supplier.isPresent()) ? new ItemStack(supplier.get(), count) : ItemStack.EMPTY;
    }
}
//?}