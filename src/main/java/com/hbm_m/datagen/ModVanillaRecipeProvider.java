package com.hbm_m.datagen;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModIngots;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.ModPowders;
import com.hbm_m.lib.RefStrings;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModVanillaRecipeProvider extends RecipeProvider {

    public ModVanillaRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        registerAll(writer);
    }

    public void registerVanillaRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        registerAll(writer);
    }

    private void registerAll(@NotNull Consumer<FinishedRecipe> writer) {
        registerToolAndArmorSets(writer);
        registerCrates(writer);
        registerStamps(writer);
        registerGrenades(writer);
        registerUtilityRecipes(writer);
        registerPowderCooking(writer);
        registerOreAndRawCooking(writer);
    }


    private void registerToolAndArmorSets(Consumer<FinishedRecipe> writer) {
        ItemLike titaniumIngot = ModItems.getIngot(ModIngots.TITANIUM).get();
        buildToolSet(writer, "titanium", titaniumIngot,
                ModItems.TITANIUM_SWORD.get(), ModItems.TITANIUM_SHOVEL.get(), ModItems.TITANIUM_PICKAXE.get(),
                ModItems.TITANIUM_HOE.get(), ModItems.TITANIUM_AXE.get());
        buildArmorSet(writer, "titanium", titaniumIngot,
                ModItems.TITANIUM_HELMET.get(), ModItems.TITANIUM_CHESTPLATE.get(),
                ModItems.TITANIUM_LEGGINGS.get(), ModItems.TITANIUM_BOOTS.get());

        ItemLike steelIngot = ModItems.getIngot(ModIngots.STEEL).get();
        buildToolSet(writer, "steel", steelIngot,
                ModItems.STEEL_SWORD.get(), ModItems.STEEL_SHOVEL.get(), ModItems.STEEL_PICKAXE.get(),
                ModItems.STEEL_HOE.get(), ModItems.STEEL_AXE.get());
        buildArmorSet(writer, "steel", steelIngot,
                ModItems.STEEL_HELMET.get(), ModItems.STEEL_CHESTPLATE.get(),
                ModItems.STEEL_LEGGINGS.get(), ModItems.STEEL_BOOTS.get());

        ItemLike starmetalIngot = ModItems.getIngot(ModIngots.STARMETAL).get();
        buildToolSet(writer, "starmetal", starmetalIngot,
                ModItems.STARMETAL_SWORD.get(), ModItems.STARMETAL_SHOVEL.get(), ModItems.STARMETAL_PICKAXE.get(),
                ModItems.STARMETAL_HOE.get(), ModItems.STARMETAL_AXE.get());
        buildArmorSet(writer, "starmetal", starmetalIngot,
                ModItems.STARMETAL_HELMET.get(), ModItems.STARMETAL_CHESTPLATE.get(),
                ModItems.STARMETAL_LEGGINGS.get(), ModItems.STARMETAL_BOOTS.get());

        ItemLike alloyIngot = ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get();
        buildToolSet(writer, "alloy", alloyIngot,
                ModItems.ALLOY_SWORD.get(), ModItems.ALLOY_SHOVEL.get(), ModItems.ALLOY_PICKAXE.get(),
                ModItems.ALLOY_HOE.get(), ModItems.ALLOY_AXE.get());
        buildArmorSet(writer, "alloy", alloyIngot,
                ModItems.ALLOY_HELMET.get(), ModItems.ALLOY_CHESTPLATE.get(),
                ModItems.ALLOY_LEGGINGS.get(), ModItems.ALLOY_BOOTS.get());

        ItemLike cobaltIngot = ModItems.getIngot(ModIngots.COBALT).get();
        buildArmorSet(writer, "cobalt", cobaltIngot,
                ModItems.COBALT_HELMET.get(), ModItems.COBALT_CHESTPLATE.get(),
                ModItems.COBALT_LEGGINGS.get(), ModItems.COBALT_BOOTS.get());

        ItemLike asbestosSheet = ModItems.getIngot(ModIngots.ASBESTOS).get();
        buildArmorSet(writer, "asbestos", asbestosSheet,
                ModItems.ASBESTOS_HELMET.get(), ModItems.ASBESTOS_CHESTPLATE.get(),
                ModItems.ASBESTOS_LEGGINGS.get(), ModItems.ASBESTOS_BOOTS.get());
    }

    private void buildToolSet(Consumer<FinishedRecipe> writer, String name, ItemLike material,
                              Item sword, Item shovel, Item pickaxe, Item hoe, Item axe) {
        buildSword(writer, material, sword, name + "_sword");
        buildShovel(writer, material, shovel, name + "_shovel");
        buildPickaxe(writer, material, pickaxe, name + "_pickaxe");
        buildHoe(writer, material, hoe, name + "_hoe");
        buildAxe(writer, material, axe, name + "_axe");
    }

    private void buildArmorSet(Consumer<FinishedRecipe> writer, String name, ItemLike material,
                               Item helmet, Item chestplate, Item leggings, Item boots) {
        buildHelmet(writer, material, helmet, name + "_helmet");
        buildChestplate(writer, material, chestplate, name + "_chestplate");
        buildLeggings(writer, material, leggings, name + "_leggings");
        buildBoots(writer, material, boots, name + "_boots");
    }

    private void registerCrates(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_IRON.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("BBB")
                .define('A', ModItems.PLATE_IRON.get())
                .define('B', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.PLATE_IRON.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/crate_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_STEEL.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("BBB")
                .define('A', ModItems.PLATE_STEEL.get())
                .define('B', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/crate_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_DESH.get())
                .pattern("ABA")
                .pattern(" A ")
                .pattern("   ")
                .define('A', ModItems.PLATE_DESH.get())
                .define('B', ModBlocks.CRATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_DESH.get()), has(ModItems.PLATE_DESH.get()))
                .save(writer, recipeId("crafting/crate_desh"));
    }

    private void registerStamps(Consumer<FinishedRecipe> writer) {
        buildStamp(writer, ModItems.STAMP_STONE_FLAT.get(), Items.STONE, "stamp_stone_flat");
        buildStamp(writer, ModItems.STAMP_IRON_FLAT.get(), Items.IRON_INGOT, "stamp_iron_flat");
        buildStamp(writer, ModItems.STAMP_STEEL_FLAT.get(), ModItems.getIngot(ModIngots.STEEL).get(), "stamp_steel_flat");
        buildStamp(writer, ModItems.STAMP_TITANIUM_FLAT.get(), ModItems.getIngot(ModIngots.TITANIUM).get(), "stamp_titanium_flat");
        buildStamp(writer, ModItems.STAMP_OBSIDIAN_FLAT.get(), Blocks.OBSIDIAN.asItem(), "stamp_obsidian_flat");
        buildStamp(writer, ModItems.STAMP_DESH_FLAT.get(), ModItems.getIngot(ModIngots.DESH).get(), "stamp_desh_flat");
    }

    private void registerGrenades(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADE.get())
                .pattern("%# ")
                .pattern("#$#")
                .pattern(" # ")
                .define('%', ModItems.WIRE_RED_COPPER.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('$', Items.TNT)
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/grenade"));

        buildGrenadeUpgrade(writer, ModItems.GRENADEHE.get(), Items.TNT, "grenadehe");
        buildGrenadeUpgrade(writer, ModItems.GRENADESLIME.get(), Items.SLIME_BALL, "grenadeslime");
        buildGrenadeUpgrade(writer, ModItems.GRENADEFIRE.get(), ModItems.getIngot(ModIngots.PHOSPHORUS).get(), "grenadefire");

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADESMART.get())
                .pattern(" @ ")
                .pattern("&%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE.get())
                .define('&', ModItems.getIngot(ModIngots.PHOSPHORUS).get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.MICROCHIP.get())
                .define('$', Items.TNT)
                .unlockedBy(getHasName(ModItems.GRENADE.get()), has(ModItems.GRENADE.get()))
                .save(writer, recipeId("crafting/grenadesmart"));
    }

    private void registerUtilityRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE.get())
                .pattern("#$#")
                .pattern("$#$")
                .pattern("#$#")
                .define('#', Blocks.COBBLESTONE)
                .define('$', Blocks.STONE)
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DET_MINER.get())
                .pattern("$$$")
                .pattern("%#%")
                .pattern("%#%")
                .define('%', ModItems.PLATE_IRON.get())
                .define('#', Items.TNT)
                .define('$', Items.FLINT)
                .unlockedBy(getHasName(ModItems.PLATE_IRON.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/det_miner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRECLAY_BALL.get(), 4)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModBlocks.ALUMINUM_ORE.get())
                .define('B', Items.CLAY_BALL)
                .unlockedBy(getHasName(ModBlocks.ALUMINUM_ORE.get()), has(ModBlocks.ALUMINUM_ORE.get()))
                .save(writer, recipeId("crafting/alclay_fireclay"));

        registerSmelting(writer, ModItems.FIRECLAY_BALL.get(), ModItems.FIREBRICK.get(), 0.1F, 100, "firebrick_smelting");
    }

    private void registerPowderCooking(Consumer<FinishedRecipe> writer) {
        Item ironPowder = ModItems.getPowders(ModPowders.IRON).get();
        Item goldPowder = ModItems.getPowders(ModPowders.GOLD).get();
        registerSmelting(writer, ironPowder, Items.IRON_INGOT, 0.0F, 200, "powder_iron_smelting");
        registerBlasting(writer, ironPowder, Items.IRON_INGOT, 0.0F, 100, "powder_iron_blasting");
        registerSmelting(writer, goldPowder, Items.GOLD_INGOT, 0.0F, 200, "powder_gold_smelting");
        registerBlasting(writer, goldPowder, Items.GOLD_INGOT, 0.0F, 100, "powder_gold_blasting");
    }

    private void registerOreAndRawCooking(Consumer<FinishedRecipe> writer) {
        ItemLike uraniumIngot = ModItems.getIngot(ModIngots.URANIUM).get();
        registerSmeltingAndBlasting(writer, ModItems.URANIUM_RAW.get(), uraniumIngot, 2.1F, 3.0F, "uranium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.URANIUM_ORE.get(), uraniumIngot, 2.1F, 3.0F, "uranium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.URANIUM_ORE_DEEPSLATE.get(), uraniumIngot, 2.1F, 3.0F, "uranium_ore_deepslate");

        ItemLike thoriumIngot = ModItems.getIngot(ModIngots.THORIUM232).get();
        registerSmeltingAndBlasting(writer, ModItems.THORIUM_RAW.get(), thoriumIngot, 2.1F, 3.0F, "thorium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.THORIUM_ORE.get(), thoriumIngot, 2.1F, 3.0F, "thorium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.THORIUM_ORE_DEEPSLATE.get(), thoriumIngot, 2.1F, 3.0F, "thorium_ore_deepslate");

        ItemLike titaniumIngot = ModItems.getIngot(ModIngots.TITANIUM).get();
        registerSmeltingAndBlasting(writer, ModItems.TITANIUM_RAW.get(), titaniumIngot, 0.7F, 1.0F, "titanium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.TITANIUM_ORE.get(), titaniumIngot, 0.7F, 1.0F, "titanium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.TITANIUM_ORE_DEEPSLATE.get(), titaniumIngot, 0.7F, 1.0F, "titanium_ore_deepslate");

        ItemLike tungstenIngot = ModItems.getIngot(ModIngots.TUNGSTEN).get();
        registerSmeltingAndBlasting(writer, ModItems.TUNGSTEN_RAW.get(), tungstenIngot, 0.7F, 1.0F, "tungsten_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.TUNGSTEN_ORE.get(), tungstenIngot, 0.7F, 1.0F, "tungsten_ore");

        ItemLike leadIngot = ModItems.getIngot(ModIngots.LEAD).get();
        registerSmeltingAndBlasting(writer, ModItems.LEAD_RAW.get(), leadIngot, 0.7F, 1.0F, "lead_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.LEAD_ORE.get(), leadIngot, 0.7F, 1.0F, "lead_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.LEAD_ORE_DEEPSLATE.get(), leadIngot, 0.7F, 1.0F, "lead_ore_deepslate");

        ItemLike cobaltIngot = ModItems.getIngot(ModIngots.COBALT).get();
        registerSmeltingAndBlasting(writer, ModItems.COBALT_RAW.get(), cobaltIngot, 0.7F, 1.0F, "cobalt_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.COBALT_ORE.get(), cobaltIngot, 0.7F, 1.0F, "cobalt_ore");

        ItemLike berylliumIngot = ModItems.getIngot(ModIngots.BERYLLIUM).get();
        registerSmeltingAndBlasting(writer, ModItems.BERYLLIUM_RAW.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.BERYLLIUM_ORE.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_ore_deepslate");

        ItemLike aluminumIngot = ModItems.getIngot(ModIngots.ALUMINUM).get();
        registerSmeltingAndBlasting(writer, ModItems.ALUMINUM_RAW.get(), aluminumIngot, 0.7F, 1.0F, "aluminum_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.ALUMINUM_ORE.get(), aluminumIngot, 0.7F, 1.0F, "aluminum_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.ALUMINUM_ORE_DEEPSLATE.get(), aluminumIngot, 0.7F, 1.0F, "aluminum_ore_deepslate");
    }

    private void buildGrenadeUpgrade(Consumer<FinishedRecipe> writer, Item result, ItemLike core, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(" # ")
                .pattern("$%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('$', core)
                .unlockedBy(getHasName(ModItems.GRENADE.get()), has(ModItems.GRENADE.get()))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildStamp(Consumer<FinishedRecipe> writer, Item result, ItemLike material, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("###")
                .pattern("$$$")
                .pattern("   ")
                .define('#', Items.BRICK)
                .define('$', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildSword(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern(" # ")
                .pattern(" # ")
                .pattern(" $ ")
                .define('#', material)
                .define('$', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildShovel(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern(" # ")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('#', material)
                .define('$', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildPickaxe(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("###")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('#', material)
                .define('$', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildHoe(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("## ")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('#', material)
                .define('$', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildAxe(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("## ")
                .pattern("#$ ")
                .pattern(" $ ")
                .define('#', material)
                .define('$', Items.STICK)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildHelmet(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("###")
                .pattern("# #")
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildChestplate(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("# #")
                .pattern("###")
                .pattern("###")
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildLeggings(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("###")
                .pattern("# #")
                .pattern("# #")
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildBoots(Consumer<FinishedRecipe> writer, ItemLike material, Item result, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("# #")
                .pattern("# #")
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void registerSmeltingAndBlasting(Consumer<FinishedRecipe> writer, ItemLike input, ItemLike output,
                                             float smeltXp, float blastXp, String baseName) {
        registerSmelting(writer, input, output, smeltXp, 200, baseName + "_smelting");
        registerBlasting(writer, input, output, blastXp, 100, baseName + "_blasting");
    }

    private void registerSmelting(Consumer<FinishedRecipe> writer, ItemLike input, ItemLike result,
                                  float xp, int time, String name) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, result, xp, time)
                .unlockedBy(getHasName(input), has(input))
                .save(writer, recipeId(name));
    }

    private void registerBlasting(Consumer<FinishedRecipe> writer, ItemLike input, ItemLike result,
                                  float xp, int time, String name) {
        SimpleCookingRecipeBuilder.blasting(Ingredient.of(input), RecipeCategory.MISC, result, xp, time)
                .unlockedBy(getHasName(input), has(input))
                .save(writer, recipeId(name));
    }

    private ResourceLocation recipeId(String path) {
        return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
    }
}

