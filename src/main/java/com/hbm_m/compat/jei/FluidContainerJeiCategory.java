package com.hbm_m.compat.jei;
//? if forge || neoforge {

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.compat.jei.NeiUniversalJeiCategory.Slot;
import com.hbm_m.inventory.FluidContainerRegistry;
import com.hbm_m.inventory.FluidContainerRegistry.FluidContainer;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 1:1 {@code FluidRecipeHandler} ("Fluid Containers"): Fluessigkeit und leerer Behaelter links,
 * gefuellter Behaelter rechts, Textur {@code gui_nei_fluid.png}. Quelle ist wie im Original
 * {@code FluidContainerRegistry.allContainers}.
 */
public class FluidContainerJeiCategory implements IRecipeCategory<FluidContainer> {

    public static final RecipeType<FluidContainer> RECIPE_TYPE = RecipeType.create(RefStrings.MODID, "fluidcons", FluidContainer.class);

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_fluid.png");

    private final IDrawable background;
    private final IDrawable icon;

    public FluidContainerJeiCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()));
    }

    /** Original {@code getMachinesForRecipe}. */
    public static ItemStack[] machines() {
        return new ItemStack[] {
                new ItemStack(ModItems.FLUID_BARREL_EMPTY.get()),
                new ItemStack(ModItems.FLUID_TANK_EMPTY.get()),
                new ItemStack(ModItems.FLUID_TANK_LEAD_EMPTY.get()),
                new ItemStack(ModItems.CANISTER_EMPTY.get()),
                new ItemStack(ModItems.GAS_EMPTY.get()),
                new ItemStack(ModItems.CELL_EMPTY.get()),
                new ItemStack(ModItems.DISPERSER_CANISTER_EMPTY.get()),
                new ItemStack(ModItems.GLYPHID_GLAND_EMPTY.get())};
    }

    public static List<FluidContainer> recipes() {
        FluidContainerRegistry.init();
        List<FluidContainer> list = new ArrayList<>();
        for (FluidContainer con : FluidContainerRegistry.allContainers) if (con != null) list.add(con);
        return list;
    }

    @Override public RecipeType<FluidContainer> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatableWithFallback("jei.hbm_m.fluidcons", "Fluid Containers"); }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return 166; }
    @Override public int getHeight() { return 65; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FluidContainer recipe, IFocusGroup focuses) {
        NeiUniversalJeiCategory.addSlot(builder, RecipeIngredientRole.INPUT, 30, 24, Slot.fluid(recipe.type(), recipe.content()), null);
        if (recipe.emptyContainer() != null && !recipe.emptyContainer().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 48, 24).addItemStack(recipe.emptyContainer());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 24).addItemStack(recipe.fullContainer());
    }
}
//?} else {
/*public final class FluidContainerJeiCategory {
    private FluidContainerJeiCategory() {}
}*///?}
