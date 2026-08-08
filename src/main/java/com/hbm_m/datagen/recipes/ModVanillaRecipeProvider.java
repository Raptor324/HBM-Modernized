package com.hbm_m.datagen.recipes;
//? if forge {
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.item.tags_and_tiers.ModIngots;
import com.hbm_m.item.tags_and_tiers.ModPowders;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import net.minecraftforge.common.Tags;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

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
        registerMeteoriteSword(writer);
        registerBilletNuggetPairs(writer);
        registerTurretRecipes(writer);
        registerRbmkFuelRecipes(writer);
    }

    /**
     * RBMK fuel chain - 1:1 in structure to the original's {@code RodRecipes.addRBMKRod}/
     * {@code addPellet} (empty casing + 8 loaded units -&gt; rod), adapted to this port's item
     * model: the original loaded rods directly from ore-dict billets; this port has an explicit
     * pellet item as the rod's stated precursor (see {@code RBMKPelletItem}'s class doc), so each
     * pellet is first crafted from the matching billet (1:1), then 8 pellets + the empty casing
     * assemble into the rod (matching the original's 8-billet loading pattern exactly, just with
     * the pellet as the intermediate unit). The empty casing recipe (zirconium + rod_quad_empty)
     * is a 1:1 port of the original's own {@code rbmk_fuel_empty} recipe.
     */
    private void registerRbmkFuelRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_FUEL_EMPTY.get())
                .pattern("ZRZ")
                .pattern("Z Z")
                .pattern("ZRZ")
                .define('Z', ModItems.getIngot(ModIngots.ZIRCONIUM).get())
                .define('R', ModItems.ROD_QUAD_EMPTY.get())
                .unlockedBy(getHasName(ModItems.ROD_QUAD_EMPTY.get()), has(ModItems.ROD_QUAD_EMPTY.get()))
                .save(writer, recipeId("crafting/rbmk_fuel_empty"));

        rbmkPellet(writer, ModItems.RBMK_PELLET_UEU, ModItems.BILLET_URANIUM);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MEU, ModItems.BILLET_URANIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEU233, ModItems.BILLET_U233);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEU235, ModItems.BILLET_U235);
        rbmkPellet(writer, ModItems.RBMK_PELLET_UZH, ModItems.BILLET_UZH);
        rbmkPellet(writer, ModItems.RBMK_PELLET_THMEU, ModItems.BILLET_THORIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_LEP, ModItems.BILLET_PLUTONIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MEP, ModItems.BILLET_PU_MIX);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEP, ModItems.BILLET_PU239);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEP241, ModItems.BILLET_PU241);
        rbmkPellet(writer, ModItems.RBMK_PELLET_LEA, ModItems.BILLET_AMERICIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MEA, ModItems.BILLET_AM_MIX);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEA241, ModItems.BILLET_AM241);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEA242, ModItems.BILLET_AM242);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MEN, ModItems.BILLET_NEPTUNIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEN, ModItems.BILLET_NEPTUNIUM);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MOX, ModItems.BILLET_MOX_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_LES, ModItems.BILLET_LES);
        rbmkPellet(writer, ModItems.RBMK_PELLET_MES, ModItems.BILLET_SCHRABIDIUM_FUEL);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HES, ModItems.BILLET_HES);
        rbmkPellet(writer, ModItems.RBMK_PELLET_LEAUS, ModItems.BILLET_AUSTRALIUM_LESSER);
        rbmkPellet(writer, ModItems.RBMK_PELLET_HEAUS, ModItems.BILLET_AUSTRALIUM_GREATER);
        rbmkPellet(writer, ModItems.RBMK_PELLET_PO210BE, ModItems.BILLET_PO210BE);
        rbmkPellet(writer, ModItems.RBMK_PELLET_RA226BE, ModItems.BILLET_RA226BE);
        rbmkPellet(writer, ModItems.RBMK_PELLET_PU238BE, ModItems.BILLET_PU238BE);
        rbmkPellet(writer, ModItems.RBMK_PELLET_BALEFIRE_GOLD, ModItems.BILLET_BALEFIRE_GOLD);
        rbmkPellet(writer, ModItems.RBMK_PELLET_FLASHLEAD, ModItems.BILLET_FLASHLEAD);
        rbmkPellet(writer, ModItems.RBMK_PELLET_ZFB_BISMUTH, ModItems.BILLET_ZFB_BISMUTH);
        rbmkPellet(writer, ModItems.RBMK_PELLET_ZFB_PU241, ModItems.BILLET_ZFB_PU241);
        rbmkPellet(writer, ModItems.RBMK_PELLET_ZFB_AM_MIX, ModItems.BILLET_ZFB_AM_MIX);

        rbmkRod(writer, ModItems.RBMK_FUEL_UEU, ModItems.RBMK_PELLET_UEU);
        rbmkRod(writer, ModItems.RBMK_FUEL_MEU, ModItems.RBMK_PELLET_MEU);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEU233, ModItems.RBMK_PELLET_HEU233);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEU235, ModItems.RBMK_PELLET_HEU235);
        rbmkRod(writer, ModItems.RBMK_FUEL_UZH, ModItems.RBMK_PELLET_UZH);
        rbmkRod(writer, ModItems.RBMK_FUEL_THMEU, ModItems.RBMK_PELLET_THMEU);
        rbmkRod(writer, ModItems.RBMK_FUEL_LEP, ModItems.RBMK_PELLET_LEP);
        rbmkRod(writer, ModItems.RBMK_FUEL_MEP, ModItems.RBMK_PELLET_MEP);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEP, ModItems.RBMK_PELLET_HEP);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEP_ALT, ModItems.RBMK_PELLET_HEP);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEP241, ModItems.RBMK_PELLET_HEP241);
        rbmkRod(writer, ModItems.RBMK_FUEL_LEA, ModItems.RBMK_PELLET_LEA);
        rbmkRod(writer, ModItems.RBMK_FUEL_MEA, ModItems.RBMK_PELLET_MEA);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEA241, ModItems.RBMK_PELLET_HEA241);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEA242, ModItems.RBMK_PELLET_HEA242);
        rbmkRod(writer, ModItems.RBMK_FUEL_MEN, ModItems.RBMK_PELLET_MEN);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEN, ModItems.RBMK_PELLET_HEN);
        rbmkRod(writer, ModItems.RBMK_FUEL_MOX, ModItems.RBMK_PELLET_MOX);
        rbmkRod(writer, ModItems.RBMK_FUEL_LES, ModItems.RBMK_PELLET_LES);
        rbmkRod(writer, ModItems.RBMK_FUEL_MES, ModItems.RBMK_PELLET_MES);
        rbmkRod(writer, ModItems.RBMK_FUEL_HES, ModItems.RBMK_PELLET_HES);
        rbmkRod(writer, ModItems.RBMK_FUEL_LEAUS, ModItems.RBMK_PELLET_LEAUS);
        rbmkRod(writer, ModItems.RBMK_FUEL_HEAUS, ModItems.RBMK_PELLET_HEAUS);
        rbmkRod(writer, ModItems.RBMK_FUEL_PO210BE, ModItems.RBMK_PELLET_PO210BE);
        rbmkRod(writer, ModItems.RBMK_FUEL_RA226BE, ModItems.RBMK_PELLET_RA226BE);
        rbmkRod(writer, ModItems.RBMK_FUEL_PU238BE, ModItems.RBMK_PELLET_PU238BE);
        rbmkRod(writer, ModItems.RBMK_FUEL_BALEFIRE_GOLD, ModItems.RBMK_PELLET_BALEFIRE_GOLD);
        rbmkRod(writer, ModItems.RBMK_FUEL_FLASHLEAD, ModItems.RBMK_PELLET_FLASHLEAD);
        rbmkRod(writer, ModItems.RBMK_FUEL_BALEFIRE, ModItems.RBMK_PELLET_BALEFIRE);
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_BISMUTH, ModItems.RBMK_PELLET_ZFB_BISMUTH);
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_PU241, ModItems.RBMK_PELLET_ZFB_PU241);
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_AM_MIX, ModItems.RBMK_PELLET_ZFB_AM_MIX);
        rbmkRod(writer, ModItems.RBMK_FUEL_LEU235, ModItems.RBMK_PELLET_LEU235);
    }

    private void rbmkPellet(Consumer<FinishedRecipe> writer, RegistrySupplier<Item> pellet, RegistrySupplier<Item> billet) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, pellet.get())
                .requires(billet.get())
                .unlockedBy(getHasName(billet.get()), has(billet.get()))
                .save(writer, recipeId("crafting/" + pellet.getId().getPath()));
    }

    private void rbmkRod(Consumer<FinishedRecipe> writer, RegistrySupplier<Item> rod, RegistrySupplier<Item> pellet) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, rod.get())
                .requires(ModItems.RBMK_FUEL_EMPTY.get())
                .requires(pellet.get(), 8)
                .unlockedBy(getHasName(pellet.get()), has(pellet.get()))
                .save(writer, recipeId("crafting/" + rod.getId().getPath()));
    }

    //Sentry-Turret + MVP-Munition (Original-Rezept aus WeaponRecipes.java, GUNMETAL.mechanism() -> generisches PART_MECHANISM)
    private void registerTurretRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModBlocks.TURRET_SENTRY.get())
                .pattern("PPL")
                .pattern(" MD")
                .pattern(" SC")
                .define('P', ModItems.PLATE_STEEL.get())
                .define('M', ModItems.MOTOR.get())
                .define('L', ModItems.PART_MECHANISM.get())
                .define('S', ModBlocks.STEEL_SCAFFOLD.get())
                .define('C', ModItems.SILICON_CIRCUIT.get())
                .define('D', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModItems.CRT_DISPLAY.get()), has(ModItems.CRT_DISPLAY.get()))
                .save(writer, recipeId("crafting/turret_sentry"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TURRET_AMMO.get(), 8)
                .pattern("L")
                .pattern("G")
                .pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/turret_ammo"));

        // 9mm-Munition fuer den Sentry-Turret (Original hatte hierfuer keine dokumentierten Table-Rezepte -
        // plausible Annaeherung analog turret_ammo, siehe TurretBaseBlockEntity#isAcceptedAmmo).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_9MM_SP.get(), 12)
                .pattern("L")
                .pattern("G")
                .pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_9mm_sp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_9MM_JHP.get(), 12)
                .pattern("L")
                .pattern("G")
                .pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_9mm_jhp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_9MM_FMJ.get(), 12)
                .pattern("L")
                .pattern("I")
                .pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_9mm_fmj"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_9MM_AP.get(), 12)
                .pattern("L")
                .pattern("I")
                .pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_9mm_ap"));

        // .50 BMG-Munition fuer den Chekhov-Turret
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_50_SP.get(), 8)
                .pattern("L").pattern("G").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_50_sp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_50_JHP.get(), 8)
                .pattern("L").pattern("G").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_50_jhp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_50_FMJ.get(), 8)
                .pattern("L").pattern("I").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_50_fmj"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_50_AP.get(), 8)
                .pattern("L").pattern("I").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_50_ap"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_50_DU.get(), 8)
                .pattern("L").pattern("I").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', ModItems.getIngot(ModIngots.URANIUM238).get())
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_50_du"));

        // 5.56mm-Munition fuer den Friendly-Turret
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_556_SP.get(), 12)
                .pattern("L").pattern("G").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_556_sp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_556_JHP.get(), 12)
                .pattern("L").pattern("G").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('G', Items.GUNPOWDER)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/ammo_556_jhp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_556_FMJ.get(), 12)
                .pattern("L").pattern("I").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_556_fmj"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_556_AP.get(), 12)
                .pattern("L").pattern("I").pattern("C")
                .define('L', ModItems.getIngot(ModIngots.LEAD).get())
                .define('I', Items.IRON_INGOT)
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/ammo_556_ap"));

        // Gelenkte Raketen fuer Richard/Himars-Turret (Original hatte hierfuer keine dokumentierten
        // Table-Rezepte - plausible Annaeherung, siehe TurretRocketEntity).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ROCKET_TURRET_STANDARD.get())
                .pattern(" P ")
                .pattern(" M ")
                .pattern(" G ")
                .define('P', ModItems.PLATE_STEEL.get())
                .define('M', ModItems.MOTOR.get())
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/rocket_turret_standard"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ROCKET_HIMARS_STANDARD.get())
                .pattern(" P ")
                .pattern(" M ")
                .pattern(" G ")
                .define('P', ModItems.PLATE_TITANIUM.get())
                .define('M', ModItems.MOTOR.get())
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/rocket_himars_standard"));

        // Tauon-Turret Munition (Original hatte hierfuer keine dokumentierte Table-Rezept)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_TAU_URANIUM.get(), 4)
                .pattern("U")
                .pattern("C")
                .define('U', ModItems.getIngot(ModIngots.URANIUM).get())
                .define('C', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.URANIUM).get()), has(ModItems.getIngot(ModIngots.URANIUM).get()))
                .save(writer, recipeId("crafting/ammo_tau_uranium"));

        // Fritz-Turret Brennstoff (MVP-Item statt Fluid-Tank, siehe TurretBaseBlockEntity#tickFritz)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AMMO_FLAME_DIESEL.get(), 8)
                .pattern("C")
                .pattern("B")
                .define('C', ModItems.CANNED_DIESEL.get())
                .define('B', ModItems.CASING_BAG.get())
                .unlockedBy(getHasName(ModItems.CANNED_DIESEL.get()), has(ModItems.CANNED_DIESEL.get()))
                .save(writer, recipeId("crafting/ammo_flame_diesel"));

        // Missile-Assembly-Station + fehlende Teile (Original-Rezept nicht auffindbar, plausible Annaeherung)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModBlocks.MACHINE_MISSILE_ASSEMBLY.get())
                .pattern("PPP")
                .pattern("MCM")
                .pattern("SSS")
                .define('P', ModItems.PLATE_STEEL.get())
                .define('M', ModItems.MOTOR.get())
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .define('S', ModBlocks.STEEL_SCAFFOLD.get())
                .unlockedBy(getHasName(ModItems.ADVANCED_CIRCUIT.get()), has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/machine_missile_assembly"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MISSILE_FUSELAGE.get(), 2)
                .pattern("P")
                .pattern("P")
                .define('P', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/missile_fuselage"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, ModItems.MISSILE_CHIP.get())
                .requires(ModItems.SILICON_CIRCUIT.get())
                .requires(ModItems.getIngot(ModIngots.LEAD).get())
                .unlockedBy(getHasName(ModItems.SILICON_CIRCUIT.get()), has(ModItems.SILICON_CIRCUIT.get()))
                .save(writer, recipeId("crafting/missile_chip"));
    }

    //  БЕЗОПАСНАЯ ПРОВЕРКА NULL
    private boolean isItemSafe(RegistrySupplier<?> itemObj) {
        return itemObj != null && itemObj.isPresent() && itemObj.get() != null;
    }

    private ItemLike safeIngot(ModIngots ingot) {
        RegistrySupplier<?> obj = ModItems.getIngot(ingot);
        return isItemSafe(obj) ? (ItemLike) obj.get() : Items.AIR;
    }

    private Item safePowder(ModPowders powder) {
        RegistrySupplier<?> obj = ModItems.getPowders(powder);
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
                .save(writer, recipeId("crafting/motor1"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR.get(), 2)
                .pattern(" $ ")
                .pattern("%#%")
                .pattern(" @ ")
                .define('%', ModItems.PLATE_STEEL.get())
                .define('$', ModItems.WIRE_RED_COPPER.get())
                .define('#', ModItems.COIL_COPPER.get())
                .define('@', ModItems.COIL_COPPER_TORUS.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER_TORUS.get()), has(ModItems.COIL_COPPER_TORUS.get()))
                .save(writer, recipeId("crafting/motor2"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STEAM_TURBINE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModItems.getIngot(ModIngots.STEEL).get())
                .define('B', ModItems.COIL_COPPER.get())
                .define('C', Ingredient.of(ModItems.getIngot(ModIngots.POLYMER).get(), ModItems.getIngot(ModIngots.BAKELITE).get()))
                .define('D', ModItems.TURBINE_TITANIUM.get())
                .unlockedBy(getHasName(ModItems.TURBINE_TITANIUM.get()), has(ModItems.TURBINE_TITANIUM.get()))
                .save(writer, recipeId("crafting/steam_turbine"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DECON.get())
                .pattern("BGB")
                .pattern("SAS")
                .pattern("BSB")
                .define('B', ModItems.getIngot(ModIngots.BERYLLIUM).get())
                .define('G', Items.IRON_BARS)
                .define('S', ModItems.getIngot(ModIngots.STEEL).get())
                .define('A', Ingredient.of(BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), BlockAbsorber.EnumAbsorberTier.BASE)))
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.BERYLLIUM).get()), has(ModItems.getIngot(ModIngots.BERYLLIUM).get()))
                .save(writer, recipeId("crafting/decon"));

        registerRadAbsorberRecipes(writer);

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DESIGNATOR.get())
                .pattern("  A")
                .pattern("#B#")
                .pattern("#B#")
                .define('#', Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()))
                .define('A', ModItems.PLATE_STEEL.get())
                .define('B', ModItems.ANALOG_CIRCUIT.get())
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/designator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DESIGNATOR_MANUAL.get())
                .pattern("  A")
                .pattern("#C#")
                .pattern("#B#")
                .define('#', Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()))
                .define('A', ModItems.PLATE_LEAD.get())
                .define('B', ModItems.ADVANCED_CIRCUIT.get())
                .define('C', ModItems.DESIGNATOR.get())
                .unlockedBy(getHasName(ModItems.DESIGNATOR.get()), has(ModItems.DESIGNATOR.get()))
                .save(writer, recipeId("crafting/designator_manual"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DESIGNATOR_RANGE.get())
                .requires(ModItems.RANGEFINDER.get())
                .requires(ModItems.DESIGNATOR.get())
                .requires(Ingredient.of(
                        ModItems.getIngot(ModIngots.POLYMER).get(),
                        ModItems.getIngot(ModIngots.BAKELITE).get()))
                .unlockedBy(getHasName(ModItems.RANGEFINDER.get()), has(ModItems.RANGEFINDER.get()))
                .save(writer, recipeId("crafting/designator_range"));

        // TODO: временная заглушка — заменить на литьё из плутония, когда переработка будет портирована
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BILLET_PLUTONIUM.get())
                .requires(ModItems.getIngot(ModIngots.URANIUM).get(), 6)
                .requires(ModItems.getIngot(ModIngots.LEAD).get(), 3)
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.URANIUM).get()), has(ModItems.getIngot(ModIngots.URANIUM).get()))
                .save(writer, recipeId("crafting/billet_plutonium_stub"));

        // TODO: временная заглушка — заменить на ass.mancore в сборочной машине
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.FAT_MAN_CORE.get())
                .requires(ModItems.BILLET_PLUTONIUM.get(), 1)
                .requires(ModItems.getIngot(ModIngots.BERYLLIUM).get(), 2)
                .unlockedBy(getHasName(ModItems.BILLET_PLUTONIUM.get()), has(ModItems.BILLET_PLUTONIUM.get()))
                .save(writer, recipeId("crafting/fat_man_core_stub"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRT_DISPLAY.get(), 4)
                .pattern(" # ")
                .pattern("$@$")
                .pattern(" % ")
                .define('$', ModItems.PLATE_STEEL.get())
                .define('#', ModItems.getPowder(ModIngots.ALUMINUM).get())
                .define('%', ModItems.VACUUM_TUBE.get())
                .define('@', Ingredient.of(Tags.Items.GLASS_PANES))
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
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SCREWDRIVER.get())
                .pattern("  #")
                .pattern(" # ")
                .pattern("$  ")
                .define('#', Items.IRON_INGOT)
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/screwdriver"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STEAM_CONDENSER.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModItems.getIngot(ModIngots.STEEL).get())
                .define('B', ModItems.PLATE_IRON.get())
                .define('C', ModItems.PLATE_CAST_COPPER.get())
                .unlockedBy(getHasName(ModItems.PLATE_CAST_COPPER.get()), has(ModItems.PLATE_CAST_COPPER.get()))
                .save(writer, recipeId("crafting/steam_condenser"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CONVERTER_BLOCK.get())
                .pattern("###")
                .pattern("@@@")
                .pattern("$$$")
                .define('#', ModItems.CAPACITOR.get())
                .define('@', Items.REDSTONE)
                .define('$', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.STEEL).get()), has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/converter_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MACHINE_BATTERY_SOCKET.get())
                .pattern("$@$")
                .define('$', ModItems.PLATE_STEEL.get())
                .define('@', ModItems.getIngot(ModIngots.RED_COPPER).get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/machine_battery_socket"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MACHINE_BATTERY_SOCKET.get())
                .pattern("I I")
                .pattern("I I")
                .pattern("IRI")
                .define('I', ModItems.PLATE_ALUMINUM.get())
                .define('R', ModItems.COIL_COPPER.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/machine_battery_socket_frame"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOLT_STEEL.get(), 16)
                .pattern("$")
                .pattern("$")
                .define('$', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/bolt_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOLT_HIGHSPEED_STEEL.get(), 16)
                .pattern("$")
                .pattern("$")
                .define('$', ModItems.getIngot(ModIngots.DURA_STEEL).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.DURA_STEEL).get()), has(ModItems.getIngot(ModIngots.DURA_STEEL).get()))
                .save(writer, recipeId("crafting/bolt_highspeed_steel"));

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
                .pattern("$")
                .pattern("#")
                .pattern("@")
                .define('$', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('#', ModItems.WIRE_TUNGSTEN.get())
                .define('@', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.WIRE_TUNGSTEN.get()), has(ModItems.WIRE_TUNGSTEN.get()))
                .save(writer, recipeId("crafting/vacuum_tube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPACITOR.get(), 2)
                .pattern("$#$")
                .pattern("% %")
                .define('$', ModItems.INSULATOR.get())
                .define('%', Ingredient.of(ModItems.WIRE_COPPER.get(), ModItems.WIRE_ALUMINIUM.get()))
                .define('#', ModItems.getPowder(ModIngots.ALUMINUM).get())
                .unlockedBy(getHasName(ModItems.getPowder(ModIngots.ALUMINUM).get()), has(ModItems.getPowder(ModIngots.ALUMINUM).get()))
                .save(writer, recipeId("crafting/capacitor"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CAPACITOR_TANTALUM.get())
                .requires(ModItems.INSULATOR.get())
                .requires(ModItems.NUGGET_TANTALIUM.get())
                .requires(ModItems.WIRE_COPPER.get())
                .unlockedBy(getHasName(ModItems.NUGGET_TANTALIUM.get()), has(ModItems.NUGGET_TANTALIUM.get()))
                .save(writer, recipeId("crafting/capacitor_tantalum"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CAGE_LAMP.get(), 4)
                .pattern("%")
                .pattern("@")
                .pattern("!")
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('@', ModItems.WIRE_TUNGSTEN.get())
                .define('!', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/cage_lamp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLOOD_LAMP.get(), 8)
                .pattern("%")
                .pattern("@")
                .pattern("!")
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('@', ModItems.getPowder(ModIngots.RED_COPPER).get())
                .define('!', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.PLATE_STEEL.get()), has(ModItems.PLATE_STEEL.get()))
                .save(writer, recipeId("crafting/flood_lamp"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RANGEFINDER.get())
                .pattern("GRC")
                .pattern("  S")
                .define('G', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('R', Items.REDSTONE)
                .define('C', ModItems.INTEGRATED_CIRCUIT.get())
                .define('S', ModItems.PLATE_STEEL.get())
                .unlockedBy(getHasName(ModItems.INTEGRATED_CIRCUIT.get()), has(ModItems.INTEGRATED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/rangefinder"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.TEMPLATE_FOLDER.get())
                .pattern("@#@")
                .pattern("@#@")
                .pattern("@#@")
                .define('@', Ingredient.of(Items.BLUE_DYE, Items.LAPIS_LAZULI))
                .define('#', Items.PAPER)
                .unlockedBy(getHasName(Items.PAPER), has(Items.PAPER))
                .save(writer, recipeId("crafting/template_folder"));

        // BUILDING BLOCKS START

        // DECO
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.DECO_STEEL_SCAFFOLD.get(), 8)
                .pattern("###")
                .pattern(" # ")
                .pattern("###")
                .define('#', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy("has_steel_ingot", has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/deco_steel_scaffold"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_POLE.get(), 16)
                .pattern("# #")
                .pattern("###")
                .pattern("# #")
                .define('#', ModItems.getIngot(ModIngots.STEEL).get())
                .unlockedBy("has_steel_ingot", has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/steel_pole"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.ANTENNA_TOP.get(), 1)
                .pattern("# #")
                .pattern("#@#")
                .pattern("$$$")
                .define('#', ModItems.getIngot(ModIngots.STEEL).get())
                .define('@', ModItems.getIngot(ModIngots.RED_COPPER).get())
                .define('$', ModItems.getIngot(ModIngots.BERYLLIUM).get())
                .unlockedBy("has_steel_ingot", has(ModItems.getIngot(ModIngots.STEEL).get()))
                .save(writer, recipeId("crafting/antenna_top"));

        // OTHER
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_GLASS.get(), 5)
                .pattern("$#$")
                .pattern("#$#")
                .pattern("$#$")
                .define('#', Blocks.GLASS)
                .define('$', Blocks.IRON_BARS)
                .unlockedBy("has_iron_Ingot", has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/reinforced_glass"));

        // CONCRETES AND STONES
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REBAR.get(), 8)
                .pattern("## ")
                .pattern("## ")
                .pattern("   ")
                .define('#', ModItems.BOLT_STEEL.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/rebar"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE.get(), 4)
                .pattern("#$#")
                .pattern("$#$")
                .pattern("#$#")
                .define('#', Blocks.COBBLESTONE)
                .define('$', Blocks.STONE)
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE_STAIRS.get(), 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .define('#', ModBlocks.REINFORCED_STONE.get())
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone_stairs"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_STONE_SLAB.get(), 6)
                .pattern("###")
                .define('#', ModBlocks.REINFORCED_STONE.get())
                .unlockedBy("has_stone", has(Blocks.STONE))
                .save(writer, recipeId("crafting/reinforced_stone_slab"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_HAZARD.get(), 6)
                .pattern("###")
                .pattern("$ @")
                .pattern("###")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', ModItems.SULFUR.get())
                .define('@', Ingredient.of(Tags.Items.DYES_GREEN))
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_hazard"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_HAZARD_STAIRS.get(), 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .define('#', ModBlocks.CONCRETE_HAZARD.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_hazard_stairs"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_HAZARD_SLAB.get(), 6)
                .pattern("###")
                .define('#', ModBlocks.CONCRETE_HAZARD.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_hazard_slab"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BRICK_CONCRETE.get(), 6)
                .pattern(" # ")
                .pattern("#$#")
                .pattern(" # ")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', Items.CLAY_BALL)
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/brick_concrete"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BRICK_CONCRETE_STAIRS.get(), 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .define('#', ModBlocks.BRICK_CONCRETE.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/brick_conrete_stairs"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BRICK_CONCRETE_SLAB.get(), 6)
                .pattern("###")
                .define('#', ModBlocks.BRICK_CONCRETE.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/brick_concrete_slab"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_PILLAR.get(), 6)
                .pattern("#$#")
                .pattern("#$#")
                .pattern("#$#")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', Blocks.IRON_BARS)
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_rebar"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_REBAR_ALT.get(), 5)
                .pattern("#$#")
                .pattern("$#$")
                .pattern("#$#")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', Blocks.IRON_BARS)
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_rebar_alt"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_VENT.get(), 3)
                .pattern("$#")
                .pattern("##")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', Blocks.IRON_TRAPDOOR)
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_vent"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_TILE_TREFOIL.get(), 1)
                .pattern("#$ ")
                .define('#', ModBlocks.CONCRETE_TILE.get())
                .define('$', Tags.Items.DYES_BLACK)
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_tile_marked"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_FAN.get(), 3)
                .pattern("$#")
                .pattern("##")
                .define('#', ModBlocks.CONCRETE.get())
                .define('$', ModItems.PLATE_IRON.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_fan"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_TILE.get(), 4)
                .pattern("## ")
                .pattern("## ")
                .define('#', ModBlocks.CONCRETE.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_tile"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_TILE_STAIRS.get(), 4)
                .pattern("#  ")
                .pattern("## ")
                .pattern("###")
                .define('#', ModBlocks.CONCRETE_TILE.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_tile_stairs"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.CONCRETE_TILE_SLAB.get(), 6)
                .pattern("###")
                .define('#', ModBlocks.CONCRETE_TILE.get())
                .unlockedBy("has_concrete", has(ModBlocks.CONCRETE.get()))
                .save(writer, recipeId("crafting/concrete_tile_slab"));

        // BUILDING BLOCKS END

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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FOUNDRY_BASIN.get())
                .pattern("% %")
                .pattern("% %")
                .pattern("%#%")
                .define('%', ModItems.FIREBRICK.get())
                .define('#', Ingredient.of(ModTags.Items.SLABS_HARD))
                .unlockedBy(getHasName(ModItems.FIREBRICK.get()), has(ModItems.FIREBRICK.get()))
                .save(writer, recipeId("crafting/foundry_basin"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FOUNDRY_CHANNEL.get(), 4)
                .pattern("% %")
                .pattern(" # ")
                .pattern("   ")
                .define('%', ModItems.FIREBRICK.get())
                .define('#', Ingredient.of(ModTags.Items.SLABS_HARD))
                .unlockedBy(getHasName(ModItems.FIREBRICK.get()), has(ModItems.FIREBRICK.get()))
                .save(writer, recipeId("crafting/foundry_channel"));

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
                .define('@', ModItems.FAT_MAN_CORE.get())
                .define('#', ModItems.CONTROLLER.get())
                .define('$', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.FAT_MAN_CORE.get()), has(ModItems.FAT_MAN_CORE.get()))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.getPowders(ModPowders.CEMENT).get(), 4)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModItems.getPowders(ModPowders.LIMESTONE).get())
                .define('B', Items.CLAY_BALL)
                .unlockedBy(getHasName(ModItems.getPowders(ModPowders.LIMESTONE).get()), has(ModItems.getPowders(ModPowders.LIMESTONE).get()))
                .save(writer, recipeId("crafting/limestone_cement"));

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

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_TUNGSTEN.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("BBB")
                .define('A', ModItems.PLATE_STEEL.get())
                .define('B', ModItems.getIngot(ModIngots.TUNGSTEN).get())
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.TUNGSTEN).get()), has(ModItems.getIngot(ModIngots.TUNGSTEN).get()))
                .save(writer, recipeId("crafting/crate_tungsten"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_TEMPLATE.get())
                .pattern("ABA")
                .pattern("B B")
                .pattern("ABA")
                .define('A', Items.IRON_INGOT)
                .define('B', Items.REDSTONE)
                .unlockedBy(getHasName(Items.REDSTONE), has(Items.REDSTONE))
                .save(writer, recipeId("crafting/crate_template"));
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
    //  ИСПРАВЛЕННЫЙ МЕТОД - ВСЕ СТРОКИ ОДИНАКОВОЙ ШИРИНЫ (3x3)
    private void buildStamp(Consumer<FinishedRecipe> writer, Item result, ItemLike material, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("###")
                .pattern("$$$")
                .pattern("   ")  //  БЫЛО " ", ТЕПЕРЬ "   " (3 пробела для ширины 3)
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

    private void registerMeteoriteSword(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.METEORITE_SWORD.get())
                .pattern(" M ")
                .pattern("PMP")
                .pattern(" S ")
                .define('M', ModItems.getIngot(ModIngots.METEORITE_FORGED).get())
                .define('P', ModItems.PLATE_GOLD.get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModItems.getIngot(ModIngots.METEORITE_FORGED).get()),
                        has(ModItems.getIngot(ModIngots.METEORITE_FORGED).get()))
                .save(writer, recipeId("meteorite_sword"));

        registerSmelting(writer, ModItems.METEORITE_SWORD.get(), ModItems.METEORITE_SWORD_SEARED.get(),
                0.7F, 200, "meteorite_sword_seared");
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

    private void registerRadAbsorberRecipes(Consumer<FinishedRecipe> writer) {
        BlockAbsorber.EnumAbsorberTier base = BlockAbsorber.EnumAbsorberTier.BASE;
        BlockAbsorber.EnumAbsorberTier red = BlockAbsorber.EnumAbsorberTier.RED;
        BlockAbsorber.EnumAbsorberTier green = BlockAbsorber.EnumAbsorberTier.GREEN;
        BlockAbsorber.EnumAbsorberTier pink = BlockAbsorber.EnumAbsorberTier.PINK;

        ItemStack baseStack = BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), base);
        ItemStack redStack = BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), red);
        ItemStack greenStack = BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), green);

        String[] pattern = {"ICI", "CPC", "ICI"};

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_base"), baseStack, pattern,
                mapOf(
                        'I', Ingredient.of(Items.COPPER_INGOT),
                        'C', Ingredient.of(ModItems.getPowders(ModPowders.COAL).get()),
                        'P', Ingredient.of(ModItems.getPowder(ModIngots.LEAD).get())
                ),
                Items.COPPER_INGOT, "has_copper");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_red"), redStack, pattern,
                mapOf(
                        'I', Ingredient.of(ModItems.getIngot(ModIngots.TITANIUM).get()),
                        'C', Ingredient.of(ModItems.getPowders(ModPowders.COAL).get()),
                        'P', Ingredient.of(baseStack)
                ),
                baseStack.getItem(), "has_rad_absorber_base");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_green"), greenStack, pattern,
                mapOf(
                        'I', Ingredient.of(
                                ModItems.getIngot(ModIngots.BAKELITE).get(),
                                ModItems.getIngot(ModIngots.POLYMER).get()),
                        'C', Ingredient.of(ModItems.POWDER_DESH_MIX.get()),
                        'P', Ingredient.of(redStack)
                ),
                redStack.getItem(), "has_rad_absorber_red");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_pink"),
                BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), pink), pattern,
                mapOf(
                        'I', Ingredient.of(ModItems.getIngot(ModIngots.SATURNITE).get()),
                        'C', Ingredient.of(ModItems.POWDER_NITAN_MIX.get()),
                        'P', Ingredient.of(greenStack)
                ),
                greenStack.getItem(), "has_rad_absorber_green");
    }

    private static Map<Character, Ingredient> mapOf(Object... entries) {
        Map<Character, Ingredient> map = new LinkedHashMap<>();
        for (int i = 0; i < entries.length; i += 2) {
            map.put((Character) entries[i], (Ingredient) entries[i + 1]);
        }
        return map;
    }

    private void saveShapedStackRecipe(Consumer<FinishedRecipe> writer, ResourceLocation recipeId,
            ItemStack result, String[] pattern, Map<Character, Ingredient> keys,
            ItemLike unlockItem, String unlockCriterion) {
        Advancement.Builder advancement = Advancement.Builder.advancement();
        CriterionTriggerInstance criterion = has(unlockItem);
        advancement.addCriterion(unlockCriterion, criterion);
        ResourceLocation advancementId = recipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/");

        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(@NotNull JsonObject json) {
                json.addProperty("type", "minecraft:crafting_shaped");
                json.addProperty("category", "misc");
                JsonArray patternJson = new JsonArray();
                for (String line : pattern) {
                    patternJson.add(line);
                }
                json.add("pattern", patternJson);
                JsonObject keyJson = new JsonObject();
                keys.forEach((symbol, ingredient) -> keyJson.add(String.valueOf(symbol), ingredient.toJson()));
                json.add("key", keyJson);
                json.add("result", stackToJson(result));
            }

            @Override
            public ResourceLocation getId() {
                return recipeId;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return RecipeSerializer.SHAPED_RECIPE;
            }

            @Override
            @Nullable
            public JsonObject serializeAdvancement() {
                return advancement.serializeToJson();
            }

            @Override
            @Nullable
            public ResourceLocation getAdvancementId() {
                return advancementId;
            }
        });
    }

    private static JsonObject stackToJson(ItemStack stack) {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) {
            json.addProperty("count", stack.getCount());
        }
        if (stack.hasTag()) {
            json.addProperty("nbt", stack.getTag().toString());
        }
        return json;
    }

    private ResourceLocation recipeId(String path) {
        //? if fabric && < 1.21.1 {
        /*return new ResourceLocation(RefStrings.MODID, path);
        *///?} else {
                return ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, path);
        //?}
    }


    /**
     * Billet <-> nugget compression (6 nuggets -> 1 billet, 1 billet -> 6 nuggets).
     * Port of the 1.7.10 MineralRecipes.java addBillet(billet, nugget) family.
     */
    private void registerBilletNuggetPairs(Consumer<FinishedRecipe> writer) {
        registerBilletNuggetPair(writer, ModItems.BILLET_ACTINIUM.get(), ModItems.NUGGET_ACTINIUM.get(), "actinium");
        registerBilletNuggetPair(writer, ModItems.BILLET_AM241.get(), ModItems.NUGGET_AM241.get(), "am241");
        registerBilletNuggetPair(writer, ModItems.BILLET_AM242.get(), ModItems.NUGGET_AM242.get(), "am242");
        registerBilletNuggetPair(writer, ModItems.BILLET_AMERICIUM_FUEL.get(), ModItems.NUGGET_AMERICIUM_FUEL.get(), "americium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_AM_MIX.get(), ModItems.NUGGET_AM_MIX.get(), "am_mix");
        registerBilletNuggetPair(writer, ModItems.BILLET_AU198.get(), ModItems.NUGGET_AU198.get(), "au198");
        registerBilletNuggetPair(writer, ModItems.BILLET_AUSTRALIUM.get(), ModItems.NUGGET_AUSTRALIUM.get(), "australium");
        registerBilletNuggetPair(writer, ModItems.BILLET_AUSTRALIUM_GREATER.get(), ModItems.NUGGET_AUSTRALIUM_GREATER.get(), "australium_greater");
        registerBilletNuggetPair(writer, ModItems.BILLET_AUSTRALIUM_LESSER.get(), ModItems.NUGGET_AUSTRALIUM_LESSER.get(), "australium_lesser");
        registerBilletNuggetPair(writer, ModItems.BILLET_BERYLLIUM.get(), ModItems.NUGGET_BERYLLIUM.get(), "beryllium");
        registerBilletNuggetPair(writer, ModItems.BILLET_BISMUTH.get(), ModItems.NUGGET_BISMUTH.get(), "bismuth");
        registerBilletNuggetPair(writer, ModItems.BILLET_CO60.get(), ModItems.NUGGET_CO60.get(), "co60");
        registerBilletNuggetPair(writer, ModItems.BILLET_COBALT.get(), ModItems.NUGGET_COBALT.get(), "cobalt");
        registerBilletNuggetPair(writer, ModItems.BILLET_GH336.get(), ModItems.NUGGET_GH336.get(), "gh336");
        registerBilletNuggetPair(writer, ModItems.BILLET_HES.get(), ModItems.NUGGET_HES.get(), "hes");
        registerBilletNuggetPair(writer, ModItems.BILLET_LES.get(), ModItems.NUGGET_LES.get(), "les");
        registerBilletNuggetPair(writer, ModItems.BILLET_MOX_FUEL.get(), ModItems.NUGGET_MOX_FUEL.get(), "mox_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_NEPTUNIUM.get(), ModItems.NUGGET_NEPTUNIUM.get(), "neptunium");
        registerBilletNuggetPair(writer, ModItems.BILLET_NEPTUNIUM_FUEL.get(), ModItems.NUGGET_NEPTUNIUM_FUEL.get(), "neptunium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_PB209.get(), ModItems.NUGGET_PB209.get(), "pb209");
        registerBilletNuggetPair(writer, ModItems.BILLET_PLUTONIUM.get(), ModItems.NUGGET_PLUTONIUM.get(), "plutonium");
        registerBilletNuggetPair(writer, ModItems.BILLET_PLUTONIUM_FUEL.get(), ModItems.NUGGET_PLUTONIUM_FUEL.get(), "plutonium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_POLONIUM.get(), ModItems.NUGGET_POLONIUM.get(), "polonium");
        registerBilletNuggetPair(writer, ModItems.BILLET_PU238.get(), ModItems.NUGGET_PU238.get(), "pu238");
        registerBilletNuggetPair(writer, ModItems.BILLET_PU239.get(), ModItems.NUGGET_PU239.get(), "pu239");
        registerBilletNuggetPair(writer, ModItems.BILLET_PU240.get(), ModItems.NUGGET_PU240.get(), "pu240");
        registerBilletNuggetPair(writer, ModItems.BILLET_PU241.get(), ModItems.NUGGET_PU241.get(), "pu241");
        registerBilletNuggetPair(writer, ModItems.BILLET_PU_MIX.get(), ModItems.NUGGET_PU_MIX.get(), "pu_mix");
        registerBilletNuggetPair(writer, ModItems.BILLET_RA226.get(), ModItems.NUGGET_RA226.get(), "ra226");
        registerBilletNuggetPair(writer, ModItems.BILLET_SCHRABIDIUM.get(), ModItems.NUGGET_SCHRABIDIUM.get(), "schrabidium");
        registerBilletNuggetPair(writer, ModItems.BILLET_SCHRABIDIUM_FUEL.get(), ModItems.NUGGET_SCHRABIDIUM_FUEL.get(), "schrabidium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_SILICON.get(), ModItems.NUGGET_SILICON.get(), "silicon");
        registerBilletNuggetPair(writer, ModItems.BILLET_SOLINIUM.get(), ModItems.NUGGET_SOLINIUM.get(), "solinium");
        registerBilletNuggetPair(writer, ModItems.BILLET_SR90.get(), ModItems.NUGGET_SR90.get(), "sr90");
        registerBilletNuggetPair(writer, ModItems.BILLET_TECHNETIUM.get(), ModItems.NUGGET_TECHNETIUM.get(), "technetium");
        registerBilletNuggetPair(writer, ModItems.BILLET_TH232.get(), ModItems.NUGGET_TH232.get(), "th232");
        registerBilletNuggetPair(writer, ModItems.BILLET_THORIUM_FUEL.get(), ModItems.NUGGET_THORIUM_FUEL.get(), "thorium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_U233.get(), ModItems.NUGGET_U233.get(), "u233");
        registerBilletNuggetPair(writer, ModItems.BILLET_U235.get(), ModItems.NUGGET_U235.get(), "u235");
        registerBilletNuggetPair(writer, ModItems.BILLET_U238.get(), ModItems.NUGGET_U238.get(), "u238");
        registerBilletNuggetPair(writer, ModItems.BILLET_URANIUM.get(), ModItems.NUGGET_URANIUM.get(), "uranium");
        registerBilletNuggetPair(writer, ModItems.BILLET_URANIUM_FUEL.get(), ModItems.NUGGET_URANIUM_FUEL.get(), "uranium_fuel");
        registerBilletNuggetPair(writer, ModItems.BILLET_ZIRCONIUM.get(), ModItems.NUGGET_ZIRCONIUM.get(), "zirconium");
    }

    private void registerBilletNuggetPair(Consumer<FinishedRecipe> writer, Item billet, Item nugget, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, billet)
                .pattern("###")
                .pattern("###")
                .define('#', nugget)
                .unlockedBy(getHasName(nugget), has(nugget))
                .save(writer, recipeId("crafting/billet_" + name + "_compress"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 6)
                .requires(billet)
                .unlockedBy(getHasName(billet), has(billet))
                .save(writer, recipeId("crafting/nugget_" + name + "_decompress"));
    }
}
//?}