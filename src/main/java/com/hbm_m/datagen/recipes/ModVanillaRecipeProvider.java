package com.hbm_m.datagen.recipes;

import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
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
import net.minecraftforge.registries.RegistryObject;

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

    //ЗАРЕГЕСТРИРУЙ ТУТ СВОИ РЕЦЕПТЫ, ИНАЧЕ НЕ ПРОСТИТ
    private void registerAll(@NotNull Consumer<FinishedRecipe> writer) {
        registerCoil(writer);
        registerCoilTorus(writer);
        registerGrenades(writer);
        registerUtilityRecipes(writer);
        registerPowderCooking(writer);
        registerOreAndRawCooking(writer);
    }

    //  БЕЗОПАСНАЯ ПРОВЕРКА NULL
    private boolean isItemSafe(RegistryObject<?> itemObj) {
        return itemObj != null && itemObj.get() != null;
    }


    private Item safePowder(ModPowders powder) {
        RegistryObject<?> obj = ModItems.getPowders(powder);
        return isItemSafe(obj) ? (Item) obj.get() : null;
    }

    //основные рецепты
    private void registerUtilityRecipes(Consumer<FinishedRecipe> writer) {
        //двери
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOOR_BUNKER.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModItems.PLATE_LEAD.get())
                .define('$', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_LEAD.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/door_bunker"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.METAL_DOOR.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModItems.PLATE_STEEL.get())
                .define('$', ModItems.PLATE_IRON.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/metal_door"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOOR_OFFICE.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModItems.PLATE_IRON.get())
                .define('$', Items.OAK_WOOD)
                .unlockedBy(getHasName(ModItems.PLATE_IRON.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/door_office"));

        //МОТОРЫ
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR.get(), 2)
                .pattern(" $ ")
                .pattern("%#%")
                .pattern("%@%")
                .define('%', ModItems.PLATE_IRON.get())
                .define('$', ModItems.WIRE_RED_COPPER.get())
                .define('#', ModItems.COIL_COPPER.get())
                .define('@', ModItems.COIL_COPPER_TORUS.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER_TORUS.get()), has(ModItems.COIL_COPPER_TORUS.get()))
                .save(writer, recipeId("crafting/motor"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR_DESH.get(), 2)
                .pattern("@$@")
                .pattern("%#%")
                .pattern("@$@")
                .define('%', ModItems.getIngot(ModIngots.DESH).get())
                .define('$', ModItems.COIL_GOLD_TORUS.get())
                .define('#', ModItems.MOTOR.get())
                .define('@', Ingredient.of(ModItems.getIngot(ModIngots.BAKELITE).get(), ModItems.getIngot(ModIngots.POLYMER).get()))
                .unlockedBy(getHasName(ModItems.PLATE_DESH.get()), has(ModItems.PLATE_DESH.get()))
                .save(writer, recipeId("crafting/motor_desh"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INSULATOR.get(), 4)
                .pattern("$  ")
                .pattern("$  ")
                .pattern("   ")
                .define('$', Items.BRICK)
                .unlockedBy(getHasName(Items.BRICK), has(Items.BRICK))
                .save(writer, recipeId("crafting/insulator2"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INSULATOR.get(), 4)
                .pattern("$#$")
                .pattern("   ")
                .pattern("   ")
                .define('$', Items.STRING)
                .define('#', Items.WHITE_WOOL)
                .unlockedBy(getHasName(Items.WHITE_WOOL), has(Items.WHITE_WOOL))
                .save(writer, recipeId("crafting/insulator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.INSULATOR.get(), 16)
                .pattern("## ")
                .pattern("   ")
                .pattern("   ")
                .define('#', ModItems.getIngot(ModIngots.ASBESTOS).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.ASBESTOS).get()), has(ModItems.getIngot(ModIngots.ASBESTOS).get()))
                .save(writer, recipeId("crafting/insulator3"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEFUSER.get())
                .pattern(" # ")
                .pattern("$ $")
                .pattern("$ $")
                .define('$', ModItems.BOLT_STEEL.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/defuser"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRT_DISPLAY.get(), 4)
                .pattern(" # ")
                .pattern("$@$")
                .pattern(" % ")
                .define('$', ModItems.PLATE_STEEL.get())
                .define('#', ModItems.getPowder(ModIngots.ALUMINUM).get())
                .define('%', ModItems.VACUUM_TUBE.get())
                .define('@', Items.GLASS_PANE)
                .unlockedBy(getHasName(ModItems.VACUUM_TUBE.get()), has(ModItems.VACUUM_TUBE.get()))
                .save(writer, recipeId("crafting/crt_ds"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP.get())
                .pattern("#  ")
                .pattern("@  ")
                .pattern("%  ")
                .define('#', ModItems.INSULATOR.get())
                .define('%', Ingredient.of(ModItems.WIRE_COPPER.get(), ModItems.WIRE_GOLD.get()))
                .define('@', ModItems.SILICON_CIRCUIT.get())
                .unlockedBy(getHasName(ModItems.SILICON_CIRCUIT.get()), has(ModItems.SILICON_CIRCUIT.get()))
                .save(writer, recipeId("crafting/microchip"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PCB.get(), 4)
                .pattern("#  ")
                .pattern("@  ")
                .pattern("   ")
                .define('#', ModItems.INSULATOR.get())
                .define('@', Ingredient.of(ModItems.PLATE_COPPER.get(), ModItems.PLATE_GOLD.get()))
                .unlockedBy(getHasName(ModItems.INSULATOR.get()), has(ModItems.INSULATOR.get()))
                .save(writer, recipeId("crafting/pcb"));

        
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CROWBAR.get())
                .pattern("$$ ")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/crowbar"));


        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SCREWDRIVER.get())
                .pattern("  #")
                .pattern(" # ")
                .pattern("$  ")
                .define('#', Items.IRON_INGOT)
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/screwdriver"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOLT_STEEL.get(), 16)
                .pattern("$  ")
                .pattern("$  ")
                .pattern("   ")
                .define('$', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/bolt_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VACUUM_TUBE.get())
                .pattern("$  ")
                .pattern("#  ")
                .pattern("@  ")
                .define('$', Items.GLASS_PANE)
                .define('#', ModItems.WIRE_TUNGSTEN.get())
                .define('@', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.WIRE_TUNGSTEN.get()), has(ModItems.WIRE_TUNGSTEN.get()))
                .save(writer, recipeId("crafting/vacuum_tube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPACITOR.get(), 2)
                .pattern("$#$")
                .pattern("% %")
                .pattern("   ")
                .define('$', ModItems.INSULATOR.get())
                .define('%', Ingredient.of(ModItems.WIRE_COPPER.get(), ModItems.WIRE_ALUMINIUM.get()))
                .define('#', ModItems.getPowder(ModIngots.ALUMINUM).get())
                .unlockedBy(getHasName(ModItems.getPowder(ModIngots.ALUMINUM).get()), has(ModItems.getPowder(ModIngots.ALUMINUM).get()))
                .save(writer, recipeId("crafting/capacitor"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BARBED_WIRE.get(), 16)
                .pattern("$@$")
                .pattern("@ @")
                .pattern("$@$")
                .define('$', ModItems.WIRE_FINE.get())
                .define('@', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.WIRE_FINE.get()), has(ModItems.WIRE_FINE.get()))
                .save(writer, recipeId("crafting/barbed_wire"));

        

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BALL_TNT.get(), 4)
                .pattern("#$ ")
                .pattern("@  ")
                .pattern("   ")
                .define('@', ModItems.SEQUESTRUM.get())
                .define('#', Items.GUNPOWDER)
                .define('$', Items.SUGAR)
                .unlockedBy(getHasName(ModItems.SEQUESTRUM.get()), has(ModItems.SEQUESTRUM.get()))
                .save(writer, recipeId("crafting/ball_tnt"));



        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE.get(), 4)
                .pattern("#$#")
                .pattern("$#$")
                .pattern("#$#")
                .define('#', Blocks.COBBLESTONE)
                .define('$', Blocks.STONE)
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTROLLER_CHASSIS.get())
                .pattern("$$$")
                .pattern("%##")
                .pattern("$$$")
                .define('$', ModItems.PLATE_ALUMINUM.get())
                .define('#', ModItems.PCB.get())
                .define('%', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModItems.CRT_DISPLAY.get()), has(ModItems.CRT_DISPLAY.get()))
                .save(writer, recipeId("crafting/controller_chassis"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FIRECLAY_BALL.get(), 4)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModItems.ALUMINUM_RAW.get())
                .define('B', Items.CLAY_BALL)
                .unlockedBy(getHasName(ModItems.ALUMINUM_RAW.get()), has(ModItems.ALUMINUM_RAW.get()))
                .save(writer, recipeId("crafting/alclay_fireclay"));

        registerSmelting(writer, ModItems.FIRECLAY_BALL.get(), ModItems.FIREBRICK.get(), 0.1F, 100, "firebrick_smelting");
    }

    //переплавка порошков -  ИСПРАВЛЕННАЯ ВЕРСИЯ
    private void registerPowderCooking(Consumer<FinishedRecipe> writer) {
        //  ПРОВЕРЯЕМ КАЖДЫЙ ПОРОШОК ПЕРЕД ИСПОЛЬЗОВАНИЕМ
        Item ironPowder = safePowder(ModPowders.IRON);
        Item goldPowder = safePowder(ModPowders.GOLD);
        Item coalPowder = safePowder(ModPowders.COAL);

        // Регистрируем только если порошок существует
        if (ironPowder != null) {
            registerSmelting(writer, ironPowder, Items.IRON_INGOT, 0.0F, 200, "powder_iron_smelting");
            registerBlasting(writer, ironPowder, Items.IRON_INGOT, 0.0F, 100, "powder_iron_blasting");
        }

        if (goldPowder != null) {
            registerSmelting(writer, goldPowder, Items.GOLD_INGOT, 0.0F, 200, "powder_gold_smelting");
            registerBlasting(writer, goldPowder, Items.GOLD_INGOT, 0.0F, 100, "powder_gold_blasting");
        }

        if (coalPowder != null) {
            registerSmelting(writer, coalPowder, Items.COAL, 0.0F, 200, "powder_coal_smelting");
            registerBlasting(writer, coalPowder, Items.COAL, 0.0F, 100, "powder_coal_blasting");
        }
    }

    //переплавка руд
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

    private void buildBarbedWireUpgrade(Consumer<FinishedRecipe> writer, Item result, ItemLike core, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 8)
                .pattern("###")
                .pattern("#@#")
                .pattern("###")
                .define('#', ModBlocks.BARBED_WIRE.get())
                .define('@', core)
                .unlockedBy(getHasName(ModBlocks.BARBED_WIRE.get()), has(ModBlocks.BARBED_WIRE.get()))
                .save(writer, recipeId("crafting/" + name));
    }



    private void buildBlades(Consumer<FinishedRecipe> writer, Item result, ItemLike material, ItemLike material2, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern(" $ ")
                .pattern("$#$")
                .pattern(" $ ")
                .define('$', material2)
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }


    //крафты гранат
    private void registerGrenades(Consumer<FinishedRecipe> writer) {

        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_FIRE.get()), Items.BLAZE_POWDER, "barbed_wire_fire");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_POISON.get()), Items.SPIDER_EYE, "barbed_wire_poison");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_WITHER.get()), Items.WITHER_SKELETON_SKULL, "barbed_wire_wither");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_RAD.get()), ModItems.BILLET_PLUTONIUM.get(), "barbed_wire_rad");

    }


    //крафты катушек
    private void registerCoil(Consumer<FinishedRecipe> writer) {
        buildCoil(writer, ModItems.COIL_ADVANCED_ALLOY.get(), ModItems.WIRE_ADVANCED_ALLOY.get(), "coil_advanced_alloy");
        buildCoil(writer, ModItems.COIL_COPPER.get(), ModItems.WIRE_RED_COPPER.get(), "coil_copper");
        buildCoil(writer, ModItems.COIL_GOLD.get(), ModItems.WIRE_GOLD.get(), "coil_gold");
        buildCoil(writer, ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), ModItems.WIRE_MAGNETIZED_TUNGSTEN.get(), "coil_magnetized_tungsten");
        buildCoil(writer, ModItems.COIL_TUNGSTEN.get(), ModItems.WIRE_TUNGSTEN.get(), "coil_tungsten");
    }

    //крафты кольцевых катушек
    private void registerCoilTorus(Consumer<FinishedRecipe> writer) {
        buildCoilTorus(writer, ModItems.COIL_ADVANCED_ALLOY_TORUS.get(), ModItems.COIL_ADVANCED_ALLOY.get(), "coil_advanced_alloy_torus");
        buildCoilTorus(writer, ModItems.COIL_COPPER_TORUS.get(), ModItems.COIL_COPPER.get(), "coil_copper_torus");
        buildCoilTorus(writer, ModItems.COIL_GOLD_TORUS.get(), ModItems.COIL_GOLD.get(), "coil_gold_torus");
        buildCoilTorus(writer, ModItems.COIL_MAGNETIZED_TUNGSTEN_TORUS.get(), ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), "coil_magnetized_tungsten_torus");
    }

    //билды для рецептов
    private void buildCoil(Consumer<FinishedRecipe> writer, Item result, ItemLike material, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("###")
                .pattern("#$#")
                .pattern("###")
                .define('$', Items.IRON_INGOT)
                .define('#', material)
                .unlockedBy(getHasName(material), has(material))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildCoilTorus(Consumer<FinishedRecipe> writer, Item result, ItemLike material, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, 2)
                .pattern(" # ")
                .pattern("#$#")
                .pattern(" # ")
                .define('$', ModItems.PLATE_IRON.get())
                .define('#', material)
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

    //регистрация и прочее
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