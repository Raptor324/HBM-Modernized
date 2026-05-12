package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.gui.GUIMachineCentrifuge;
import com.hbm_m.inventory.gui.GUIMachineChemicalPlant;
import com.hbm_m.inventory.gui.GUIMachineCrucible;
import com.hbm_m.item.ModItems;
import com.hbm_m.recipe.CrucibleAlloyingRecipe;
import com.hbm_m.recipe.CrucibleAlloyingRecipes;
import com.hbm_m.recipe.CrucibleMoldRecipes;
import com.hbm_m.recipe.CrucibleRecipes;
import com.hbm_m.recipe.CrucibleSmeltingRecipes;
import com.hbm_m.item.industrial.ItemAssemblyTemplate;
import com.hbm_m.item.liquids.FluidBarrelItem;
import com.hbm_m.item.liquids.FluidIdentifierItem;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipes;
import com.hbm_m.recipe.CentrifugeRecipes.RecipeInput;
import com.hbm_m.recipe.ChemicalPlantRecipes;
import com.hbm_m.recipe.ChemicalPlantRecipes.ChemicalRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class HbmJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CentrifugeJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ChemicalPlantJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrucibleCastingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrucibleAlloyingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CrucibleSmeltingJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        CrucibleRecipes.INSTANCE.registerDefaults();
        ensureCrucibleFallbackRecipes();
        registration.addRecipes(CentrifugeJeiCategory.RECIPE_TYPE, getCentrifugeRecipes());
        registration.addRecipes(ChemicalPlantJeiCategory.RECIPE_TYPE, getChemicalPlantRecipes());
        registration.addRecipes(CrucibleCastingJeiCategory.RECIPE_TYPE, getCrucibleCastingRecipes());
        registration.addRecipes(CrucibleAlloyingJeiCategory.RECIPE_TYPE, getCrucibleAlloyingRecipes());
        registration.addRecipes(CrucibleSmeltingJeiCategory.RECIPE_TYPE, getCrucibleSmeltingRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CENTRIFUGE.get()), CentrifugeJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CHEMICAL_PLANT.get()), ChemicalPlantJeiCategory.RECIPE_TYPE);
        // Foundry basin is the primary catalyst; mold and strand caster are registered once those blocks are ported
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FOUNDRY_BASIN.get()), CrucibleCastingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleAlloyingJeiCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CRUCIBLE.get()), CrucibleSmeltingJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Matches the old NEI transfer rect: new Rectangle(56, 0, 80, 38)
        registration.addRecipeClickArea(GUIMachineCentrifuge.class, 56, 0, 80, 38, CentrifugeJeiCategory.RECIPE_TYPE);
        // Chemical Plant click area around the progress bar
        registration.addRecipeClickArea(GUIMachineChemicalPlant.class, 62, 126, 70, 16, ChemicalPlantJeiCategory.RECIPE_TYPE);
        // Crucible casting — matches the legacy NEI transfer rect: new Rectangle(65, 23, 36, 18)
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleCastingJeiCategory.RECIPE_TYPE);
        // Crucible alloying — same click zone on the crucible GUI
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleAlloyingJeiCategory.RECIPE_TYPE);
        // Crucible smelting — same click zone on the crucible GUI
        registration.addRecipeClickArea(GUIMachineCrucible.class, 65, 23, 36, 18, CrucibleSmeltingJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.registerSubtypeInterpreter(
            ModItems.FLUID_BARREL.get(),
            (stack, ctx) -> {
                FluidStack fluid = FluidBarrelItem.getFluid(stack);
                if (fluid.isEmpty()) return "empty";
                ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(fluid.getFluid());
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
                ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(output.getItem());
                return (itemId != null ? itemId.toString() : "unknown") +
                    (output.hasTag() ? output.getTag().toString() : "");
            }
        );
    }

    private static List<CentrifugeJeiRecipe> getCentrifugeRecipes() {
        List<CentrifugeJeiRecipe> recipes = new ArrayList<>();
        
        Map<RecipeInput, ItemStack[]> allRecipes = CentrifugeRecipes.getAllRecipes();
        
        for (Map.Entry<RecipeInput, ItemStack[]> entry : allRecipes.entrySet()) {
            RecipeInput input = entry.getKey();
            ItemStack[] outputs = entry.getValue();
            
            List<ItemStack> inputStacks = input.getDisplayStacks();
            if (!inputStacks.isEmpty()) {
                recipes.add(new CentrifugeJeiRecipe(inputStacks, outputs));
            }
        }
        
        return recipes;
    }

    private static List<ChemicalPlantJeiRecipe> getChemicalPlantRecipes() {
        List<ChemicalPlantJeiRecipe> recipes = new ArrayList<>();
        
        for (ChemicalRecipe recipe : ChemicalPlantRecipes.getRecipeList()) {
            recipes.add(new ChemicalPlantJeiRecipe(recipe));
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
        List<CrucibleSmeltingJeiRecipe> recipes = new ArrayList<>();
        for (var entry : CrucibleSmeltingRecipes.getRecipes().entrySet()) {
            recipes.add(new CrucibleSmeltingJeiRecipe(entry.getKey(), entry.getValue()));
        }
        return recipes;
    }

    private static void ensureCrucibleFallbackRecipes() {
        if (CrucibleSmeltingRecipes.getRecipes().isEmpty()) {
            CrucibleSmeltingRecipes.register(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_INGOT));
            CrucibleSmeltingRecipes.register(new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_INGOT));
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
