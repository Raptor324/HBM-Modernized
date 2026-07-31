package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
import com.hbm_m.inventory.recipes.ArcWelderRecipes;
import com.hbm_m.inventory.recipes.SolderingRecipes;
import com.hbm_m.item.BlockAbsorberItem;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.AnvilRecipe;
import com.hbm_m.recipe.AnvilRecipeManager;
import com.hbm_m.recipe.AssemblerRecipe;
import com.hbm_m.recipe.PressRecipe;
import com.hbm_m.recipe.ShredderRecipe;
import com.hbm_m.recipe.BlastFurnaceRecipe;
import com.hbm_m.recipe.CrucibleAlloyingRecipe;
import com.hbm_m.recipe.CrucibleAlloyingRecipes;
import com.hbm_m.recipe.CrucibleMoldRecipes;
import com.hbm_m.recipe.CrucibleRecipes;
import com.hbm_m.recipe.CrucibleSmeltingRecipes;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidDuctItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipes;
import com.hbm_m.recipe.CentrifugeRecipes.RecipeInput;
import com.hbm_m.recipe.ChemicalPlantRecipe;
import com.hbm_m.recipe.CyclotronRecipes;
import com.hbm_m.recipe.PressRecipe;

import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
        registration.addRecipeCategories(new CrucibleCastingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrucibleAlloyingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrucibleSmeltingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ArcWelderJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new SolderingStationJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrystallizerJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new PressJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ShredderJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new BlastFurnaceJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new GasCentrifugeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@Nonnull IRecipeRegistration registration) {
        CrucibleRecipes.INSTANCE.registerDefaults();
        ensureCrucibleFallbackRecipes();
        registration.addRecipes(AnvilJeiCategory.RECIPE_TYPE, getAnvilRecipes());
        registration.addRecipes(AssemblerJeiCategory.RECIPE_TYPE, getAssemblerRecipes());
        registration.addRecipes(CentrifugeJeiCategory.RECIPE_TYPE, getCentrifugeRecipes());
        registration.addRecipes(ChemicalPlantJeiCategory.RECIPE_TYPE, getChemicalPlantRecipes());
        registration.addRecipes(CyclotronJeiCategory.RECIPE_TYPE, getCyclotronRecipes());
        registration.addRecipes(CrucibleCastingJeiCategory.RECIPE_TYPE, getCrucibleCastingRecipes());
        registration.addRecipes(CrucibleAlloyingJeiCategory.RECIPE_TYPE, getCrucibleAlloyingRecipes());
        registration.addRecipes(CrucibleSmeltingJeiCategory.RECIPE_TYPE, getCrucibleSmeltingRecipes());
        registration.addRecipes(ArcWelderJeiCategory.RECIPE_TYPE, ArcWelderJeiRecipe.fromRecipes());
        registration.addRecipes(SolderingStationJeiCategory.RECIPE_TYPE, SolderingStationJeiRecipe.fromRecipes());
        registration.addRecipes(CrystallizerJeiCategory.RECIPE_TYPE, CrystallizerJeiRecipe.fromAll());
        registration.addRecipes(PressJeiCategory.RECIPE_TYPE, getPressRecipes());
        registration.addRecipes(ShredderJeiCategory.RECIPE_TYPE, getShredderRecipes());
        registration.addRecipes(BlastFurnaceJeiCategory.RECIPE_TYPE, getBlastFurnaceRecipes());
        registration.addRecipes(GasCentrifugeJeiCategory.RECIPE_TYPE, GasCentrifugeJeiCategory.getDefaultRecipes());
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
        // Foundry basin is the primary catalyst; mold and strand caster are registered once those blocks are ported
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()), CrucibleCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleAlloyingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleSmeltingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.ARC_WELDER.get()), ArcWelderJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SOLDERING_STATION.get()), SolderingStationJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRYSTALLIZER.get()), CrystallizerJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.PRESS.get()), PressJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.SHREDDER.get()), ShredderJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.BLAST_FURNACE.get()), BlastFurnaceJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GAS_CENTRIFUGE.get()), GasCentrifugeJeiCategory.RECIPE_TYPE);
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
        // Crucible casting — matches the legacy NEI transfer rect: new Rectangle(65, 23, 36, 18)
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleCastingJeiCategory.RECIPE_TYPE);
        // Crucible alloying — same click zone on the crucible GUI
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleAlloyingJeiCategory.RECIPE_TYPE);
        // Crucible smelting — same click zone on the crucible GUI
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
                    (output.hasTag() ? output.getTag().toString() : "");
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

    private static List<AnvilRecipe> getAnvilRecipes() {
        return AnvilRecipeManager.getClientRecipes();
    }

    private static List<AssemblerRecipe> getAssemblerRecipes() {
        if (net.minecraft.client.Minecraft.getInstance().level == null) {
            return List.of();
        }

        return net.minecraft.client.Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(AssemblerRecipe.Type.INSTANCE);
    }

    private static List<PressRecipe> getPressRecipes() {
        if (net.minecraft.client.Minecraft.getInstance().level == null) {
            return List.of();
        }

        return net.minecraft.client.Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(PressRecipe.Type.INSTANCE);
    }

    private static List<CentrifugeJeiCategory.Recipe> getCentrifugeRecipes() {
        List<CentrifugeJeiCategory.Recipe> recipes = new ArrayList<>();

        Map<RecipeInput, ItemStack[]> allRecipes = CentrifugeRecipes.getAllRecipes();

        for (Map.Entry<RecipeInput, ItemStack[]> entry : allRecipes.entrySet()) {
            RecipeInput input = entry.getKey();
            if (!input.getDisplayStacks().isEmpty()) {
                recipes.add(new CentrifugeJeiCategory.Recipe(input, entry.getValue()));
            }
        }

        return recipes;
    }

    private static List<ChemicalPlantRecipe> getChemicalPlantRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        return mc.level.getRecipeManager().getAllRecipesFor(ChemicalPlantRecipe.Type.INSTANCE);
    }

    private static List<CyclotronJeiRecipe> getCyclotronRecipes() {
        CyclotronRecipes.registerRecipes();
        List<CyclotronJeiRecipe> recipes = new ArrayList<>();
        for (CyclotronRecipes.JeiRecipe recipe : CyclotronRecipes.getJeiRecipes()) {
            ItemStack output = recipe.output();
            if (output.isEmpty()) {
                continue;
            }

            ItemStack[] targets = recipe.target().getItems();
            ItemStack[] inputs = recipe.input().getItems();
            if (targets.length == 0 || inputs.length == 0) {
                continue;
            }

            recipes.add(CyclotronJeiRecipe.of(recipe.target(), recipe.input(), output, recipe.amatProduced()));
        }
        return recipes;
    }

    private static List<CrucibleCastingJeiRecipe> getCrucibleCastingRecipes() {
        List<CrucibleCastingJeiRecipe> recipes = new ArrayList<>();
        for (ItemStack[] r : CrucibleMoldRecipes.getMoldRecipes()) {
            // r[0]=material, r[1]=mold, r[2]=unused, r[3]=output
            recipes.add(new CrucibleCastingJeiRecipe(r[0], r[1], r[3]));
        }
        return recipes;
    }

    private static List<CrucibleAlloyingJeiRecipe> getCrucibleAlloyingRecipes() {
        List<CrucibleAlloyingJeiRecipe> recipes = new ArrayList<>();
        for (CrucibleAlloyingRecipe r : CrucibleAlloyingRecipes.getRecipes()) {
            recipes.add(new CrucibleAlloyingJeiRecipe(r));
        }
        return recipes;
    }

    private static List<CrucibleSmeltingJeiRecipe> getCrucibleSmeltingRecipes() {
        List<CrucibleSmeltingJeiRecipe> result = new ArrayList<>();
        for (CrucibleSmeltingRecipes.SmeltingEntry e : CrucibleSmeltingRecipes.getRecipes()) {
            // Show ingredient items → material name as a pseudo-output via cast plate if available
            List<ItemStack> inputs = List.of(e.input().getItems());
            var plate = e.output().hasCastPlate() ? e.output().getCastPlate(1) : null;
            List<ItemStack> outputs = plate != null ? List.of(plate) : List.of();
            result.add(new CrucibleSmeltingJeiRecipe(
                    inputs.isEmpty() ? ItemStack.EMPTY : inputs.get(0), outputs));
        }
        return result;
    }

    private static List<ShredderRecipe> getShredderRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        return mc.level.getRecipeManager().getAllRecipesFor(ShredderRecipe.Type.INSTANCE);
    }

    private static List<BlastFurnaceRecipe> getBlastFurnaceRecipes() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return List.of();
        }
        return mc.level.getRecipeManager().getAllRecipesFor(BlastFurnaceRecipe.Type.INSTANCE);
    }

    private static void ensureCrucibleFallbackRecipes() {
        if (CrucibleSmeltingRecipes.getRecipes().isEmpty()) {
            CrucibleSmeltingRecipes.registerDefaults();
        }
        if (CrucibleAlloyingRecipes.getRecipes().isEmpty()) {
            CrucibleAlloyingRecipes.register(new CrucibleAlloyingRecipe("crucible.jei_fallback")
                    .setup(1, new ItemStack(Items.IRON_INGOT))
                    .inputs(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.COAL))
                    .outputs(new ItemStack(Items.IRON_NUGGET, 3)));
        }
        if (CrucibleMoldRecipes.getMoldRecipes().isEmpty()) {
            CrucibleMoldRecipes.register(new ItemStack(Items.CLAY_BALL), new ItemStack(Items.BRICK), new ItemStack(Items.FLOWER_POT));
        }
    }
}
//?} else {
/*public final class HbmJeiPlugin {
    private HbmJeiPlugin() {}
}*///?}
