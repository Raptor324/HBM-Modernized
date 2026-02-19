package com.hbm_m.block.custom.machines.crates;

import com.hbm_m.lib.RefStrings;
import net.minecraft.resources.ResourceLocation;

/**
 * Перечисление типов ящиков HBM. Определяет размеры сетки слотов,
 * размеры GUI и путь к текстуре.
 */
public enum CrateType {
    IRON(9, 4, 176, 186, "gui_crate_iron"),
    STEEL(9, 6, 176, 222, "gui_crate_steel"),
    TUNGSTEN(9, 3, 176, 168, "gui_crate_tungsten"),
    TEMPLATE(9, 3, 176, 168, "gui_crate_template"),
    DESH(13, 8, 248, 256, "gui_crate_desh");

    private final int cols;
    private final int rows;
    private final int guiWidth;
    private final int guiHeight;
    private final String textureName;

    CrateType(int cols, int rows, int guiWidth, int guiHeight, String textureName) {
        this.cols = cols;
        this.rows = rows;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;
        this.textureName = textureName;
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }
    public int getSlotCount() { return cols * rows; }
    public int getGuiWidth() { return guiWidth; }
    public int getGuiHeight() { return guiHeight; }

    public ResourceLocation getTexture() {
        return new ResourceLocation(RefStrings.MODID, "textures/gui/storage/" + textureName + ".png");
    }

    /** X-отступ для слотов ящика (позиция первого слота в Menu) */
    public int getCrateSlotStartX() {
        return 8;
    }

    /** Y-отступ для слотов ящика */
    public int getCrateSlotStartY() {
        return 18;
    }

    /** X-отступ для инвентаря игрока */
    public int getPlayerInvStartX() {
        if (this == DESH) return 44;
        return 8;
    }

    /**
     * Y-отступ для инвентаря игрока.
     * Вычисляется на основе оригинальных координат из 1.7.10:
     * Iron: 104, Steel: 140, Tungsten/Template: 86, Desh: 174
     */
    public int getPlayerInvStartY() {
        int crateBottom = getCrateSlotStartY() + rows * 18;
        if (this == DESH) return crateBottom + 12;
        return crateBottom + 14;
    }

    /** Y-отступ для хотбара */
    public int getHotbarStartY() {
        return getPlayerInvStartY() + 3 * 18 + 4;
    }

    /** Цвет текста заголовка (белый для tungsten, черный для остальных) */
    public int getTitleColor() {
        return this == TUNGSTEN ? 0xFFFFFF : 0x404040;
    }
}
