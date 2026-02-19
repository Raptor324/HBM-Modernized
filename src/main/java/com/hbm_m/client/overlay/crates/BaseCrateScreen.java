package com.hbm_m.client.overlay.crates;

import com.hbm_m.block.custom.machines.crates.CrateType;
import com.hbm_m.menu.BaseCrateMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * Базовый экран GUI для всех ящиков HBM.
 * Использует кастомные текстуры для каждого типа:
 * - Iron: gui_crate_iron.png (176×186)
 * - Steel: gui_crate_steel.png (176×222)
 * - Tungsten: gui_crate_tungsten.png (176×168)
 * - Template: gui_crate_template.png (176×168)
 * - Desh: gui_crate_desh.png (248×256)
 */
public class BaseCrateScreen<T extends BaseCrateMenu> extends AbstractContainerScreen<T> {

    protected final CrateType crateType;
    protected final ResourceLocation texture;

    public BaseCrateScreen(T menu, Inventory inventory, Component title, CrateType crateType) {
        super(menu, inventory, title);
        this.crateType = crateType;
        this.texture = crateType.getTexture();
        this.imageWidth = crateType.getGuiWidth();
        this.imageHeight = crateType.getGuiHeight();
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = crateType.getPlayerInvStartX();
        this.inventoryLabelY = crateType.getPlayerInvStartY() - 12;
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int color = crateType.getTitleColor();
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, color, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, color, false);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(texture, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
