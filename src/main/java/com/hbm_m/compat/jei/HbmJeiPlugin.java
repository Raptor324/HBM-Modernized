package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.inventory.gui.GUIAnvil;
import com.hbm_m.inventory.gui.GUIMachineAdvancedAssembler;
import com.hbm_m.inventory.gui.GUIMachineAssembler;
import com.hbm_m.inventory.gui.GUIMachineCentrifuge;
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
import com.hbm_m.inventory.gui.GUIMachineCyclotron;
import com.hbm_m.inventory.gui.GUIMachineCrucible;
import com.hbm_m.inventory.gui.GUIMachineArcWelder;
import com.hbm_m.inventory.gui.GUIMachineSolderingStation;
import com.hbm_m.inventory.gui.GUIMachineCrystallizer;
import com.hbm_m.inventory.gui.GUIMachinePress;
import com.hbm_m.inventory.gui.GUIMachineShredder;
import com.hbm_m.inventory.gui.GUIBlastFurnace;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.platform.PlatformHooks;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.hbm_m.recipe.ArcWelderRecipe;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import com.hbm_m.recipe.CentrifugeRecipe;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.CrucibleSmeltingRecipe;
import com.hbm_m.recipe.CrystallizerRecipe;
import com.hbm_m.recipe.CyclotronRecipe;
import com.hbm_m.recipe.GasCentrifugeRecipe;
import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.recipe.SolderingRecipe;
import com.hbm_m.recipe.ArcFurnaceRecipe;
import com.hbm_m.recipe.AmmoPressRecipe;
import com.hbm_m.recipe.PurexRecipe;
import com.hbm_m.recipe.ExposureChamberRecipe;
import com.hbm_m.recipe.RotaryFurnaceRecipe;
import com.hbm_m.recipe.CompressorRecipe;
import com.hbm_m.recipe.CrackingTowerRecipe;
import com.hbm_m.recipe.RadiolysisRecipe;
import com.hbm_m.recipe.ElectrolyserFluidRecipe;
import com.hbm_m.recipe.ElectrolyserMetalRecipe;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if forge {
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;

import javax.annotation.Nonnull;

@JeiPlugin
public class HbmJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            //? if fabric && < 1.21.1 {
            /*new ResourceLocation(RefStrings.MODID, "jei_plugin");
            *///?} else {
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "jei_plugin");
            //?}


    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AnvilJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AssemblerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CentrifugeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ChemicalPlantJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CyclotronJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        // Crucible casting / alloying categories УДАЛЕНЫ — они были JEI-only зеркалами поверх
        // удалённых статических CrucibleAlloyingRecipes / CrucibleMoldRecipes (MoltenAlloy/MoldCasting
        // остаются in-memory, но их предметные JEI-зеркала не имеют data-driven источника правды).
        registration.addRecipeCategories(new CrucibleSmeltingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ArcWelderJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SolderingStationJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrystallizerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PressJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ShredderJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RBMKDisassemblyJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RBMKWasteDecayJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RBMKOutgasserJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new BlastFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new GasCentrifugeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ArcFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AmmoPressJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PurexJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ExposureChamberJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RotaryFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CompressorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ElectrolyserFluidJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ElectrolyserMetalJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrackingTowerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RadiolysisJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        // JEI иногда грузит плагин до старта мира — единая защита level==null.
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        // Anvil — особый случай: рецепты идут через AnvilRecipeManager.getClientRecipes() (внутренний реестр),
        // а не через RecipeManager (AnvilRecipe — предметно-теговая система не через PlatformRecipe).
        registration.addRecipes(AnvilJeiCategory.RECIPE_TYPE, AnvilRecipeManager.getClientRecipes());

        // Все остальные машины — data-driven (JSON), читаем напрямую через кросс-версионный RecipeHooks.
        registration.addRecipes(AssemblerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE));
        registration.addRecipes(CentrifugeJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CentrifugeRecipe.Type.INSTANCE));
        registration.addRecipes(ChemicalPlantJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE));
        registration.addRecipes(CyclotronJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CyclotronRecipe.Type.INSTANCE));
        registration.addRecipes(CrucibleSmeltingJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrucibleSmeltingRecipe.Type.INSTANCE));
        registration.addRecipes(CrystallizerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrystallizerRecipe.Type.INSTANCE));
        registration.addRecipes(PressJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, PressRecipe.Type.INSTANCE));
        registration.addRecipes(ShredderJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ShredderRecipe.Type.INSTANCE));
        registration.addRecipes(BlastFurnaceJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, BlastFurnaceRecipe.Type.INSTANCE));

        // ArcWelder / SolderingStation / GasCentrifuge — теперь data-driven (JSON), раньше — статика.
        registration.addRecipes(ArcWelderJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ArcWelderRecipe.Type.INSTANCE));
        registration.addRecipes(SolderingStationJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, SolderingRecipe.Type.INSTANCE));
        registration.addRecipes(GasCentrifugeJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, GasCentrifugeRecipe.Type.INSTANCE));

        // FFA: дополнительные категории (машины, добавленные в FFA-ветке)
        registration.addRecipes(ArcFurnaceJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ArcFurnaceRecipe.Type.INSTANCE));
        registration.addRecipes(AmmoPressJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, AmmoPressRecipe.Type.INSTANCE));
        registration.addRecipes(PurexJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, PurexRecipe.Type.INSTANCE));
        registration.addRecipes(ExposureChamberJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ExposureChamberRecipe.Type.INSTANCE));
        registration.addRecipes(RotaryFurnaceJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, RotaryFurnaceRecipe.Type.INSTANCE));
        // Compressor / CrackingTower / Radiolysis / Electrolyser — теперь data-driven (JSON), раньше — статика.
        registration.addRecipes(CompressorJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CompressorRecipe.Type.INSTANCE));
        registration.addRecipes(CrackingTowerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrackingTowerRecipe.Type.INSTANCE));
        registration.addRecipes(RadiolysisJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, RadiolysisRecipe.Type.INSTANCE));
        registration.addRecipes(ElectrolyserFluidJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ElectrolyserFluidRecipe.Type.INSTANCE));
        registration.addRecipes(ElectrolyserMetalJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ElectrolyserMetalRecipe.Type.INSTANCE));

        // FFA: Crucible- und RBMK-Kategorien (in funni-stuff nicht enthalten).
        registration.addRecipes(RBMKDisassemblyJeiCategory.RECIPE_TYPE, RBMKDisassemblyJeiRecipe.all());
        registration.addRecipes(RBMKWasteDecayJeiCategory.RECIPE_TYPE, RBMKWasteDecayJeiCategory.all());
        registration.addRecipes(RBMKOutgasserJeiCategory.RECIPE_TYPE, RBMKOutgasserJeiCategory.all());
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        for (var anvil : ModBlocks.getAnvilBlocks()) {
            registration.addRecipeCatalyst(new ItemStack(anvil.get()), AnvilJeiCategory.RECIPE_TYPE);
        }
        registration.addRecipeCatalyst(new ItemStack(ModItems.MACHINE_ASSEMBLER.get()), AssemblerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get()), AssemblerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CENTRIFUGE.get()), CentrifugeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_PLANT.get()), ChemicalPlantJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_FACTORY.get()), ChemicalPlantJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CYCLOTRON.get()), CyclotronJeiCategory.RECIPE_TYPE);
        // Каталисты CrucibleCasting/CrucibleAlloying JEI удалены вместе с этими категориями.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleSmeltingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_WELDER.get()), ArcWelderJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOLDERING_STATION.get()), SolderingStationJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYSTALLIZER.get()), CrystallizerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.PRESS.get()), PressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHREDDER.get()), ShredderJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE.get()), BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BLAST_FURNACE.get()), BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GAS_CENTRIFUGE.get()), GasCentrifugeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_FURNACE.get()), ArcFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.AMMO_PRESS.get()), AmmoPressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PUREX.get()), PurexJeiCategory.RECIPE_TYPE);
        // E-Press und Conveyor Press teilen sich PressRecipe mit dem Basis-Press.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EPRESS.get()), PressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CONVEYOR_PRESS.get()), PressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.EXPOSURE_CHAMBER.get()), ExposureChamberJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ROTARY_FURNACE.get()), RotaryFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COMPRESSOR.get()), CompressorJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTROLYSER.get()), ElectrolyserFluidJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ELECTROLYSER.get()), ElectrolyserMetalJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRACKING_TOWER.get()), CrackingTowerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.RADIOLYSIS.get()), RadiolysisJeiCategory.RECIPE_TYPE);
        // Microwave teilt sich Vanilla-Ofen-Rezepte mit dem eingebauten JEI-Ofen-Kategorie.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_MICROWAVE.get()), mezz.jei.api.constants.RecipeTypes.SMELTING);
    }

    @Override
    public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(GUIAnvil.class, 11, 42, 36, 18, AnvilJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIAnvil.class, 65, 42, 36, 18, AnvilJeiCategory.RECIPE_TYPE);
        // Assembler recipe click area around the progress bar
        registration.addRecipeClickArea(GUIMachineAssembler.class, 45, 82, 83, 32, AssemblerJeiCategory.RECIPE_TYPE);
        // Advanced assembler: JEI on progress arrow only (7,125 is recipe selector → GUIScreenRecipeSelector)
        registration.addRecipeClickArea(GUIMachineAdvancedAssembler.class, 62, 126, 70, 16, AssemblerJeiCategory.RECIPE_TYPE);
        // Matches the old NEI transfer rect: new Rectangle(56, 0, 80, 38)
        registration.addRecipeClickArea(GUIMachineCentrifuge.class, 56, 0, 80, 38, CentrifugeJeiCategory.RECIPE_TYPE);
        // Chemical Plant click area around the progress bar
        registration.addRecipeClickArea(GUIMachineChemicalPlant.class, 62, 126, 70, 16, ChemicalPlantJeiCategory.RECIPE_TYPE);
        // Cyclotron click area around the main accelerator progress
        registration.addRecipeClickArea(GUIMachineCyclotron.class, 48, 27, 79, 34, CyclotronJeiCategory.RECIPE_TYPE);
        // Crucible smelting — same click zone on the crucible GUI
        // (CrucibleCasting/CrucibleAlloying click areas удалены вместе с этими категориями.)
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleSmeltingJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIMachineArcWelder.class, 72, 37, 33, 14, ArcWelderJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIMachineSolderingStation.class, 72, 28, 33, 14, SolderingStationJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIMachineCrystallizer.class, 80, 39, 33, 14, CrystallizerJeiCategory.RECIPE_TYPE);
        // Press — matches legacy NEI transfer rect: new Rectangle(74 + 6 + 18, 23, 24, 18)
        registration.addRecipeClickArea(GUIMachinePress.class, 98, 23, 24, 18, PressJeiCategory.RECIPE_TYPE);
        // Shredder: progress arrow area
        registration.addRecipeClickArea(GUIMachineShredder.class, 63, 89, 34, 18, ShredderJeiCategory.RECIPE_TYPE);
        // Blast Furnace: progress arrow area
        registration.addRecipeClickArea(GUIBlastFurnace.class, 101, 35, 24, 17, BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineBlastFurnace.class, 62, 60, 56, 46, BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineArcFurnace.class, 45, 37, 38, 5, ArcFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineAmmoPress.class, 96, 20, 20, 32, AmmoPressJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachinePUREX.class, 45, 40, 24, 8, PurexJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerItemSubtypes(@Nonnull ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
            ModItems.FLUID_BARREL.get(),
            (stack, ctx) -> {
                dev.architectury.fluid.FluidStack fluid = FluidBarrelItem.getFluid(stack);
                if (fluid.isEmpty()) return "empty";
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
                return (fluidId != null ? fluidId.toString() : "unknown") + ":" + fluid.getAmount();
            }
        );

        registration.registerSubtypeInterpreter(
            ModItems.FLUID_IDENTIFIER.get(),
            (stack, ctx) -> {
                String f1 = FluidIdentifierItem.getTypeName(stack, true);
                String f2 = FluidIdentifierItem.getTypeName(stack, false);
                return f1 + ";" + f2;
            }
        );

        registration.registerSubtypeInterpreter(
            ModItems.ASSEMBLY_TEMPLATE.get(),
            (stack, ctx) -> {
                ItemStack output = ItemAssemblyTemplate.getRecipeOutput(stack);
                if (output.isEmpty()) return "empty";
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(output.getItem());
                return (itemId != null ? itemId.toString() : "unknown") +
                    (PlatformHooks.hasItemTag(output) ? PlatformHooks.getItemTag(output).toString() : "");
            }
        );

        // Rad Absorber subtypes by tier
        registration.registerSubtypeInterpreter(
            ModBlocks.RAD_ABSORBER.get().asItem(),
            (stack, ctx) -> {
                BlockAbsorber.EnumAbsorberTier tier = BlockAbsorberItem.readTier(stack);
                return tier != null ? tier.getSerializedName() : "base";
            }
        );

        // Регистрация для труб
        registerDuctSubtype(registration, ModItems.FLUID_DUCT);
        registerDuctSubtype(registration, ModItems.FLUID_DUCT_COLORED);
        registerDuctSubtype(registration, ModItems.FLUID_DUCT_SILVER);
    }

    private void registerDuctSubtype(ISubtypeRegistration registration, RegistrySupplier<Item> ductSupplier) {
        registration.registerSubtypeInterpreter(
            ductSupplier.get(),
            (stack, ctx) -> {
                dev.architectury.fluid.FluidStack fluid = FluidDuctItem.getFluidType(stack);
                if (fluid.isEmpty()) return "empty";
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
                return fluidId != null ? fluidId.toString() : "unknown";
            }
        );
    }
}
//?} else {
/*public final class HbmJeiPlugin {
    private HbmJeiPlugin() {}
}*///?}
