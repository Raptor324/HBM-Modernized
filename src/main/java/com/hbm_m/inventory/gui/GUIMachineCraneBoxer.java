package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.MachineCraneBoxerBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineCraneBoxerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineCraneBoxer extends GuiInfoScreen<MachineCraneBoxerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crane_boxer.png");

    private final MachineCraneBoxerBlockEntity boxer;

    public GUIMachineCraneBoxer(MachineCraneBoxerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.boxer = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 185;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        if (boxer != null) { // тайл может отсутствовать в реплее Flashback
            guiGraphics.blit(TEXTURE, this.leftPos + 151, this.topPos + 34, 176, boxer.getMode() * 18, 18, 18);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        if (boxer != null && isHovering(151, 34, 18, 18, mouseX, mouseY)) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 151, 34, 18, 18, mouseX, mouseY,
                    Component.literal(modeLabel(boxer.getMode())));
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private String modeLabel(byte mode) {
        return switch (mode) {
            case MachineCraneBoxerBlockEntity.MODE_8 -> "Pack 8 full stacks";
            case MachineCraneBoxerBlockEntity.MODE_16 -> "Pack 16 full stacks";
            case MachineCraneBoxerBlockEntity.MODE_REDSTONE -> "Pack all on redstone pulse";
            default -> "Pack 4 full stacks";
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (boxer != null && isHovering(151, 34, 18, 18, (int) mouseX, (int) mouseY)) {
            boxer.nextMode();
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
