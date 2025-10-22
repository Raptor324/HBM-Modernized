package com.hbm_m.client.overlay;

import com.hbm_m.lib.RefStrings;
import com.hbm_m.menu.IronCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * GUI экран для Iron Crate (36 слотов: 4 ряда × 9 колонок)
 */
public class GUIIronCrate extends AbstractContainerScreen<IronCrateMenu> {

    // ===== ИЗМЕНЕНО: Используем текстуру обычного сундука (27 слотов) =====
    // Minecraft не имеет встроенной текстуры для 36 слотов,
    // поэтому используем текстуру обычного сундука и расширяем её
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");

    public GUIIronCrate(IronCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        // ===== ИЗМЕНЕНО: Размеры для 4 рядов =====
        this.imageWidth = 176;    // Стандартная ширина
        this.imageHeight = 186;   // 4 ряда: 18 + 4*18 + 14 + 3*18 + 4 + 18 + 96 = 186
    }

    @Override
    protected void init() {
        super.init();

        this.titleLabelX = 8;
        this.titleLabelY = 6;

        // ===== ИЗМЕНЕНО: Позиция надписи "Inventory" =====
        this.inventoryLabelX = 8;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Рисуем верхнюю часть (заголовок + 3 ряда)
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, 71);

        // Рисуем 4-й ряд крейта (копируем 3-й ряд из текстуры)
        guiGraphics.blit(TEXTURE, x, y + 71, 0, 53, imageWidth, 18);

        // Рисуем инвентарь игрока (3 ряда + хотбар)
        guiGraphics.blit(TEXTURE, x, y + 89, 0, 125, imageWidth, 97);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}