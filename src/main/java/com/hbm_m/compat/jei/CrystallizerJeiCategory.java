package com.hbm_m.compat.jei;

import java.util.HashSet;
import java.util.Set;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrystallizerRecipe;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.RegistrySupplier;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * JEI-порт 1.7.10 {@code CrystallizerRecipeHandler} (подкласс {@code NEIUniversalHandler}).
 *
 * <p>Экран 1:1 с оригинальным NEI: общий фон {@code gui_nei.png} (166×65), кислота в слоте (48,24),
 * предметный вход (30,24) — как {@code getInputCoords(2)} для ключа рецепта
 * {@code {иконка кислоты, предмет}}, выход (102,24), блок-машина в высокой рамке (74,14, 18×36)
 * на (75,31) — как {@code RecipeSet.machinePositioned}. {@code drawExtras} оригинала рисует
 * "Effectiveness: +N% per level" при {@code productivity > 0}; строка в оригинале захардкожена
 * и не переводилась, поэтому здесь тоже литерал.</p>
 */
@OnlyIn(Dist.CLIENT)
public class CrystallizerJeiCategory implements IRecipeCategory<CrystallizerRecipe> {

    public static final RecipeType<CrystallizerRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crystallizer", CrystallizerRecipe.class);

    /** Оригинал: CrystallizerRecipes.getRecipes() пропускал scrap_plastic (CrystallizerRecipes.java:306). */
    private static final Set<Item> HIDDEN_INPUTS = buildHiddenInputs();

    private static Set<Item> buildHiddenInputs() {
        Set<Item> items = new HashSet<>();
        items.add(ModItems.scrapPlastic(ModItems.SCRAP_PLASTIC_TYPES.get(0)));
        for (RegistrySupplier<Item> variant : ModItems.SCRAP_PLASTIC_VARIANTS.values()) {
            items.add(variant.get());
        }
        return items;
    }

    /** Фильтр отображения: машина рецепты обрабатывает, но в JEI они скрыты, как в оригинале. */
    public static boolean isVisible(CrystallizerRecipe recipe) {
        for (ItemStack stack : recipe.getInput().getItems()) {
            if (HIDDEN_INPUTS.contains(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    private final IDrawable background;
    private final IDrawable itemSlotBackground;
    private final IDrawable icon;

    public CrystallizerJeiCategory(IGuiHelper guiHelper) {
        this.background = JeiNeiTextures.createRecipeBackground(guiHelper);
        this.itemSlotBackground = JeiNeiTextures.createItemSlotBackground(guiHelper);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.CRYSTALLIZER.get()));
    }

    @Override public RecipeType<CrystallizerRecipe> getRecipeType() { return RECIPE_TYPE; }
    @Override public Component getTitle() { return Component.translatable("container.hbm_m.crystallizer"); }
    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }
    @Override public IDrawable getIcon() { return icon; }
    @Override public int getWidth() { return JeiNeiTextures.RECIPE_WIDTH; }
    @Override public int getHeight() { return JeiNeiTextures.RECIPE_HEIGHT; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrystallizerRecipe recipe, IFocusGroup focuses) {
        // Оригинал: ключ рецепта = {иконка кислоты, предмет} → getInputCoords(2) = {(48,24), (30,24)},
        // т.е. кислота в слоте у машины, предметный вход левее. Выход — getOutputCoords(1) = (102,24).
        FluidStack acid = recipe.getAcid();
        if (acid != null && !acid.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 48, 24)
                    .setBackground(itemSlotBackground, -1, -1)
                    .setCustomRenderer(JeiTypes.FLUID, new HbmFluidJeiRenderer(16, 16))
                    .addFluidStack(acid.getFluid(), acid.getAmount());
        }

        // Оригинал: getRecipes() копирует вход с itemAmount (CrystallizerRecipes.java:301/305),
        // т.е. NEI рисует требуемое количество (bedrock 4×, crumbs 64×). Здесь то же —
        // addCountedIngredient со счётчиком рецепта (как в ArcFurnaceJeiCategory).
        var inputSlot = builder.addSlot(RecipeIngredientRole.INPUT, 30, 24)
                .setBackground(itemSlotBackground, -1, -1);
        JeiIngredientSlots.addCountedIngredient(inputSlot, recipe.getInput(), recipe.getInputCount());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 102, 24)
                .setBackground(itemSlotBackground, -1, -1)
                .addItemStack(recipe.getOutput());

        // Машина — как machinePositioned в RecipeSet (75, 31); рамка рисуется в draw().
        builder.addSlot(RecipeIngredientRole.CATALYST, 75, 31)
                .addItemStack(new ItemStack(ModBlocks.CRYSTALLIZER.get()));
    }

    @Override
    public void draw(CrystallizerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // Оригинал drawBackground: drawTexturedModalRect(74, 14, 59, 87, 18, 36)
        JeiNeiRendering.drawMachineSlot(guiGraphics, 0, false);

        // Оригинал drawExtras: строка эффективности при productivity > 0, проценты порезаны до 99.
        float productivity = recipe.getProductivity();
        if (productivity > 0F) {
            String effectiveness = "Effectiveness: +" + Math.min((int) (productivity * 100), 99) + "% per level";
            guiGraphics.drawString(Minecraft.getInstance().font, effectiveness, 8, 52, 0x404040, false);
        }
    }
}
