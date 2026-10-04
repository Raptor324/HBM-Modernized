package com.hbm_m.inventory.gui;

import java.awt.Color;

import com.hbm_m.blockentity.machines.MachineSilexBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.ModFluids;
import com.hbm_m.inventory.menu.MachineSilexMenu;
import com.hbm_m.item.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.recipe.SilexRecipe;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUISILEX}: Peroxidtank als waagerechter Balken, Ladeleiste mit Inhaltsangabe, Fortschritt, "Inhalt
 * verwerfen"-Knopf, Name und Sinuswelle der anliegenden Laser-Wellenlaenge (Frequenz verdoppelt sich je Stufe).
 */
public class GUIMachineSilex extends GuiInfoScreen<MachineSilexMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_silex.png");

    private final MachineSilexBlockEntity silex;

    public GUIMachineSilex(MachineSilexMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.silex = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (silex != null) {
            silex.tank.renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 8, topPos + 42, 52, 7);

            if (silex.hasCurrent()) {
                drawCustomInfoStat(g, mouseX, mouseY, 27, 72, 16, 52, mouseX, mouseY,
                        Component.literal(silex.currentFill + "/" + MachineSilexBlockEntity.maxFill + "mB"), silex.getCurrentName());
            }

            drawCustomInfoStat(g, mouseX, mouseY, 10, 92, 10, 10, mouseX, mouseY, Component.literal("Void contents"));
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (silex != null && leftPos + 10 <= x && leftPos + 10 + 12 > x && topPos + 92 < y && topPos + 92 + 12 >= y) {
            playClickSound();
            CompoundTag data = new CompoundTag();
            data.putBoolean("void", true);
            com.hbm_m.network.NBTControlPacket.sendToServer(silex.getBlockPos(), data);
            return true;
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, (this.imageWidth / 2 - this.font.width(name) / 2) - 54, 8, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);

        if (silex != null && silex.mode != EnumWavelengths.NULL) {
            Component mode = Component.translatable(silex.mode.name).withStyle(silex.mode.textColor);
            g.drawString(this.font, mode, 100 + (32 - this.font.width(mode) / 2), 16, 0, false);
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (silex == null) return; // тайл может отсутствовать в реплее Flashback

        long time = silex.getLevel() != null ? silex.getLevel().getGameTime() : 0;

        if (silex.mode != EnumWavelengths.NULL) {
            float freq = 0.1F * (float) Math.pow(2, silex.mode.ordinal());
            int color = (silex.mode != EnumWavelengths.VISIBLE) ? silex.mode.guiColor : Color.HSBtoRGB(time / 50.0F, 0.5F, 1F) & 16777215;
            drawWave(g, 81, 46, 16, 84, 0.5F, freq, color, 3F, time);
        }

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (silex.tank.getFill() > 0) {
            boolean usable = silex.tank.getTankType() == ModFluids.PEROXIDE.getSource()
                    || SilexRecipe.forFluid(silex.getLevel(), silex.tank.getTankType()) != null;
            if (usable) g.blit(TEXTURE, leftPos + 7, topPos + 41, 176, 118, 54, 9);
            else g.blit(TEXTURE, leftPos + 7, topPos + 41, 176, 109, 54, 9);
        }

        int p = silex.getProgressScaled(69);
        g.blit(TEXTURE, leftPos + 45, topPos + 82, 176, 0, p, 43);

        int f = silex.getFillScaled(52);
        g.blit(TEXTURE, leftPos + 26, topPos + 124 - f, 176, 109 - f, 16, f);

        int i = silex.getFluidScaled(52);
        g.blit(TEXTURE, leftPos + 8, topPos + 42, 176, silex.tank.getTankType() == ModFluids.PEROXIDE.getSource() ? 43 : 50, i, 7);
    }

    /** Original {@code drawWave}: Sinus aus kurzen Liniensegmenten, laeuft mit der Weltzeit. */
    private void drawWave(GuiGraphics g, int x, int y, int height, int width, float resolution, float freq, int color, float thickness, long time) {
        float samples = ((float) width) / resolution;
        float scale = ((float) height) / 2F;
        float offset = (float) ((float) time % (4 * Math.PI / freq));
        for (int i = 1; i < samples; i++) {
            double currentX = offset + x + i * resolution;
            double nextX = offset + x + (i + 1) * resolution;
            double currentY = y + scale * Math.sin(freq * currentX);
            double nextY = y + scale * Math.sin(freq * nextX);
            GuiLineHelper.drawLine(g, leftPos + currentX - offset, topPos + currentY, leftPos + nextX - offset, topPos + nextY, thickness, color);
        }
    }
}
