package com.hbm_m.compat.jei;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.client.overlay.GUIMachineCentrifuge;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CentrifugeRecipes;
import com.hbm_m.recipe.CentrifugeRecipes.RecipeInput;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(CentrifugeJeiCategory.RECIPE_TYPE, getCentrifugeRecipes());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CENTRIFUGE.get()), CentrifugeJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Matches the old NEI transfer rect: new Rectangle(56, 0, 80, 38)
        registration.addRecipeClickArea(GUIMachineCentrifuge.class, 56, 0, 80, 38, CentrifugeJeiCategory.RECIPE_TYPE);
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
}
