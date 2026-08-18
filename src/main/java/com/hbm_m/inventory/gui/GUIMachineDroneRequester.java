package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.MachineDroneRequesterBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.MachineDroneRequesterMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class GUIMachineDroneRequester extends GuiInfoScreen<MachineDroneRequesterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/storage/gui_drone_requester.png");

    private final MachineDroneRequesterBlockEntity requester;

    public GUIMachineDroneRequester(MachineDroneRequesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.requester = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
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

        for (int i = 0; i < 9; i++) {
            Slot slot = this.menu.slots.get(i);
            String mode = requester.getMatcher().getMode(i);
            if (mode != null && isHoveringSlot(slot, mouseX, mouseY)) {
                guiGraphics.renderComponentTooltip(this.font,
                        java.util.List.of(Component.literal("§cRight click to change"),
                                Component.literal(ModulePatternMatcher.getLabel(mode))),
                        mouseX, mouseY);
            }
        }

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int i = 0; i < 9; i++) {
            Slot slot = this.menu.slots.get(i);
            if (isHoveringSlot(slot, (int) mouseX, (int) mouseY) && button == 1 && slot.hasItem()) {
                requester.nextFilterMode(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringSlot(Slot slot, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= slot.x && localX < slot.x + 16 && localY >= slot.y && localY < slot.y + 16;
    }
}
