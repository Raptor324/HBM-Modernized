package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.MachineCraneExtractorBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineCraneExtractorMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class GUIMachineCraneExtractor extends GuiInfoScreen<MachineCraneExtractorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_crane_ejector.png");

    private final MachineCraneExtractorBlockEntity extractor;

    public GUIMachineCraneExtractor(MachineCraneExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.extractor = menu.getBlockEntity();
        this.imageWidth = 212;
        this.imageHeight = 185;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (extractor.isMaxEject() || extractor.isWhitelist()) { // тайл может отсутствовать в реплее Flashback
            if (extractor.isMaxEject()) {
                guiGraphics.blit(TEXTURE, this.leftPos + 187, this.topPos + 34, 212, 0, 18, 18);
            }
            if (extractor.isWhitelist()) {
                guiGraphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 33, 212, 18, 3, 6);
            } else {
                guiGraphics.blit(TEXTURE, this.leftPos + 139, this.topPos + 47, 212, 18, 3, 6);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 26, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        for (int i = 0; i < 9 && extractor != null; i++) {
            Slot slot = this.menu.slots.get(i);
            String mode = extractor.getMatcher().getMode(i);
            if (mode != null && isHoveringSlot(slot, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font,
                        java.util.List.of(Component.literal("§cRight click to change"),
                                Component.literal(ModulePatternMatcher.getLabel(mode))),
                        mouseX, mouseY);
            }
        }

        if (extractor != null && isHovering(187, 34, 18, 18, mouseX, mouseY)) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, 187, 34, 18, 18, mouseX, mouseY,
                    Component.literal("Only take maximum possible: " + (extractor.isMaxEject() ? "ON" : "OFF")));
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (extractor == null) return super.mouseClicked(mouseX, mouseY, button); // тайл может отсутствовать в реплее Flashback
        for (int i = 0; i < 9; i++) {
            Slot slot = this.menu.slots.get(i);
            if (isHoveringSlot(slot, (int) mouseX, (int) mouseY) && button == 1 && slot.hasItem()) {
                extractor.nextMode(i);
                return true;
            }
        }

        if (isHovering(187, 34, 18, 18, (int) mouseX, (int) mouseY)) {
            extractor.toggleMaxEject();
            return true;
        }
        if (isHovering(128, 30, 14, 26, (int) mouseX, (int) mouseY)) {
            extractor.toggleWhitelist();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringSlot(Slot slot, int mouseX, int mouseY) {
        return isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
