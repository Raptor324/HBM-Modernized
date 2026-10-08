package com.hbm_m.inventory.gui;

import java.util.Arrays;

import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.SoyuzLauncherMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.SoyuzLauncherControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 1:1 {@code GUISoyuzLauncher}: 194x244, Original-Textur {@code gui/machine/gui_soyuz.png}, Strom bei 134, Kerosin bei 152,
 * Sauerstoff bei 170 (je 16x52 ab y 44), Modusknoepfe Fracht 79/52 und Satellit 97/52, Startknopf 88/97, Countdown 85/121.
 */
public class GUISoyuzLauncher extends GuiInfoScreen<SoyuzLauncherMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_soyuz.png");

    public GUISoyuzLauncher(SoyuzLauncherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 244;
    }

    private static Component[] lines(String key) {
        return Arrays.stream(Component.translatable(key).getString().split("\\$")).map(Component::literal).toArray(Component[]::new);
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, gui, mouseX, mouseY, partialTick);
        super.render(gui, mouseX, mouseY, partialTick);

        SoyuzLauncherBlockEntity launcher = menu.getBlockEntity();
        if (launcher != null) {
            launcher.getTanks()[0].renderTankInfo(gui, this.font, mouseX, mouseY, leftPos + 152, topPos + 44, 16, 52);
            launcher.getTanks()[1].renderTankInfo(gui, this.font, mouseX, mouseY, leftPos + 170, topPos + 44, 16, 52);
            this.drawElectricityInfo(gui, mouseX, mouseY, 134, 44, 16, 52, menu.getEnergyStored(), menu.getMaxEnergyStored());
        }

        this.drawCustomInfoStat(gui, mouseX, mouseY, -16, 53, 16, 16, leftPos - 8, topPos + 53 + 16, lines("desc.gui.soyuz.desc"));

        this.drawCustomInfoStat(gui, mouseX, mouseY, 79, 52, 18, 18, mouseX, mouseY, lines("desc.gui.soyuz.cargo"));
        this.drawCustomInfoStat(gui, mouseX, mouseY, 97, 52, 18, 18, mouseX, mouseY, lines("desc.gui.soyuz.satellite"));

        this.renderTooltip(gui, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double x, double y, int i) {
        boolean result = super.mouseClicked(x, y, i);

        // тайл может отсутствовать в реплее Flashback
        SoyuzLauncherBlockEntity launcher = menu.getBlockEntity();
        if (launcher == null) return result;
        var pos = launcher.getBlockPos();

        if (leftPos + 97 <= x && leftPos + 97 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y) {
            playClickSound();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL, SoyuzLauncherControlPacket.setMode(pos, 0));
        }

        if (leftPos + 79 <= x && leftPos + 79 + 18 > x && topPos + 52 < y && topPos + 52 + 18 >= y) {
            playClickSound();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL, SoyuzLauncherControlPacket.setMode(pos, 1));
        }

        if (leftPos + 88 <= x && leftPos + 88 + 18 > x && topPos + 97 < y && topPos + 97 + 18 >= y) {
            playClickSound();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL, SoyuzLauncherControlPacket.start(pos));
        }

        return result;
    }

    @Override
    protected void renderLabels(GuiGraphics gui, int mouseX, int mouseY) {
        String name = this.title.getString();
        gui.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 4, 0xffffff, false);
        gui.drawString(this.font, this.playerInventoryTitle, 17, this.imageHeight - 96 + 2, 4210752, false);

        int countdown = menu.getCountdown();
        String secs = "" + countdown / 20;
        String cents = "" + (countdown % 20) * 5;
        if (secs.length() == 1) secs = "0" + secs;
        if (cents.length() == 1) cents += "0";

        gui.drawString(this.font, secs + ":" + cents, 85, 121, 0xff0000, false);
    }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        gui.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        SoyuzLauncherBlockEntity launcher = menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        if (launcher == null) return;

        long maxPower = menu.getMaxEnergyStored();
        int i = maxPower > 0 ? (int) (menu.getEnergyStored() * 52 / maxPower) : 0;
        gui.blit(TEXTURE, leftPos + 134, topPos + 96 - i, 194, 52 - i, 16, i);

        gui.blit(TEXTURE, leftPos + 97, topPos + 79, 210 + (launcher.hasRocket() ? 18 : 0), 8, 18, 18);
        int j = launcher.designator();

        if (j > 0)
            gui.blit(TEXTURE, leftPos + 79, topPos + 79, 210 + (j - 1) * 18, 8, 18, 18);

        int k = menu.getMode();
        gui.blit(TEXTURE, leftPos + 97 - k * 18, topPos + 52, 228 - k * 18, 26, 18, 18);

        int l = launcher.orbital();

        if (l > 0)
            gui.blit(TEXTURE, leftPos + 79, topPos + 25, 210 + (l - 1) * 18, 8, 18, 18);

        int m = launcher.satellite();

        if (m > 0)
            gui.blit(TEXTURE, leftPos + 97, topPos + 25, 210 + (m - 1) * 18, 8, 18, 18);

        if (menu.isStarting())
            gui.blit(TEXTURE, leftPos + 88, topPos + 97, 210, 44, 18, 18);

        gui.blit(TEXTURE, leftPos + 157, topPos + 31, launcher.hasFuel() ? 210 : 216, 0, 6, 8);
        gui.blit(TEXTURE, leftPos + 175, topPos + 31, launcher.hasOxy() ? 210 : 216, 0, 6, 8);
        gui.blit(TEXTURE, leftPos + 139, topPos + 31, menu.getEnergyStored() >= launcher.getPowerRequired() ? 210 : 216, 0, 6, 8);

        launcher.getTanks()[0].renderTank(gui, leftPos + 152, topPos + 96 - 52, 16, 52);
        launcher.getTanks()[1].renderTank(gui, leftPos + 170, topPos + 96 - 52, 16, 52);

        this.drawInfoPanel(gui, -16, 53, PanelType.LARGE_BLUE_INFO);
    }
}
