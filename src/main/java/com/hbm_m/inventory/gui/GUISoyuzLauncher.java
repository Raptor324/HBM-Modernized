package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.inventory.menu.SoyuzLauncherMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.SoyuzLauncherControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the Soyuz Launcher - direct port of legacy {@code GUISoyuzLauncher}
 * (same texture, same UV offsets/coordinates).
 */
public class GUISoyuzLauncher extends AbstractContainerScreen<SoyuzLauncherMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_soyuz.png");

    public GUISoyuzLauncher(SoyuzLauncherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        SoyuzLauncherBlockEntity be = menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;

        int powerHeight = (int) Math.min(34, menu.getMaxEnergyStored() > 0
                ? menu.getEnergyStored() * 34 / menu.getMaxEnergyStored() : 0);
        if (powerHeight > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 49, topPos + 106 - powerHeight, 194, 52 - powerHeight, 6, powerHeight);
        }

        guiGraphics.blit(TEXTURE, leftPos + 61, topPos + 17, 176 + (be.hasRocket() ? 18 : 0), 0, 18, 18);

        int designator = be.designator();
        if (designator > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 61, topPos + 35, 176 + (designator - 1) * 18, 0, 18, 18);
        }

        int mode = menu.getMode();
        guiGraphics.blit(TEXTURE, leftPos + 88, topPos + 17 + mode * 18, 176, 18 + mode * 18, 18, 18);

        int orbital = be.orbital();
        if (orbital > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 115, topPos + 35, 176 + (orbital - 1) * 18, 0, 18, 18);
        }

        int satellite = be.satellite();
        if (satellite > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 115, topPos + 17, 176 + (satellite - 1) * 18, 0, 18, 18);
        }

        if (menu.isStarting()) {
            guiGraphics.blit(TEXTURE, leftPos + 151, topPos + 17, 176, 54, 18, 18);
        }

        blitLed(guiGraphics, leftPos + 13, topPos + 23, be.hasFuel());
        blitLed(guiGraphics, leftPos + 31, topPos + 23, be.hasOxy());
        blitLed(guiGraphics, leftPos + 49, topPos + 59, be.hasPower());

        be.getTanks()[0].renderTank(guiGraphics, leftPos + 8, topPos + 88, 16, 52);
        be.getTanks()[1].renderTank(guiGraphics, leftPos + 26, topPos + 88, 16, 52);
    }

    private void blitLed(GuiGraphics guiGraphics, int x, int y, boolean on) {
        guiGraphics.blit(TEXTURE, x, y, on ? 212 : 218, 0, 6, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        SoyuzLauncherBlockEntity be = menu.getBlockEntity();
        int countdown = menu.getCountdown();
        String secs = String.valueOf(countdown / 20);
        String cents = String.valueOf((countdown % 20) * 5);
        if (secs.length() == 1) secs = "0" + secs;
        if (cents.length() == 1) cents += "0";

        guiGraphics.pose().pushPose();
        float scale = 0.5F;
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.drawString(font, secs + ":" + cents, (int) (153.5F / scale), (int) (37.5F / scale), 0xFF0000, false);
        guiGraphics.pose().popPose();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        renderStatArea(guiGraphics, mouseX, mouseY, 43, 17, "The Soyuz goes here");
        renderStatArea(guiGraphics, mouseX, mouseY, 43, 35, "Designator only for CARGO MODE");
        renderStatArea(guiGraphics, mouseX, mouseY, 133, 17, "The payload for SATELLITE MODE");
        renderStatArea(guiGraphics, mouseX, mouseY, 133, 35, "The orbital module for special payloads");
        renderStatArea(guiGraphics, mouseX, mouseY, 88, 17, "SATELLITE MODE");
        renderStatArea(guiGraphics, mouseX, mouseY, 88, 35, "CARGO MODE");

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderStatArea(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, String text) {
        int sx = leftPos + x;
        int sy = topPos + y;
        if (mouseX >= sx && mouseX < sx + 18 && mouseY >= sy && mouseY < sy + 18) {
            guiGraphics.renderTooltip(font, List.of(Component.literal(text)), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);

        // тайл может отсутствовать в реплее Flashback
        SoyuzLauncherBlockEntity clickBe = menu.getBlockEntity();
        if (clickBe == null) return result;
        var pos = clickBe.getBlockPos();

        if (inRegion(mouseX, mouseY, 88, 17)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL,
                    SoyuzLauncherControlPacket.setMode(pos, SoyuzLauncherBlockEntity.MODE_SATELLITE));
        } else if (inRegion(mouseX, mouseY, 88, 35)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL,
                    SoyuzLauncherControlPacket.setMode(pos, SoyuzLauncherBlockEntity.MODE_CARGO));
        } else if (inRegion(mouseX, mouseY, 151, 17)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL,
                    SoyuzLauncherControlPacket.start(pos));
        }

        return result;
    }

    private boolean inRegion(double mouseX, double mouseY, int x, int y) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + 18
                && mouseY >= topPos + y && mouseY < topPos + y + 18;
    }

    private void playClick() {
        this.minecraft.getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }
}
