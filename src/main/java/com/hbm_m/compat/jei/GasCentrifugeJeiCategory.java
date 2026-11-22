package com.hbm_m.compat.jei;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.GasCentrifugeRecipe;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.registries.RegistrySupplier;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 1:1 порт оригинального 1.7.10 {@code GasCentrifugeRecipeHandler}.
 *
 * <p>Своя текстура {@code textures/gui/nei/gui_nei_centrifuge_gas.png} (не общий {@code gui_nei.png}):
 * <ul>
 *   <li>Вход — жидкость (ориг. {@code ItemFluidIcon}) на (52 - 5, 35 - 11) = (47, 24)</li>
 *   <li>Выходы — фиксированная сетка 2x2 (134/152 - 5, 26/44 - 11) = (129/147, 15/33)</li>
 *   <li>Батарея-топливо на (3, 42): {@code MachineRecipes.getBatteries()} — battery_pack
 *       (все {@code EnumBatteryPack}), battery_sc (все {@code EnumBatterySC}), battery_creative</li>
 *   <li>Вертикальная шкала заряда слева: {@code drawProgressBar(3, 51 - 45, 176, 0, 16, 34, 480, 7)}</li>
 *   <li>Главный прогресс-бар: {@code drawProgressBar(79 - 5, 28 - 11, 208, 0, 44, 37, 150 (80 при high-speed), 0)}</li>
 *   <li>Маркер high-speed 16x16 из drawBackground при {@code isHighSpeed}: tex (192, 0) на (30 - 5, 35 - 11)</li>
 *   <li>Зелёная надпись «{@code <centNumber> G. Cents}» (цвет 65280) по центру x = 50, y = 21 - 11</li>
 * </ul>
 * Фон рисуется из области (5, 11, 166, 65) — как {@code TemplateRecipeHandler.drawBackground}.</p>
 *
 * <p>Рецепты — data-driven {@link GasCentrifugeRecipe} (JSON, JEI-only): те же 4 канонических
 * каскад-результата (UF6 x2 / PUF6 / WATZ), что и карта {@code GasCentrifugeRecipes.gasCent}
 * оригинала (у оригинального {@code FluidStack} нет equals/hashCode, поэтому оба UF6-входа
 * сосуществуют и NEI показывал все 4). Runtime работает на cascade-enrichment через
 * {@code PseudoFluidType} — эти рецепты только для JEI.</p>
 */
@OnlyIn(Dist.CLIENT)
public class GasCentrifugeJeiCategory implements IRecipeCategory<GasCentrifugeRecipe> {

    public static final RecipeType<GasCentrifugeRecipe> RECIPE_TYPE =
            RecipeType.create(RefStrings.MODID, "gas_centrifuge", GasCentrifugeRecipe.class);

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/nei/gui_nei_centrifuge_gas.png");

    /** Ёмкость для полоски жидкости (ориг. вход 900–1200 mB). */
    private static final int FLUID_RENDERER_CAPACITY = 2000;

    /** Оригинальный цвет надписи: 65280 = 0x00FF00. */
    private static final int INFO_TEXT_COLOR = 0x00FF00;

    /** Оригинал {@code SmeltingSet}: выходы по индексу, (134/152 - 5, 26/44 - 11). */
    private static final int[][] OUTPUT_POS = {{129, 15}, {147, 15}, {129, 33}, {147, 33}};

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable itemSlotBackground;
    private final IDrawableAnimated batteryBar;
    private final IDrawableAnimated progressBarNormal;
    private final IDrawableAnimated progressBarHighSpeed;
    private final IDrawable highSpeedMarker;
    private final ItemStack[] batteries;

