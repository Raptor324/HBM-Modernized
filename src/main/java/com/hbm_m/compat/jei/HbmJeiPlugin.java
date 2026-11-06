package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.block.generic.BlockAbsorber;
import com.hbm_m.inventory.gui.GUIAnvil;
import com.hbm_m.inventory.gui.GUIElectrolyserFluid;
import com.hbm_m.inventory.gui.GUIElectrolyserMetal;
import com.hbm_m.inventory.gui.GUIMachineAdvancedAssembler;
import com.hbm_m.inventory.gui.GUIMachineAssembler;
import com.hbm_m.inventory.gui.GUIMachineCentrifuge;
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
import com.hbm_m.inventory.gui.GUIMachineCyclotron;
import com.hbm_m.inventory.gui.GUIMachineCrucible;
import com.hbm_m.inventory.gui.GUIMachineArcWelder;
import com.hbm_m.inventory.gui.GUIMachineSolderingStation;
import com.hbm_m.inventory.gui.GUIMachineCrystallizer;
import com.hbm_m.inventory.gui.GUIMachineExposureChamber;
import com.hbm_m.inventory.gui.GUIMachineRotaryFurnace;
import com.hbm_m.inventory.gui.GUIMachinePress;
import com.hbm_m.inventory.gui.GUIMachineShredder;
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
import com.hbm_m.recipe.AmmoPressRecipe;
import com.hbm_m.recipe.PurexRecipe;
import com.hbm_m.recipe.ExposureChamberRecipe;
import com.hbm_m.recipe.RotaryFurnaceRecipe;
import com.hbm_m.recipe.CompressorRecipe;
import com.hbm_m.recipe.CrackingTowerRecipe;
import com.hbm_m.recipe.ElectrolyserFluidRecipe;
import com.hbm_m.recipe.ElectrolyserMetalRecipe;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;

