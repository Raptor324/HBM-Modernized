package com.hbm_m.datagen.recipes;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.ModItems;
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

    //ЗАРЕГЕСТРИРУЙ ТУТ СВОИ РЕЦЕПТЫ, ИНАЧЕ НЕ ПРОСТИТ
    private void registerAll(@NotNull Consumer<FinishedRecipe> writer) {
        registerToolAndArmorSets(writer);
        registerCrates(writer);
        registerCoil(writer);
        registerCoilTorus(writer);
        registerStamps(writer);
        registerGrenades(writer);
        registerUtilityRecipes(writer);
        registerPowderCooking(writer);
        registerOreAndRawCooking(writer);
    }

    // ✅ БЕЗОПАСНАЯ ПРОВЕРКА NULL
    private boolean isItemSafe(RegistryObject<?> itemObj) {
        return itemObj != null && itemObj.get() != null;
    }

    private ItemLike safeIngot(ModIngots ingot) {
        RegistryObject<?> obj = ModItems.getIngot(ingot);
        return isItemSafe(obj) ? (ItemLike) obj.get() : Items.AIR;
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SWITCH.get())
                .pattern("#  ")
                .pattern("@  ")
                .pattern("   ")
                .define('#', ModBlocks.WIRE_COATED.get())
                .define('@', Items.LEVER)
                .unlockedBy(getHasName(ModBlocks.WIRE_COATED.get()), has(ModBlocks.WIRE_COATED.get()))
                .save(writer, recipeId("crafting/switch"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.GEIGER_COUNTER_BLOCK.get())
                .pattern("#  ")
                .pattern("   ")
                .pattern("   ")
                .define('#', ModItems.GEIGER_COUNTER.get())
                .unlockedBy(getHasName(ModItems.GEIGER_COUNTER.get()), has(ModItems.GEIGER_COUNTER.get()))
                .save(writer, recipeId("crafting/geiger1"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GEIGER_COUNTER.get())
                .pattern("#  ")
                .pattern("   ")
                .pattern("   ")
                .define('#', ModBlocks.GEIGER_COUNTER_BLOCK.get())
                .unlockedBy(getHasName(ModBlocks.GEIGER_COUNTER_BLOCK.get()), has(ModBlocks.GEIGER_COUNTER_BLOCK.get()))
                .save(writer, recipeId("crafting/geiger2"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GEIGER_COUNTER.get())
                .pattern("###")
                .pattern("%$@")
                .pattern("%&&")
                .define('%', ModItems.WIRE_GOLD.get())
                .define('#', Items.GOLD_INGOT)
                .define('$', ModItems.INTEGRATED_CIRCUIT.get())
                .define('&', ModItems.getIngot(ModIngots.BERYLLIUM).get())
                .define('@', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/geiger3"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DOSIMETER.get())
                .pattern("$%$")
                .pattern("$@$")
                .pattern("$#$")
                .define('$', Items.OAK_PLANKS)
                .define('%', Items.GLASS_PANE)
                .define('#', ModItems.getIngot(ModIngots.BERYLLIUM).get())
                .define('@', ModItems.VACUUM_TUBE.get())

                .unlockedBy(getHasName(ModItems.VACUUM_TUBE.get()), has(ModItems.VACUUM_TUBE.get()))
                .save(writer, recipeId("crafting/dosimeter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CROWBAR.get())
                .pattern("$$ ")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/crowbar"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.OIL_DETECTOR.get())
                .pattern("# @")
                .pattern("#$@")
                .pattern("&&&")
                .define('&', ModItems.PLATE_STEEL.get())
                .define('@', Items.COPPER_INGOT)
                .define('$', ModItems.ANALOG_CIRCUIT.get())
                .define('#', ModItems.WIRE_GOLD.get())
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/oil_detector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEPTH_ORES_SCANNER.get())
                .pattern("###")
                .pattern("@%@")
                .pattern("$$$")
                .define('#', ModItems.VACUUM_TUBE.get())
                .define('@', ModItems.CAPACITOR.get())
                .define('%', ModItems.CONTROLLER_CHASSIS.get())
                .define('$', ModItems.PLATE_GOLD.get())
                .unlockedBy(getHasName(ModItems.CONTROLLER_CHASSIS.get()), has(ModItems.CONTROLLER_CHASSIS.get()))
                .save(writer, recipeId("crafting/depth_ores_scanner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CONVERTER_BLOCK.get())
                .pattern("###")
                .pattern("@@@")
                .pattern("$$$")
                .define('#', ModItems.CAPACITOR.get())
                .define('@', Items.REDSTONE)
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/converter_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOLT_STEEL.get(), 16)
                .pattern("$  ")
                .pattern("$  ")
                .pattern("   ")
                .define('$', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/bolt_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRENADE_IF.get())
                .pattern(" $ ")
                .pattern("#@#")
                .pattern(" # ")
                .define('$', ModItems.COIL_TUNGSTEN.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/grenade_if"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CAGE_LAMP.get(), 4)
                .pattern(" % ")
                .pattern(" @ ")
                .pattern(" ! ")
                .define('%', Items.GLASS_PANE)
                .define('@', ModItems.WIRE_TUNGSTEN.get())
                .define('!', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/cage_lamp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BARBED_WIRE.get(), 16)
                .pattern("$@$")
                .pattern("@ @")
                .pattern("$@$")
                .define('$', ModItems.WIRE_FINE.get())
                .define('@', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.WIRE_FINE.get()), has(ModItems.WIRE_FINE.get()))
                .save(writer, recipeId("crafting/barbed_wire"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WIRE_COATED.get(), 16)
                .pattern(" $ ")
                .pattern("@@@")
                .pattern(" $ ")
                .define('$', ModItems.INSULATOR.get())
                .define('@', ModItems.WIRE_RED_COPPER.get())
                .unlockedBy(getHasName(ModItems.WIRE_RED_COPPER.get()), has(ModItems.WIRE_RED_COPPER.get()))
                .save(writer, recipeId("crafting/wire_coated"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WOOD_BURNER.get())
                .pattern("$$$")
                .pattern("@&@")
                .pattern("% %")
                .define('$', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.COIL_COPPER.get())
                .define('&', Items.FURNACE)
                .define('%', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/wood_burner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ARMOR_TABLE.get())
                .pattern("$$$")
                .pattern("%&%")
                .pattern("%#%")
                .define('$', ModItems.PLATE_STEEL.get())
                .define('%', ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .define('&', Items.CRAFTING_TABLE)
                .define('#', ModBlocks.getIngotBlock(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/armor_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DETONATOR.get())
                .pattern("#  ")
                .pattern("@  ")
                .pattern("   ")
                .define('#', ModItems.INTEGRATED_CIRCUIT.get())
                .define('@', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.INTEGRATED_CIRCUIT.get()), has(ModItems.INTEGRATED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/detonator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MULTI_DETONATOR.get())
                .pattern("@# ")
                .pattern("   ")
                .pattern("   ")
                .define('#', ModItems.ADVANCED_CIRCUIT.get())
                .define('@', ModItems.DETONATOR.get())
                .unlockedBy(getHasName(ModItems.ADVANCED_CIRCUIT.get()), has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/multi_detonator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MINE_AP.get())
                .pattern("@  ")
                .pattern("#  ")
                .pattern("$  ")
                .define('#', ModItems.BALL_TNT.get())
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .define('@', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.BALL_TNT.get()), has(ModItems.BALL_TNT.get()))
                .save(writer, recipeId("crafting/mine_ap"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MINE_FAT.get())
                .pattern("@  ")
                .pattern("#  ")
                .pattern("$  ")
                .define('#', ModBlocks.MINE_AP.get())
                .define('$', ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .define('@', ModItems.BILLET_PLUTONIUM.get())
                .unlockedBy(getHasName(ModItems.BILLET_PLUTONIUM.get()), has(ModItems.BILLET_PLUTONIUM.get()))
                .save(writer, recipeId("crafting/mine_fat"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RANGE_DETONATOR.get())
                .pattern("##$")
                .pattern("№&%")
                .pattern("  @")
                .define('#', Items.REDSTONE)
                .define('№', Items.REDSTONE_BLOCK)
                .define('$', Items.EMERALD)
                .define('%', ModItems.ADVANCED_CIRCUIT.get())
                .define('&', ModItems.CAPACITOR_BOARD.get())
                .define('@', ModItems.BOLT_STEEL.get())
                .unlockedBy(getHasName(ModItems.ADVANCED_CIRCUIT.get()), has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/range_detonator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BALL_TNT.get(), 4)
                .pattern("#$ ")
                .pattern("@  ")
                .pattern("   ")
                .define('@', ModItems.SEQUESTRUM.get())
                .define('#', Items.GUNPOWDER)
                .define('$', Items.SUGAR)
                .unlockedBy(getHasName(ModItems.SEQUESTRUM.get()), has(ModItems.SEQUESTRUM.get()))
                .save(writer, recipeId("crafting/ball_tnt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLUEPRINT_FOLDER.get())
                .pattern("@#@")
                .pattern("@#@")
                .pattern("@#@")
                .define('@', Ingredient.of(Items.BLUE_DYE, Items.LAPIS_LAZULI))
                .define('#', Items.PAPER)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, recipeId("crafting/blueprint_folder"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE.get(), 4)
                .pattern("#$#")
                .pattern("$#$")
                .pattern("#$#")
                .define('#', Blocks.COBBLESTONE)
                .define('$', Blocks.STONE)
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DET_MINER.get(), 4)
                .pattern("$$$")
                .pattern("%#%")
                .pattern("%#%")
                .define('%', ModItems.PLATE_IRON.get())
                .define('#', Items.TNT)
                .define('$', Items.FLINT)
                .unlockedBy(getHasName(ModItems.PLATE_IRON.get()), has(ModItems.PLATE_IRON.get()))
                .save(writer, recipeId("crafting/det_miner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PRESS.get())
                .pattern("%$%")
                .pattern("%#%")
                .pattern("%@%")
                .define('%', Items.IRON_INGOT)
                .define('@', Items.IRON_BLOCK)
                .define('#', Items.PISTON)
                .define('$', Items.FURNACE)
                .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                .save(writer, recipeId("crafting/press"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BLAST_FURNACE_EXTENSION.get())
                .pattern(" $ ")
                .pattern("%#%")
                .pattern("%#%")
                .define('#', ModItems.PLATE_STEEL.get())
                .define('%', ModItems.FIREBRICK.get())
                .define('$', ModItems.PLATE_COPPER.get())
                .unlockedBy(getHasName(Items.PISTON), has(Items.PISTON))
                .save(writer, recipeId("crafting/blast_furnace_extension"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.EXPLOSIVE_CHARGE.get())
                .pattern("$% ")
                .pattern("%$ ")
                .pattern("   ")
                .define('%', ModItems.BALL_TNT.get())
                .define('$', Items.SAND)
                .unlockedBy(getHasName(ModItems.BALL_TNT.get()), has(ModItems.BALL_TNT.get()))
                .save(writer, recipeId("crafting/explosive_charge"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ANVIL_IRON.get())
                .pattern("###")
                .pattern(" @ ")
                .pattern("###")
                .define('#', Items.IRON_INGOT)
                .define('@', Items.IRON_BLOCK)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/anvil_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ANVIL_LEAD.get())
                .pattern("###")
                .pattern(" @ ")
                .pattern("###")
                .define('#', ModItems.getIngot(ModIngots.LEAD).get())
                .define('@', ModBlocks.getIngotBlock(ModIngots.LEAD).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.LEAD).get()), has(ModItems.getIngot(ModIngots.LEAD).get()))
                .save(writer, recipeId("crafting/anvil_lead"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NUCLEAR_CHARGE.get())
                .pattern("$$$")
                .pattern("%@%")
                .pattern("%#%")
                .define('%', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.MAN_CORE.get())
                .define('#', ModItems.CONTROLLER.get())
                .define('$', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.MAN_CORE.get()), has(ModItems.MAN_CORE.get()))
                .save(writer, recipeId("crafting/nuclear_charge"));

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

    //переплавка порошков - ✅ ИСПРАВЛЕННАЯ ВЕРСИЯ
    private void registerPowderCooking(Consumer<FinishedRecipe> writer) {
        // ✅ ПРОВЕРЯЕМ КАЖДЫЙ ПОРОШОК ПЕРЕД ИСПОЛЬЗОВАНИЕМ
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

    //крафты ящиков
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
                .pattern("AAA")
                .pattern("ABA")
                .pattern("AAA")
                .define('A', ModItems.PLATE_DESH.get())
                .define('B', ModBlocks.CRATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_DESH.get()), has(ModItems.PLATE_DESH.get()))
                .save(writer, recipeId("crafting/crate_desh"));
    }

    //крафты штампов
    private void registerStamps(Consumer<FinishedRecipe> writer) {
        buildStamp(writer, ModItems.STAMP_STONE_FLAT.get(), Items.STONE, "stamp_stone_flat");
        buildStamp(writer, ModItems.STAMP_IRON_FLAT.get(), Items.IRON_INGOT, "stamp_iron_flat");
        buildStamp(writer, ModItems.STAMP_STEEL_FLAT.get(), ModItems.getIngot(ModIngots.STEEL).get(), "stamp_steel_flat");
        buildStamp(writer, ModItems.STAMP_TITANIUM_FLAT.get(), ModItems.getIngot(ModIngots.TITANIUM).get(), "stamp_titanium_flat");
        buildStamp(writer, ModItems.STAMP_OBSIDIAN_FLAT.get(), Blocks.OBSIDIAN.asItem(), "stamp_obsidian_flat");
        buildStamp(writer, ModItems.STAMP_DESH_FLAT.get(), ModItems.getIngot(ModIngots.DESH).get(), "stamp_desh_flat");
    }

    //крафты гранат
    private void registerGrenades(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADE.get())
                .pattern("%@ ")
                .pattern("#$#")
                .pattern(" # ")
                .define('%', ModItems.WIRE_RED_COPPER.get())
                .define('@', ModItems.PLATE_STEEL.get())
                .define('#', ModItems.PLATE_IRON.get())
                .define('$', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/grenade"));

        buildGrenadeUpgrade(writer, ModItems.GRENADEHE.get(), ModItems.BALL_TNT.get(), "grenadehe");
        buildGrenadeUpgrade(writer, ModItems.GRENADESLIME.get(), Items.SLIME_BALL, "grenadeslime");
        buildGrenadeUpgrade(writer, ModItems.GRENADEFIRE.get(), ModItems.getIngot(ModIngots.PHOSPHORUS).get(), "grenadefire");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_HE.get(), ModItems.BALL_TNT.get(), "grenade_if_he");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_SLIME.get(), Items.SLIME_BALL, "grenade_if_slime");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_FIRE.get(), ModItems.getIngot(ModIngots.PHOSPHORUS).get(), "grenade_if_fire");

        buildBlades(writer, ModItems.BLADE_STEEL.get(), ModItems.getIngot(ModIngots.STEEL).get(), ModItems.PLATE_STEEL.get(),"blades_steel");
        buildBlades(writer, ModItems.BLADE_TITANIUM.get(), ModItems.getIngot(ModIngots.TITANIUM).get(), ModItems.PLATE_TITANIUM.get(),"blades_titanium");
        buildBlades(writer, ModItems.BLADE_ALLOY.get(), ModItems.getIngot(ModIngots.ADVANCED_ALLOY).get(), ModItems.PLATE_ADVANCED_ALLOY.get(),"blades_advanced_alloy");



        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_FIRE.get()), Items.BLAZE_POWDER, "barbed_wire_fire");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_POISON.get()), Items.SPIDER_EYE, "barbed_wire_poison");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_WITHER.get()), Items.WITHER_SKELETON_SKULL, "barbed_wire_wither");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_RAD.get()), ModItems.BILLET_PLUTONIUM.get(), "barbed_wire_rad");

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADESMART.get(), 4)
                .pattern(" @ ")
                .pattern("&%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE.get())
                .define('&', ModItems.getIngot(ModIngots.PHOSPHORUS).get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.MICROCHIP.get())
                .define('$', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModItems.GRENADE.get()), has(ModItems.GRENADE.get()))
                .save(writer, recipeId("crafting/grenadesmart"));
    }

    //крафты брони и инструментов
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

    private void buildGrenadeUpgrade(Consumer<FinishedRecipe> writer, Item result, ItemLike core, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 4)
                .pattern(" # ")
                .pattern("$%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('$', core)
                .unlockedBy(getHasName(ModItems.GRENADE.get()), has(ModItems.GRENADE.get()))
                .save(writer, recipeId("crafting/" + name));
    }

    private void buildGrenadeIfUpgrade(Consumer<FinishedRecipe> writer, Item result, ItemLike core, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result, 2)
                .pattern(" # ")
                .pattern("$%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE_IF.get())
                .define('#', ModItems.PLATE_STEEL.get())
                .define('$', core)
                .unlockedBy(getHasName(ModItems.GRENADE_IF.get()), has(ModItems.GRENADE_IF.get()))
                .save(writer, recipeId("crafting/" + name));
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
    // ✅ ИСПРАВЛЕННЫЙ МЕТОД - ВСЕ СТРОКИ ОДИНАКОВОЙ ШИРИНЫ (3x3)
    private void buildStamp(Consumer<FinishedRecipe> writer, Item result, ItemLike material, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("###")
                .pattern("$$$")
                .pattern("   ")  // ✅ БЫЛО " ", ТЕПЕРЬ "   " (3 пробела для ширины 3)
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