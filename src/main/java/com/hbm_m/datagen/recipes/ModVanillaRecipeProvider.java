package com.hbm_m.datagen.recipes;
//? if forge {
import java.util.LinkedHashMap;
import java.util.List;
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
import com.hbm_m.item.material.MaterialShape;
import com.hbm_m.item.material.ModMaterialItems;
import com.hbm_m.item.material.ModMaterials;
import com.hbm_m.item.tags_and_tiers.ModTags;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.PlatformHooks;
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
import net.minecraft.tags.ItemTags;
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
        registerRbmkBlockRecipes(writer);
        registerBookOfWagons(writer);
        registerPileRecipes(writer);
        registerPinkWoodRecipes(writer);
        registerMassStorageRecipes(writer);
        registerPneumaticRecipes(writer);
        registerWasteCompression(writer);
    }

    /**
     * RBMK fuel chain, 1:1 with the original's {@code crafting/RodRecipes.java:89-121}:
     * an empty zirconium casing plus eight billets of the matching material assemble
     * shapelessly into the loaded rod. The original has no billet-to-pellet step - pellets
     * only ever come *out* of a rod via the disassembly recipe, ported from
     * {@code crafting/handlers/RBMKFuelCraftingHandler.java}.
     */
    /**
     * Книга Вагонов ({@code book_of_}): «золотой» рецепт из оригинала (B = осколок яйца
     * белфайра, G = золотой слиток, A = книга). В 1.7.10 он регистрировался только при
     * включённом LBSM-конфиге; здесь он безусловный, поскольку шуточный рецепт из 8
     * страниц ({@code page_of_}, предмет не портирован) недоступен — иначе книга была бы
     * получаема только через лут Красной комнаты.
     */
    /**
     * 1:1-Port der drei {@code pile_device}-Rezepte aus {@code CraftingManager} (1.7.10). Dort ist
     * es ein Block mit drei Metadaten, hier sind es drei Bloecke - die Muster bleiben gleich.
     */
    private void registerPileRecipes(Consumer<FinishedRecipe> writer) {

        // Ladevorrichtung: " A " / "CBS"
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PILE_LOADER.get())
                .pattern(" A ")
                .pattern("CBS")
                .define('A', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .define('C', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST))
                .define('B', ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT))
                .define('S', ModItems.SHELL_STEEL.get())
                .unlockedBy(getHasName(ModItems.SHELL_STEEL.get()), has(ModItems.SHELL_STEEL.get()))
                .save(writer, recipeId("crafting/pile_loader"));

        // Geblaese: " M " / "ACA" / " S "
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PILE_VENT.get())
                .pattern(" M ")
                .pattern("ACA")
                .pattern(" S ")
                .define('M', ModItems.MOTOR.get())
                .define('A', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .define('C', ModItems.SHELL_COPPER.get())
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST))
                .unlockedBy(getHasName(ModItems.MOTOR.get()), has(ModItems.MOTOR.get()))
                .save(writer, recipeId("crafting/pile_vent"));

        // Steuerstabantrieb: " B " / "SBS" / "SBS"
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PILE_CONTROL.get())
                .pattern(" B ")
                .pattern("SBS")
                .pattern("SBS")
                .define('B', ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT))
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT)),
                        has(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/pile_control"));
    }

    /** 1:1 CraftingManager 632/633: pink_slab x6 "WWW", pink_stairs x6 "W  ","WW ","WWW" aus pink_planks. */
    private void registerPinkWoodRecipes(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PINK_SLAB.get(), 6)
                .pattern("WWW")
                .define('W', ModBlocks.PINK_PLANKS.get())
                .unlockedBy(getHasName(ModBlocks.PINK_PLANKS.get()), has(ModBlocks.PINK_PLANKS.get()))
                .save(writer, recipeId("crafting/pink_slab"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.PINK_STAIRS.get(), 6)
                .pattern("W  ")
                .pattern("WW ")
                .pattern("WWW")
                .define('W', ModBlocks.PINK_PLANKS.get())
                .unlockedBy(getHasName(ModBlocks.PINK_PLANKS.get()), has(ModBlocks.PINK_PLANKS.get()))
                .save(writer, recipeId("crafting/pink_stairs"));
        // CraftingManager 676: obj_tester "P","I","S" = Polaroid, Flammenpony, Stahlplatte
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.OBJ_TESTER.get())
                .pattern("P")
                .pattern("I")
                .pattern("S")
                .define('P', ModItems.POLAROID.get())
                .define('I', ModItems.FLAME_PONY.get())
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModItems.POLAROID.get()), has(ModItems.POLAROID.get()))
                .save(writer, recipeId("crafting/obj_tester"));
    }

    /** 1:1-Port der beiden Rohrrezepte aus {@code CraftingManager} (1.7.10). */
    /** 1:1-Port der vier Massenspeicher-Rezepte aus {@code CraftingManager} (1.7.10). */
    private void registerMassStorageRecipes(Consumer<FinishedRecipe> writer) {

        // Holz: "PPP" / "PIP" / "PPP" - Bretter um eine Eisenplatte. Fasst hundert Stueck.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MASS_STORAGE_WOOD.get())
                .pattern("PPP")
                .pattern("PIP")
                .pattern("PPP")
                .define('P', net.minecraft.tags.ItemTags.PLANKS)
                .define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)),
                        has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/mass_storage_wood"));

        // Eisen: " L " / "ICI" / " I " - Titan um eine Stahlkiste, oben eine Vakuumroehre.
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.MASS_STORAGE_IRON.get())
                .pattern(" L ")
                .pattern("ICI")
                .pattern(" I ")
                .define('I', ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT))
                .define('C', ModItems.CRATE_STEEL.get())
                .define('L', ModItems.VACUUM_TUBE.get())
                .unlockedBy(getHasName(ModItems.CRATE_STEEL.get()), has(ModItems.CRATE_STEEL.get()))
                .save(writer, recipeId("crafting/mass_storage_iron"));

        // Die Aufwertungen Eisen -> Desh -> Stahl: Original ContainerUpgradeCraftingHandler, siehe
        // RestRecipeGenerator.crafting() (hbm_m:container_upgrade, Inhalt geht mit).
    }

    private void registerPneumaticRecipes(Consumer<FinishedRecipe> writer) {

        // "CRC" mit gegossener Kupferplatte - acht Rohre
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PNEUMATIC_TUBE.get(), 8)
                .pattern("CRC")
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST))
                .define('R', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)),
                        has(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/pneumatic_tube"));

        // dasselbe mit geschweisster Platte - vierundzwanzig Rohre
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PNEUMATIC_TUBE.get(), 24)
                .pattern("CRC")
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED))
                .define('R', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)),
                        has(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_WELDED)))
                .save(writer, recipeId("crafting/pneumatic_tube_welded"));

        // 1:1 aus dem Original: vier bemalbare Rohre aus vier gewoehnlichen und vier Stahlplatten.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PNEUMATIC_TUBE_PAINTABLE.get(), 4)
                .pattern("SAS")
                .pattern("A A")
                .pattern("SAS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE_CAST))
                .define('A', ModBlocks.PNEUMATIC_TUBE.get())
                .unlockedBy(getHasName(ModBlocks.PNEUMATIC_TUBE.get().asItem()),
                        has(ModBlocks.PNEUMATIC_TUBE.get()))
                .save(writer, recipeId("crafting/pneumatic_tube_paintable"));
    }

    private void registerBookOfWagons(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BOOK_OF_.get())
                .pattern("BGB")
                .pattern("GAG")
                .pattern("BGB")
                .define('B', ModItems.EGG_BALEFIRE_SHARD.get())
                .define('G', Items.GOLD_INGOT)
                .define('A', Items.BOOK)
                .unlockedBy(getHasName(ModItems.EGG_BALEFIRE_SHARD.get()), has(ModItems.EGG_BALEFIRE_SHARD.get()))
                .save(writer, recipeId("crafting/book_of_wagons"));
    }

    private void registerRbmkFuelRecipes(Consumer<FinishedRecipe> writer) {
        // RodRecipes.java:89
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_FUEL_EMPTY.get())
                .pattern("ZRZ")
                .pattern("Z Z")
                .pattern("ZRZ")
                .define('Z', ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT))
                .define('R', ModItems.ROD_QUAD_EMPTY.get())
                .unlockedBy(getHasName(ModItems.ROD_QUAD_EMPTY.get()), has(ModItems.ROD_QUAD_EMPTY.get()))
                .save(writer, recipeId("crafting/rbmk_fuel_empty"));

        // RodRecipes.java:90-120 - addRBMKRod(billet, rod)
        rbmkRod(writer, ModItems.RBMK_FUEL_UEU,            ModMaterialItems.get(ModMaterials.URANIUM, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MEU,            ModMaterialItems.get(ModMaterials.URANIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEU233,         ModMaterialItems.get(ModMaterials.URANIUM233, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEU235,         ModMaterialItems.get(ModMaterials.URANIUM235, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_UZH,            ModMaterialItems.get(ModMaterials.UZH, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_THMEU,          ModMaterialItems.get(ModMaterials.THORIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MOX,            ModMaterialItems.get(ModMaterials.MOX_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_LEP,            ModMaterialItems.get(ModMaterials.PLUTONIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MEP,            ModMaterialItems.get(ModMaterials.PU_MIX, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEP,            ModMaterialItems.get(ModMaterials.PLUTONIUM239, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEP241,         ModMaterialItems.get(ModMaterials.PLUTONIUM241, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_LEA,            ModMaterialItems.get(ModMaterials.AMERICIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MEA,            ModMaterialItems.get(ModMaterials.AM_MIX, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEA241,         ModMaterialItems.get(ModMaterials.AM241, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEA242,         ModMaterialItems.get(ModMaterials.AM242, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MEN,            ModMaterialItems.get(ModMaterials.NEPTUNIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEN,            ModMaterialItems.get(ModMaterials.NEPTUNIUM, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_PO210BE,        ModMaterialItems.get(ModMaterials.PO210BE, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_RA226BE,        ModMaterialItems.get(ModMaterials.RA226BE, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_PU238BE,        ModMaterialItems.get(ModMaterials.PU238BE, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_LEAUS,          ModMaterialItems.get(ModMaterials.AUSTRALIUM_LESSER, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HEAUS,          ModMaterialItems.get(ModMaterials.AUSTRALIUM_GREATER, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_BALEFIRE,       ModItems.EGG_BALEFIRE_SHARD);
        rbmkRod(writer, ModItems.RBMK_FUEL_LES,            ModMaterialItems.get(ModMaterials.LES_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_MES,            ModMaterialItems.get(ModMaterials.SCHRABIDIUM_FUEL, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_HES,            ModMaterialItems.get(ModMaterials.HES, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_BALEFIRE_GOLD,  ModMaterialItems.get(ModMaterials.BALEFIRE_GOLD, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_FLASHLEAD,      ModMaterialItems.get(ModMaterials.FLASHLEAD, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_BISMUTH,    ModMaterialItems.get(ModMaterials.ZFB_BISMUTH, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_PU241,      ModMaterialItems.get(ModMaterials.ZFB_PU241, MaterialShape.BILLET));
        rbmkRod(writer, ModItems.RBMK_FUEL_ZFB_AM_MIX,     ModMaterialItems.get(ModMaterials.ZFB_AM_MIX, MaterialShape.BILLET));

        // RodRecipes.java:121
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.RBMK_FUEL_DRX.get())
                .requires(ModItems.RBMK_FUEL_BALEFIRE.get())
                .requires(ModItems.PARTICLE_DIGAMMA.get())
                .unlockedBy(getHasName(ModItems.PARTICLE_DIGAMMA.get()), has(ModItems.PARTICLE_DIGAMMA.get()))
                .save(writer, recipeId("crafting/rbmk_fuel_drx"));

        // RBMKFuelCraftingHandler - a lone rod disassembles back into 8 pellets.
        net.minecraft.data.recipes.SpecialRecipeBuilder
                .special((net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<?>)
                        com.hbm_m.recipe.RBMKFuelDisassemblyRecipe.SERIALIZER)
                .save(writer, recipeId("crafting/rbmk_fuel_disassembly").toString());

        // GrenadeCraftingHandler - Baukastengranate aus Huelle, Fuellung, Zuender (+ Extra)
        net.minecraft.data.recipes.SpecialRecipeBuilder
                .special((net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<?>)
                        com.hbm_m.recipe.GrenadeCraftingRecipe.SERIALIZER)
                .save(writer, recipeId("crafting/grenades").toString());

        // CargoShellCraftingHandler - leere Frachtgranate + ein Gegenstand = beladene Frachtgranate
        net.minecraft.data.recipes.SpecialRecipeBuilder
                .special((net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<?>)
                        com.hbm_m.recipe.CargoShellCraftingRecipe.SERIALIZER)
                .save(writer, recipeId("crafting/cargo_shell").toString());

        // MKUCraftingHandler - MKUNICORN, Anordnung haengt vom Weltseed ab
        net.minecraft.data.recipes.SpecialRecipeBuilder
                .special((net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<?>)
                        com.hbm_m.recipe.MKUCraftingRecipe.SERIALIZER)
                .save(writer, recipeId("crafting/mku").toString());
    }

    /** RodRecipes.java:246 - empty casing + 8 billets, shapeless. */
    private void rbmkRod(Consumer<FinishedRecipe> writer, RegistrySupplier<Item> rod, RegistrySupplier<Item> billet) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, rod.get())
                .requires(ModItems.RBMK_FUEL_EMPTY.get())
                .requires(billet.get(), 8)
                .unlockedBy(getHasName(billet.get()), has(billet.get()))
                .save(writer, recipeId("crafting/" + rod.getId().getPath()));
    }

    /**
     * Every RBMK block/panel/lid/tool recipe, 1:1 with the original's
     * {@code main/CraftingManager.java:751-793 and 987-993} plus {@code crafting/ToolRecipes.java:133}.
     */
    private void registerRbmkBlockRecipes(Consumer<FinishedRecipe> writer) {
        Ingredient steelPlate  = Ingredient.of(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE));
        Ingredient graphiteIng = Ingredient.of(ModMaterialItems.item(ModMaterials.GRAPHITE, MaterialShape.INGOT));
        Ingredient boronIngot  = Ingredient.of(ModMaterialItems.item(ModMaterials.BORON, MaterialShape.INGOT));

        // :751 - rbmk_lid x4
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_LID.get(), 4)
                .pattern("PPP").pattern("CCC").pattern("PPP")
                .define('P', steelPlate)
                .define('C', ModBlocks.CONCRETE_ASBESTOS.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/rbmk_lid"));

        // :752 and :753 - rbmk_lid_glass x4, two layer orders
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_LID_GLASS.get(), 4)
                .pattern("LLL").pattern("BBB").pattern("P P")
                .define('P', steelPlate)
                .define('L', ModBlocks.GLASS_LEAD.get())
                .define('B', ModBlocks.GLASS_BORON.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/rbmk_lid_glass"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_LID_GLASS.get(), 4)
                .pattern("BBB").pattern("LLL").pattern("P P")
                .define('P', steelPlate)
                .define('L', ModBlocks.GLASS_LEAD.get())
                .define('B', ModBlocks.GLASS_BORON.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/rbmk_lid_glass_alt"));

        // :755
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_MODERATOR.get())
                .pattern(" G ").pattern("GRG").pattern(" G ")
                .define('G', ModBlocks.BLOCK_GRAPHITE.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_moderator"));
        // :756
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_ABSORBER.get())
                .pattern("GGG").pattern("GRG").pattern("GGG")
                .define('G', boronIngot)
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_absorber"));
        // :757
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_REFLECTOR.get())
                .pattern("GGG").pattern("GRG").pattern("GGG")
                .define('G', ModItems.NEUTRON_REFLECTOR.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_reflector"));

        // :759
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CONTROL.get())
                .pattern(" B ").pattern("GRG").pattern(" B ")
                .define('G', graphiteIng)
                .define('B', ModItems.MOTOR.get())
                .define('R', ModBlocks.RBMK_ABSORBER.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_ABSORBER.get()), has(ModBlocks.RBMK_ABSORBER.get()))
                .save(writer, recipeId("crafting/rbmk_control"));
        // :760
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CONTROL_MOD.get())
                .pattern("BGB").pattern("GRG").pattern("BGB")
                .define('G', ModBlocks.BLOCK_GRAPHITE.get())
                .define('R', ModBlocks.RBMK_CONTROL.get())
                .define('B', ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET))
                .unlockedBy(getHasName(ModBlocks.RBMK_CONTROL.get()), has(ModBlocks.RBMK_CONTROL.get()))
                .save(writer, recipeId("crafting/rbmk_control_mod"));
        // :761
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CONTROL_AUTO.get())
                .pattern("C").pattern("R").pattern("D")
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .define('R', ModBlocks.RBMK_CONTROL.get())
                .define('D', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_CONTROL.get()), has(ModBlocks.RBMK_CONTROL.get()))
                .save(writer, recipeId("crafting/rbmk_control_auto"));
        // :763
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CONTROL_REASIM.get())
                .pattern(" B ").pattern("GRG").pattern(" B ")
                .define('G', graphiteIng)
                .define('B', ModItems.MOTOR.get())
                .define('R', ModBlocks.RBMK_ABSORBER.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_ABSORBER.get()), has(ModBlocks.RBMK_ABSORBER.get()))
                .save(writer, recipeId("crafting/rbmk_control_reasim"));
        // :764
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CONTROL_REASIM_AUTO.get())
                .pattern("C").pattern("R").pattern("D")
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .define('R', ModBlocks.RBMK_CONTROL.get())
                .define('D', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_CONTROL.get()), has(ModBlocks.RBMK_CONTROL.get()))
                .save(writer, recipeId("crafting/rbmk_control_reasim_auto"));

        // :766
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_ROD_REASIM.get())
                .pattern("ZCZ").pattern("ZRZ").pattern("ZCZ")
                .define('C', ModItems.SHELL_STEEL.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .define('Z', ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_element_reasim"));
        // :767
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_ROD_REASIM_MOD.get())
                .pattern("BGB").pattern("GRG").pattern("BGB")
                .define('G', ModBlocks.BLOCK_GRAPHITE.get())
                .define('R', ModBlocks.RBMK_ROD_REASIM.get())
                .define('B', ModMaterialItems.item(ModMaterials.TCALLOY, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModBlocks.RBMK_ROD_REASIM.get()), has(ModBlocks.RBMK_ROD_REASIM.get()))
                .save(writer, recipeId("crafting/rbmk_element_reasim_mod"));
        // :768
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_OUTGASSER.get())
                .pattern("GHG").pattern("GRG").pattern("GTG")
                .define('G', ModBlocks.STEEL_GRATE.get())
                .define('H', Items.HOPPER)
                .define('T', ModItems.TANK_STEEL.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_outgasser"));
        // :769
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_STORAGE.get())
                .pattern("C").pattern("R").pattern("C")
                .define('C', ModItems.CRATE_STEEL.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_storage"));
        // :770
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_LOADER.get())
                .pattern("SCS").pattern("CBC").pattern("SCS")
                .define('S', steelPlate)
                .define('C', Items.COPPER_INGOT)
                .define('B', ModItems.TANK_STEEL.get())
                .unlockedBy(getHasName(ModItems.TANK_STEEL.get()), has(ModItems.TANK_STEEL.get()))
                .save(writer, recipeId("crafting/rbmk_loader"));
        // :771
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_STEAM_INLET.get())
                .pattern("SCS").pattern("CBC").pattern("SCS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('C', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('B', ModItems.TANK_STEEL.get())
                .unlockedBy(getHasName(ModItems.TANK_STEEL.get()), has(ModItems.TANK_STEEL.get()))
                .save(writer, recipeId("crafting/rbmk_steam_inlet"));
        // :772
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_STEAM_OUTLET.get())
                .pattern("SCS").pattern("CBC").pattern("SCS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))
                .define('B', ModItems.TANK_STEEL.get())
                .unlockedBy(getHasName(ModItems.TANK_STEEL.get()), has(ModItems.TANK_STEEL.get()))
                .save(writer, recipeId("crafting/rbmk_steam_outlet"));

        // :774 - rbmk_display_blank x8
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_DISPLAY_BLANK.get(), 8)
                .pattern("B").pattern("D")
                .define('B', boronIngot)
                .define('D', ModBlocks.CONCRETE_ASBESTOS.get())
                .unlockedBy(getHasName(ModBlocks.CONCRETE_ASBESTOS.get()), has(ModBlocks.CONCRETE_ASBESTOS.get()))
                .save(writer, recipeId("crafting/rbmk_display_blank"));
        // :775
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_DISPLAY.get())
                .pattern("C").pattern("B")
                .define('C', ModItems.CRT_DISPLAY.get())
                .define('B', ModBlocks.RBMK_DISPLAY_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_DISPLAY_BLANK.get()), has(ModBlocks.RBMK_DISPLAY_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_display"));
        // :776
        rbmkPanel(writer, ModBlocks.RBMK_KEYPAD, ModBlocks.RADIO_TORCH_SENDER, Ingredient.of(ModItems.VACUUM_TUBE.get()));
        // :777
        rbmkPanel(writer, ModBlocks.RBMK_GAUGE, ModBlocks.RADIO_TORCH_RECEIVER, Ingredient.of(ModItems.VACUUM_TUBE.get()));
        // :778 - the numitron uses its own 3-circuit pattern
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_NUMITRON.get())
                .pattern(" R ").pattern("CCC").pattern(" B ")
                .define('R', ModBlocks.RADIO_TORCH_RECEIVER.get())
                .define('B', ModBlocks.RBMK_DISPLAY_BLANK.get())
                .define('C', ModItems.CIRCUIT_NUMITRON.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_DISPLAY_BLANK.get()), has(ModBlocks.RBMK_DISPLAY_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_numitron"));
        // :779
        rbmkPanel(writer, ModBlocks.RBMK_GRAPH, ModBlocks.RADIO_TORCH_RECEIVER, Ingredient.of(ModItems.CRT_DISPLAY.get()));
        // :780
        rbmkPanel(writer, ModBlocks.RBMK_LEVER, ModBlocks.RADIO_TORCH_SENDER, Ingredient.of(Items.COPPER_INGOT));
        // :781
        rbmkPanel(writer, ModBlocks.RBMK_INDICATOR, ModBlocks.RADIO_TORCH_RECEIVER, Ingredient.of(ModItems.COIL_TUNGSTEN.get()));
        // :782
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_TERMINAL.get())
                .pattern("R ").pattern("CD").pattern("B ")
                .define('R', ModBlocks.RADIO_TORCH_SENDER.get())
                .define('B', ModBlocks.RBMK_DISPLAY_BLANK.get())
                .define('C', ModItems.ANALOG_CIRCUIT.get())
                .define('D', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_DISPLAY_BLANK.get()), has(ModBlocks.RBMK_DISPLAY_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_terminal"));

        // :786-789 - deco blocks and the blank column
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK.get(), 8)
                .requires(ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/deco_rbmk"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK_SMOOTH.get())
                .requires(ModBlocks.DECO_RBMK.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK.get()), has(ModBlocks.DECO_RBMK.get()))
                .save(writer, recipeId("crafting/deco_rbmk_smooth"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK_PANEL.get())
                .requires(ModBlocks.DECO_RBMK.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK.get()), has(ModBlocks.DECO_RBMK.get()))
                .save(writer, recipeId("crafting/deco_rbmk_panel"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK_SMOOTH_PANEL.get())
                .requires(ModBlocks.DECO_RBMK_SMOOTH.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK_SMOOTH.get()), has(ModBlocks.DECO_RBMK_SMOOTH.get()))
                .save(writer, recipeId("crafting/deco_rbmk_smooth_panel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK_PANEL_SLAB2.get(), 6)
                .pattern("PPP")
                .define('P', ModBlocks.DECO_RBMK_PANEL.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK_PANEL.get()), has(ModBlocks.DECO_RBMK_PANEL.get()))
                .save(writer, recipeId("crafting/deco_rbmk_panel_slab2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.DECO_RBMK_SMOOTH_PANEL_SLAB2.get(), 6)
                .pattern("PPP")
                .define('P', ModBlocks.DECO_RBMK_SMOOTH_PANEL.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK_SMOOTH_PANEL.get()), has(ModBlocks.DECO_RBMK_SMOOTH_PANEL.get()))
                .save(writer, recipeId("crafting/deco_rbmk_smooth_panel_slab2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_BLANK.get())
                .pattern("RRR").pattern("R R").pattern("RRR")
                .define('R', ModBlocks.DECO_RBMK.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK.get()), has(ModBlocks.DECO_RBMK.get()))
                .save(writer, recipeId("crafting/rbmk_blank"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_BLANK.get())
                .pattern("RRR").pattern("R R").pattern("RRR")
                .define('R', ModBlocks.DECO_RBMK_SMOOTH.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK_SMOOTH.get()), has(ModBlocks.DECO_RBMK_SMOOTH.get()))
                .save(writer, recipeId("crafting/rbmk_blank_from_smooth"));

        // :987
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RBMK_CONSOLE.get())
                .pattern("BBB").pattern("DGD").pattern("DCD")
                .define('B', boronIngot)
                .define('D', ModBlocks.DECO_RBMK.get())
                .define('G', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('C', ModItems.ANALOG_CIRCUIT.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK.get()), has(ModBlocks.DECO_RBMK.get()))
                .save(writer, recipeId("crafting/rbmk_console"));
        // :988
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_CRANE_CONSOLE.get())
                .pattern("BCD").pattern("DDD")
                .define('B', boronIngot)
                .define('D', ModBlocks.DECO_RBMK.get())
                .define('C', ModItems.ANALOG_CIRCUIT.get())
                .unlockedBy(getHasName(ModBlocks.DECO_RBMK.get()), has(ModBlocks.DECO_RBMK.get()))
                .save(writer, recipeId("crafting/rbmk_crane_console"));
        // :989
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_ROD.get())
                .pattern("C").pattern("R").pattern("C")
                .define('C', ModItems.SHELL_STEEL.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_element"));
        // :990
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_ROD_MOD.get())
                .pattern("BGB").pattern("GRG").pattern("BGB")
                .define('G', ModBlocks.BLOCK_GRAPHITE.get())
                .define('R', ModBlocks.RBMK_ROD.get())
                .define('B', ModMaterialItems.item(ModMaterials.BISMUTH, MaterialShape.NUGGET))
                .unlockedBy(getHasName(ModBlocks.RBMK_ROD.get()), has(ModBlocks.RBMK_ROD.get()))
                .save(writer, recipeId("crafting/rbmk_element_mod"));
        // :991
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_BOILER.get())
                .pattern("CPC").pattern("CRC").pattern("CPC")
                .define('C', ModItems.PIPE_COPPER.get())
                .define('P', ModItems.SHELL_COPPER.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_boiler"));
        // :992
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_HEATER.get())
                .pattern("CIC").pattern("PRP").pattern("CIC")
                .define('C', ModItems.PIPE_COPPER.get())
                .define('P', ModItems.SHELL_STEEL.get())
                .define('R', ModBlocks.RBMK_BLANK.get())
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_heater"));
        // :993
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RBMK_COOLER.get())
                .pattern("IGI").pattern("GCG").pattern("IGI")
                .define('C', ModBlocks.RBMK_BLANK.get())
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('G', ModBlocks.STEEL_GRATE.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_BLANK.get()), has(ModBlocks.RBMK_BLANK.get()))
                .save(writer, recipeId("crafting/rbmk_cooler"));

        // ToolRecipes.java:133
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RBMK_TOOL.get())
                .pattern(" A ").pattern(" IA").pattern("I  ")
                .define('A', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT))
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/rbmk_tool"));
    }

    /** The shared radio-torch + circuit + blank-panel column used by most RBMK panel devices. */
    private void rbmkPanel(Consumer<FinishedRecipe> writer,
            RegistrySupplier<net.minecraft.world.level.block.Block> panel,
            RegistrySupplier<net.minecraft.world.level.block.Block> torch,
            Ingredient circuit) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, panel.get())
                .pattern("R").pattern("C").pattern("B")
                .define('R', torch.get())
                .define('C', circuit)
                .define('B', ModBlocks.RBMK_DISPLAY_BLANK.get())
                .unlockedBy(getHasName(ModBlocks.RBMK_DISPLAY_BLANK.get()), has(ModBlocks.RBMK_DISPLAY_BLANK.get()))
                .save(writer, recipeId("crafting/" + panel.getId().getPath()));
    }

    //Sentry-Turret + MVP-Munition (Original-Rezept aus WeaponRecipes.java, GUNMETAL.mechanism() -> generisches PART_MECHANISM)
    private void registerTurretRecipes(Consumer<FinishedRecipe> writer) {

        // ── Противогазы (1.7.10 ArmorRecipes) ──────────────────────────────────
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GAS_MASK.get())
                .pattern("PPP")
                .pattern("GPG")
                .pattern(" F ")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('G', Items.GLASS_PANE)
                .define('F', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/gas_mask"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GAS_MASK_M65.get())
                .pattern("PPP")
                .pattern("GPG")
                .pattern(" F ")
                .define('P', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .define('G', Items.GLASS_PANE)
                .define('F', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/gas_mask_m65"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GAS_MASK_OLDE.get())
                .pattern("PPP")
                .pattern("GPG")
                .pattern(" F ")
                .define('P', Items.LEATHER)
                .define('G', Items.GLASS_PANE)
                .define('F', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.LEATHER), has(Items.LEATHER))
                .save(writer, recipeId("crafting/gas_mask_olde"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GAS_MASK_MONO.get())
                .pattern(" P ")
                .pattern("PPP")
                .pattern(" F ")
                .define('P', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .define('F', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/gas_mask_mono"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MASK_RAG.get())
                .pattern("RRR")
                .define('R', ModItems.RAG_DAMP.get())
                .unlockedBy(getHasName(ModItems.RAG_DAMP.get()), has(ModItems.RAG_DAMP.get()))
                .save(writer, recipeId("crafting/mask_rag"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MASK_PISS.get())
                .pattern("RRR")
                .define('R', ModItems.RAG_PISS.get())
                .unlockedBy(getHasName(ModItems.RAG_PISS.get()), has(ModItems.RAG_PISS.get()))
                .save(writer, recipeId("crafting/mask_piss"));

        // ── Фильтры (1.7.10 ConsumableRecipes) ────────────────────────────────
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_MASK_FILTER.get())
                .pattern("I")
                .pattern("F")
                .define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('F', ModItems.FILTER_COAL.get())
                .unlockedBy(getHasName(ModItems.FILTER_COAL.get()), has(ModItems.FILTER_COAL.get()))
                .save(writer, recipeId("crafting/gas_mask_filter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_MASK_FILTER_MONO.get())
                .pattern("ZZZ")
                .pattern("ZCZ")
                .pattern("ZZZ")
                .define('Z', ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.NUGGET))
                .define('C', ModItems.CATALYST_CLAY.get())
                .unlockedBy(getHasName(ModItems.CATALYST_CLAY.get()), has(ModItems.CATALYST_CLAY.get()))
                .save(writer, recipeId("crafting/gas_mask_filter_mono"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_MASK_FILTER_COMBO.get())
                .pattern("ZCZ")
                .pattern("CFC")
                .pattern("ZCZ")
                .define('Z', ModMaterialItems.item(ModMaterials.ZIRCONIUM, MaterialShape.INGOT))
                .define('C', ModItems.CATALYST_CLAY.get())
                .define('F', ModItems.GAS_MASK_FILTER.get())
                .unlockedBy(getHasName(ModItems.GAS_MASK_FILTER.get()), has(ModItems.GAS_MASK_FILTER.get()))
                .save(writer, recipeId("crafting/gas_mask_filter_combo"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_MASK_FILTER_RAG.get())
                .pattern("I")
                .pattern("F")
                .define('I', Items.IRON_INGOT)
                .define('F', ModItems.RAG_DAMP.get())
                .unlockedBy(getHasName(ModItems.RAG_DAMP.get()), has(ModItems.RAG_DAMP.get()))
                .save(writer, recipeId("crafting/gas_mask_filter_rag"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_MASK_FILTER_PISS.get())
                .pattern("I")
                .pattern("F")
                .define('I', Items.IRON_INGOT)
                .define('F', ModItems.RAG_PISS.get())
                .unlockedBy(getHasName(ModItems.RAG_PISS.get()), has(ModItems.RAG_PISS.get()))
                .save(writer, recipeId("crafting/gas_mask_filter_piss"));

        // ── Крепления противогазов к шлемам (1.7.10 ConsumableRecipes) ────────
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ATTACHMENT_MASK.get())
                .pattern("DID")
                .pattern("IGI")
                .pattern(" F ")
                .define('D', ModItems.DUCTTAPE.get())
                .define('I', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .define('G', Items.GLASS_PANE)
                .define('F', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModItems.DUCTTAPE.get()), has(ModItems.DUCTTAPE.get()))
                .save(writer, recipeId("crafting/attachment_mask"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.ATTACHMENT_MASK_MONO.get())
                .pattern(" D ")
                .pattern("DID")
                .pattern(" F ")
                .define('D', ModItems.DUCTTAPE.get())
                .define('I', ModMaterialItems.item(ModMaterials.RUBBER, MaterialShape.INGOT))
                .define('F', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModItems.DUCTTAPE.get()), has(ModItems.DUCTTAPE.get()))
                .save(writer, recipeId("crafting/attachment_mask_mono"));

        // Medizin: siehe ConsumableRecipeGenerator (1:1). pill_herbal braucht die Fingerhut-Blume
        // (plant_flower FOXGLOVE) und folgt mit den Pflanzen.


        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModBlocks.TURRET_SENTRY.get())
                .pattern("PPL")
                .pattern(" MD")
                .pattern(" SC")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('M', ModItems.MOTOR.get())
                .define('L', ModItems.PART_MECHANISM.get())
                .define('S', ModBlocks.STEEL_SCAFFOLD.get())
                .define('C', ModItems.SILICON_CIRCUIT.get())
                .define('D', ModItems.CRT_DISPLAY.get())
                .unlockedBy(getHasName(ModItems.CRT_DISPLAY.get()), has(ModItems.CRT_DISPLAY.get()))
                .save(writer, recipeId("crafting/turret_sentry"));

        // Gelenkte Raketen fuer den Himars-Turret (Original hatte hierfuer keine dokumentierten
        // Table-Rezepte - plausible Annaeherung, siehe TurretRocketEntity).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ROCKET_HIMARS_STANDARD.get())
                .pattern(" P ")
                .pattern(" M ")
                .pattern(" G ")
                .define('P', ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE))
                .define('M', ModItems.MOTOR.get())
                .define('G', Items.GUNPOWDER)
                .unlockedBy(getHasName(Items.GUNPOWDER), has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/rocket_himars_standard"));

        // Missile-Assembly-Station + fehlende Teile (Original-Rezept nicht auffindbar, plausible Annaeherung)
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModBlocks.MACHINE_MISSILE_ASSEMBLY.get())
                .pattern("PPP")
                .pattern("MCM")
                .pattern("SSS")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('M', ModItems.MOTOR.get())
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .define('S', ModBlocks.STEEL_SCAFFOLD.get())
                .unlockedBy(getHasName(ModItems.ADVANCED_CIRCUIT.get()), has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/machine_missile_assembly"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.MISSILE_FUSELAGE.get(), 2)
                .pattern("P")
                .pattern("P")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/missile_fuselage"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, ModItems.MISSILE_CHIP.get())
                .requires(ModItems.SILICON_CIRCUIT.get())
                .requires(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModItems.SILICON_CIRCUIT.get()), has(ModItems.SILICON_CIRCUIT.get()))
                .save(writer, recipeId("crafting/missile_chip"));
    }

    //основные рецепты
    private void registerUtilityRecipes(Consumer<FinishedRecipe> writer) {
        //двери
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOOR_BUNKER.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/door_bunker"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.METAL_DOOR.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('$', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/metal_door"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOOR_OFFICE.get())
                .pattern("$$$")
                .pattern("###")
                .pattern("$$$")
                .define('#', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('$', Items.OAK_WOOD)
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/door_office"));

        //МОТОРЫ
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR.get(), 2)
                .pattern(" $ ")
                .pattern("%#%")
                .pattern("%@%")
                .define('%', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('$', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .define('#', ModItems.COIL_COPPER.get())
                .define('@', ModItems.COIL_COPPER_TORUS.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER_TORUS.get()), has(ModItems.COIL_COPPER_TORUS.get()))
                .save(writer, recipeId("crafting/motor1"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR.get(), 2)
                .pattern(" $ ")
                .pattern("%#%")
                .pattern(" @ ")
                .define('%', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('$', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .define('#', ModItems.COIL_COPPER.get())
                .define('@', ModItems.COIL_COPPER_TORUS.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER_TORUS.get()), has(ModItems.COIL_COPPER_TORUS.get()))
                .save(writer, recipeId("crafting/motor2"));

        // ══════════ Сеть длинной ЛЭП (порт рецептов CraftingManager 1.7.10) ══════════

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.WIRING_RED_COPPER.get())
                .pattern("PPP").pattern("PIP").pattern("PPP")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('I', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/wiring_red_copper"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_WIRE_COATED.get(), 16)
                .pattern("WRW").pattern("RIR").pattern("WRW")
                .define('W', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('I', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT))
                .define('R', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)), has(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)))
                .save(writer, recipeId("crafting/red_wire_coated"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_CONNECTOR.get(), 4)
                .pattern("C").pattern("I").pattern("S")
                .define('C', ModItems.COIL_COPPER.get())
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_connector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_CONNECTOR_SUPER.get(), 2)
                .pattern("CCC").pattern("III").pattern(" S ")
                .define('C', ModItems.COIL_COPPER.get())
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_connector_super"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_PYLON.get(), 4)
                .pattern("CWC").pattern("PWP").pattern(" S ")
                .define('C', ModItems.COIL_COPPER.get())
                .define('W', ItemTags.PLANKS)
                .define('P', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', Blocks.COBBLESTONE)
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_pylon"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_PYLON_STEEL.get(), 4)
                .pattern("CWC").pattern("PWP").pattern(" S ")
                .define('C', ModItems.COIL_COPPER.get())
                .define('W', ModItems.PIPE_STEEL.get())
                .define('P', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', Blocks.COBBLESTONE)
                .unlockedBy(getHasName(ModItems.PIPE_STEEL.get()), has(ModItems.PIPE_STEEL.get()))
                .save(writer, recipeId("crafting/red_pylon_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_PYLON_MEDIUM_WOOD.get(), 2)
                .pattern("CCW").pattern("IIW").pattern("  S")
                .define('C', ModItems.COIL_COPPER.get())
                .define('W', ItemTags.PLANKS)
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', Blocks.COBBLESTONE)
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_pylon_medium_wood"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.RED_PYLON_MEDIUM_STEEL.get(), 2)
                .pattern("CCW").pattern("IIW").pattern("  S")
                .define('C', ModItems.COIL_COPPER.get())
                .define('W', ModItems.PIPE_STEEL.get())
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('S', Blocks.COBBLESTONE)
                .unlockedBy(getHasName(ModItems.PIPE_STEEL.get()), has(ModItems.PIPE_STEEL.get()))
                .save(writer, recipeId("crafting/red_pylon_medium_steel"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.RED_PYLON_MEDIUM_WOOD_TRANSFORMER.get())
                .requires(ModBlocks.RED_PYLON_MEDIUM_WOOD.get())
                .requires(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .requires(ModItems.COIL_COPPER.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_pylon_medium_wood_transformer"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.RED_PYLON_MEDIUM_STEEL_TRANSFORMER.get())
                .requires(ModBlocks.RED_PYLON_MEDIUM_STEEL.get())
                .requires(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .requires(ModItems.COIL_COPPER.get())
                .unlockedBy(getHasName(ModItems.COIL_COPPER.get()), has(ModItems.COIL_COPPER.get()))
                .save(writer, recipeId("crafting/red_pylon_medium_steel_transformer"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MOTOR_DESH.get(), 2)
                .pattern("@$@")
                .pattern("%#%")
                .pattern("@$@")
                .define('%', ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT))
                .define('$', ModItems.COIL_GOLD_TORUS.get())
                .define('#', ModItems.MOTOR.get())
                .define('@', Ingredient.of(ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.DESH, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/motor_desh"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STEAM_TURBINE.get())
                .pattern("ABA")
                .pattern("CDC")
                .pattern("ABA")
                .define('A', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('B', ModItems.COIL_COPPER.get())
                .define('C', Ingredient.of(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)))
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
                .define('#', ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT)))
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
                .define('B', ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT))
                .define('G', Items.IRON_BARS)
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('A', Ingredient.of(BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), BlockAbsorber.EnumAbsorberTier.BASE)))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/decon"));

        registerRadAbsorberRecipes(writer);

        // SmeltingRecipes: Meteorerze (je 10 XP)
        saveSmeltingStackRecipe(writer, recipeId("smelting/ore_meteor_iron"), ModBlocks.ORE_METEOR_IRON.get(), new ItemStack(Items.IRON_INGOT, 16), 10.0F);
        saveSmeltingStackRecipe(writer, recipeId("smelting/ore_meteor_copper"), ModBlocks.ORE_METEOR_COPPER.get(), new ItemStack(Items.COPPER_INGOT, 16), 10.0F);
        saveSmeltingStackRecipe(writer, recipeId("smelting/ore_meteor_aluminium"), ModBlocks.ORE_METEOR_ALUMINIUM.get(), new ItemStack(ModItems.CRYOLITE_CHUNK.get(), 16), 10.0F);
        saveSmeltingStackRecipe(writer, recipeId("smelting/ore_meteor_rareearth"), ModBlocks.ORE_METEOR_RAREEARTH.get(), new ItemStack(ModItems.RAREGROUND_ORE_CHUNK.get(), 16), 10.0F);
        saveSmeltingStackRecipe(writer, recipeId("smelting/ore_meteor_cobalt"), ModBlocks.ORE_METEOR_COBALT.get(), new ItemStack(ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT), 4), 10.0F);


        // ---- R4 (CraftingManager 1:1) ----
        Ingredient r4Plastic = Ingredient.of(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_SC_EMPTY.get())
                .pattern("PGP").pattern("L L").pattern("PGP")
                .define('P', r4Plastic)
                .define('G', r4("wire_gold"))
                .define('L', r4("plate_lead"))
                .unlockedBy("has_plate_lead", has(r4("plate_lead")))
                .save(writer, recipeId("crafting/battery_sc_empty"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_WASTE.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_nuclear_waste"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_waste"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_RA226.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_ra226"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_ra226"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_TC99.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_technetium"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_tc99"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_CO60.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_co60"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_co60"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_PU238.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_pu238"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_pu238"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_PO210.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_polonium"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_po210"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_AU198.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_au198"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_au198"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_PB209.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_pb209"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_pb209"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BATTERY_SC_AM241.get())
                .requires(ModItems.BATTERY_SC_EMPTY.get()).requires(r4("billet_am241"), 2)
                .unlockedBy("has_battery_sc_empty", has(ModItems.BATTERY_SC_EMPTY.get()))
                .save(writer, recipeId("crafting/battery_sc_am241"));
        {
            ItemStack fullPotato = new ItemStack(ModItems.BATTERY_POTATO.get());
            com.hbm_m.item.fekal_electric.ModBatteryItem.setEnergy(fullPotato, ((com.hbm_m.item.fekal_electric.ModBatteryItem) ModItems.BATTERY_POTATO.get()).getCapacity());
            ItemStack fullPotatos = new ItemStack(ModItems.BATTERY_POTATOS.get());
            com.hbm_m.item.fekal_electric.ModBatteryItem.setEnergy(fullPotatos, ((com.hbm_m.item.fekal_electric.ModBatteryItem) ModItems.BATTERY_POTATOS.get()).getCapacity());
            saveShapelessStackRecipe(writer, recipeId("crafting/battery_potatos"), fullPotatos,
                    List.of(net.minecraftforge.common.crafting.StrictNBTIngredient.of(fullPotato), Ingredient.of(ModItems.TURRET_CHIP.get()), Ingredient.of(Items.REDSTONE)),
                    ModItems.TURRET_CHIP.get(), "has_turret_chip");
        }
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.KEY_KIT.get())
                .pattern("PKP").pattern("DTD").pattern("PKP")
                .define('P', r4("plate_gold"))
                .define('K', ModItems.KEY.get())
                .define('D', r4("desh_powder"))
                .define('T', Ingredient.of(ModItems.SCREWDRIVER.get(), ModItems.SCREWDRIVER_DESH.get()))
                .unlockedBy("has_key", has(ModItems.KEY.get()))
                .save(writer, recipeId("crafting/key_kit"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RECORD_LC.get())
                .pattern(" S ").pattern("SDS").pattern(" S ")
                .define('S', r4Plastic)
                .define('D', Ingredient.of(r4("lapis_powder")))
                .unlockedBy("has_polymer", has(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/record_lc"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RECORD_SS.get())
                .pattern(" S ").pattern("SDS").pattern(" S ")
                .define('S', r4Plastic)
                .define('D', Ingredient.of(r4("red_copper_powder")))
                .unlockedBy("has_polymer", has(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/record_ss"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RECORD_VC.get())
                .pattern(" S ").pattern("SDS").pattern(" S ")
                .define('S', r4Plastic)
                .define('D', Ingredient.of(ModMaterialItems.item(ModMaterials.COMBINE_STEEL, MaterialShape.POWDER)))
                .unlockedBy("has_polymer", has(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/record_vc"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BOOK_OF_.get())
                .requires(ModItems.PAGE_OF_PAGE1.get())
                .requires(ModItems.PAGE_OF_PAGE2.get())
                .requires(ModItems.PAGE_OF_PAGE3.get())
                .requires(ModItems.PAGE_OF_PAGE4.get())
                .requires(ModItems.PAGE_OF_PAGE5.get())
                .requires(ModItems.PAGE_OF_PAGE6.get())
                .requires(ModItems.PAGE_OF_PAGE7.get())
                .requires(ModItems.PAGE_OF_PAGE8.get())
                .requires(ModItems.EGG_BALEFIRE.get())
                .unlockedBy("has_page_of_page1", has(ModItems.PAGE_OF_PAGE1.get()))
                .save(writer, recipeId("crafting/book_of_"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CIRCUIT_STAR_COMPONENT_CHIPSET.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_BIOS.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_BUS.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_CHIPSET.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_CMOS.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_IO.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_NORTH.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_SOUTH.get())
                .unlockedBy("has_piece", has(ModItems.CIRCUIT_STAR_PIECE_BRIDGE_BIOS.get()))
                .save(writer, recipeId("crafting/circuit_star_component_chipset"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CIRCUIT_STAR_COMPONENT_CPU.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_CACHE.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_CLOCK.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_EXT.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_LOGIC.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_REGISTER.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CPU_SOCKET.get())
                .unlockedBy("has_piece", has(ModItems.CIRCUIT_STAR_PIECE_CPU_CACHE.get()))
                .save(writer, recipeId("crafting/circuit_star_component_cpu"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CIRCUIT_STAR_COMPONENT_RAM.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_MEM_SOCKET.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_MEM_16K_A.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_MEM_16K_B.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_MEM_16K_C.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_MEM_16K_D.get())
                .unlockedBy("has_piece", has(ModItems.CIRCUIT_STAR_PIECE_MEM_SOCKET.get()))
                .save(writer, recipeId("crafting/circuit_star_component_ram"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CIRCUIT_STAR_COMPONENT_CARD.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CARD_BOARD.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_CARD_PROCESSOR.get())
                .unlockedBy("has_piece", has(ModItems.CIRCUIT_STAR_PIECE_CARD_BOARD.get()))
                .save(writer, recipeId("crafting/circuit_star_component_card"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CIRCUIT_STAR.get())
                .requires(ModItems.CIRCUIT_STAR_COMPONENT_CHIPSET.get())
                .requires(ModItems.CIRCUIT_STAR_COMPONENT_CPU.get())
                .requires(ModItems.CIRCUIT_STAR_COMPONENT_RAM.get())
                .requires(ModItems.CIRCUIT_STAR_COMPONENT_CARD.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BOARD_TRANSISTOR.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BOARD_CONVERTER.get())
                .requires(ModItems.CIRCUIT_STAR_PIECE_BOARD_BLANK.get())
                .unlockedBy("has_component", has(ModItems.CIRCUIT_STAR_COMPONENT_CHIPSET.get()))
                .save(writer, recipeId("crafting/circuit_star"));

        // ToolRecipes: Coltan-Kompass und Werkzeugkiste; CraftingManager: Holobaender
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.COLTAN_TOOL.get())
                .pattern("ACA").pattern("CXC").pattern("ACA")
                .define('A', Items.COPPER_INGOT)
                .define('C', ModItems.CINNEBAR.get())
                .define('X', Items.COMPASS)
                .unlockedBy("has_cinnabar", has(ModItems.CINNEBAR.get()))
                .save(writer, recipeId("crafting/coltan_tool"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.TOOLBOX.get())
                .pattern("CCC").pattern("CIC")
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_iron_ingot", has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/toolbox"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HOLOTAPE_IMAGE_RESTORED.get())
                .requires(ModItems.HOLOTAPE_IMAGE_DIGAMMA.get())
                .requires(Ingredient.of(ModItems.SCREWDRIVER.get(), ModItems.SCREWDRIVER_DESH.get()))
                .requires(ModItems.DUCTTAPE.get())
                .requires(ModItems.ARMOR_POLISH.get())
                .unlockedBy("has_holotape", has(ModItems.HOLOTAPE_IMAGE_DIGAMMA.get()))
                .save(writer, recipeId("crafting/holotape_image_restored"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.HOLOTAPE_DAMAGED.get())
                .requires(ModItems.HOLOTAPE_IMAGE_RESTORED.get())
                .requires(ModItems.UPGRADE_MUFFLER.get())
                .requires(ModItems.CRT_DISPLAY.get())
                .requires(ModItems.GEM_ALEXANDRITE.get())
                .unlockedBy("has_holotape", has(ModItems.HOLOTAPE_IMAGE_RESTORED.get()))
                .save(writer, recipeId("crafting/holotape_damaged"));

        // R5 CraftingManager: Behaelter
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CANISTER_EMPTY.get(), 2)
                .pattern("S ").pattern("AA").pattern("AA")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('A', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .unlockedBy("has_steel_plate", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/canister_empty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GAS_EMPTY.get(), 2)
                .pattern("S ").pattern("AA").pattern("AA")
                .define('A', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('S', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))
                .unlockedBy("has_steel_plate", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/gas_empty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLUID_TANK_EMPTY.get(), 8)
                .pattern("121").pattern("1G1").pattern("121")
                .define('1', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .define('2', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('G', net.minecraftforge.common.Tags.Items.GLASS_PANES)
                .unlockedBy("has_al_plate", has(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/fluid_tank_empty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLUID_TANK_LEAD_EMPTY.get(), 4)
                .pattern("LUL").pattern("LTL").pattern("LUL")
                .define('L', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))
                .define('U', r4("billet_u238"))
                .define('T', ModItems.FLUID_TANK_EMPTY.get())
                .unlockedBy("has_fluid_tank", has(ModItems.FLUID_TANK_EMPTY.get()))
                .save(writer, recipeId("crafting/fluid_tank_lead_empty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.FLUID_BARREL_EMPTY.get(), 2)
                .pattern("121").pattern("1G1").pattern("121")
                .define('1', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('2', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .define('G', net.minecraftforge.common.Tags.Items.GLASS_PANES)
                .unlockedBy("has_steel_plate", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/fluid_barrel_empty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DISPERSER_CANISTER_EMPTY.get(), 4)
                .pattern(" P ").pattern("PGP").pattern(" P ")
                .define('P', Ingredient.of(ModMaterialItems.item(ModMaterials.POLYMER_COMPOSITE, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.PVC, MaterialShape.INGOT)))
                .define('G', ModBlocks.GLASS_BORON.get())
                .unlockedBy("has_glass_boron", has(ModBlocks.GLASS_BORON.get()))
                .save(writer, recipeId("crafting/disperser_canister_empty"));

        // ---- R5: Rezepte mit Fluessigkeitsbehaeltern (Original Fluids.X.getDict(menge) / fluid_tank_full mit Meta) ----
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.BALL_TNT.get(), 4)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("aromatics"), 1000))
                .requires(r4("niter"))
                .requires(Ingredient.of(r4("chemistry_set"), r4("chemistry_set_boron")))
                .unlockedBy("has_x", has(r4("niter")))
                .save(writer, recipeId("crafting/ball_tnt"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("c4_ingot"), 4)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("unsaturateds"), 1000))
                .requires(r4("niter"))
                .requires(Ingredient.of(r4("chemistry_set"), r4("chemistry_set_boron")))
                .unlockedBy("has_x", has(r4("niter")))
                .save(writer, recipeId("crafting/c4_ingot"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 4)
                .requires(net.minecraft.tags.ItemTags.SAND)
                .requires(r4("dust"))
                .requires(r4("dust"))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("water"), 1000))
                .unlockedBy("has_x", has(r4("dust")))
                .save(writer, recipeId("crafting/clay_ball_dust"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("bakelite_powder"), 2)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("aromatics"), 1000))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("petroleum"), 1000))
                .requires(Ingredient.of(r4("chemistry_set"), r4("chemistry_set_boron")))
                .unlockedBy("has_x", has(r4("chemistry_set")))
                .save(writer, recipeId("crafting/bakelite_powder"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("chocolate_milk"), 1)
                .requires(net.minecraftforge.common.Tags.Items.GLASS_PANES)
                .requires(Items.COCOA_BEANS)
                .requires(Items.MILK_BUCKET)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("nitroglycerin"), 1000))
                .unlockedBy("has_x", has(Items.COCOA_BEANS))
                .save(writer, recipeId("crafting/chocolate_milk"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("can_creature"), 1)
                .requires(r4("can_empty"))
                .requires(net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.alchemy.PotionUtils.setPotion(new net.minecraft.world.item.ItemStack(Items.POTION), net.minecraft.world.item.alchemy.Potions.WATER)))
                .requires(Items.SUGAR)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("diesel"), 1000))
                .unlockedBy("has_x", has(r4("can_empty")))
                .save(writer, recipeId("crafting/can_creature"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MISSILE_TAINT.get(), 1)
                .requires(r4("missile_assembly"))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("watz"), 1000))
                .requires(r4("spark_mix_powder"))
                .requires(r4("magic_powder"))
                .unlockedBy("has_x", has(r4("missile_assembly")))
                .save(writer, recipeId("crafting/missile_taint"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("ammo_fireext"), 1)
                .pattern(" P ")
                .pattern("BDB")
                .pattern(" P ")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('B', r4("bolt_steel"))
                .define('D', strict(ModItems.FLUID_TANK_FULL.get(), "water"))
                .unlockedBy("has_x", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/ammo_fireext"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("lamp_tritium_green_off"), 1)
                .requires(net.minecraftforge.common.Tags.Items.GLASS)
                .requires(r4("fire_powder"))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("tritium"), 1000))
                .requires(r4("sulfur"))
                .unlockedBy("has_x", has(r4("fire_powder")))
                .save(writer, recipeId("crafting/lamp_tritium_green_off"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("lamp_tritium_blue_off"), 1)
                .requires(net.minecraftforge.common.Tags.Items.GLASS)
                .requires(r4("fire_powder"))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("tritium"), 1000))
                .requires(r4("aluminum_powder"))
                .unlockedBy("has_x", has(r4("fire_powder")))
                .save(writer, recipeId("crafting/lamp_tritium_blue_off"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("flame_conspiracy"), 1)
                .pattern(" S ")
                .pattern("STS")
                .pattern(" S ")
                .define('S', com.hbm_m.recipe.FluidContainerIngredient.of(fl("kerosene"), 1000))
                .define('T', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_x", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/flame_conspiracy"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("jetpack_tank"), 1)
                .pattern(" S ")
                .pattern("BKB")
                .pattern(" S ")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('B', r4("bolt_steel"))
                .define('K', com.hbm_m.recipe.FluidContainerIngredient.of(fl("kerosene"), 1000))
                .unlockedBy("has_x", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/jetpack_tank"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("gun_kit_1"), 1)
                .requires(com.hbm_m.item.tags_and_tiers.ModTags.Items.RUBBER_BAR)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("woodoil"), 1000))
                .requires(Items.IRON_INGOT)
                .unlockedBy("has_x", has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/gun_kit_1"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("gun_kit_2"), 1)
                .requires(r4("gun_kit_1"))
                .requires(r4("wrench"))
                .requires(r4("ducttape"))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("lubricant"), 1000))
                .unlockedBy("has_x", has(r4("gun_kit_1")))
                .save(writer, recipeId("crafting/gun_kit_2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("inf_water"), 1)
                .pattern("222")
                .pattern("131")
                .pattern("222")
                .define('1', com.hbm_m.recipe.FluidContainerIngredient.of(fl("water"), 1000))
                .define('2', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .define('3', Items.DIAMOND)
                .unlockedBy("has_x", has(Items.DIAMOND))
                .save(writer, recipeId("crafting/inf_water"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("ams_core_eyeofharmony"), 1)
                .pattern("ALA")
                .pattern("LSL")
                .pattern("ALA")
                .define('A', r4("plate_dalekanium"))
                .define('L', strict(ModItems.FLUID_BARREL_FULL.get(), "lava"))
                .define('S', r4("black_hole"))
                .unlockedBy("has_x", has(r4("black_hole")))
                .save(writer, recipeId("crafting/ams_core_eyeofharmony"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.SLIME_BALL, 16)
                .requires(Items.BONE_MEAL)
                .requires(Items.BONE_MEAL)
                .requires(Items.BONE_MEAL)
                .requires(Items.BONE_MEAL)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("sulfuric_acid"), 1000))
                .unlockedBy("has_x", has(Items.BONE_MEAL))
                .save(writer, recipeId("crafting/slime_ball_acid"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("solid_fuel"), 3)
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("heatingoil"), 16000))
                .requires(Ingredient.of(r4("chemistry_set"), r4("chemistry_set_boron")))
                .unlockedBy("has_x", has(r4("chemistry_set")))
                .save(writer, recipeId("crafting/solid_fuel"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("press_preheater"), 1)
                .pattern("CCC")
                .pattern("SLS")
                .pattern("TST")
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))
                .define('S', Items.STONE)
                .define('L', com.hbm_m.recipe.FluidContainerIngredient.of(fl("lava"), 1000))
                .define('T', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT))
                .unlockedBy("has_x", has(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/press_preheater"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("part_generic_piston_hydraulic"), 4)
                .pattern(" I ")
                .pattern("CPC")
                .pattern(" I ")
                .define('I', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('C', ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT))
                .define('P', com.hbm_m.recipe.FluidContainerIngredient.of(fl("lubricant"), 1000))
                .unlockedBy("has_x", has(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/piston_hydraulic"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("drone_patrol_express"), 1)
                .pattern(" P ")
                .pattern("KDK")
                .pattern(" P ")
                .define('P', ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED))
                .define('K', com.hbm_m.recipe.FluidContainerIngredient.of(fl("kerosene"), 1000))
                .define('D', r4("drone_patrol"))
                .unlockedBy("has_x", has(r4("drone_patrol")))
                .save(writer, recipeId("crafting/drone_patrol_express"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("drone_patrol_express_chunkloading"), 1)
                .pattern(" P ")
                .pattern("KDK")
                .pattern(" P ")
                .define('P', ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE_WELDED))
                .define('K', com.hbm_m.recipe.FluidContainerIngredient.of(fl("kerosene"), 1000))
                .define('D', r4("drone_patrol_chunkloading"))
                .unlockedBy("has_x", has(r4("drone_patrol_chunkloading")))
                .save(writer, recipeId("crafting/drone_patrol_express_chunkloading"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, r4("bdcl"), 1)
                .requires(Ingredient.of(r4("oil_tar_crude"), r4("oil_tar_crack"), r4("oil_tar_coal"), r4("oil_tar_wood"), r4("oil_tar_wax"), r4("oil_tar_paraffin")))
                .requires(com.hbm_m.recipe.FluidContainerIngredient.of(fl("water"), 1000))
                .requires(net.minecraftforge.common.Tags.Items.DYES_WHITE)
                .unlockedBy("has_x", has(Items.WHITE_DYE))
                .save(writer, recipeId("crafting/bdcl"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONVEYOR_WAND_EXPRESS.get(), 8)
                .pattern("CCC")
                .pattern("CLC")
                .pattern("CCC")
                .define('C', ModItems.CONVEYOR_WAND_REGULAR.get())
                .define('L', com.hbm_m.recipe.FluidContainerIngredient.of(fl("lubricant"), 1000))
                .unlockedBy("has_x", has(ModItems.CONVEYOR_WAND_REGULAR.get()))
                .save(writer, recipeId("crafting/conveyor_wand_express"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("laser_crystal_co2"), 1)
                .pattern("QDQ")
                .pattern("NCN")
                .pattern("QDQ")
                .define('Q', r4("glass_quartz"))
                .define('D', ModMaterialItems.item(ModMaterials.DESH, MaterialShape.INGOT))
                .define('N', ModMaterialItems.item(ModMaterials.NIOBIUM, MaterialShape.INGOT))
                .define('C', strict(ModItems.FLUID_TANK_FULL.get(), "carbondioxide"))
                .unlockedBy("has_x", has(r4("glass_quartz")))
                .save(writer, recipeId("crafting/laser_crystal_co2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, r4("barbed_wire_acid"), 8)
                .pattern("BBB")
                .pattern("BIB")
                .pattern("BBB")
                .define('B', r4("barbed_wire"))
                .define('I', strict(ModItems.FLUID_TANK_FULL.get(), "peroxide"))
                .unlockedBy("has_x", has(r4("barbed_wire")))
                .save(writer, recipeId("crafting/barbed_wire_acid"));
        {
            // CraftingManager: 2 Kanister Schmiermittel aus Heizoel + Ungesaettigten
            net.minecraft.world.item.ItemStack lube = com.hbm_m.item.liquids.ItemFluidTank.make(ModItems.CANISTER_FULL.get(), fl("lubricant"), 2);
            saveShapelessStackRecipe(writer, recipeId("crafting/canister_full_lubricant"), lube,
                    List.of(com.hbm_m.recipe.FluidContainerIngredient.of(fl("heatingoil"), 1000), com.hbm_m.recipe.FluidContainerIngredient.of(fl("unsaturateds"), 1000),
                            Ingredient.of(ModItems.CANISTER_EMPTY.get()), Ingredient.of(ModItems.CANISTER_EMPTY.get()), Ingredient.of(r4("chemistry_set"), r4("chemistry_set_boron"))),
                    ModItems.CANISTER_EMPTY.get(), "has_canister");
        }

        // CraftingManager: Buch + Kartoffel = RBMK-Handbuch, Buch + Eisenbarren = Starterhandbuch
        saveShapelessStackRecipe(writer, recipeId("crafting/book_guide_rbmk"),
                com.hbm_m.item.tool.ItemGuideBook.make(com.hbm_m.item.tool.ItemGuideBook.BookType.RBMK),
                List.of(Ingredient.of(Items.BOOK), Ingredient.of(Items.POTATO)), Items.BOOK, "has_book");
        saveShapelessStackRecipe(writer, recipeId("crafting/book_guide_starter"),
                com.hbm_m.item.tool.ItemGuideBook.make(com.hbm_m.item.tool.ItemGuideBook.BookType.STARTER),
                List.of(Ingredient.of(Items.BOOK), Ingredient.of(Items.IRON_INGOT)), Items.BOOK, "has_book");

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
                .define('%', ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE))
                .define('#', Items.GOLD_INGOT)
                .define('$', ModItems.INTEGRATED_CIRCUIT.get())
                .define('&', ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT))
                .define('@', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/geiger3"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEFUSER.get())
                .pattern(" # ")
                .pattern("$ $")
                .pattern("$ $")
                .define('$', ModItems.BOLT_STEEL.get())
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/defuser"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DESIGNATOR.get())
                .pattern("  A")
                .pattern("#B#")
                .pattern("#B#")
                .define('#', Ingredient.of(
                        ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT),
                        ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)))
                .define('A', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('B', ModItems.ANALOG_CIRCUIT.get())
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/designator"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DESIGNATOR_MANUAL.get())
                .pattern("  A")
                .pattern("#C#")
                .pattern("#B#")
                .define('#', Ingredient.of(
                        ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT),
                        ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)))
                .define('A', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))
                .define('B', ModItems.ADVANCED_CIRCUIT.get())
                .define('C', ModItems.DESIGNATOR.get())
                .unlockedBy(getHasName(ModItems.DESIGNATOR.get()), has(ModItems.DESIGNATOR.get()))
                .save(writer, recipeId("crafting/designator_manual"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DESIGNATOR_RANGE.get())
                .requires(ModItems.RANGEFINDER.get())
                .requires(ModItems.DESIGNATOR.get())
                .requires(Ingredient.of(
                        ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT),
                        ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)))
                .unlockedBy(getHasName(ModItems.RANGEFINDER.get()), has(ModItems.RANGEFINDER.get()))
                .save(writer, recipeId("crafting/designator_range"));


        // (Stub-Rezepte billet_plutonium_stub und fat_man_core_stub entfernt: Plutonium kommt jetzt aus dem PUREX, der Kern aus ass.mancore)

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CRT_DISPLAY.get(), 4)
                .pattern(" # ")
                .pattern("$@$")
                .pattern(" % ")
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('#', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER))
                .define('%', ModItems.VACUUM_TUBE.get())
                .define('@', Ingredient.of(Tags.Items.GLASS_PANES))
                .unlockedBy(getHasName(ModItems.VACUUM_TUBE.get()), has(ModItems.VACUUM_TUBE.get()))
                .save(writer, recipeId("crafting/crt_ds"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MICROCHIP.get())
                .pattern("#  ")
                .pattern("@  ")
                .pattern("%  ")
                .define('#', ModItems.INSULATOR.get())
                .define('%', Ingredient.of(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE), ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE)))
                .define('@', ModItems.SILICON_CIRCUIT.get())
                .unlockedBy(getHasName(ModItems.SILICON_CIRCUIT.get()), has(ModItems.SILICON_CIRCUIT.get()))
                .save(writer, recipeId("crafting/microchip"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PCB.get(), 4)
                .pattern("#  ")
                .pattern("@  ")
                .pattern("   ")
                .define('#', ModItems.INSULATOR.get())
                .define('@', Ingredient.of(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE), ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE)))
                .unlockedBy(getHasName(ModItems.INSULATOR.get()), has(ModItems.INSULATOR.get()))
                .save(writer, recipeId("crafting/pcb"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DOSIMETER.get())
                .pattern("$%$")
                .pattern("$@$")
                .pattern("$#$")
                .define('$', Items.OAK_PLANKS)
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('#', ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT))
                .define('@', ModItems.VACUUM_TUBE.get())
                .unlockedBy(getHasName(ModItems.VACUUM_TUBE.get()), has(ModItems.VACUUM_TUBE.get()))
                .save(writer, recipeId("crafting/dosimeter"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CROWBAR.get())
                .pattern("$$ ")
                .pattern(" $ ")
                .pattern(" $ ")
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/crowbar"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.OIL_DETECTOR.get())
                .pattern("# @")
                .pattern("#$@")
                .pattern("&&&")
                .define('&', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', Items.COPPER_INGOT)
                .define('$', ModItems.ANALOG_CIRCUIT.get())
                .define('#', ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE))
                .unlockedBy(getHasName(ModItems.ANALOG_CIRCUIT.get()), has(ModItems.ANALOG_CIRCUIT.get()))
                .save(writer, recipeId("crafting/oil_detector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DEPTH_ORES_SCANNER.get())
                .pattern("###")
                .pattern("@%@")
                .pattern("$$$")
                .define('#', ModItems.VACUUM_TUBE.get())
                .define('@', ModItems.CAPACITOR.get())
                .define('%', ModItems.CONTROLLER_CHASSIS.get())
                .define('$', ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModItems.CONTROLLER_CHASSIS.get()), has(ModItems.CONTROLLER_CHASSIS.get()))
                .save(writer, recipeId("crafting/depth_ores_scanner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.SCREWDRIVER.get())
                .pattern("  #")
                .pattern(" # ")
                .pattern("$  ")
                .define('#', Items.IRON_INGOT)
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/screwdriver"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STEAM_CONDENSER.get())
                .pattern("ABA")
                .pattern("BCB")
                .pattern("ABA")
                .define('A', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('B', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('C', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)), has(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE_CAST)))
                .save(writer, recipeId("crafting/steam_condenser"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CONVERTER_BLOCK.get())
                .pattern("###")
                .pattern("@@@")
                .pattern("$$$")
                .define('#', ModItems.CAPACITOR.get())
                .define('@', Items.REDSTONE)
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/converter_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MACHINE_BATTERY_SOCKET.get())
                .pattern("$@$")
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/machine_battery_socket"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MACHINE_BATTERY_SOCKET.get())
                .pattern("I I")
                .pattern("I I")
                .pattern("IRI")
                .define('I', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
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
                .define('$', ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.DURA_STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/bolt_highspeed_steel"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GRENADE_IF.get())
                .pattern(" $ ")
                .pattern("#@#")
                .pattern(" # ")
                .define('$', ModItems.COIL_TUNGSTEN.get())
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/grenade_if"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.VACUUM_TUBE.get())
                .pattern("$")
                .pattern("#")
                .pattern("@")
                .define('$', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('#', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE))
                .define('@', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE)), has(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE)))
                .save(writer, recipeId("crafting/vacuum_tube"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CAPACITOR.get(), 2)
                .pattern("$#$")
                .pattern("% %")
                .define('$', ModItems.INSULATOR.get())
                .define('%', Ingredient.of(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE), ModMaterialItems.item(ModMaterials.ALUMINIUM, MaterialShape.WIRE)))
                .define('#', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER)), has(ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.POWDER)))
                .save(writer, recipeId("crafting/capacitor"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CAPACITOR_TANTALUM.get())
                .requires(ModItems.INSULATOR.get())
                .requires(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.NUGGET))
                .requires(ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.WIRE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.NUGGET)), has(ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.NUGGET)))
                .save(writer, recipeId("crafting/capacitor_tantalum"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CAGE_LAMP.get(), 8)
                .pattern("%")
                .pattern("@")
                .pattern("!")
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('@', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE))
                .define('!', Items.IRON_INGOT)
                .unlockedBy(getHasName(Items.IRON_INGOT), has(Items.IRON_INGOT))
                .save(writer, recipeId("crafting/cage_lamp"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLOOD_LAMP.get(), 8)
                .pattern("%")
                .pattern("@")
                .pattern("!")
                .define('%', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('@', ModMaterialItems.item(ModMaterials.BROMINE, MaterialShape.POWDER))
                .define('!', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/flood_lamp"));

        // CraftingManager: lantern
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.LANTERN.get(), 1)
                .pattern("PGP").pattern(" S ").pattern(" S ")
                .define('P', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('G', Items.GLOWSTONE_DUST)
                .define('S', ModBlocks.STEEL_BEAM.get())
                .unlockedBy("has_steel_beam", has(ModBlocks.STEEL_BEAM.get()))
                .save(writer, recipeId("crafting/lantern"));

        // CraftingManager: filing_cabinet (Stahl)
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.FILE_CABINET_STEEL.get(), 1)
                .pattern(" P ").pattern("PIP").pattern(" P ")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .unlockedBy("has_steel_plate", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/file_cabinet_steel"));

        // CraftingManager: plushie Yomi / Number Nine
        saveShapedStackRecipe(writer, recipeId("crafting/plushie_yomi"),
                com.hbm_m.item.TrinketBlockItem.make(ModBlocks.PLUSHIE.get().asItem(), com.hbm_m.block.decorations.TrinketTypes.PlushieType.YOMI.ordinal()),
                new String[] { "LCR" },
                mapOf('L', Ingredient.of(Tags.Items.CROPS_CARROT), 'C', Ingredient.of(ModItems.RAG.get()), 'R', Ingredient.of(ModItems.VACUUM_TUBE.get())),
                ModItems.RAG.get(), "has_rag");
        saveShapedStackRecipe(writer, recipeId("crafting/plushie_numbernine"),
                com.hbm_m.item.TrinketBlockItem.make(ModBlocks.PLUSHIE.get().asItem(), com.hbm_m.block.decorations.TrinketTypes.PlushieType.NUMBERNINE.ordinal()),
                new String[] { " C ", "LCR", " C " },
                mapOf('L', Ingredient.of(ModItems.CIGARETTE.get()), 'C', Ingredient.of(ModItems.RAG.get()), 'R', Ingredient.of(Items.COAL)),
                ModItems.RAG.get(), "has_rag");

        // CraftingManager: det_cord / det_charge / det_nuke, MineralRecipes: block_semtex/block_c4 <-> Barren
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.DET_CORD.get(), 4)
                .pattern(" P ").pattern("PGP").pattern(" P ")
                .define('P', Items.PAPER).define('G', Items.GUNPOWDER)
                .unlockedBy("has_gunpowder", has(Items.GUNPOWDER))
                .save(writer, recipeId("crafting/det_cord"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.DET_CHARGE.get(), 1)
                .pattern("PDP").pattern("DTD").pattern("PDP")
                .define('P', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('D', ModBlocks.DET_CORD.get())
                .define('T', Ingredient.of(ModMaterialItems.item(ModMaterials.SEMTEX, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT)))
                .unlockedBy("has_det_cord", has(ModBlocks.DET_CORD.get()))
                .save(writer, recipeId("crafting/det_charge"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.DET_NUKE.get(), 1)
                .pattern("PFP").pattern("DCD").pattern("PFP")
                .define('P', ModItems.NEUTRON_REFLECTOR.get())
                .define('D', ModBlocks.DET_CHARGE.get())
                .define('C', ModItems.FAT_MAN_CORE.get())
                .define('F', ModItems.CONTROLLER.get())
                .unlockedBy("has_det_charge", has(ModBlocks.DET_CHARGE.get()))
                .save(writer, recipeId("crafting/det_nuke"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BLOCK_SEMTEX.get(), 1)
                .pattern("###").pattern("###").pattern("###")
                .define('#', ModMaterialItems.item(ModMaterials.SEMTEX, MaterialShape.INGOT))
                .unlockedBy("has_ingot_semtex", has(ModMaterialItems.item(ModMaterials.SEMTEX, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/block_semtex"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.SEMTEX, MaterialShape.INGOT), 9)
                .requires(ModBlocks.BLOCK_SEMTEX.get())
                .unlockedBy("has_block_semtex", has(ModBlocks.BLOCK_SEMTEX.get()))
                .save(writer, recipeId("crafting/ingot_semtex_from_block"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.BLOCK_C4.get(), 1)
                .pattern("###").pattern("###").pattern("###")
                .define('#', ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT))
                .unlockedBy("has_ingot_c4", has(ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/block_c4"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.C4, MaterialShape.INGOT), 9)
                .requires(ModBlocks.BLOCK_C4.get())
                .unlockedBy("has_block_c4", has(ModBlocks.BLOCK_C4.get()))
                .save(writer, recipeId("crafting/ingot_c4_from_block"));
        // CraftingManager: fireworks
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.FIREWORKS.get(), 1)
                .pattern("PPP").pattern("PPP").pattern("WIW")
                .define('P', Items.PAPER).define('W', Ingredient.of(ItemTags.PLANKS)).define('I', Tags.Items.INGOTS_IRON)
                .unlockedBy("has_paper", has(Items.PAPER))
                .save(writer, recipeId("crafting/fireworks"));
        // CraftingManager: flame_war, emp_bomb
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.FLAME_WAR.get(), 1)
                .pattern("WHW").pattern("CTP").pattern("WOW")
                .define('W', Ingredient.of(ItemTags.PLANKS)).define('T', Items.TNT)
                .define('H', ModItems.FLAME_PONY.get()).define('C', ModItems.FLAME_CONSPIRACY.get())
                .define('P', ModItems.FLAME_POLITICS.get()).define('O', ModItems.FLAME_OPINION.get())
                .unlockedBy("has_flame_pony", has(ModItems.FLAME_PONY.get()))
                .save(writer, recipeId("crafting/flame_war"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.EMP_BOMB.get(), 1)
                .pattern("LML").pattern("LCL").pattern("LML")
                .define('L', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.PLATE))
                .define('M', ModItems.MAGNETRON.get()).define('C', ModItems.ADVANCED_CIRCUIT.get())
                .unlockedBy("has_magnetron", has(ModItems.MAGNETRON.get()))
                .save(writer, recipeId("crafting/emp_bomb"));
        // CraftingManager: Haftladungen
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.CHARGE_DYNAMITE.get(), 1)
                .requires(ModItems.STICK_DYNAMITE.get()).requires(ModItems.STICK_DYNAMITE.get()).requires(ModItems.STICK_DYNAMITE.get()).requires(ModItems.DUCTTAPE.get())
                .unlockedBy("has_stick_dynamite", has(ModItems.STICK_DYNAMITE.get()))
                .save(writer, recipeId("crafting/charge_dynamite"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.CHARGE_MINER.get(), 1)
                .pattern(" F ").pattern("FCF").pattern(" F ")
                .define('F', Items.FLINT).define('C', ModBlocks.CHARGE_DYNAMITE.get())
                .unlockedBy("has_charge_dynamite", has(ModBlocks.CHARGE_DYNAMITE.get()))
                .save(writer, recipeId("crafting/charge_miner"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.CHARGE_SEMTEX.get(), 1)
                .requires(ModItems.STICK_SEMTEX.get()).requires(ModItems.STICK_SEMTEX.get()).requires(ModItems.STICK_SEMTEX.get()).requires(ModItems.DUCTTAPE.get())
                .unlockedBy("has_stick_semtex", has(ModItems.STICK_SEMTEX.get()))
                .save(writer, recipeId("crafting/charge_semtex"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.REDSTONE, ModBlocks.CHARGE_C4.get(), 1)
                .requires(ModItems.STICK_C4.get()).requires(ModItems.STICK_C4.get()).requires(ModItems.STICK_C4.get()).requires(ModItems.DUCTTAPE.get())
                .unlockedBy("has_stick_c4", has(ModItems.STICK_C4.get()))
                .save(writer, recipeId("crafting/charge_c4"));
        // WeaponRecipes: dynamite / tnt_ntm / semtex / fissure_bomb
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.DYNAMITE.get(), 1)
                .pattern("DDD").pattern("DSD").pattern("DDD")
                .define('D', ModItems.STICK_DYNAMITE.get()).define('S', ModItems.SAFETY_FUSE.get())
                .unlockedBy("has_stick_dynamite", has(ModItems.STICK_DYNAMITE.get()))
                .save(writer, recipeId("crafting/dynamite"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.TNT_NTM.get(), 1)
                .pattern("DDD").pattern("DSD").pattern("DDD")
                .define('D', ModItems.STICK_TNT.get()).define('S', ModItems.SAFETY_FUSE.get())
                .unlockedBy("has_stick_tnt", has(ModItems.STICK_TNT.get()))
                .save(writer, recipeId("crafting/tnt_ntm"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.SEMTEX.get(), 1)
                .pattern("DDD").pattern("DSD").pattern("DDD")
                .define('D', ModItems.STICK_SEMTEX.get()).define('S', ModItems.SAFETY_FUSE.get())
                .unlockedBy("has_stick_semtex", has(ModItems.STICK_SEMTEX.get()))
                .save(writer, recipeId("crafting/semtex"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.FISSURE_BOMB.get(), 1)
                .pattern("SUS").pattern("RPR").pattern("SUS")
                .define('S', ModBlocks.SEMTEX.get())
                .define('U', ModBlocks.getIngotBlock(ModMaterials.URANIUM238).get())
                .define('R', ModMaterialItems.item(ModMaterials.TANTALIUM, MaterialShape.INGOT))
                .define('P', ModMaterialItems.item(ModMaterials.PLUTONIUM239, MaterialShape.BILLET))
                .unlockedBy("has_semtex", has(ModBlocks.SEMTEX.get()))
                .save(writer, recipeId("crafting/fissure_bomb"));

        // CraftingManager: Kabelschalter/-melder/-diode (red_wire_coated des Originals)
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.CABLE_SWITCH.get(), 1)
                .pattern("S").pattern("W").define('S', net.minecraft.world.item.Items.LEVER).define('W', ModBlocks.RED_WIRE_COATED.get())
                .unlockedBy("has_wire", has(ModBlocks.RED_WIRE_COATED.get())).save(writer, recipeId("crafting/cable_switch"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.CABLE_DETECTOR.get(), 1)
                .pattern("S").pattern("W").define('S', net.minecraft.world.item.Items.REDSTONE).define('W', ModBlocks.RED_WIRE_COATED.get())
                .unlockedBy("has_wire", has(ModBlocks.RED_WIRE_COATED.get())).save(writer, recipeId("crafting/cable_detector"));
        ShapedRecipeBuilder.shaped(RecipeCategory.REDSTONE, ModBlocks.CABLE_DIODE.get(), 1)
                .pattern(" Q ").pattern("CAC").pattern(" Q ")
                .define('Q', ModMaterialItems.item(ModMaterials.SILICON, MaterialShape.NUGGET)).define('C', ModBlocks.RED_CABLE.get())
                .define('A', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT))
                .unlockedBy("has_cable", has(ModBlocks.RED_CABLE.get())).save(writer, recipeId("crafting/cable_diode"));

        // CraftingManager: Schienen
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.RAIL_WOOD.get(), 16)
                .pattern("S S").pattern("SRS").pattern("S S")
                .define('S', net.minecraft.world.item.Items.STICK).define('R', com.hbm_m.item.PartTabMetaItems.itemOrNull("plant_item_rope"))
                .unlockedBy("has_rope", has(com.hbm_m.item.PartTabMetaItems.itemOrNull("plant_item_rope"))).save(writer, recipeId("crafting/rail_wood"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.RAIL_NARROW.get(), 64)
                .pattern("S S").pattern("S S").pattern("S S")
                .define('S', ModBlocks.STEEL_BEAM.get())
                .unlockedBy("has_steel_beam", has(ModBlocks.STEEL_BEAM.get())).save(writer, recipeId("crafting/rail_narrow"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.RAIL_HIGHSPEED.get(), 16)
                .pattern("S S").pattern("SIS").pattern("S S")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)).define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .unlockedBy("has_steel", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))).save(writer, recipeId("crafting/rail_highspeed"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TRANSPORTATION, ModBlocks.RAIL_BOOSTER.get(), 6)
                .pattern("S S").pattern("CIC").pattern("SRS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)).define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('R', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT)).define('C', ModItems.COIL_COPPER.get())
                .unlockedBy("has_steel", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))).save(writer, recipeId("crafting/rail_booster"));

        // CraftingManager: hadron_coil_* zurueck zu dichtem Draht
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE_DENSE), 4)
                .requires(ModBlocks.HADRON_COIL_GOLD.get())
                .unlockedBy("has_hadron_coil_gold", has(ModBlocks.HADRON_COIL_GOLD.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_gold"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.NEODYMIUM, MaterialShape.WIRE_DENSE), 4)
                .requires(ModBlocks.HADRON_COIL_NEODYMIUM.get())
                .unlockedBy("has_hadron_coil_neodymium", has(ModBlocks.HADRON_COIL_NEODYMIUM.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_neodymium"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE_DENSE), 4)
                .requires(ModBlocks.HADRON_COIL_MAGTUNG.get())
                .unlockedBy("has_hadron_coil_magtung", has(ModBlocks.HADRON_COIL_MAGTUNG.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_magtung"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.SCHRABIDIUM, MaterialShape.WIRE_DENSE), 2)
                .requires(ModBlocks.HADRON_COIL_SCHRABIDIUM.get())
                .unlockedBy("has_hadron_coil_schrabidium", has(ModBlocks.HADRON_COIL_SCHRABIDIUM.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_schrabidium"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.SCHRABIDATE, MaterialShape.WIRE_DENSE), 2)
                .requires(ModBlocks.HADRON_COIL_SCHRABIDATE.get())
                .unlockedBy("has_hadron_coil_schrabidate", has(ModBlocks.HADRON_COIL_SCHRABIDATE.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_schrabidate"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.STAR_METAL, MaterialShape.WIRE_DENSE), 2)
                .requires(ModBlocks.HADRON_COIL_STARMETAL.get())
                .unlockedBy("has_hadron_coil_starmetal", has(ModBlocks.HADRON_COIL_STARMETAL.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_starmetal"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.POWDER_CHLOROPHYTE.get(), 2)
                .requires(ModBlocks.HADRON_COIL_CHLOROPHYTE.get())
                .unlockedBy("has_hadron_coil_chlorophyte", has(ModBlocks.HADRON_COIL_CHLOROPHYTE.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_chlorophyte"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.DNT, MaterialShape.WIRE_DENSE), 1)
                .requires(ModBlocks.HADRON_COIL_MESE.get())
                .unlockedBy("has_hadron_coil_mese", has(ModBlocks.HADRON_COIL_MESE.get()))
                .save(writer, recipeId("crafting/uncraft_hadron_coil_mese"));

        // CraftingManager: vinyl_tile (Meta 0 = vinyl_tile, Meta 1 = vinyl_tile_small)
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.VINYL_TILE.get(), 4)
                .pattern(" I ").pattern("IBI").pattern(" I ")
                .define('I', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .define('B', ModBlocks.BRICK_LIGHT.get())
                .unlockedBy("has_brick_light", has(ModBlocks.BRICK_LIGHT.get()))
                .save(writer, recipeId("crafting/vinyl_tile"));
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.VINYL_TILE_SMALL.get(), 4)
                .pattern("BB").pattern("BB")
                .define('B', ModBlocks.VINYL_TILE.get())
                .unlockedBy("has_vinyl_tile", has(ModBlocks.VINYL_TILE.get()))
                .save(writer, recipeId("crafting/vinyl_tile_small"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, ModBlocks.VINYL_TILE.get())
                .requires(ModBlocks.VINYL_TILE_SMALL.get())
                .unlockedBy("has_vinyl_tile_small", has(ModBlocks.VINYL_TILE_SMALL.get()))
                .save(writer, recipeId("crafting/vinyl_tile_from_small"));

        // WeaponRecipes: lamp_demon
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.LAMP_DEMON.get(), 1)
                .pattern(" D ").pattern("S S")
                .define('D', ModItems.DEMON_CORE_CLOSED.get())
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_demon_core", has(ModItems.DEMON_CORE_CLOSED.get()))
                .save(writer, recipeId("crafting/lamp_demon"));

        // CraftingManager: deco_emitter
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.DECO_EMITTER.get(), 1)
                .pattern("IDI").pattern("DRD").pattern("IDI")
                .define('I', Tags.Items.INGOTS_IRON)
                .define('D', Tags.Items.GEMS_DIAMOND)
                .define('R', Tags.Items.STORAGE_BLOCKS_REDSTONE)
                .unlockedBy("has_diamond", has(Items.DIAMOND))
                .save(writer, recipeId("crafting/deco_emitter"));

        // CraftingManager: floodlight
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLOODLIGHT.get(), 2)
                .pattern("CSC").pattern("TST").pattern("G G")
                .define('C', ModItems.CAPACITOR.get())
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('T', ModItems.COIL_TUNGSTEN.get())
                .define('G', Ingredient.of(Tags.Items.GLASS_PANES))
                .unlockedBy("has_coil_tungsten", has(ModItems.COIL_TUNGSTEN.get()))
                .save(writer, recipeId("crafting/floodlight"));

        // CraftingManager: spotlight_fluoro (Neonroehre)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.FLUORESCENT_LAMP.get(), 8)
                .pattern("G").pattern("M").pattern("A")
                .define('G', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('M', r4("nugget_mercury"))
                .define('A', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
                .unlockedBy("has_mercury", has(r4("nugget_mercury")))
                .save(writer, recipeId("crafting/fluorescent_lamp"));

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
                .define('@', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)), has(ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE)))
                .save(writer, recipeId("crafting/wire_coated"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.WOOD_BURNER.get())
                .pattern("$$$")
                .pattern("@&@")
                .pattern("% %")
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', ModItems.COIL_COPPER.get())
                .define('&', Items.FURNACE)
                .define('%', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/wood_burner"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ARMOR_TABLE.get())
                .pattern("$$$")
                .pattern("%&%")
                .pattern("%#%")
                .define('$', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('%', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT))
                .define('&', Items.CRAFTING_TABLE)
                .define('#', ModBlocks.getIngotBlock(ModMaterials.STEEL).get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/armor_table"));

        // SEDNA: Original WeaponRecipes - "PPP", "TCT", "TST" (P Geschuetzbronzeplatte, T Stahlbarren, C Werkbank, S Stahlblock)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MACHINE_WEAPON_TABLE.get())
                .pattern("PPP")
                .pattern("TCT")
                .pattern("TST")
                .define('P', ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.PLATE))
                .define('T', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('C', Items.CRAFTING_TABLE)
                .define('S', ModBlocks.getIngotBlock(ModMaterials.STEEL).get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.GUNMETAL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/machine_weapon_table"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.DETONATOR.get())
                .pattern("#  ")
                .pattern("@  ")
                .pattern("   ")
                .define('#', ModItems.INTEGRATED_CIRCUIT.get())
                .define('@', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
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

        // mine_ap / mine_shrap / mine_he: WeaponRecipeGenerator (1:1 WeaponRecipes). mine_fat braucht ammo_standard NUKE_DEMO (Waffenrunde).

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.RANGEFINDER.get())
                .pattern("GRC")
                .pattern("  S")
                .define('G', Ingredient.of(Tags.Items.GLASS_PANES))
                .define('R', Items.REDSTONE)
                .define('C', ModItems.INTEGRATED_CIRCUIT.get())
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
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
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_SCAFFOLD.get(), 8)
                .pattern("###")
                .pattern(" # ")
                .pattern("###")
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/steel_scaffold"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_POLE.get(), 16)
                .pattern("# #")
                .pattern("###")
                .pattern("# #")
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/steel_pole"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.ANTENNA_TOP.get(), 1)
                .pattern("# #")
                .pattern("#@#")
                .pattern("$$$")
                .define('#', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT))
                .define('@', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.INGOT))
                .define('$', ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT))
                .unlockedBy("has_tungsten_ingot", has(ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/antenna_top"));

        // CraftingManager: tape_recorder, pole_satellite_receiver, steel_beam, steel_wall, steel_corner, steel_roof
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.TAPE_RECORDER.get(), 4)
                .pattern("TST").pattern("SSS")
                .define('T', ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT))
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/tape_recorder"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.POLE_SATELLITE_RECEIVER.get(), 1)
                .pattern("SS ").pattern("SCR").pattern("SS ")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .define('C', ModItems.VACUUM_TUBE.get())
                .define('R', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .unlockedBy("has_vacuum_tube", has(ModItems.VACUUM_TUBE.get()))
                .save(writer, recipeId("crafting/pole_satellite_receiver"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_BEAM.get(), 8)
                .pattern("S").pattern("S").pattern("S")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/steel_beam"));
        // CraftingManager: steel_beam aus Geruest, chain, steel_grate(_wide), Geruest-Faerben (Block im Rezept = jede Farbe)
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_BEAM.get(), 8)
                .pattern("S").pattern("S").pattern("S")
                .define('S', Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get(), ModBlocks.STEEL_SCAFFOLD_RED.get(), ModBlocks.STEEL_SCAFFOLD_WHITE.get(), ModBlocks.STEEL_SCAFFOLD_YELLOW.get()))
                .unlockedBy("has_steel_scaffold", has(ModBlocks.STEEL_SCAFFOLD.get()))
                .save(writer, recipeId("crafting/steel_beam_from_scaffold"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.DUNGEON_CHAIN.get(), 8)
                .pattern("S").pattern("S").pattern("S")
                .define('S', ModBlocks.STEEL_BEAM.get())
                .unlockedBy("has_steel_beam", has(ModBlocks.STEEL_BEAM.get()))
                .save(writer, recipeId("crafting/dungeon_chain"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_GRATE.get(), 4)
                .pattern("SS").pattern("SS")
                .define('S', ModBlocks.STEEL_BEAM.get())
                .unlockedBy("has_steel_beam", has(ModBlocks.STEEL_BEAM.get()))
                .save(writer, recipeId("crafting/steel_grate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_GRATE_WIDE.get(), 4)
                .pattern("SS")
                .define('S', ModBlocks.STEEL_GRATE.get())
                .unlockedBy("has_steel_grate", has(ModBlocks.STEEL_GRATE.get()))
                .save(writer, recipeId("crafting/steel_grate_wide"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_GRATE.get(), 1)
                .pattern("SS")
                .define('S', ModBlocks.STEEL_GRATE_WIDE.get())
                .unlockedBy("has_steel_grate_wide", has(ModBlocks.STEEL_GRATE_WIDE.get()))
                .save(writer, recipeId("crafting/steel_grate_from_wide"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_SCAFFOLD.get(), 8)
                .pattern("SSS").pattern("SDS").pattern("SSS")
                .define('S', Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get(), ModBlocks.STEEL_SCAFFOLD_RED.get(), ModBlocks.STEEL_SCAFFOLD_WHITE.get(), ModBlocks.STEEL_SCAFFOLD_YELLOW.get()))
                .define('D', Tags.Items.DYES_GRAY)
                .unlockedBy("has_steel_scaffold", has(ModBlocks.STEEL_SCAFFOLD.get()))
                .save(writer, recipeId("crafting/steel_scaffold_dye"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_SCAFFOLD_RED.get(), 8)
                .pattern("SSS").pattern("SDS").pattern("SSS")
                .define('S', Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get(), ModBlocks.STEEL_SCAFFOLD_RED.get(), ModBlocks.STEEL_SCAFFOLD_WHITE.get(), ModBlocks.STEEL_SCAFFOLD_YELLOW.get()))
                .define('D', Tags.Items.DYES_RED)
                .unlockedBy("has_steel_scaffold", has(ModBlocks.STEEL_SCAFFOLD.get()))
                .save(writer, recipeId("crafting/steel_scaffold_red_dye"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_SCAFFOLD_WHITE.get(), 8)
                .pattern("SSS").pattern("SDS").pattern("SSS")
                .define('S', Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get(), ModBlocks.STEEL_SCAFFOLD_RED.get(), ModBlocks.STEEL_SCAFFOLD_WHITE.get(), ModBlocks.STEEL_SCAFFOLD_YELLOW.get()))
                .define('D', Tags.Items.DYES_WHITE)
                .unlockedBy("has_steel_scaffold", has(ModBlocks.STEEL_SCAFFOLD.get()))
                .save(writer, recipeId("crafting/steel_scaffold_white_dye"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_SCAFFOLD_YELLOW.get(), 8)
                .pattern("SSS").pattern("SDS").pattern("SSS")
                .define('S', Ingredient.of(ModBlocks.STEEL_SCAFFOLD.get(), ModBlocks.STEEL_SCAFFOLD_RED.get(), ModBlocks.STEEL_SCAFFOLD_WHITE.get(), ModBlocks.STEEL_SCAFFOLD_YELLOW.get()))
                .define('D', Tags.Items.DYES_YELLOW)
                .unlockedBy("has_steel_scaffold", has(ModBlocks.STEEL_SCAFFOLD.get()))
                .save(writer, recipeId("crafting/steel_scaffold_yellow_dye"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_WALL.get(), 4)
                .pattern("SSS").pattern("SSS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/steel_wall"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, ModBlocks.STEEL_CORNER.get(), 1)
                .requires(ModBlocks.STEEL_WALL.get()).requires(ModBlocks.STEEL_WALL.get())
                .unlockedBy("has_steel_wall", has(ModBlocks.STEEL_WALL.get()))
                .save(writer, recipeId("crafting/steel_corner"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.STEEL_ROOF.get(), 2)
                .pattern("SSS")
                .define('S', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy("has_steel_ingot", has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/steel_roof"));

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

        // CraftingManager: grosses Zahnrad (Meta 0)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.GEAR_LARGE.get())
                .pattern("III")
                .pattern("ICI")
                .pattern("III")
                .define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('C', Items.COPPER_INGOT)
                .unlockedBy("has_iron_plate", has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/gear_large"));

        // ToolRecipes: Geschuetz-KI-Chip (ANY_PLASTIC = Polymer oder Bakelit)
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.TURRET_CHIP.get())
                .pattern("WWW")
                .pattern("CPC")
                .pattern("WWW")
                .define('W', ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE))
                .define('P', Ingredient.of(ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT)))
                .define('C', ModItems.ADVANCED_CIRCUIT.get())
                .unlockedBy("has_advanced_circuit", has(ModItems.ADVANCED_CIRCUIT.get()))
                .save(writer, recipeId("crafting/turret_chip"));

        // CraftingManager: Redstone-Batterie und Kupfer-Kondensator (battery_pack)
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_PACK_BATTERY_REDSTONE.get())
                .pattern("IRI").pattern("PRP").pattern("IRI")
                .define('I', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('R', Items.REDSTONE_BLOCK)
                .define('P', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .unlockedBy("has_redstone_block", has(Items.REDSTONE_BLOCK))
                .save(writer, recipeId("crafting/battery_pack_redstone"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BATTERY_PACK_CAPACITOR_COPPER.get())
                .pattern("IRI").pattern("PRP").pattern("IRI")
                .define('I', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('R', Items.COPPER_BLOCK)
                .define('P', ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.PLATE))
                .unlockedBy("has_copper_block", has(Items.COPPER_BLOCK))
                .save(writer, recipeId("crafting/battery_pack_capacitor_copper"));

        // ToolRecipes: Bewehrungs-Setzer
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.REBAR_PLACER.get())
                .pattern("RDR")
                .pattern("DWD")
                .pattern("RDR")
                .define('R', ModBlocks.REBAR.get())
                .define('D', ModItems.DUCTTAPE.get())
                .define('W', ModItems.WRENCH.get())
                .unlockedBy("has_rebar", has(ModBlocks.REBAR.get()))
                .save(writer, recipeId("crafting/rebar_placer"));

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
                .define('$', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
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
                .define('%', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('#', Items.TNT)
                .define('$', Items.FLINT)
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
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
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('%', ModItems.FIREBRICK.get())
                .define('$', ModMaterialItems.item(ModMaterials.COPPER, MaterialShape.PLATE))
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
                .define('#', ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT))
                .define('@', ModBlocks.getIngotBlock(ModMaterials.LEAD).get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT)), has(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT)))
                .save(writer, recipeId("crafting/anvil_lead"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.NUCLEAR_CHARGE.get())
                .pattern("$$$")
                .pattern("%@%")
                .pattern("%#%")
                .define('%', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', ModItems.FAT_MAN_CORE.get())
                .define('#', ModItems.CONTROLLER.get())
                .define('$', ModItems.INSULATOR.get())
                .unlockedBy(getHasName(ModItems.FAT_MAN_CORE.get()), has(ModItems.FAT_MAN_CORE.get()))
                .save(writer, recipeId("crafting/nuclear_charge"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CONTROLLER_CHASSIS.get())
                .pattern("$$$")
                .pattern("%##")
                .pattern("$$$")
                .define('$', ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.PLATE))
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

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModMaterialItems.item(ModMaterials.CEMENT, MaterialShape.POWDER), 4)
                .pattern("AB")
                .pattern("BB")
                .define('A', ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER))
                .define('B', Items.CLAY_BALL)
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER)), has(ModMaterialItems.item(ModMaterials.LIMESTONE, MaterialShape.POWDER)))
                .save(writer, recipeId("crafting/limestone_cement"));

        registerSmelting(writer, ModItems.FIRECLAY_BALL.get(), ModItems.FIREBRICK.get(), 0.1F, 100, "firebrick_smelting");
        // Original SmeltingRecipes.java:125: Kies -> Bruchstein
        registerSmelting(writer, Items.GRAVEL, Items.COBBLESTONE, 0.0F, 200, "gravel_to_cobblestone_smelting");
    }

    //переплавка порошков -  ИСПРАВЛЕННАЯ ВЕРСИЯ
    private void registerPowderCooking(Consumer<FinishedRecipe> writer) {
        //  ПРОВЕРЯЕМ КАЖДЫЙ ПОРОШОК ПЕРЕД ИСПОЛЬЗОВАНИЕМ
        Item ironPowder = ModMaterialItems.item(ModMaterials.IRON, MaterialShape.POWDER);
        Item goldPowder = ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.POWDER);
        Item coalPowder = ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER);

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
        ItemLike uraniumIngot = ModMaterialItems.item(ModMaterials.URANIUM, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.URANIUM_RAW.get(), uraniumIngot, 2.1F, 3.0F, "uranium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.URANIUM_ORE.get(), uraniumIngot, 2.1F, 3.0F, "uranium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.URANIUM_ORE_DEEPSLATE.get(), uraniumIngot, 2.1F, 3.0F, "uranium_ore_deepslate");

        ItemLike thoriumIngot = ModMaterialItems.item(ModMaterials.THORIUM232, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.THORIUM_RAW.get(), thoriumIngot, 2.1F, 3.0F, "thorium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.THORIUM_ORE.get(), thoriumIngot, 2.1F, 3.0F, "thorium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.THORIUM_ORE_DEEPSLATE.get(), thoriumIngot, 2.1F, 3.0F, "thorium_ore_deepslate");

        ItemLike titaniumIngot = ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.TITANIUM_RAW.get(), titaniumIngot, 0.7F, 1.0F, "titanium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.TITANIUM_ORE.get(), titaniumIngot, 0.7F, 1.0F, "titanium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.TITANIUM_ORE_DEEPSLATE.get(), titaniumIngot, 0.7F, 1.0F, "titanium_ore_deepslate");

        ItemLike tungstenIngot = ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.TUNGSTEN_RAW.get(), tungstenIngot, 0.7F, 1.0F, "tungsten_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.TUNGSTEN_ORE.get(), tungstenIngot, 0.7F, 1.0F, "tungsten_ore");

        ItemLike leadIngot = ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.LEAD_RAW.get(), leadIngot, 0.7F, 1.0F, "lead_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.LEAD_ORE.get(), leadIngot, 0.7F, 1.0F, "lead_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.LEAD_ORE_DEEPSLATE.get(), leadIngot, 0.7F, 1.0F, "lead_ore_deepslate");

        ItemLike cobaltIngot = ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.COBALT_RAW.get(), cobaltIngot, 0.7F, 1.0F, "cobalt_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.COBALT_ORE.get(), cobaltIngot, 0.7F, 1.0F, "cobalt_ore");

        ItemLike berylliumIngot = ModMaterialItems.item(ModMaterials.BERYLLIUM, MaterialShape.INGOT);
        registerSmeltingAndBlasting(writer, ModItems.BERYLLIUM_RAW.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_raw");
        registerSmeltingAndBlasting(writer, ModBlocks.BERYLLIUM_ORE.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_ore");
        registerSmeltingAndBlasting(writer, ModBlocks.BERYLLIUM_ORE_DEEPSLATE.get(), berylliumIngot, 0.7F, 1.0F, "beryllium_ore_deepslate");

        ItemLike aluminumIngot = ModMaterialItems.item(ModMaterials.ALUMINUM, MaterialShape.INGOT);
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
                .define('A', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('B', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/crate_iron"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.CRATE_STEEL.get())
                .pattern("AAA")
                .pattern("B B")
                .pattern("BBB")
                .define('A', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('B', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT))
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/crate_steel"));

        // Desh- und Wolframkiste: Original ContainerUpgradeCraftingHandler (Inhalt der Stahlkiste geht mit),
        // siehe RestRecipeGenerator.crafting().

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
        buildStamp(writer, ModItems.STAMP_STEEL_FLAT.get(), ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), "stamp_steel_flat");
        buildStamp(writer, ModItems.STAMP_TITANIUM_FLAT.get(), ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT), "stamp_titanium_flat");
        buildStamp(writer, ModItems.STAMP_OBSIDIAN_FLAT.get(), Blocks.OBSIDIAN.asItem(), "stamp_obsidian_flat");
        // stamp_desh_flat: eigenes Original-Muster BDB/DSD/BDB mit Ferrouran, siehe OrigCraftingRecipeGenerator.part16
    }

    //крафты гранат
    private void registerGrenades(Consumer<FinishedRecipe> writer) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADE.get())
                .pattern("%@ ")
                .pattern("#$#")
                .pattern(" # ")
                .define('%', ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE))
                .define('@', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('#', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
                .define('$', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)), has(ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE)))
                .save(writer, recipeId("crafting/grenade"));

        buildGrenadeUpgrade(writer, ModItems.GRENADEHE.get(), ModItems.BALL_TNT.get(), "grenadehe");
        buildGrenadeUpgrade(writer, ModItems.GRENADESLIME.get(), Items.SLIME_BALL, "grenadeslime");
        buildGrenadeUpgrade(writer, ModItems.GRENADEFIRE.get(), ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.INGOT), "grenadefire");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_HE.get(), ModItems.BALL_TNT.get(), "grenade_if_he");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_SLIME.get(), Items.SLIME_BALL, "grenade_if_slime");
        buildGrenadeIfUpgrade(writer, ModItems.GRENADE_IF_FIRE.get(), ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.INGOT), "grenade_if_fire");

        buildBlades(writer, ModItems.BLADE_STEEL.get(), ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE),"blades_steel");
        buildBlades(writer, ModItems.BLADE_TITANIUM.get(), ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.PLATE),"blades_titanium");
        buildBlades(writer, ModItems.BLADE_ALLOY.get(), ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.INGOT), ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.PLATE),"blades_advanced_alloy");



        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_FIRE.get()), Items.BLAZE_POWDER, "barbed_wire_fire");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_POISON.get()), Items.SPIDER_EYE, "barbed_wire_poison");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_WITHER.get()), Items.WITHER_SKELETON_SKULL, "barbed_wire_wither");
        buildBarbedWireUpgrade(writer, Item.byBlock(ModBlocks.BARBED_WIRE_RAD.get()), ModMaterialItems.item(ModMaterials.PLUTONIUM, MaterialShape.BILLET), "barbed_wire_rad");

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.GRENADESMART.get(), 4)
                .pattern(" @ ")
                .pattern("&%$")
                .pattern(" # ")
                .define('%', ModItems.GRENADE.get())
                .define('&', ModMaterialItems.item(ModMaterials.PHOSPHORUS, MaterialShape.INGOT))
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
                .define('@', ModItems.MICROCHIP.get())
                .define('$', ModItems.BALL_TNT.get())
                .unlockedBy(getHasName(ModItems.GRENADE.get()), has(ModItems.GRENADE.get()))
                .save(writer, recipeId("crafting/grenadesmart"));
    }

    //крафты брони и инструментов
    private void registerToolAndArmorSets(Consumer<FinishedRecipe> writer) {
        ItemLike titaniumIngot = ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT);
        buildToolSet(writer, "titanium", titaniumIngot,
                ModItems.TITANIUM_SWORD.get(), ModItems.TITANIUM_SHOVEL.get(), ModItems.TITANIUM_PICKAXE.get(),
                ModItems.TITANIUM_HOE.get(), ModItems.TITANIUM_AXE.get());
        // Ruestungsrezepte: ArmorRecipeGenerator (1:1 ArmorRecipes)

        ItemLike steelIngot = ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.INGOT);
        buildToolSet(writer, "steel", steelIngot,
                ModItems.STEEL_SWORD.get(), ModItems.STEEL_SHOVEL.get(), ModItems.STEEL_PICKAXE.get(),
                ModItems.STEEL_HOE.get(), ModItems.STEEL_AXE.get());
        // Ruestungsrezepte: ArmorRecipeGenerator (1:1 ArmorRecipes)

        // Sternmetall-Werkzeuge: ToolRecipeGenerator (Aufwertung der verzierten Kobaltwerkzeuge, LBSM ist aus).
        // Legierungswerkzeuge haben im Original kein Rezept.

        ItemLike cobaltIngot = ModMaterialItems.item(ModMaterials.COBALT, MaterialShape.INGOT);
        // Ruestungsrezepte: ArmorRecipeGenerator (1:1 ArmorRecipes)

        ItemLike asbestosSheet = ModMaterialItems.item(ModMaterials.ASBESTOS, MaterialShape.INGOT);
        // Ruestungsrezepte: ArmorRecipeGenerator (1:1 ArmorRecipes)
    }

    //крафты катушек
    private void registerCoil(Consumer<FinishedRecipe> writer) {
        buildCoil(writer, ModItems.COIL_ADVANCED_ALLOY.get(), ModMaterialItems.item(ModMaterials.ADVANCED_ALLOY, MaterialShape.WIRE), "coil_advanced_alloy");
        buildCoil(writer, ModItems.COIL_COPPER.get(), ModMaterialItems.item(ModMaterials.RED_COPPER, MaterialShape.WIRE), "coil_copper");
        buildCoil(writer, ModItems.COIL_GOLD.get(), ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.WIRE), "coil_gold");
        buildCoil(writer, ModItems.COIL_MAGNETIZED_TUNGSTEN.get(), ModMaterialItems.item(ModMaterials.MAGNETIZED_TUNGSTEN, MaterialShape.WIRE), "coil_magnetized_tungsten");
        buildCoil(writer, ModItems.COIL_TUNGSTEN.get(), ModMaterialItems.item(ModMaterials.TUNGSTEN, MaterialShape.WIRE), "coil_tungsten");
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
                .define('$', ModMaterialItems.item(ModMaterials.IRON, MaterialShape.PLATE))
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
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
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
                .define('#', ModMaterialItems.item(ModMaterials.STEEL, MaterialShape.PLATE))
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
        // Original ToolRecipes: {"  B", "GB ", "SG "}, B = blade_meteorite, G = Goldplatte, S = Stock
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.METEORITE_SWORD.get())
                .pattern("  B")
                .pattern("GB ")
                .pattern("SG ")
                .define('B', ModItems.BLADE_METEORITE.get())
                .define('G', ModMaterialItems.item(ModMaterials.GOLD, MaterialShape.PLATE))
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModItems.BLADE_METEORITE.get()), has(ModItems.BLADE_METEORITE.get()))
                .save(writer, recipeId("meteorite_sword"));

        // Original SmeltingRecipes: meteorite_sword -> meteorite_sword_seared, 0 XP
        registerSmelting(writer, ModItems.METEORITE_SWORD.get(), ModItems.METEORITE_SWORD_SEARED.get(),
                0.0F, 200, "meteorite_sword_seared");
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
                        'C', Ingredient.of(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER)),
                        'P', Ingredient.of(ModMaterialItems.item(ModMaterials.LEAD, MaterialShape.POWDER))
                ),
                Items.COPPER_INGOT, "has_copper");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_red"), redStack, pattern,
                mapOf(
                        'I', Ingredient.of(ModMaterialItems.item(ModMaterials.TITANIUM, MaterialShape.INGOT)),
                        'C', Ingredient.of(ModMaterialItems.item(ModMaterials.COAL, MaterialShape.POWDER)),
                        'P', Ingredient.of(baseStack)
                ),
                baseStack.getItem(), "has_rad_absorber_base");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_green"), greenStack, pattern,
                mapOf(
                        'I', Ingredient.of(
                                ModMaterialItems.item(ModMaterials.BAKELITE, MaterialShape.INGOT),
                                ModMaterialItems.item(ModMaterials.POLYMER, MaterialShape.INGOT)),
                        'C', Ingredient.of(ModItems.POWDER_DESH_MIX.get()),
                        'P', Ingredient.of(redStack)
                ),
                redStack.getItem(), "has_rad_absorber_red");

        saveShapedStackRecipe(writer, recipeId("crafting/rad_absorber_pink"),
                BlockAbsorberItem.forTier(ModBlocks.RAD_ABSORBER.get(), pink), pattern,
                mapOf(
                        'I', Ingredient.of(ModMaterialItems.item(ModMaterials.SATURNITE, MaterialShape.INGOT)),
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

    /** Ofenrezept mit Stapelgroesse im Ergebnis (Forge liest "result" als Stapel). */
    private void saveSmeltingStackRecipe(Consumer<FinishedRecipe> writer, ResourceLocation recipeId, ItemLike input, ItemStack result, float xp) {
        Advancement.Builder advancement = Advancement.Builder.advancement();
        advancement.addCriterion("has_input", has(input));
        ResourceLocation advancementId = recipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/");

        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(@NotNull JsonObject json) {
                json.addProperty("type", "minecraft:smelting");
                json.addProperty("category", "misc");
                json.add("ingredient", Ingredient.of(input).toJson());
                json.add("result", stackToJson(result));
                json.addProperty("experience", xp);
                json.addProperty("cookingtime", 200);
            }

            @Override public ResourceLocation getId() { return recipeId; }
            @Override public RecipeSerializer<?> getType() { return RecipeSerializer.SMELTING_RECIPE; }
            @Override @Nullable public JsonObject serializeAdvancement() { return advancement.serializeToJson(); }
            @Override @Nullable public ResourceLocation getAdvancementId() { return advancementId; }
        });
    }

    /** R5: Fluessigkeit per Port-Name. */
    private static net.minecraft.world.level.material.Fluid fl(String name) {
        var e = com.hbm_m.inventory.fluid.ModFluids.getEntry(name);
        if (e == null) throw new IllegalStateException("R5-Rezept: unbekannte Fluessigkeit " + name);
        return e.getSource();
    }

    /** R5: genau dieser gefuellte Behaelter (Original new ItemStack(fluid_tank_full, 1, Fluids.X.getID())). */
    private static Ingredient strict(net.minecraft.world.item.Item container, String fluid) {
        return net.minecraftforge.common.crafting.StrictNBTIngredient.of(com.hbm_m.item.liquids.ItemFluidTank.make(container, fl(fluid), 1));
    }

    /** R4: Gegenstand per ID (Datagen, Registries sind eingefroren); unbekannte ID bricht den Lauf ab. */
    private static net.minecraft.world.item.Item r4(String id) {
        net.minecraft.world.item.Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("hbm_m", id));
        if (item == Items.AIR) throw new IllegalStateException("R4-Rezept: unbekannter Gegenstand " + id);
        return item;
    }

    /** Formloses Rezept mit NBT im Ergebnis (Metadaten-Varianten des Originals). */
    private void saveShapelessStackRecipe(Consumer<FinishedRecipe> writer, ResourceLocation recipeId,
            ItemStack result, List<Ingredient> ingredients, ItemLike unlockItem, String unlockCriterion) {
        Advancement.Builder advancement = Advancement.Builder.advancement();
        advancement.addCriterion(unlockCriterion, has(unlockItem));
        ResourceLocation advancementId = recipeId.withPrefix("recipes/" + RecipeCategory.MISC.getFolderName() + "/");

        writer.accept(new FinishedRecipe() {
            @Override
            public void serializeRecipeData(@NotNull JsonObject json) {
                json.addProperty("type", "minecraft:crafting_shapeless");
                json.addProperty("category", "misc");
                JsonArray ingredientsJson = new JsonArray();
                for (Ingredient ingredient : ingredients) {
                    ingredientsJson.add(ingredient.toJson());
                }
                json.add("ingredients", ingredientsJson);
                json.add("result", stackToJson(result));
            }

            @Override
            public ResourceLocation getId() {
                return recipeId;
            }

            @Override
            public RecipeSerializer<?> getType() {
                return RecipeSerializer.SHAPELESS_RECIPE;
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
        if (PlatformHooks.hasItemTag(stack)) {
            json.addProperty("nbt", PlatformHooks.getItemTag(stack).toString());
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
        // Цикл по реестру материалов: для каждого материала, у которого есть и биллет,
        // и наггет — 6 наггетов -> 1 биллет и 1 биллет -> 6 наггетов.
        // Порт семейства addBillet(billet, nugget) из 1.7.10 MineralRecipes.java.
        for (ModMaterials mat : ModMaterials.values()) {
            if (!mat.has(MaterialShape.BILLET) || !mat.has(MaterialShape.NUGGET)) continue;
            registerBilletNuggetPair(writer,
                    ModMaterialItems.item(mat, MaterialShape.BILLET),
                    ModMaterialItems.item(mat, MaterialShape.NUGGET),
                    mat.getId());
        }
    }

    /**
     * 1:1-Port von {@code MineralRecipes.add1To9PairSameMeta} fuer den Atommuell: neun kleine
     * Brocken werden zu einem vollen Stueck und umgekehrt - <b>je Abfallklasse getrennt</b>, sonst
     * liesse sich Thorium-Muell in Schrabidium-Muell umetikettieren.
     */
    private void registerWasteCompression(Consumer<FinishedRecipe> writer) {
        wastePairs(writer, "nuclear_waste_long", "nw_long", "nw_long_tiny");
        wastePairs(writer, "nuclear_waste_long_depleted", "nw_long_dep", "nw_long_dep_tiny");
        wastePairs(writer, "nuclear_waste_short", "nw_short", "nw_short_tiny");
        wastePairs(writer, "nuclear_waste_short_depleted", "nw_short_dep", "nw_short_dep_tiny");
    }

    private void wastePairs(Consumer<FinishedRecipe> writer, String name, String full, String tiny) {
        var fullItems = com.hbm_m.item.PartTabMetaItems.group(full);
        var tinyItems = com.hbm_m.item.PartTabMetaItems.group(tiny);

        for (int i = 0; i < fullItems.size() && i < tinyItems.size(); i++) {
            Item whole = fullItems.get(i);
            Item bit = tinyItems.get(i);

            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, whole)
                    .pattern("###")
                    .pattern("###")
                    .pattern("###")
                    .define('#', bit)
                    .unlockedBy(getHasName(bit), has(bit))
                    .save(writer, recipeId("crafting/" + name + "_" + i + "_compress"));

            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, bit, 9)
                    .requires(whole)
                    .unlockedBy(getHasName(whole), has(whole))
                    .save(writer, recipeId("crafting/" + name + "_" + i + "_decompress"));
        }
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