    public GasCentrifugeJeiCategory(IGuiHelper guiHelper) {
        // TemplateRecipeHandler.drawBackground: drawTexturedModalRect(0, 0, 5, 11, 166, 65)
        this.background = guiHelper.drawableBuilder(TEXTURE, 5, 11, 166, 65).setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.MACHINE_GASCENT.get()));
        this.itemSlotBackground = JeiNeiTextures.createItemSlotBackground(guiHelper);
        // drawExtras: drawProgressBar(3, 51 - 45, 176, 0, 16, 34, 480, 7) — вертикальная шкала, убывает вверх
        this.batteryBar = guiHelper.drawableBuilder(TEXTURE, 176, 0, 16, 34).setTextureSize(256, 256)
                .buildAnimated(480, IDrawableAnimated.StartDirection.BOTTOM, true);
        // drawExtras: drawProgressBar(79 - 5, 28 - 11, 208, 0, 44, 37, isHighSpeed ? 150 - 70 : 150, 0)
        this.progressBarNormal = guiHelper.drawableBuilder(TEXTURE, 208, 0, 44, 37).setTextureSize(256, 256)
                .buildAnimated(150, IDrawableAnimated.StartDirection.LEFT, false);
        this.progressBarHighSpeed = guiHelper.drawableBuilder(TEXTURE, 208, 0, 44, 37).setTextureSize(256, 256)
                .buildAnimated(80, IDrawableAnimated.StartDirection.LEFT, false);
        // drawBackground: drawTexturedModalRect(30 - 5, 35 - 11, 192, 0, 16, 16) при isHighSpeed
        this.highSpeedMarker = guiHelper.drawableBuilder(TEXTURE, 192, 0, 16, 16).setTextureSize(256, 256).build();
        this.batteries = buildBatteries();
    }

    /**
     * Оригинал {@code MachineRecipes.getBatteries()}: battery_pack по всем {@code EnumBatteryPack}
     * (в порту — отдельные предметы {@code battery_pack_*}), battery_sc по всем {@code EnumBatterySC}
     * и battery_creative. Показывается на слоте (3, 42) с циклическим перебором.
     */
    private static ItemStack[] buildBatteries() {
        List<ItemStack> list = new ArrayList<>();
        for (RegistrySupplier<Item> pack : ModItems.BATTERY_PACKS) {
            list.add(new ItemStack(pack.get()));
        }
        list.add(new ItemStack(ModItems.BATTERY_SC_EMPTY.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_WASTE.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_RA226.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_TC99.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_CO60.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_PU238.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_PO210.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_AU198.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_PB209.get()));
        list.add(new ItemStack(ModItems.BATTERY_SC_AM241.get()));
        list.add(new ItemStack(ModItems.CREATIVE_BATTERY.get()));
        return list.toArray(new ItemStack[0]);
    }

    /** NEI рисует рамку слота 18x18 на (x - 1, y - 1) — рамки уже запечены в фоновой текстуре. */
    private IRecipeSlotBuilder addItemSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y) {
        return builder.addSlot(role, x, y)
                .setBackground(itemSlotBackground, -1, -1);
    }

    @Override
    public RecipeType<GasCentrifugeRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.hbm_m.gas_centrifuge");
    }

    @Override
    @SuppressWarnings("removal")
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return JeiNeiTextures.RECIPE_WIDTH;
    }

    @Override
    public int getHeight() {
        return JeiNeiTextures.RECIPE_HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, GasCentrifugeRecipe recipe, IFocusGroup focuses) {
        // Вход: (52 - 5, 35 - 11) = (47, 24). Ориг. ItemFluidIcon — в порту жидкостный ингредиент.
        FluidStack input = recipe.getInput();
        if (input != null && !input.isEmpty()) {
            IRecipeSlotBuilder inputSlot = addItemSlot(builder, RecipeIngredientRole.INPUT, 47, 24);
            JeiFluidSlots.addFluid(inputSlot, input, FLUID_RENDERER_CAPACITY);
        }

        // Выходы: фиксированная сетка 2x2, позиции по индексу (1:1 с оригиналом).
        ItemStack[] outputs = recipe.getOutputs();
        for (int i = 0; i < outputs.length && i < OUTPUT_POS.length; i++) {
            if (!outputs[i].isEmpty()) {
                addItemSlot(builder, RecipeIngredientRole.OUTPUT, OUTPUT_POS[i][0], OUTPUT_POS[i][1])
                        .addItemStack(outputs[i]);
            }
        }

        // Батарея-топливо: ориг. getOtherStacks → fuels.get((cycleticks / 48) % fuels.size()) на (3, 42).
        addItemSlot(builder, RecipeIngredientRole.CATALYST, 3, 42)
                .addItemStacks(Arrays.asList(batteries));
    }

    @Override
    public void draw(GasCentrifugeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics,
                     double mouseX, double mouseY) {
        // drawExtras: вертикальная шкала заряда батареи слева
        batteryBar.draw(guiGraphics, 3, 6);
        // drawExtras: главный прогресс-бар (80 тиков при high-speed, иначе 150)
        IDrawableAnimated progressBar = recipe.isHighSpeed() ? progressBarHighSpeed : progressBarNormal;
        progressBar.draw(guiGraphics, 74, 17);
        // drawBackground: маркер высокоскоростного режима
        if (recipe.isHighSpeed()) {
            highSpeedMarker.draw(guiGraphics, 25, 24);
        }

        // drawExtras: «<centNumber> G. Cents» зелёным, центр по x = 50, y = 21 - 11.
        // Локализованные строки шире оригинальной «N G. Cents» — центрируем на 50,
        // но не даём тексту выйти за пределы панели 166px.
        Font font = Minecraft.getInstance().font;
        Component info = recipe.isHighSpeed()
                ? Component.translatable("jei.hbm_m.gas_centrifuge.info_high_speed", recipe.getCentrifugeCount())
                : Component.translatable("jei.hbm_m.gas_centrifuge.info", recipe.getCentrifugeCount());
        int infoX = Math.max(0, Math.min(50 - font.width(info) / 2, JeiNeiTextures.RECIPE_WIDTH - font.width(info)));
        guiGraphics.drawString(font, info, infoX, 10, INFO_TEXT_COLOR, false);
    }
}
