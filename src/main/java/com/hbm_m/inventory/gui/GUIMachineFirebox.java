package com.hbm_m.inventory.gui;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.hbm_m.blockentity.machines.HeatingOvenBlockEntity;
import com.hbm_m.inventory.menu.MachineFireboxMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** 1:1 {@code GUIFirebox} (Feuerbuechse und Heizofen, jeweils mit eigener Textur). */
public class GUIMachineFirebox<T extends MachineFireboxMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation TEXTURE_FIREBOX =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_firebox.png");

    private final ResourceLocation texture;

    public GUIMachineFirebox(T menu, Inventory playerInventory, Component title) {
        this(menu, playerInventory, title, TEXTURE_FIREBOX);
    }

    protected GUIMachineFirebox(T menu, Inventory playerInventory, Component title, ResourceLocation texture) {
        super(menu, playerInventory, title);
        this.texture = texture;
        this.imageWidth = 176;
        this.imageHeight = 168;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (this.menu.getCarried().isEmpty() && menu.getBlockEntity() != null) {
            for (int i = 0; i < 2; ++i) {
                Slot slot = this.menu.slots.get(i);
                if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && !slot.hasItem()) {
                    List<Component> bonuses = menu.getBlockEntity().getModule().getDesc();
                    if (!bonuses.isEmpty()) {
                        guiGraphics.renderTooltip(this.font, bonuses, Optional.empty(), mouseX, mouseY);
                    }
                }
            }
        }

        if (this.isHovering(80, 27, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal(String.format(Locale.US, "%,d", menu.getHeatEnergy()) + " / "
                    + String.format(Locale.US, "%,d", menu.getMaxHeat()) + "TU"), mouseX, mouseY);
        }
        if (this.isHovering(80, 36, 71, 7, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, List.of(Component.literal(menu.getBurnHeat() + "TU/t"), Component.literal((menu.getBurnTime() / 20) + "s")),
                    Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        int color = menu.getBlockEntity() instanceof HeatingOvenBlockEntity ? 0xffffff : 4210752;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, color, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int i = menu.getHeatEnergy() * 69 / Math.max(menu.getMaxHeat(), 1);
        guiGraphics.blit(texture, leftPos + 81, topPos + 28, 176, 0, i, 5);

        int j = menu.getBurnTime() * 70 / Math.max(menu.getMaxBurnTime(), 1);
        guiGraphics.blit(texture, leftPos + 81, topPos + 37, 176, 5, j, 5);

        if (menu.wasOn()) {
            guiGraphics.blit(texture, leftPos + 25, topPos + 26, 176, 10, 18, 18);
        }
    }
}
