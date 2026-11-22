package com.hbm_m.compat.jei;

import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.CrucibleSmeltingRecipe;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI-категория тигель-плавки ({@code hbm_m:crucible_smelting}) — 1:1 порт NEI-хендлера
 * {@code CrucibleSmeltingHandler}:
 * <ul>
 *   <li>фон — {@code textures/gui/nei/gui_nei_crucible_smelting.png}, регион (5, 11) 166×65 — так его
 *       рисует дефолтный {@code TemplateRecipeHandler.drawBackground} оригинала;</li>
 *   <li>вход — плавимый предмет @ (48, 24); тигель-катализатор — @ (75, 42);</li>
 *   <li>выходы — расплавленные материалы ({@link CrucibleSmeltingRecipe#getOutputs()}) в сетке 3 в ширину
 *       (102 + i%3*18, 6 + i/3*18) — координаты оригинального {@code RecipeSet}; показываются как жидкий лом
 *       ({@code ItemScraps.create(ms, true)}, как в оригинале), для материалов без представления в Mats
 *       (alloy, nickel) — цветной свотч.</li>
 * </ul>
 * Transfer rect оригинала: Rectangle(65, 23, 36, 18) в рецепт-координатах NEI — клик-зона в
 * {@code HbmJeiPlugin} зарегистрирована GUI-локально как (70, 34, 36, 18) (+5/+11 guiOffset).
 */
public class CrucibleSmeltingJeiCategory implements IRecipeCategory<CrucibleSmeltingRecipe> {

    public static final RecipeType<CrucibleSmeltingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "crucible_smelting", CrucibleSmeltingRecipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_crucible_smelting.png");

    static final int INPUT_X     = 48;
    static final int INPUT_Y     = 24;
    static final int CRUCIBLE_X  = 75;
    static final int CRUCIBLE_Y  = 42;
    /** Сетка выходов оригинального RecipeSet: 3 в ширину, шаг 18, начало (102, 6). */
    static final int OUTPUT_X    = 102;
    static final int OUTPUT_Y    = 6;

    public static final int RECIPE_WIDTH  = 166;
    public static final int RECIPE_HEIGHT = 65;

    private final IDrawable background;
    private final IDrawable icon;

    public CrucibleSmeltingJeiCategory(IGuiHelper guiHelper) {
        // В 1.7.10 NEI TemplateRecipeHandler.drawBackground рисует текстуру в регионе (5, 11) 166×65.
        this.background = guiHelper.createDrawable(TEXTURE, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override
    public RecipeType<CrucibleSmeltingRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm_m.crucible_smelting");
    }

    @Override
    public int getWidth() { return RECIPE_WIDTH; }

    @Override
    public int getHeight() { return RECIPE_HEIGHT; }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, CrucibleSmeltingRecipe recipe, IFocusGroup focuses) {
        // Вход — ингредиент (предмет, который плавится); оригинал: PositionedStack input @ (48, 24).
        builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y)
                .addIngredients(recipe.getInput());

        // Тигель — катализатор; оригинал: stack machine_crucible @ (75, 42).
        builder.addSlot(RecipeIngredientRole.CATALYST, CRUCIBLE_X, CRUCIBLE_Y)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));

        // Выходы — расплавленные материалы в сетке 3 в ширину (102 + i%3*18, 6 + i/3*18),
        // как выходы оригинального RecipeSet — жидкий лом. Материалы без представления в Mats
        // (alloy, nickel) слота не получают — свотч рисуется в draw().
        List<MaterialStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            MaterialStack stack = outputs.get(i);
            if (stack == null || stack.isEmpty()) continue;
            ItemStack scrap = JeiMoltenRendering.liquidScrap(stack);
            if (!scrap.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X + (i % 3) * 18, OUTPUT_Y + (i / 3) * 18)
                        .addItemStack(scrap);
            }
        }
    }

    @Override
    public void draw(CrucibleSmeltingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        // Свотч-fallback для материалов без представления в Mats (alloy, nickel) — на позицию
        // в сетке выходов, где слот не встал; подпись количества — под свотчем.
        List<MaterialStack> outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            MaterialStack stack = outputs.get(i);
            if (stack == null || stack.isEmpty()) continue;
            if (JeiMoltenRendering.liquidScrap(stack).isEmpty()) {
                int x = OUTPUT_X + (i % 3) * 18;
                int y = OUTPUT_Y + (i / 3) * 18;
                JeiMoltenRendering.drawSwatch(graphics, x + 1, y + 1, stack);
                JeiMoltenRendering.drawLabel(graphics, x, y + 17, stack);
            }
        }
    }

    /**
     * Фильтр отображения = правила набора оригинального NEI
     * ({@code CrucibleRecipes.getSmeltingRecipes}):
     * <ul>
     *   <li>форма выдавалась только при существующем оредикт-ингредиенте
     *       (CrucibleRecipes.java:210-219 {@code "if(!ores.isEmpty())"}) — в порту этому
     *       соответствуют рецепты с тегом, который не разрешается ни в предмет
     *       ({@code Ingredient#getItems()} пуст, например {@code forge:ingots/abronze},
     *       {@code forge:ingots/nickel});</li>
     *   <li>каждый выход был {@code ItemScraps.create(ms, true)} материала Mats — записей с
     *       материалами вне Mats (alloy, nickel; Mats.java:155 оригинала MAT_ALLOY закомментирован,
     *       MAT_NICKEL не существует) в оригинальном наборе не было, а JEI рисовал бы вместо
     *       лома цветной свотч (см. {@link JeiMoltenRendering}).</li>
     * </ul>
     * Машина такие рецепты по-прежнему обрабатывает — скрыт только показ, как у Crystallizer.
     */
    public static boolean isVisible(CrucibleSmeltingRecipe recipe) {
        if (recipe.getInput().getItems().length == 0) return false;
        for (MaterialStack stack : recipe.getOutputs()) {
            if (stack == null || stack.isEmpty()) return false;
            if (JeiMoltenRendering.liquidScrap(stack).isEmpty()) return false;
        }
        return true;
    }
}
