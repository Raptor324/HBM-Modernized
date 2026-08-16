package com.hbm_m.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

/**
 * Кросс-версионная обёртка для {@link Screen#renderBackground}.
 *
 * <p><b>Проблема:</b> сигнатура {@code renderBackground} изменилась между версиями:
 * <ul>
 *   <li><b>1.20.1</b> (forge/fabric): {@code renderBackground(GuiGraphics)} — 1 аргумент.</li>
 *   <li><b>1.21.1</b> (neoforge): {@code renderBackground(GuiGraphics, int mouseX, int mouseY, float partialTick)} — 4 аргумента.</li>
 * </ul>
 *
 * <p>Расставлять stonecutter-блоки в 64 Screen-классах нецелесообразно, поэтому весь call-site
 * сводится к этому статическому хелперу с внутренним version-gating.
 */

public final class GuiCompat {
    private GuiCompat() {}

    /**
     * Вызывает {@code screen.renderBackground(...)} с корректной для версии сигнатурой.
     *
     * @param screen       целевой Screen (обычно {@code this})
     * @param guiGraphics  контекст отрисовки
     * @param mouseX       координата мыши X
     * @param mouseY       координата мыши Y
     * @param partialTick  partial tick
     */

    public static void renderBackground(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        //? if < 1.21.1 {
        screen.renderBackground(guiGraphics);
        //?} else {
        /*screen.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        *///?}
    }

    /**
     * Рендер виджетов экрана БЕЗ фона — замена {@code super.render(...)} в кастомных
     * Screen'ах, которые рисуют фон сами. В 1.21.1 {@code Screen.render} вызывает
     * {@code renderBackground} (blur + тёмная меню-текстура) ПОВЕРХ всего, что уже
     * нарисовано до {@code super.render} — из-за этого панели/текстуры GUI оказывались
     * «под блюром». В 1.20.1 {@code Screen.render} только перебирает renderables,
     * поэтому здесь поведение идентично vanilla.
     */
    public static void renderWidgetsOnly(Screen screen, GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        for (net.minecraft.client.gui.components.Renderable renderable : screen.renderables) {
            renderable.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Фон «blur + плоский градиент»: в отличие от {@link #renderBackground} на 1.21.1
     * НЕ рисует полупрозрачную менюшную текстуру (inworld_menu_list/menu_background),
     * сквозь которую просвечивает «каша» из блюренного мира. Блюр остаётся под всем
     * меню, поверх — только чистый тёмный градиент.
     */
    public static void renderFlatBlurredBackground(Screen screen, GuiGraphics guiGraphics, float partialTick) {
        //? if < 1.21.1 {
        screen.renderBackground(guiGraphics);
        //?} else {
        /*// Эквивалент protected Screen.renderBlurredBackground, недоступного извне.
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        mc.gameRenderer.processBlurEffect(partialTick);
        mc.getMainRenderTarget().bindWrite(false);
        screen.renderTransparentBackground(guiGraphics);
        *///?}
    }
}