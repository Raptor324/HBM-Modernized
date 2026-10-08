package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineRotaryFurnaceBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.material.Mats;
import com.hbm_m.inventory.menu.MachineRotaryFurnaceMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 1:1 {@code GUIMachineRotaryFurnace}: Fortschrittspfeil (63,30), Flamme (26,69), Schmelzsaeule (98,18) in der Farbe des
 * Materials, Fluidtank waagrecht (8,36), Dampf (134,18) und Abdampf (152,18). Ueber dem leeren Brennstoffslot stehen
 * die Brennstoffboni.
 */
public class GUIMachineRotaryFurnace extends GuiInfoScreen<MachineRotaryFurnaceMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_rotary_furnace.png");

    private final MachineRotaryFurnaceBlockEntity furnace;

    public GUIMachineRotaryFurnace(MachineRotaryFurnaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.furnace = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
    }

    @Override
    public void render(GuiGraphics g, int x, int y, float interp) {
        GuiCompat.renderBackground(this, g, x, y, interp);
        super.render(g, x, y, interp);

        if (furnace != null) {
            furnace.tanks[0].renderTankInfo(g, this.font, x, y, leftPos + 8, topPos + 36, 52, 16);
            furnace.tanks[1].renderTankInfo(g, this.font, x, y, leftPos + 134, topPos + 18, 16, 52);
            furnace.tanks[2].renderTankInfo(g, this.font, x, y, leftPos + 152, topPos + 18, 16, 52);

            Slot slot = this.menu.slots.get(4);
            if (isHovering(slot.x, slot.y, 16, 16, x, y) && !slot.hasItem()) {
                List<Component> bonuses = MachineRotaryFurnaceBlockEntity.burnModule.getDesc();
                if (!bonuses.isEmpty()) g.renderComponentTooltip(this.font, bonuses, x, y);
            }

            if (furnace.output == null) {
                drawCustomInfoStat(g, x, y, 98, 18, 16, 52, x, y, Component.literal("Empty").withStyle(ChatFormatting.RED));
            } else {
                drawCustomInfoStat(g, x, y, 98, 18, 16, 52, x, y, furnace.output.material.getLocalizedName().copy()
                        .append(": " + Mats.formatAmount(furnace.output.amount, Screen.hasShiftDown())).withStyle(ChatFormatting.YELLOW));
            }
        }

        this.renderTooltip(g, x, y);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, (this.imageWidth - 54) / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float interp, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (furnace == null) return;

        int p = (int) Math.ceil(furnace.progress * 33);
        g.blit(TEXTURE, leftPos + 63, topPos + 30, 176, 0, p, 10);

        if (furnace.maxBurnTime > 0) {
            int b = furnace.burnTime * 14 / furnace.maxBurnTime;
            g.blit(TEXTURE, leftPos + 26, topPos + 69 - b, 176, 24 - b, 14, b);
        }

        if (furnace.output != null) {

            int hex = furnace.output.material.moltenColor;
            int amount = furnace.output.amount * 52 / MachineRotaryFurnaceBlockEntity.maxOutput;
            RenderSystem.setShaderColor((hex >> 16 & 255) / 255F, (hex >> 8 & 255) / 255F, (hex & 255) / 255F, 1F);
            g.blit(TEXTURE, leftPos + 98, topPos + 70 - amount, 176, 76 - amount, 16, amount);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            RenderSystem.setShaderColor(1F, 1F, 1F, 0.3F);
            g.blit(TEXTURE, leftPos + 98, topPos + 70 - amount, 176, 76 - amount, 16, amount);
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        furnace.tanks[0].renderTank(g, leftPos + 8, topPos + 36, 52, 16, 1);
        furnace.tanks[1].renderTank(g, leftPos + 134, topPos + 18, 16, 52);
        furnace.tanks[2].renderTank(g, leftPos + 152, topPos + 18, 16, 52);
    }
}
