package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.machines.MachineMixerBlockEntity;
import com.hbm_m.inventory.fluid.tank.FluidTank;
import com.hbm_m.inventory.menu.MachineMixerMenu;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for the Industrial Mixer.
 *
 * <p>Layout (derived from {@code gui_mixer.png}, 256x256, panel area 176x204):</p>
 * <ul>
 *   <li>Tank A (input): x=23, y=23, 16x69</li>
 *   <li>Tank B (input): x=43, y=23, 16x69</li>
 *   <li>Output tank:    x=117, y=23, 16x69</li>
 *   <li>Mixer paddle / progress area: x=62-111, y=22-77</li>
 *   <li>Battery slot: bottom-left under tank A, x=23, y=95</li>
 *   <li>Energy bar: solid-fill overlay (no dedicated texture region)</li>
 * </ul>
 */
public class GUIMachineMixer extends GuiInfoScreen<MachineMixerMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_mixer.png");

    private static final int TANK_A_X = 23;
    private static final int TANK_B_X = 43;
    private static final int OUTPUT_X = 117;
    private static final int TANK_Y = 23;
    private static final int TANK_W = 16;
    private static final int TANK_H = 69;

    private static final int ENERGY_BAR_X = 156;
    private static final int ENERGY_BAR_Y = 22;
    private static final int ENERGY_BAR_W = 8;
    private static final int ENERGY_BAR_H = 66;

    private static final int PROGRESS_X = 72;
    private static final int PROGRESS_Y = 37;
    private static final int PROGRESS_W = 33;
    private static final int PROGRESS_H = 14;

    private final MachineMixerBlockEntity mixer;

    public GUIMachineMixer(MachineMixerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.mixer = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 204;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        if (mixer != null) { // тайл может отсутствовать в реплее Flashback
            // Fluid tank levels
            mixer.getTank(MachineMixerBlockEntity.TANK_INPUT_A)
                    .renderTank(guiGraphics, this.leftPos + TANK_A_X, this.topPos + TANK_Y, TANK_W, TANK_H);
            mixer.getTank(MachineMixerBlockEntity.TANK_INPUT_B)
                    .renderTank(guiGraphics, this.leftPos + TANK_B_X, this.topPos + TANK_Y, TANK_W, TANK_H);
            mixer.getTank(MachineMixerBlockEntity.TANK_OUTPUT)
                    .renderTank(guiGraphics, this.leftPos + OUTPUT_X, this.topPos + TANK_Y, TANK_W, TANK_H);

            // Re-draw the panel overlay so the tank fill doesn't cover the gauge frame graphics
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

            // Energy bar (no dedicated texture region, drawn as a solid overlay)
            long power = mixer.getEnergyStored();
            long maxPower = Math.max(mixer.getMaxEnergyStored(), 1L);
            int filled = (int) (power * ENERGY_BAR_H / maxPower);
            if (filled > 0) {
                if (filled > ENERGY_BAR_H) {
                    filled = ENERGY_BAR_H;
                }
                int x0 = this.leftPos + ENERGY_BAR_X;
                int y0 = this.topPos + ENERGY_BAR_Y + (ENERGY_BAR_H - filled);
                guiGraphics.fill(x0, y0, x0 + ENERGY_BAR_W, y0 + filled, 0xFF3FCFE0);
            }

            // Progress arrow / mixing animation
            int progress = mixer.getProgressScaled(PROGRESS_W);
            if (progress > 0) {
                guiGraphics.blit(TEXTURE, this.leftPos + PROGRESS_X, this.topPos + PROGRESS_Y, 192, 0, progress, PROGRESS_H);
            }
        }

        drawInfoPanel(guiGraphics, 78, 67, PanelType.SMALL_BLUE_INFO);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void renderTooltip(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        if (mixer == null) return; // тайл может отсутствовать в реплее Flashback

        FluidTank tankA = mixer.getTank(MachineMixerBlockEntity.TANK_INPUT_A);
        FluidTank tankB = mixer.getTank(MachineMixerBlockEntity.TANK_INPUT_B);
        FluidTank tankOut = mixer.getTank(MachineMixerBlockEntity.TANK_OUTPUT);

        tankA.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + TANK_A_X, this.topPos + TANK_Y, TANK_W, TANK_H);
        tankB.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + TANK_B_X, this.topPos + TANK_Y, TANK_W, TANK_H);
        tankOut.renderTankInfo(guiGraphics, this.font, mouseX, mouseY,
                this.leftPos + OUTPUT_X, this.topPos + TANK_Y, TANK_W, TANK_H);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                ENERGY_BAR_X, ENERGY_BAR_Y, ENERGY_BAR_W, ENERGY_BAR_H,
                mixer.getEnergyStored(), mixer.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                78, 67, 8, 8,
                this.leftPos + 78, this.topPos + 67,
                Component.literal("Progress:"),
                Component.literal("   " + mixer.getProgress() + " / " + mixer.getMaxProgress()));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
