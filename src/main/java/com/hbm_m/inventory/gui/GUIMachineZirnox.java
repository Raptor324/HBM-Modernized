package com.hbm_m.inventory.gui;

import com.hbm_m.client.GuiCompat;
import com.hbm_m.blockentity.machines.MachineZirnoxBlockEntity;
import com.hbm_m.inventory.menu.MachineZirnoxMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ZirnoxControlPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUIReactorZirnox} (203x256): Tankanzeigen als Schiebe-Zeiger, Temperatur/Druck als Rundzeiger,
 * Zuendschalter (144,35), Ablassventil (151,51), Info-/Warnfelder mit den Originaltexten.
 */
public class GUIMachineZirnox extends GuiInfoScreen<MachineZirnoxMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/reactors/gui_zirnox.png");

    private final MachineZirnoxBlockEntity zirnox;

    public GUIMachineZirnox(MachineZirnoxMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.zirnox = menu.getBlockEntity();
        this.imageWidth = 203;
        this.imageHeight = 256;
    }

    /** Original {@code I18nUtil.resolveKeyArray}: Zeilen am {@code $} trennen. */
    private static Component[] lines(String key) {
        String[] split = Component.translatable(key).getString().split("\\$");
        Component[] out = new Component[split.length];
        for (int i = 0; i < split.length; i++) out[i] = Component.literal(split[i]);
        return out;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // тайл может отсутствовать в реплее Flashback
        if (zirnox == null) return;

        zirnox.steamTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 160, topPos + 108, 18, 12);
        zirnox.co2Tank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 142, topPos + 108, 18, 12);
        zirnox.waterTank.renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 178, topPos + 108, 18, 12);

        // Original drawCustomInfo: Tooltip direkt an der Maus
        if (isPointInRect(160, 33, 18, 17, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(Component.literal("Temperature:"),
                    Component.literal("   " + Math.round((zirnox.heat) * 0.00001 * 780 + 20) + "°C")), mouseX, mouseY);
        }
        if (isPointInRect(178, 33, 18, 17, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font, java.util.List.of(Component.literal("Pressure:"),
                    Component.literal("   " + Math.round((zirnox.pressure) * 0.00001 * 30) + " bar")), mouseX, mouseY);
        }

        drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 36, 16, 16, leftPos - 8, topPos + 36 + 16, lines("desc.gui.zirnox.coolant"));
        drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 36 + 16, 16, 16, leftPos - 8, topPos + 36 + 16 + 16, lines("desc.gui.zirnox.pressure"));

        if (zirnox.waterTank.getFill() <= 0) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 36 + 32, 16, 16, leftPos - 8, topPos + 36 + 32 + 16, lines("desc.gui.zirnox.warning1"));
        }

        if (zirnox.co2Tank.getFill() < 4000) {
            drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 36 + 32 + 16, 16, 16, leftPos - 8, topPos + 36 + 32 + 16 + 16, lines("desc.gui.zirnox.warning2"));
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        boolean result = super.mouseClicked(x, y, button);
        // тайл может отсутствовать в реплее Flashback
        if (zirnox == null) return result;

        if (this.leftPos + 144 <= x && this.leftPos + 144 + 14 > x && this.topPos + 35 < y && this.topPos + 35 + 14 >= y) {
            ZirnoxControlPacket.sendToServer(zirnox.getBlockPos(), ZirnoxControlPacket.ACTION_CONTROL);
            playCover();
        }

        if (this.leftPos + 151 <= x && this.leftPos + 151 + 36 > x && this.topPos + 51 < y && this.topPos + 51 + 36 >= y) {
            ZirnoxControlPacket.sendToServer(zirnox.getBlockPos(), ZirnoxControlPacket.ACTION_VENT);
            playCover();
        }

        return result;
    }

    /** Original: {@code hbm:block.rbmk_az5_cover} mit Tonhoehe 0.5. */
    private void playCover() {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(com.hbm_m.sound.ModSounds.RBMK_AZ5_COVER.get(), 0.5F));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component title = this.title;
        guiGraphics.drawString(this.font, title, this.imageWidth / 2 - this.font.width(title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        // тайл может отсутствовать в реплее Flashback
        if (zirnox == null) return;

        GuiLinearGauge.draw(guiGraphics, leftPos + 162, topPos + 114, (double) zirnox.steamTank.getFill() / zirnox.steamTank.getMaxFill(), 2, 5, 0.75, 14, 0, 0x7F0000);
        GuiLinearGauge.draw(guiGraphics, leftPos + 144, topPos + 114, (double) zirnox.co2Tank.getFill() / zirnox.co2Tank.getMaxFill(), 2, 5, 0.75, 14, 0, 0x7F0000);
        GuiLinearGauge.draw(guiGraphics, leftPos + 180, topPos + 114, (double) zirnox.waterTank.getFill() / zirnox.waterTank.getMaxFill(), 2, 5, 0.75, 14, 0, 0x7F0000);

        GuiGaugeNeedle.draw(guiGraphics, leftPos + 169, topPos + 42, (double) zirnox.heat / 100000, 5, 2, 1, 0x7F0000);
        GuiGaugeNeedle.draw(guiGraphics, leftPos + 187, topPos + 42, (double) zirnox.pressure / 100000, 5, 2, 1, 0x7F0000);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        if (zirnox.isOn) {
            for (int x = 0; x < 4; x++)
                for (int y = 0; y < 4; y++)
                    guiGraphics.blit(TEXTURE, this.leftPos + 7 + 36 * x, this.topPos + 15 + 36 * y, 238, 238, 18, 18);
            for (int x = 0; x < 3; x++)
                for (int y = 0; y < 3; y++)
                    guiGraphics.blit(TEXTURE, this.leftPos + 25 + 36 * x, this.topPos + 33 + 36 * y, 238, 238, 18, 18);
            guiGraphics.blit(TEXTURE, this.leftPos + 142, this.topPos + 15, 220, 238, 18, 18);
        }

        this.drawInfoPanel(guiGraphics, -16, 36, PanelType.LARGE_BLUE_INFO);
        this.drawInfoPanel(guiGraphics, -16, 36 + 16, PanelType.LARGE_GREEN_INFO);

        if (zirnox.waterTank.getFill() <= 0)
            this.drawInfoPanel(guiGraphics, -16, 36 + 32, PanelType.LARGE_RED_EXCLAMATION);

        // Original: Panel bei <= 4000, Tooltip bei < 4000
        if (zirnox.co2Tank.getFill() <= 4000)
            this.drawInfoPanel(guiGraphics, -16, 36 + 32 + 16, PanelType.LARGE_RED_EXCLAMATION);
    }
}
