package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineTurbineGasBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineTurbineGasMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Vereinfachtes GUI (siehe Klassenkommentar in {@link MachineTurbineGasBlockEntity}) - das
 * Original-Hintergrundbild ({@code gui_turbinegas.png}) wird uebernommen, aber die individuell
 * gezeichneten Widgets (kreisfoermiger RPM-Messer, 7-Segment-Startup-Anzeige, Thermometer-Shader,
 * Start/Stop- und Auto-Modus-Buttons, Leistungsregler) entfallen ersatzlos - reine Statusanzeige
 * (Tanks, Energiepuffer) analog zu {@link GUIMachineLargeTurbine}.
 */
public class GUIMachineTurbineGas extends GuiInfoScreen<MachineTurbineGasMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/generators/gui_turbinegas.png");

    private final MachineTurbineGasBlockEntity turbine;

    public GUIMachineTurbineGas(MachineTurbineGasMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.turbine = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 223;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        int power = (int) Math.min(142, turbine.getEnergyStored() * 142 / Math.max(1L, turbine.getMaxEnergyStored()));
        guiGraphics.blit(TEXTURE, this.leftPos + 26, this.topPos + 109, 0, 223, power, 16);

        turbine.getGasTank().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 65, 16, 48);
        turbine.getLubeTank().renderTank(guiGraphics, this.leftPos + 8, this.topPos + 103, 16, 32);
        turbine.getWaterTank().renderTank(guiGraphics, this.leftPos + 147, this.topPos + 98, 16, 36);
        turbine.getHotsteamTank().renderTank(guiGraphics, this.leftPos + 147, this.topPos + 58, 16, 36);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component name = this.title;
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 129, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        turbine.getGasTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 16, 16, 48);
        turbine.getLubeTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 8, this.topPos + 70, 16, 32);
        turbine.getWaterTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 147, this.topPos + 61, 16, 36);
        turbine.getHotsteamTank().renderTankInfo(guiGraphics, this.font, mouseX, mouseY, this.leftPos + 147, this.topPos + 21, 16, 36);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                26, 109, 142, 16,
                turbine.getEnergyStored(), turbine.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                36, 36, 16, 66,
                mouseX, mouseY,
                turbine.isActive()
                        ? Component.literal("Generator running")
                        : Component.literal("Generator offline"));

        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
