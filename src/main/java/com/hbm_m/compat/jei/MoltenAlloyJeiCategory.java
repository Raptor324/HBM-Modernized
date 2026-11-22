package com.hbm_m.compat.jei;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoltenAlloyRecipe;

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

@OnlyIn(Dist.CLIENT)
/**
 * Сплавление в тигле ({@code hbm_m:molten_alloy}) — 1:1 порт NEI-хендлера {@code CrucibleAlloyingHandler}:
 * <ul>
 *   <li>фон — {@code textures/gui/nei/gui_nei_crucible.png}, регион (5, 11) 166×65;</li>
 *   <li>входы — сетка 3 в ширину (12 + i%3*18, 6 + i/3*18), выходы — (102 + i%3*18, 6 + i/3*18),
 *       тигель-катализатор — (75, 42) — все позиции взяты из оригинального {@code RecipeSet};</li>
 *   <li>расплавленные материалы показываются как жидкий лом ({@code ItemScraps.create(ms, true)}),
 *       как в оригинале; для материалов без представления в Mats (alloy, nickel) — цветной свотч.</li>
 * </ul>
 */
public class MoltenAlloyJeiCategory implements IRecipeCategory<MoltenAlloyRecipe> {

    public static final RecipeType<MoltenAlloyRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "molten_alloy", MoltenAlloyRecipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_crucible.png");

    /** Сетки оригинального RecipeSet: входы (12, 6), выходы (102, 6), тигель (75, 42), шаг 18. */
    static final int INPUT_X    = 12;
    static final int INPUT_Y    = 6;
    static final int OUTPUT_X   = 102;
    static final int OUTPUT_Y   = 6;
    static final int CRUCIBLE_X = 75;
    static final int CRUCIBLE_Y = 42;

    public static final int RECIPE_WIDTH  = 166;
    public static final int RECIPE_HEIGHT = 65;

    private final IDrawable background;
    private final IDrawable icon;

    public MoltenAlloyJeiCategory(IGuiHelper guiHelper) {
        // В 1.7.10 NEI TemplateRecipeHandler.drawBackground рисует текстуру в регионе (5, 11) 166×65.
        this.background = guiHelper.createDrawable(TEXTURE, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    @Override public RecipeType<MoltenAlloyRecipe> getRecipeType() { return RECIPE_TYPE; }

    // Оригинальное имя хендлера: "Crucible Alloying" (ключ уже есть в обоих lang-файлах).
    @Override public Component getTitle() { return Component.translatable("jei.hbm_m.crucible_alloying"); }

    @Override public int getWidth() { return RECIPE_WIDTH; }

    @Override public int getHeight() { return RECIPE_HEIGHT; }

    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }

    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MoltenAlloyRecipe recipe, IFocusGroup focuses) {
        // Расплавы — не JEI-ингредиенты; показываем их как жидкий лом (как в оригинале).
        // Материалы без представления в Mats слота не получают — свотчи рисуются в draw().
        addGrid(builder, RecipeIngredientRole.INPUT, recipe.getInputs(), INPUT_X, INPUT_Y);
        addGrid(builder, RecipeIngredientRole.OUTPUT, recipe.getOutputs(), OUTPUT_X, OUTPUT_Y);

        // Единственный предмет на экране — сам тигель (оригинал @ (75, 42)).
        builder.addSlot(RecipeIngredientRole.CATALYST, CRUCIBLE_X, CRUCIBLE_Y)
                .addItemStack(new ItemStack(ModBlocks.CRUCIBLE.get()));
    }

    /** Сетка 3 в ширину, шаг 18 — координаты оригинального RecipeSet. */
    private static void addGrid(IRecipeLayoutBuilder builder, RecipeIngredientRole role,
                                MaterialStack[] stacks, int baseX, int baseY) {
        for (int i = 0; i < stacks.length; i++) {
            MaterialStack stack = stacks[i];
            if (stack == null || stack.isEmpty()) continue;
            ItemStack scrap = JeiMoltenRendering.liquidScrap(stack);
            if (!scrap.isEmpty()) {
                builder.addSlot(role, baseX + (i % 3) * 18, baseY + (i / 3) * 18).addItemStack(scrap);
            }
        }
    }

    @Override
    public void draw(MoltenAlloyRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        drawFallback(graphics, recipe.getInputs(), INPUT_X, INPUT_Y);
        drawFallback(graphics, recipe.getOutputs(), OUTPUT_X, OUTPUT_Y);
    }

    /** Свотч-fallback для материалов без представления в Mats (alloy, nickel) — на позицию в сетке, где слот не встал. */
    private static void drawFallback(GuiGraphics graphics, MaterialStack[] stacks, int baseX, int baseY) {
        for (int i = 0; i < stacks.length; i++) {
            MaterialStack stack = stacks[i];
            if (stack == null || stack.isEmpty()) continue;
            if (JeiMoltenRendering.liquidScrap(stack).isEmpty()) {
                int x = baseX + (i % 3) * 18;
                int y = baseY + (i / 3) * 18;
                JeiMoltenRendering.drawSwatch(graphics, x + 1, y + 1, stack);
                JeiMoltenRendering.drawLabel(graphics, x, y + 17, stack);
            }
        }
    }
}
