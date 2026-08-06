package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.MachineCraneInserterBlockEntity;
import com.hbm_m.inventory.menu.MachineCraneInserterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCraneInserter extends GuiInfoScreen<MachineCraneInserterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crane_inserter.png");

    private final MachineCraneInserterBlockEntity inserter;

    public GUIMachineCraneInserter(MachineCraneInserterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.inserter = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 185;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (inserter.isDestroyer()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 34, 176, 0, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2 - 18, 5, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (isHovering(151, 34, 18, 18, mouseX, mouseY)) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 151, 34, 18, 18, mouseX, mouseY,
                    Component.literal("Destroy overflow: " + (inserter.isDestroyer() ? "ON" : "OFF")));
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovering(151, 34, 18, 18, (int) mouseX, (int) mouseY)) {
            inserter.toggleDestroyer();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