import javax.annotation.Nonnull;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.hbm_m.platform.StackNbt;
@JeiPlugin
@OnlyIn(Dist.CLIENT)
public class HbmJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
                        ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "jei_plugin");


    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(@Nonnull IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AnvilJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AnvilSmithingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AssemblerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CentrifugeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ChemicalPlantJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new FusionJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PlasmaForgeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CyclotronJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ParticleAcceleratorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CatalyticReformerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new FractionTowerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new HydrotreaterJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new VacuumDistillJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CokerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new BreederJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new LiquefactorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SolidificationJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PyroOvenJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SilexJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        // Тигель: 1:1 порты NEI-хендлеров оригинала — CrucibleSmeltingHandler (CrucibleSmelting),
        // CrucibleCastingHandler (MoldCasting), CrucibleAlloyingHandler (MoltenAlloy).
        registration.addRecipeCategories(new MoldCastingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new MoltenAlloyJeiCategory(registration.getJeiHelpers().getGuiHelper()));
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
        registration.addRecipeCategories(new SuperComputerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RockMillJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PrecAssJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ExposureChamberJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RotaryFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CompressorJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ElectrolyserFluidJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ElectrolyserMetalJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrackingTowerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new RadiolysisJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        // Fluid Containers: порт оригинального FluidRecipeHandler (NEI "fluidcons") регистрируется
        // в NeiPortJeiCategories.registerCategories (в оригинале FluidRecipeHandler стоит в самом
        // конце NEIRegistry). Здесь второй раз НЕ регистрировать — JEI падает на дубликате uid
        // "hbm_m:fluidcons" (RecipeCategoryRegistration.addRecipeCategories).

        // Машины, у которых категории не было вовсе (см. JeiExtraCategories).
        for (mezz.jei.api.recipe.category.IRecipeCategory<?> extra
                : JeiExtraCategories.create(registration.getJeiHelpers().getGuiHelper())) {
            registration.addRecipeCategories(extra);
        }
        // Restliche NEI-Handler des Originals (Werkzeugumbau, Bauplaene, Brennstoffe, Raffinerie, Granaten ...)
        NeiPortJeiCategories.registerCategories(registration);
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        // JEI иногда грузит плагин до старта мира — единая защита level==null.
        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        // Anvil — особый случай: рецепты идут через AnvilRecipeManager.getClientRecipes() (внутренний реестр),
        // а не через RecipeManager (AnvilRecipe — предметно-теговая система не через PlatformRecipe).
        registration.addRecipes(AnvilJeiCategory.RECIPE_TYPE, AnvilRecipeManager.getConstruction(level));
        registration.addRecipes(AnvilSmithingJeiCategory.RECIPE_TYPE, AnvilRecipeManager.getSmithing(level));

        // Все остальные машины — data-driven (JSON), читаем напрямую через кросс-версионный RecipeHooks.
        // Секретные чертежи (пул "secret.") скрыты, как в оригинальном NEI (ClientConfig.NEI_HIDE_SECRETS).
        registration.addRecipes(AssemblerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, AssemblerRecipe.Type.INSTANCE).stream()
                .filter(recipe -> !AssemblerJeiCategory.isSecretPool(recipe))
                .toList());
        registration.addRecipes(CentrifugeJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CentrifugeRecipe.Type.INSTANCE));
        registration.addRecipes(ChemicalPlantJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ChemicalPlantRecipe.Type.INSTANCE));
        registration.addRecipes(FusionJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.FusionRecipe.Type.INSTANCE));
        registration.addRecipes(PlasmaForgeJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.PlasmaForgeRecipe.Type.INSTANCE));
        registration.addRecipes(CyclotronJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CyclotronRecipe.Type.INSTANCE));
        registration.addRecipes(ParticleAcceleratorJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.ParticleAcceleratorRecipe.Type.INSTANCE));
        registration.addRecipes(CatalyticReformerJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.CatalyticReformerRecipe.Type.INSTANCE));
        registration.addRecipes(FractionTowerJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.FractionTowerRecipe.Type.INSTANCE));
        registration.addRecipes(HydrotreaterJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.HydrotreaterRecipe.Type.INSTANCE));
        registration.addRecipes(VacuumDistillJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.VacuumDistillRecipe.Type.INSTANCE));
        registration.addRecipes(CokerJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.CokerRecipe.Type.INSTANCE));
        registration.addRecipes(BreederJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.BreederRecipe.Type.INSTANCE));
        registration.addRecipes(LiquefactorJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.LiquefactorRecipe.Type.INSTANCE));
        registration.addRecipes(SolidificationJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.SolidificationRecipe.Type.INSTANCE));
        registration.addRecipes(PyroOvenJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.PyroOvenRecipe.Type.INSTANCE));
        registration.addRecipes(SilexJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.SilexRecipe.Type.INSTANCE));
        // Тигель-литьё: фильтр набора как в оригинальном registerMoldsForNEI
        // (CrucibleRecipes.java:259-262 "if(out != null)") — без него в категорию попадают
        // рецепты с выходным тегом, который не разрешается ни в один предмет (пустой слот выхода).
        registration.addRecipes(MoldCastingJeiCategory.RECIPE_TYPE,
                MoldCastingJeiCategory.filter(RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.MoldCastingRecipe.Type.INSTANCE)));
        registration.addRecipes(MoltenAlloyJeiCategory.RECIPE_TYPE,
                RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.MoltenAlloyRecipe.Type.INSTANCE));
        // Тигель-плавка: фильтр как в оригинальном getSmeltingRecipes ("if(!ores.isEmpty())",
        // CrucibleRecipes.java:210-219) — скрывает рецепты с неразрешаемым входным тегом и
        // с выходными материалами вне Mats (alloy/nickel), которых в наборе оригинала не было.
        registration.addRecipes(CrucibleSmeltingJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrucibleSmeltingRecipe.Type.INSTANCE).stream()
                .filter(CrucibleSmeltingJeiCategory::isVisible)
                .toList());
        // Crystallizer: оригинал CrystallizerRecipes.getRecipes() пропускал рецепты scrap_plastic
        // в NEI (CrystallizerRecipes.java:306 "if(stack.item == ModItems.scrap_plastic) continue") —
        // машина их по-прежнему обрабатывает, скрыт только показ.
        registration.addRecipes(CrystallizerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrystallizerRecipe.Type.INSTANCE).stream()
                .filter(CrystallizerJeiCategory::isVisible).toList());
        registration.addRecipes(PressJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, PressRecipe.Type.INSTANCE));
        registration.addRecipes(ShredderJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ShredderRecipe.Type.INSTANCE));
        // Blast Furnace: BlastFurnaceHandler показывал только BlastFurnaceRecipesNT (isNT) — легаси-набор
        // (BlastFurnaceRecipes, ориг. AlloyFurnaceRecipeHandler/DiFurnace) в категорию не входит.
        registration.addRecipes(BlastFurnaceJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, BlastFurnaceRecipe.Type.INSTANCE).stream()
                .filter(BlastFurnaceRecipe::isNT).toList());

        // ArcWelder / SolderingStation / GasCentrifuge — теперь data-driven (JSON), раньше — статика.
        registration.addRecipes(ArcWelderJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ArcWelderRecipe.Type.INSTANCE));
        registration.addRecipes(SolderingStationJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, SolderingRecipe.Type.INSTANCE));
        registration.addRecipes(GasCentrifugeJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, GasCentrifugeRecipe.Type.INSTANCE));

        // FFA: дополнительные категории (машины, добавленные в FFA-ветке)
        // Arc Furnace: источник рецептов — тот же, что у машины (динамический ArcFurnaceRecipes.getOutput,
        // включая автогены), как в оригинале NEIRegistry.java:76-77 поверх getSolidRecipes/getFluidRecipes;
        // datapack-JSON hbm_m:arc_furnace машиной не потребляется и в JEI не показывается.
        registration.addRecipes(ArcFurnaceJeiCategory.RECIPE_TYPE, ArcFurnaceJeiCategory.recipes(level));
        registration.addRecipes(AmmoPressJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, AmmoPressRecipe.Type.INSTANCE));
        registration.addRecipes(PurexJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, PurexRecipe.Type.INSTANCE));
        registration.addRecipes(SuperComputerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.SuperComputerRecipe.Type.INSTANCE));
        registration.addRecipes(RockMillJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.RockMillRecipe.Type.INSTANCE));
        registration.addRecipes(PrecAssJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, com.hbm_m.recipe.PrecAssRecipe.Type.INSTANCE));
        registration.addRecipes(ExposureChamberJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ExposureChamberRecipe.Type.INSTANCE));
        registration.addRecipes(RotaryFurnaceJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, RotaryFurnaceRecipe.Type.INSTANCE));
        // Compressor / CrackingTower / Radiolysis / Electrolyser — теперь data-driven (JSON), раньше — статика.
        // Оригинал CompressorRecipes.getRecipes() скрывал в NEI рецепты с одинаковой входной и
        // выходной жидкостью (это генерический «+1 давление»-сценарий машины) — фильтр повторён
        // в CompressorJeiCategory.isVisible.
        registration.addRecipes(CompressorJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CompressorRecipe.Type.INSTANCE).stream()
                .filter(CompressorJeiCategory::isVisible)
                .toList());
        registration.addRecipes(CrackingTowerJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, CrackingTowerRecipe.Type.INSTANCE));
        // Radiolysis: eigenes Wasser-Rezept + alle Cracking-Rezepte (Original: RadiolysisRecipes
        // legte WATER + CrackingRecipes in eine NEI-Map), Dampf-Spalten wie im Original entfallen.
        registration.addRecipes(RadiolysisJeiCategory.RECIPE_TYPE, RadiolysisJeiCategory.recipes(level));
        registration.addRecipes(ElectrolyserFluidJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ElectrolyserFluidRecipe.Type.INSTANCE));
        registration.addRecipes(ElectrolyserMetalJeiCategory.RECIPE_TYPE, RecipeHooks.getAllRecipes(level, ElectrolyserMetalRecipe.Type.INSTANCE));

        // FFA: Crucible- und RBMK-Kategorien (in funni-stuff nicht enthalten).
        registration.addRecipes(RBMKDisassemblyJeiCategory.RECIPE_TYPE, RBMKDisassemblyJeiRecipe.all());
        registration.addRecipes(RBMKWasteDecayJeiCategory.RECIPE_TYPE, RBMKWasteDecayJeiCategory.all());
        registration.addRecipes(RBMKOutgasserJeiCategory.RECIPE_TYPE, RBMKOutgasserJeiCategory.all());

        // Fluid Containers: источник как в оригинале (MachineRecipes.getFluidContainers ->
        // FluidContainerRegistry.allContainers) — добавляются в NeiPortJeiCategories.registerRecipes,
        // здесь дубликат не добавлять (JEI 15 бросает исключение на повторной категории uid).

        JeiExtraCategories.registerRecipes(registration, level);
        NeiPortJeiCategories.registerRecipes(registration, level);
    }

    @Override
    public void registerRecipeCatalysts(@Nonnull IRecipeCatalystRegistration registration) {
        JeiExtraCategories.registerCatalysts(registration);
        for (var anvil : ModBlocks.getAnvilBlocks()) {
            registration.addRecipeCatalyst(new ItemStack(anvil.get()), AnvilJeiCategory.RECIPE_TYPE, AnvilSmithingJeiCategory.RECIPE_TYPE);
        }
        registration.addRecipeCatalyst(new ItemStack(ModItems.MACHINE_ASSEMBLER.get()), AssemblerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.ADVANCED_ASSEMBLY_MACHINE.get()), AssemblerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_CENTRIFUGE.get()), CentrifugeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_PLANT.get()), ChemicalPlantJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.TORUS.get()), FusionJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PLASMA_FORGE.get()), PlasmaForgeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_FACTORY.get()), ChemicalPlantJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CYCLOTRON.get()), CyclotronJeiCategory.RECIPE_TYPE);

        // Teilchenbeschleuniger: die Quelle nimmt die Ausgangsstoffe, der Detektor liefert das
        // Ergebnis - beide sollen im Rezeptbrowser als Maschine gelten.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PA_SOURCE.get()), ParticleAcceleratorJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PA_DETECTOR.get()), ParticleAcceleratorJeiCategory.RECIPE_TYPE);

        // Die Erdoelverarbeitung war bislang komplett unsichtbar im Rezeptbrowser.
        registration.addRecipeCatalyst(new ItemStack(ModItems.CATALYTIC_REFORMER.get()), CatalyticReformerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.FRACTION_TOWER.get()), FractionTowerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.HYDROTREATER.get()), HydrotreaterJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.VACUUM_DISTILL.get()), VacuumDistillJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.COKER.get()), CokerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.BREEDER.get()), BreederJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.LIQUEFACTOR.get()), LiquefactorJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOLIDIFIER.get()), SolidificationJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PYROOVEN.get()), PyroOvenJeiCategory.RECIPE_TYPE);
        // SILEX: оригинальный SILEXRecipeHandler.getMachinesForRecipe() — только machine_silex
        // (FEL и лазерные кристаллы в оригинале катализаторами SILEX не были).
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SILEX.get()), SilexJeiCategory.RECIPE_TYPE);
        // Машины оригинальных NEI-хендлеров (getMachinesForRecipe()): CrucibleCastingHandler —
        // foundry_basin + foundry_mold + machine_strand_caster; CrucibleAlloyingHandler и
        // CrucibleSmeltingHandler — machine_crucible.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FOUNDRY_MOLD.get()), MoldCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()), MoldCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.STRAND_CASTER.get()), MoldCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), MoltenAlloyJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleSmeltingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_WELDER.get()), ArcWelderJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOLDERING_STATION.get()), SolderingStationJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYSTALLIZER.get()), CrystallizerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.PRESS.get()), PressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHREDDER.get()), ShredderJeiCategory.RECIPE_TYPE);
        // Blast Furnace: только machine_blast_furnace (как BlastFurnaceHandler); легаси-блок blast_furnace
        // (ориг. MachineDifurnace) не может крафтить NT-рецепты и категорией не катализируется.
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_BLAST_FURNACE.get()), BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_GASCENT.get()), GasCentrifugeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_FURNACE.get()), ArcFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.AMMO_PRESS.get()), AmmoPressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.PUREX.get()), PurexJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_SUPERCOMPUTER.get()), SuperComputerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_ROCKMILL.get()), RockMillJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MACHINE_PRECASS.get()), PrecAssJeiCategory.RECIPE_TYPE);
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

        // Fluid Containers: катализаторы — пустые ёмкости из оригинального
        // FluidRecipeHandler.getMachinesForRecipe() (NEI-IMC регистрировал их катализаторами,
        // см. IMCHandlerNHNEI.java:19-21) — добавляются в NeiPortJeiCategories.registerCatalysts,
        // здесь дубликаты не добавлять (та же категория uid "hbm_m:fluidcons").

        NeiPortJeiCategories.registerCatalysts(registration);
    }

    @Override
    public void registerGuiHandlers(@Nonnull IGuiHandlerRegistration registration) {
        // GUIAnvil: только smithing-зоны — как в оригинале. Construction-зоны (11,42)/(65,42,36,18)
        // в оригинальном AnvilRecipeHandler.loadTransferRects закомментированы (AnvilRecipes-оверлей
        // на GUI не вешается), поэтому в порту их тоже нет.
        registration.addRecipeClickArea(GUIAnvil.class, 65, 15, 18, 18, AnvilSmithingJeiCategory.RECIPE_TYPE);  // orig. Rectangle(56+9, 24-9, 18, 18)
        registration.addRecipeClickArea(GUIAnvil.class, 29, 15, 18, 18, AnvilSmithingJeiCategory.RECIPE_TYPE);  // orig. Rectangle(56+9-36, 24-9, 18, 18)
        // Assembler recipe click area around the progress bar
        registration.addRecipeClickArea(GUIMachineAssembler.class, 45, 82, 83, 32, AssemblerJeiCategory.RECIPE_TYPE);
        // Advanced assembler: JEI on progress arrow only (7,125 is recipe selector → GUIScreenRecipeSelector)
        registration.addRecipeClickArea(GUIMachineAdvancedAssembler.class, 62, 126, 70, 16, AssemblerJeiCategory.RECIPE_TYPE);
        // Matches the old NEI transfer rect of CentrifugeRecipeHandler: new Rectangle(63, 7, 80, 38).
        // NEI-Transfer-Rects sind Rezept-Space, nicht GUI-lokal: NEIs RecipeTransferRectHandler prueft
        // Klicks gegen "pos - gui.guiLeft - guiOffset" (NEI TemplateRecipeHandler.transferRect:
        // "Point relMouse = new Point(pos.x - gui.guiLeft - offsetx, pos.y - gui.guiTop - offsety)"),
        // guiOffset = RecipeInfo.getGuiOffset-Default {5, 11} (HBM registriert keinen eigenen Offset).
        // Effektive GUI-lokale Zone des Originals: (63+5, 7+11) = (68, 18) — dieselbe Konvention, mit der
        // die Original-Autoren selbst rechnen (CrystallizerRecipeHandler: "new Rectangle(80 - 5, 47 - 11, ...)").
        registration.addRecipeClickArea(GUIMachineCentrifuge.class, 68, 18, 80, 38, CentrifugeJeiCategory.RECIPE_TYPE);
        // Chemical Plant click area around the progress bar
        registration.addRecipeClickArea(GUIMachineChemicalPlant.class, 62, 126, 70, 16, ChemicalPlantJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineFusionTorus.class, 98, 81, 70, 6, FusionJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineFusionPlasmaForge.class, 62, 81, 70, 16, PlasmaForgeJeiCategory.RECIPE_TYPE);
        // Cyclotron — orig. NEI GUI transfer rect new Rectangle(48 - 5, 27 - 11, 34, 34)
        // (CyclotronRecipeHandler.loadTransferRects). NEI compares clicks against mouse - gui offset
        // {5, 11}, so the GUI-local zone is (48, 27, 34, 34) — exactly the accelerator progress bar
        // of GUIMachineCyclotron (blit at leftPos + 48, topPos + 27, 34x34).
        registration.addRecipeClickArea(GUIMachineCyclotron.class, 48, 27, 34, 34, CyclotronJeiCategory.RECIPE_TYPE);
        // Crucible smelting + alloying + casting — все три хендлера оригинала объявили один и тот же
        // transfer rect Rectangle(65, 23, 36, 18) (CrucibleSmeltingHandler.java:137,
        // CrucibleAlloyingHandler.java:157, CrucibleCastingHandler.java:135). NEI проверяет клики
        // против mouse - guiLeft - guiOffset {5, 11} (RecipeInfo.getGuiOffset-Default,
        // TemplateRecipeHandler.transferRect), поэтому эффективная GUI-локальная зона — (70, 34, 36, 18)
        // — тот же сдвиг +5/+11, что и у остальных портованных клик-зон выше.
        registration.addRecipeClickArea(GUIMachineCrucible.class, 70, 34, 36, 18,
                CrucibleSmeltingJeiCategory.RECIPE_TYPE, MoltenAlloyJeiCategory.RECIPE_TYPE,
                MoldCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIMachineArcWelder.class, 72, 37, 33, 14, ArcWelderJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIMachineSolderingStation.class, 72, 28, 33, 14, SolderingStationJeiCategory.RECIPE_TYPE);
        // Matches the 1.7.10 NEI transfer rect of CrystallizerRecipeHandler: new Rectangle(80 - 5, 47 - 11, 27, 12)
        registration.addRecipeClickArea(GUIMachineCrystallizer.class, 75, 36, 27, 12, CrystallizerJeiCategory.RECIPE_TYPE);
        // Exposure Chamber — orig. Transfer-Rect ExposureChamberHandler: new Rectangle(31, 28, 41, 10);
        // NEI testet Klicks gegen Maus - guiOffset {5, 11}, daher GUI-lokal (36, 39) — genau auf dem
        // Fortschrittsbalken des Original-GUI (drawTexturedModalRect(guiLeft + 36, guiTop + 39, ..., 42, 10)).
        registration.addRecipeClickArea(GUIMachineExposureChamber.class, 36, 39, 41, 10, ExposureChamberJeiCategory.RECIPE_TYPE);
        // Press — NEI-Transfer-Rect new Rectangle(74 + 6 + 18, 23, 24, 18) ist in Rezept-Space koordiniert;
        // NEI testet Klicks gegen Maus - guiOffset {5, 11} (RecipeInfo.getGuiOffset-Default, HBM registriert
        // keinen eigenen Offset), daher liegt die effektive GUI-lokale Zone bei (103, 34) — genau auf dem
        // Presskolben von processing/gui_press.png ((102,34)-(127,52)).
        registration.addRecipeClickArea(GUIMachinePress.class, 103, 34, 24, 18, PressJeiCategory.RECIPE_TYPE);
        // E-Press — orig. Transfer-Rect new Rectangle(13 + 6 + 18, 23, 24, 18) war aufs Original-GUI (186px)
        // abgestimmt; die neu gebaute Port-GUI (imageHeight 166) hat den Presskolben bei (79,35)-(97,51)
        // (gui_epress.png), daher Area dort registriert.
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineEPress.class, 79, 35, 19, 17, PressJeiCategory.RECIPE_TYPE);
        // Shredder: progress arrow area
        registration.addRecipeClickArea(GUIMachineShredder.class, 63, 89, 34, 18, ShredderJeiCategory.RECIPE_TYPE);
        // Electrolyser — 1.7.10 NEI transfer rects (ElectrolyserFluidHandler / ElectrolyserMetalHandler) sind
        // rezeptlokal; NEI prueft Klicks gegen Maus - guiOffset {5, 11}, daher GUI-lokal +5/+11:
        // Fluid new Rectangle(57, 15, 12, 40) -> (62, 26, 12, 40) — genau die Elektrolyse-Anzeige (GUI 62,26,12,e).
        // Metal new Rectangle(2, 35, 22, 25)   -> (7, 46, 22, 25)   — genau die Elektrolyse-Anzeige (GUI 7,71-o,22,o).
        registration.addRecipeClickArea(GUIElectrolyserFluid.class, 62, 26, 12, 40, ElectrolyserFluidJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(GUIElectrolyserMetal.class, 7, 46, 22, 25, ElectrolyserMetalJeiCategory.RECIPE_TYPE);
        // Blast Furnace (machine_blast_furnace): click area over the progress arrow / firing column.
        // GUIBlastFurnace — легаси-плавильный GUI (ориг. GUIDiFurnace, "alloysmelting"), к этой категории не относится.
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineBlastFurnace.class, 62, 60, 56, 46, BlastFurnaceJeiCategory.RECIPE_TYPE);
        // Arc Furnace: оригинальные ArcFurnaceSolidHandler/ArcFurnaceFluidHandler не регистрировали
        // in-GUI transfer rect (NEIUniversalHandler.java:31-32,363-369 оставляет guiRec/guiGui пустыми;
        // его Rectangle(147,1,18,18) живёт только в панели рецептов) — клик-зона в GUI не добавляется.
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineAmmoPress.class, 96, 20, 20, 32, AmmoPressJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachinePUREX.class, 45, 40, 24, 8, PurexJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineSuperComputer.class, 62, 81, 70, 16, SuperComputerJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineRockMill.class, 62, 90, 70, 16, RockMillJeiCategory.RECIPE_TYPE);
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachinePrecAss.class, 62, 126, 70, 16, PrecAssJeiCategory.RECIPE_TYPE);
        // SILEX: matches the 1.7.10 NEI transfer rect of SILEXRecipeHandler: new Rectangle(39, 60, 60, 50)
        // (NEI's RecipeTransferRectHandler clicks compare against mouse - gui offset {5, 11},
        //  so the raw GUI-local region is 39+5, 60+11, 60, 50 — the laser chamber graphic.)
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineSilex.class, 44, 71, 60, 50, SilexJeiCategory.RECIPE_TYPE);
        // Radiolysis: matches 1.7.10 NEI transfer rect: new Rectangle(66, 25, 25, 14)
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineRadiolysis.class, 66, 25, 25, 14, RadiolysisJeiCategory.RECIPE_TYPE);
        // Coker: matches the 1.7.10 NEI transfer rect of CokingHandler: new Rectangle(55, 15, 36, 18) -> GUIMachineCoker
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachineCoker.class, 55, 15, 36, 18, CokerJeiCategory.RECIPE_TYPE);
        // Pyrolysis oven: matches the 1.7.10 NEI transfer rect of PyroHandler: new Rectangle(50, 35, 28, 14) on GUIPyroOven
        registration.addRecipeClickArea(com.hbm_m.inventory.gui.GUIMachinePyroOven.class, 50, 35, 28, 14, PyroOvenJeiCategory.RECIPE_TYPE);
        // Rotary Furnace — orig. Transfer-Rect RotaryFurnaceHandler: new Rectangle(58, 19, 32, 10) on GUIMachineRotaryFurnace;
        // NEI testet Klicks gegen Maus - guiOffset {5, 11} (RecipeInfo.getGuiOffset-Default), daher liegt die
        // effektive GUI-lokale Zone bei (63, 30) — genau auf dem Fortschrittsbalken des Original- und Port-GUI
        // (drawTexturedModalRect(guiLeft + 63, guiTop + 30, 176, 0, p, 10)).
        registration.addRecipeClickArea(GUIMachineRotaryFurnace.class, 63, 30, 32, 10, RotaryFurnaceJeiCategory.RECIPE_TYPE);

        NeiPortJeiCategories.registerGuiHandlers(registration);
    }

    @Override
    public void registerRecipeTransferHandlers(@Nonnull mezz.jei.api.registration.IRecipeTransferRegistration registration) {
        // AnvilOverlayHandler (ориг. API.registerGuiOverlayHandler(GUIAnvil.class, ..., "ntmAnvil")):
        // выбор construction-рецепта при открытом GUIAnvil фокусирует список рецептов в GUI
        // (focusRecipe), без переноса предметов. Smithing в оригинале overlay-хендлера не имеет.
        registration.addUniversalRecipeTransferHandler(new AnvilOverlayHandler());
    }

    @Override
    public void registerItemSubtypes(@Nonnull ISubtypeRegistration registration) {
        // Fluessigkeitsbehaelter: je Fluessigkeit ein eigener Eintrag (Original: Metadaten)
        for (var sup : java.util.List.of(ModItems.FLUID_TANK_FULL, ModItems.FLUID_TANK_LEAD_FULL, ModItems.FLUID_BARREL_FULL, ModItems.FLUID_PACK_FULL,
                ModItems.CANISTER_FULL, ModItems.GAS_FULL, ModItems.DISPERSER_CANISTER, ModItems.GLYPHID_GLAND)) {
            subtype(registration, sup.get(), (stack, ctx) -> {
                ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(com.hbm_m.item.liquids.ItemFluidTank.getFluid(stack));
                return fluidId != null ? fluidId.toString() : "none";
            });
        }

        subtype(registration,
            ModItems.FLUID_IDENTIFIER.get(),
            (stack, ctx) -> {
                String f1 = FluidIdentifierItem.getTypeName(stack, true);
                String f2 = FluidIdentifierItem.getTypeName(stack, false);
                return f1 + ";" + f2;
            }
        );

        subtype(registration,
            ModItems.ASSEMBLY_TEMPLATE.get(),
            (stack, ctx) -> {
                ItemStack output = ItemAssemblyTemplate.getRecipeOutput(stack);
                if (output.isEmpty()) return "empty";
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(output.getItem());
                return (itemId != null ? itemId.toString() : "unknown") +
                    (StackNbt.has(output) ? StackNbt.read(output).toString() : "");
            }
        );

        // Rad Absorber subtypes by tier
        subtype(registration,
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

        // RBMK Pellets: 10 states (depletion 0..4, + xenon 5..9)
        for (com.hbm_m.item.rbmk.RBMKPelletItem pellet : com.hbm_m.item.rbmk.RBMKPelletItem.pellets) {
            subtype(registration, pellet, (stack, ctx) -> String.valueOf(com.hbm_m.item.rbmk.RBMKPelletItem.getState(stack)));
        }

        NeiPortJeiCategories.registerSubtypes(registration);
    }

    /** Versionsswitch: JEI 15 (1.20.1) kennt nur IIngredientSubtypeInterpreter, JEI 19+ (1.21.1) will das nicht-funktionale ISubtypeInterpreter. */
    //? if < 1.21.1 {
    static void subtype(ISubtypeRegistration registration, Item item, SubtypeFn fn) {
        registration.registerSubtypeInterpreter(item, (stack, ctx) -> fn.data(stack, ctx));
    }
    //?} elif neoforge {
    /*static void subtype(ISubtypeRegistration registration, Item item, SubtypeFn fn) {
        registration.registerSubtypeInterpreter(item, new mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter<ItemStack>() {
            @Override
            public Object getSubtypeData(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return fn.data(stack, context);
            }
            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context) {
                return fn.data(stack, context);
            }
        });
    }
    *///?}

    @FunctionalInterface
    interface SubtypeFn {
        String data(ItemStack stack, mezz.jei.api.ingredients.subtypes.UidContext context);
    }

    private void registerDuctSubtype(ISubtypeRegistration registration, RegistrySupplier<Item> ductSupplier) {
        subtype(registration,
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
