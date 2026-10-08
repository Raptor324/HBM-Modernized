package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.api.fluids.HbmFluidRegistry;
import com.hbm_m.blockentity.machines.MachineRefineryBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineRefineryMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.NotNull;

/**
 * 1:1-Port von {@code GUIMachineRefinery} (1.7.10): 182x240, Eingangssaeule (12, 18-88, 16x70),
 * vier Ausgangstanks (64/82/100/118, Unterkante 88), Energie (158, 18-106) und die vier in der
 * Produktfarbe getoenten Rohre (Textur 256x256, ab y 240).
 */
public class GUIMachineRefinery extends GuiInfoScreen<MachineRefineryMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_refinery.png");

    private final MachineRefineryBlockEntity refinery;

    public GUIMachineRefinery(MachineRefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.refinery = menu.getBlockEntity();
        this.imageWidth = 182;
        this.imageHeight = 240;
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (refinery == null) return; // тайл может отсутствовать в реплее Flashback

        // power
        long max = Math.max(refinery.getMaxEnergyStored(), 1L);
        int j = (int) (refinery.getEnergyStored() * 88 / max);
        g.blit(TEXTURE, leftPos + 158, topPos + 106 - j, 182, 88 - j, 16, j);

        // input tank - Original: renderTank(guiLeft + 12, guiTop + 88, ..., 16, 70), Port erwartet die Oberkante
        FluidTank input = refinery.getTank(MachineRefineryBlockEntity.TANK_INPUT);
        if (input.getFill() != 0) {
            input.renderTank(g, leftPos + 12, topPos + 88 - 70, 16, 70);
        }

        // pipes
        if (!refinery.hasDisplayRecipe()) {
            g.blit(TEXTURE, leftPos + 30, topPos + 30, 0, 248, 43, 4);
            g.blit(TEXTURE, leftPos + 30, topPos + 26, 0, 240, 61, 8);
            g.blit(TEXTURE, leftPos + 30, topPos + 22, 61, 240, 79, 12);
            g.blit(TEXTURE, leftPos + 30, topPos + 18, 140, 240, 97, 16);
        } else {
            Fluid[] out = refinery.getDisplayPipeFluids();
            RenderSystem.enableBlend();
            tinted(g, out[0], 30, 30, 0, 248, 43, 4);    // Heavy Oil Products
            tinted(g, out[1], 30, 26, 0, 240, 61, 8);    // Naphtha Oil Products
            tinted(g, out[2], 30, 22, 61, 240, 79, 12);  // Light Oil Products
            tinted(g, out[3], 30, 18, 140, 240, 97, 16); // Gaseous Products
            RenderSystem.disableBlend();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }

        // output tanks
        refinery.getTank(MachineRefineryBlockEntity.TANK_HEAVY).renderTank(g, leftPos + 64, topPos + 88 - 52, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_NAPHTHA).renderTank(g, leftPos + 82, topPos + 88 - 52, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_LIGHT).renderTank(g, leftPos + 100, topPos + 88 - 52, 16, 52);
        refinery.getTank(MachineRefineryBlockEntity.TANK_PETROLEUM).renderTank(g, leftPos + 118, topPos + 88 - 52, 16, 52);
    }

    private void tinted(GuiGraphics g, Fluid fluid, int x, int y, int u, int v, int w, int h) {
        int color = HbmFluidRegistry.getTintColor(fluid) & 0xFFFFFF;
        RenderSystem.setShaderColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1F);
        g.blit(TEXTURE, leftPos + x, topPos + y, u, v, w, h);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, this.imageWidth / 2 - 36 / 2 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 11, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(@NotNull GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        if (refinery != null) {
            FluidTank[] tanks = refinery.getTanks();
            tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 12, topPos + 17, 16, 70);
            tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 64, topPos + 35, 16, 52);
            tanks[2].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 82, topPos + 35, 16, 52);
            tanks[3].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 100, topPos + 35, 16, 52);
            tanks[4].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 118, topPos + 35, 16, 52);
            this.drawElectricityInfo(g, mouseX, mouseY, 158, 18, 16, 88, refinery.getEnergyStored(), refinery.getMaxEnergyStored());
        }
        this.renderTooltip(g, mouseX, mouseY);
    }
}
