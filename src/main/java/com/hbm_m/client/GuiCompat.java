package com.hbm_m.client;
import com.hbm_m.client.GuiCompat;

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
        //? if forge {
        screen.renderBackground(guiGraphics);
        //?}
        //? if neoforge {
        /*screen.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        *///?}
    }
}
