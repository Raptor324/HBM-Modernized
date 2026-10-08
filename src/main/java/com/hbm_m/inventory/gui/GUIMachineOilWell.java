package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * 1:1-Port von {@code GUIMachineOilWell} (1.7.10): das gemeinsame GUI von Bohrturm, Pumpe und
 * Fracking-Turm. 184x190, Oel-Tank bei 76, Gas-Tank bei 112, beim Fracking-Turm zusaetzlich der
 * schmale Fracksol-Tank bei 54 (sonst die Abdeckung aus der Textur).
 */
public abstract class GUIMachineOilWell<T extends AbstractContainerMenu> extends GuiInfoScreen<T> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_well.png");

    protected GUIMachineOilWell(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 184;
        this.imageHeight = 190;
    }

    /** Tanks wie im Original: 0 = Oel, 1 = Gas, optional 2 = Fracksol. Null, wenn der Tile fehlt. */
    protected abstract FluidTank[] tanks();

    protected abstract long power();

    protected abstract long maxPower();

    protected abstract int indicator();

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        FluidTank[] tanks = tanks();
        if (tanks != null) { // тайл может отсутствовать в реплее Flashback
            tanks[0].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 76, topPos + 74 - 52, 16, 52);
            tanks[1].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 112, topPos + 74 - 52, 16, 52);
            if (tanks.length >= 3) {
                tanks[2].renderTankInfo(g, this.font, mouseX, mouseY, leftPos + 54, topPos + 45, 6, 32);
            }
        }

        this.drawCustomInfoStat(g, mouseX, mouseY, 160, 21, 8, 8, mouseX, mouseY,
                Component.translatable("desc.gui.upgrade"),
                Component.translatable("desc.gui.upgrade.speed"),
                Component.translatable("desc.gui.upgrade.power"),
                Component.translatable("desc.gui.upgrade.afterburner"),
                Component.translatable("desc.gui.upgrade.overdrive"));

        this.drawElectricityInfo(g, mouseX, mouseY, 8, 22, 16, 34, power(), maxPower());
        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        String name = this.title.getString();
        g.drawString(this.font, name, 126 - this.font.width(name) / 2, 10, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 12, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        long max = maxPower();
        int i = max <= 0 ? 0 : (int) (power() * 34 / max);
        g.blit(TEXTURE, leftPos + 8, topPos + 56 - i, 184, 34 - i, 16, i);

        int k = indicator();
        if (k != 0) {
            g.blit(TEXTURE, leftPos + 50, topPos + 19, 184 + (k - 1) * 14, 34, 14, 14);
        }

        FluidTank[] tanks = tanks();
        if (tanks == null || tanks.length < 3) {
            g.blit(TEXTURE, leftPos + 48, topPos + 44, 200, 0, 18, 34);
        }

        // Original: renderTank(guiLeft + 76, guiTop + 74, ...) - Port-renderTank erwartet die Oberkante
        if (tanks != null) {
            tanks[0].renderTank(g, leftPos + 76, topPos + 74 - 52, 16, 52);
            tanks[1].renderTank(g, leftPos + 112, topPos + 74 - 52, 16, 52);
            if (tanks.length > 2) {
                tanks[2].renderTank(g, leftPos + 54, topPos + 77 - 32, 6, 32);
            }
        }

        this.drawInfoPanel(g, 160, 21, PanelType.SMALL_BLUE_STAR);
    }
}
