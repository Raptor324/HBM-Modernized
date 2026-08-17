package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.rbmk.RBMKControlBlockEntity;
import com.hbm_m.inventory.menu.RBMKControlMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.RBMKControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Port of {@code GUIRBMKControl} (1.7.10 Original), rendered with the real
 * {@code gui_rbmk_control.png} texture instead of a flat-fill placeholder.
 */
public class GUIRBMKControl extends GuiInfoScreen<RBMKControlMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_rbmk_control.png");

    private final RBMKControlBlockEntity be;

    public GUIRBMKControl(RBMKControlMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.be = menu.getBlockEntity();
        this.imageWidth  = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Rod-level bar: the higher the rod is withdrawn, the taller the "empty" overlay drawn from the top.
        int height = (int) (56 * (1.0 - be.level));
        if (height > 0) {
            g.blit(TEXTURE, leftPos + 75, topPos + 29, 176, 56 - height, 8, height);
        }

        // Color-group marker, if this rod is assigned to a farbgroup (0-4). -1 = ungrouped.
        if (be.color >= 0 && be.color < 5) {
            g.blit(TEXTURE, leftPos + 28, topPos + 26 + be.color * 11, 184, be.color * 10, 12, 10);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);
        super.render(g, mx, my, partial);

        drawCustomInfoStat(g, mx, my, 71, 29, 16, 56, mx, my,
                Component.literal((int) (be.level * 100) + "%"));

        renderTooltip(g, mx, my);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        for (int k = 0; k < 5; k++) {
            // Manual rod-level presets (100%, 75%, 50%, 25%, 0%), stacked column of 5 buttons.
            if (isPointInRect(118, 26 + k * 11, 30, 10, (int) mouseX, (int) mouseY)) {
                playClickSound();
                double target = 1.0 - k * 0.25;
                be.setTarget(target); // client-side prediction so the bar/label update immediately
                RBMKControlPacket.sendSetTarget(be.getBlockPos(), target);
                return true;
            }

            // Farbgroup (color group) assignment buttons.
            if (isPointInRect(28, 26 + k * 11, 12, 10, (int) mouseX, (int) mouseY)) {
                playClickSound();
                be.color = (short) k;
                RBMKControlPacket.sendSetColor(be.getBlockPos(), k);
                return true;
            }
        }

        return handled;
    }
}
