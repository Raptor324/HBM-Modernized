package com.hbm_m.compat.jei;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.inventory.material.MaterialStack;
import com.hbm_m.item.material.ItemCastMold;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.MoldCastingRecipe;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@OnlyIn(Dist.CLIENT)
/**
 * Литьё в форму ({@code hbm_m:mold_casting}) — 1:1 порт NEI-хендлера {@code CrucibleCastingHandler}:
 * <ul>
 *   <li>фон — {@code textures/gui/nei/gui_nei_foundry.png}, регион (5, 11) 166×65;</li>
 *   <li>вход — жидкий материал в размере стоимости формы (оригинал: {@code ItemScraps.create(ms, true)})
 *       @ (48, 24); форма — @ (75, 6); basin — @ (75, 42) (малая форма — {@code foundry_mold},
 *       большая — {@code foundry_basin}, как в оригинальном {@code registerMoldsForNEI}); выход — @ (102, 24);</li>
 *   <li>набор рецептов фильтруется как в оригинальном {@code registerMoldsForNEI} — пары без
 *       разрешаемого выхода и пары материалов вне Mats не регистрируются (см. {@link #filter});
 *       свотч в {@link #draw} остаётся только защитным fallback'ом.</li>
 * </ul>
 */
public class MoldCastingJeiCategory implements IRecipeCategory<MoldCastingRecipe> {

    public static final RecipeType<MoldCastingRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "mold_casting", MoldCastingRecipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_foundry.png");

    /** Позиции оригинального RecipeSet: вход (48, 24), форма (75, 6), basin (75, 42), выход (102, 24). */
    private static final int INPUT_X  = 48;
    private static final int INPUT_Y  = 24;
    private static final int MOLD_X   = 75;
    private static final int MOLD_Y   = 6;
    private static final int BASIN_X  = 75;
    private static final int BASIN_Y  = 42;
    private static final int OUTPUT_X = 102;
    private static final int OUTPUT_Y = 24;

    public static final int RECIPE_WIDTH  = 166;
    public static final int RECIPE_HEIGHT = 65;

    private static Map<ItemCastMold.MoldType, ItemStack> moldItems;

    private final IDrawable background;
    private final IDrawable icon;

    public MoldCastingJeiCategory(IGuiHelper guiHelper) {
        // В 1.7.10 NEI TemplateRecipeHandler.drawBackground рисует текстуру в регионе (5, 11) 166×65.
        this.background = guiHelper.createDrawable(TEXTURE, 5, 11, RECIPE_WIDTH, RECIPE_HEIGHT);
        this.icon = guiHelper.createDrawableIngredient(
                VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.FOUNDRY_MOLD.get()));
    }

    @Override public RecipeType<MoldCastingRecipe> getRecipeType() { return RECIPE_TYPE; }

    // Оригинальное имя хендлера: "Crucible Casting" (ключ уже есть в обоих lang-файлах).
    @Override public Component getTitle() { return Component.translatable("jei.hbm_m.crucible_casting"); }

    @Override public int getWidth() { return RECIPE_WIDTH; }

    @Override public int getHeight() { return RECIPE_HEIGHT; }

    @Override @SuppressWarnings("removal") public IDrawable getBackground() { return background; }

    @Override public IDrawable getIcon() { return icon; }

    /** Формы регистрируются поштучно; собираем соответствие тип → предмет один раз по реестру. */
    private static Map<ItemCastMold.MoldType, ItemStack> molds() {
        if (moldItems == null) {
            Map<ItemCastMold.MoldType, ItemStack> map = new EnumMap<>(ItemCastMold.MoldType.class);
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof ItemCastMold mold) {
                    map.putIfAbsent(mold.getMoldType(), new ItemStack(item));
                }
            }
            moldItems = map;
        }
        return moldItems;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MoldCastingRecipe recipe, IFocusGroup focuses) {
        // Вход — жидкий материал в размере стоимости формы; оригинал: ItemScraps.create(ms, true) @ (48, 24).
        MaterialStack molten = new MaterialStack(recipe.getMaterial(), recipe.getMold().getCost());
        ItemStack scrap = JeiMoltenRendering.liquidScrap(molten);
        if (!scrap.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, INPUT_X, INPUT_Y).addItemStack(scrap);
        }

        // Форма (оригинал @ (75, 6)).
        ItemStack mold = molds().get(recipe.getMold());
        if (mold != null && !mold.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.CATALYST, MOLD_X, MOLD_Y).addItemStack(mold);
        }

        // Basin (оригинал @ (75, 42)): малая форма — foundry_mold, большая — foundry_basin
        // (оригинал: mold.size == 0 ? foundry_mold : foundry_basin).
        builder.addSlot(RecipeIngredientRole.CATALYST, BASIN_X, BASIN_Y)
                .addItemStack(new ItemStack(recipe.getMold().getSize() == 1
                        ? ModBlocks.FOUNDRY_BASIN.get() : ModBlocks.FOUNDRY_MOLD.get()));

        // Выход (оригинал @ (102, 24)).
        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_X, OUTPUT_Y).addItemStack(recipe.getOutput());
    }

    @Override
    public void draw(MoldCastingRecipe recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        // Свотч-fallback для материалов без представления в Mats (alloy, nickel) — на позицию входа (48, 24);
        // подпись количества — на свободном левом поле панели, чтобы не задевать basin (75, 42) и выход.
        MaterialStack molten = new MaterialStack(recipe.getMaterial(), recipe.getMold().getCost());
        if (!JeiMoltenRendering.liquidScrap(molten).isEmpty()) return;
        JeiMoltenRendering.drawSwatch(graphics, INPUT_X + 1, INPUT_Y + 1, molten);
        JeiMoltenRendering.drawLabel(graphics, 6, INPUT_Y + 4, molten);
    }

    /**
     * Рецептов литья сотни; вытаскиваем их один раз для регистрации.
     *
     * <p>Фильтр = набор оригинального NEI ({@code CrucibleRecipes.registerMoldsForNEI},
     * CrucibleRecipes.java:259-262): пара попадала в хендлер только при
     * {@code mold.getOutput(material) != null}. В порту этому соответствуют два отбраковочных
     * правила:</p>
     * <ul>
     *   <li>выход не разрешается (JSON ссылается на тег, который не определяет никто —
     *       {@code getOutput()} даёт {@link ItemStack#EMPTY}; 142 из 463 сгенерированных JSON);</li>
     *   <li>материал пары без представления в Mats (alloy, nickel — Mats.java:155 оригинала
     *       MAT_ALLOY закомментирован, MAT_NICKEL не существует): оригинал всегда показывал
     *       вход как жидкий лом {@code ItemScraps.create(ms, true)}, свотчей в его наборе не было.</li>
     * </ul>
     */
    public static List<MoldCastingRecipe> filter(List<MoldCastingRecipe> all) {
        List<MoldCastingRecipe> out = new ArrayList<>(all.size());
        for (MoldCastingRecipe recipe : all) {
            if (recipe.getOutput().isEmpty()) continue;
            MaterialStack molten = new MaterialStack(recipe.getMaterial(), recipe.getMold().getCost());
            if (JeiMoltenRendering.liquidScrap(molten).isEmpty()) continue;
            out.add(recipe);
        }
        return out;
    }
}
