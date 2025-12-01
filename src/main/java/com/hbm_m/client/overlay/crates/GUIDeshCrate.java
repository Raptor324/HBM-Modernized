package com.hbm_m.client.overlay.crates;

import com.hbm_m.menu.DeshCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * GUI экран для Desh Crate - точная копия оригинала
 * 8 рядов × 13 колонок = 104 слота
 */
public class GUIDeshCrate extends AbstractContainerScreen<DeshCrateMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/generic_54.png");

    public GUIDeshCrate(DeshCrateMenu menu, Inventory inventory, Component component) {
        super(menu, inventory, component);

        // 13 колонок * 18 + 4 (отступы по 2px с каждой стороны) + 4 (бордеры) = 242
        this.imageWidth = 242;

        // 18 (заголовок) + 8*18 (крейт) + 14 (разделитель) + 3*18 (инвентарь) + 4 (отступ) + 18 (хотбар) + 4 (бордер) = 248
        this.imageHeight = 248;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 10;
        this.titleLabelY = 7;
        this.inventoryLabelX = 10;
        this.inventoryLabelY = 168;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // === ЦВЕТА КАК НА ОРИГИНАЛЕ ===
        int bgColor = 0xFFCA6666;           // Розово-красный фон (светлее)
        int borderDark = 0xFF8B3030;        // Тёмный бордер
        int borderLight = 0xFFDB9090;       // Светлый бордер
        int slotBg = 0xFFB05555;            // Фон слота
        int slotBorderDark = 0xFF6B2020;    // Тёмная рамка слота
        int slotBorderLight = 0xFFCA7070;   // Светлая рамка слота
        int headerBg = 0xFFB84545;          // Заголовок (тёмнее)

        // === ОСНОВНОЙ ФОН С БОРДЕРОМ ===
        // Внешний тёмный бордер (2px)
        guiGraphics.fill(x, y, x + imageWidth, y + imageHeight, borderDark);
        // Внутренний светлый бордер (1px внутри)
        guiGraphics.fill(x + 2, y + 2, x + imageWidth - 2, y + imageHeight - 2, borderLight);
        // Основной фон
        guiGraphics.fill(x + 3, y + 3, x + imageWidth - 3, y + imageHeight - 3, bgColor);

        // === ЗАГОЛОВОК ===
        guiGraphics.fill(x + 3, y + 3, x + imageWidth - 3, y + 18, headerBg);
        // Линия под заголовком
        guiGraphics.fill(x + 3, y + 17, x + imageWidth - 3, y + 18, borderDark);

        // === СЛОТЫ КРЕЙТА (8 рядов × 13 колонок) ===
        int slotStartX = x + 5;
        int slotStartY = y + 20;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 13; col++) {
                int slotX = slotStartX + col * 18;
                int slotY = slotStartY + row * 18;
                drawSlot(guiGraphics, slotX, slotY, slotBg, slotBorderDark, slotBorderLight);
            }
        }

        // === РАЗДЕЛИТЕЛЬ "INVENTORY" ===
        int separatorY = y + 20 + 8 * 18;
        guiGraphics.fill(x + 3, separatorY, x + imageWidth - 3, separatorY + 1, borderDark);

        // === ИНВЕНТАРЬ ИГРОКА (3 ряда × 9 колонок) ===
        int invStartX = x + 42; // Центрируем: (242 - 9*18) / 2 = 42
        int invStartY = y + 182;

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = invStartX + col * 18;
                int slotY = invStartY + row * 18;
                drawSlot(guiGraphics, slotX, slotY, slotBg, slotBorderDark, slotBorderLight);
            }
        }

        // === ХОТБАР (1 ряд × 9 колонок) ===
        int hotbarY = y + 182 + 3 * 18 + 4; // Под инвентарём
        for (int col = 0; col < 9; col++) {
            int slotX = invStartX + col * 18;
            drawSlot(guiGraphics, slotX, hotbarY, slotBg, slotBorderDark, slotBorderLight);
        }
    }

    /**
     * Рисует один слот с 3D эффектом как на оригинале
     */
    private void drawSlot(GuiGraphics guiGraphics, int x, int y, int bgColor, int darkBorder, int lightBorder) {
        // Фон слота
        guiGraphics.fill(x, y, x + 18, y + 18, bgColor);

        // Тёмная рамка (верх и лево) - эффект вдавленности
        guiGraphics.fill(x, y, x + 18, y + 1, darkBorder);           // Верх
        guiGraphics.fill(x, y, x + 1, y + 18, darkBorder);           // Лево

        // Светлая рамка (низ и право) - эффект объёма
        guiGraphics.fill(x, y + 17, x + 18, y + 18, lightBorder);    // Низ
        guiGraphics.fill(x + 17, y, x + 18, y + 18, lightBorder);    // Право

        // Внутренний фон (чуть темнее)
        int innerBg = 0xFF9B4545;
        guiGraphics.fill(x + 1, y + 1, x + 17, y + 17, innerBg);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}