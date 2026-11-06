package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.platform.recipe.RecipeHooks;
import com.hbm_m.recipe.RadGenRecipe;

import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Дополнительные простые JEI-категории для машин, не имеющих выделенных классов категорий
 * (остальные машины зарегистрированы в {@link HbmJeiPlugin} или {@link NeiPortJeiCategories}).
 */
public final class JeiExtraCategories {

    private JeiExtraCategories() {}

    public static final RecipeType<RadGenRecipe> RADGEN =
            RecipeType.create(RefStrings.MODID, "radgen", RadGenRecipe.class);

    private static ItemStack machine(net.minecraft.world.level.block.Block block) {
        return new ItemStack(block);
    }

    private static List<ItemStack> items(ItemStack... stacks) {
        List<ItemStack> list = new ArrayList<>(stacks.length);
        for (ItemStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) list.add(stack);
        }
        return list;
    }

    public static List<IRecipeCategory<?>> create(IGuiHelper helper) {
        List<IRecipeCategory<?>> out = new ArrayList<>();

        out.add(new JeiSimpleMachineCategory<>(helper, RADGEN, "block.hbm_m.radgen",
                machine(ModBlocks.RADGEN.get()), new JeiMachineView<RadGenRecipe>() {
            @Override public List<ItemInput> itemInputs(RadGenRecipe r) {
                return List.of(new ItemInput(r.getInput(), 1));
            }
            @Override public List<ItemStack> itemOutputs(RadGenRecipe r) { return items(r.getOutput()); }
            @Override public List<Component> notes(RadGenRecipe r) {
                return List.of(Component.literal(r.getPower() + " HE/t, " + r.getDuration() + " t"));
            }
        }).withNotes(1));

        return out;
    }

    public static void registerRecipes(IRecipeRegistration registration, Level level) {
        registration.addRecipes(RADGEN, RecipeHooks.getAllRecipes(level, RadGenRecipe.Type.INSTANCE));
    }

    public static void registerCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(machine(ModBlocks.RADGEN.get()), RADGEN);
    }
}
