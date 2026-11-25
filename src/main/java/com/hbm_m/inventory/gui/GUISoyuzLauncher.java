package com.hbm_m.inventory.gui;

import java.util.List;

import com.hbm_m.blockentity.machines.SoyuzLauncherBlockEntity;
import com.hbm_m.inventory.menu.SoyuzLauncherMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.SoyuzLauncherControlPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen for the Soyuz Launcher - pixel-faithful port of legacy {@code GUISoyuzLauncher}
 * (original {@code textures/gui/machine/gui_soyuz.png}, 194x244, all UVs/coordinates 1:1).
 */
@OnlyIn(Dist.CLIENT)
public class GUISoyuzLauncher extends AbstractContainerScreen<SoyuzLauncherMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_soyuz.png");

    public GUISoyuzLauncher(SoyuzLauncherMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 244;
        this.inventoryLabelX = 17;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        SoyuzLauncherBlockEntity be = menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;

        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int i = (int) be.getPowerScaled(52);
        if (i > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 134, topPos + 96 - i, 194, 52 - i, 16, i);
        }

        guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 79, 210 + (be.hasRocket() ? 18 : 0), 8, 18, 18);

        int j = be.designator();
        if (j > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 79, 210 + (j - 1) * 18, 8, 18, 18);
        }

        int k = menu.getMode();
        guiGraphics.blit(TEXTURE, leftPos + 97 - k * 18, topPos + 52, 228 - k * 18, 26, 18, 18);

        int l = be.orbital();
        if (l > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 25, 210 + (l - 1) * 18, 8, 18, 18);
        }

        int m = be.satellite();
        if (m > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 25, 210 + (m - 1) * 18, 8, 18, 18);
        }

        if (menu.isStarting()) {
            guiGraphics.blit(TEXTURE, leftPos + 88, topPos + 97, 210, 44, 18, 18);
        }

        blitLed(guiGraphics, leftPos + 157, topPos + 31, be.hasFuel());
        blitLed(guiGraphics, leftPos + 175, topPos + 31, be.hasOxy());
        blitLed(guiGraphics, leftPos + 139, topPos + 31, be.hasPower());

        // Оригинал: renderTank(x, y НИЗ бака) рисует вверх; порт принимает ВЕРХ области.
        be.getTanks()[0].renderTank(guiGraphics, leftPos + 152, topPos + 44, 16, 52);
        be.getTanks()[1].renderTank(guiGraphics, leftPos + 170, topPos + 44, 16, 52);
    }

    private void blitLed(GuiGraphics guiGraphics, int x, int y, boolean on) {
        guiGraphics.blit(TEXTURE, x, y, on ? 210 : 216, 0, 6, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Оригинал: имя по центру на y=4, инвентарь на x=17, отсчёт красным на (85,121).
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 4, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"), this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        int countdown = Math.max(0, menu.getCountdown());
        String secs = String.valueOf(countdown / 20);
        String cents = String.valueOf((countdown % 20) * 5);
        if (secs.length() == 1) secs = "0" + secs;
        if (cents.length() == 1) cents = "0" + cents;

        guiGraphics.drawString(this.font, secs + ":" + cents, 85, 121, 0xFF0000, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        SoyuzLauncherBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            be.getTanks()[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 152, topPos + 44, 16, 52);
            be.getTanks()[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 170, topPos + 44, 16, 52);
            renderAreaTooltip(guiGraphics, mouseX, mouseY, 134, 44, 16, 52,
                    Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " HE"));
        }

        renderAreaTooltip(guiGraphics, mouseX, mouseY, -16, 53, 16, 16, Component.translatable("desc.gui.soyuz.desc"));
        renderAreaTooltip(guiGraphics, mouseX, mouseY, 79, 52, 18, 18, Component.translatable("desc.gui.soyuz.cargo"));
        renderAreaTooltip(guiGraphics, mouseX, mouseY, 97, 52, 18, 18, Component.translatable("desc.gui.soyuz.satellite"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderAreaTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int w, int h, Component text) {
        int sx = leftPos + x;
        int sy = topPos + y;
        if (mouseX >= sx && mouseX < sx + w && mouseY >= sy && mouseY < sy + h) {
            guiGraphics.renderTooltip(font, List.of(text), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);

        // тайл может отсутствовать в реплее Flashback
        SoyuzLauncherBlockEntity clickBe = menu.getBlockEntity();
        if (clickBe == null) return result;
        var pos = clickBe.getBlockPos();

        // Оригинальные клик-регионы: (97,52) aux0 -> satellite, (79,52) aux1 -> cargo, (88,97) -> launch.
        if (inRegion(mouseX, mouseY, 97, 52)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL,
                    SoyuzLauncherControlPacket.setMode(pos, SoyuzLauncherBlockEntity.MODE_SATELLITE));
        } else if (inRegion(mouseX, mouseY, 79, 52)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.SOYUZ_LAUNCHER_CONTROL,
                    SoyuzLauncherControlPacket.setMode(pos, SoyuzLauncherBlockEntity.MODE_CARGO));
        } else if (inRegion(mouseX, mouseY, 88, 97)) {
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
