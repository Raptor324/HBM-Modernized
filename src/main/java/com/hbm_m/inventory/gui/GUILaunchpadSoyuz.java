package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.LaunchpadSoyuzBlockEntity;
import com.hbm_m.inventory.menu.LaunchpadSoyuzMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.LaunchpadSoyuzControlPacket;
import com.hbm_m.network.ModPacketHandler;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen of the Soyuz launch pad - pixel-faithful port of legacy {@code GUILaunchpadSoyuz}
 * (original {@code textures/gui/machine/gui_launchpad_soyuz.png}, 194x244, UVs 1:1).
 * Status label via {@code drawConstrainedLabel}, launch button only from READY.
 */
@OnlyIn(Dist.CLIENT)
public class GUILaunchpadSoyuz extends AbstractContainerScreen<LaunchpadSoyuzMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_launchpad_soyuz.png");

    public GUILaunchpadSoyuz(LaunchpadSoyuzMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 194;
        this.imageHeight = 244;
        this.inventoryLabelX = 17;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        LaunchpadSoyuzBlockEntity be = menu.getBlockEntity();
        // тайл может отсутствовать в реплее Flashback
        if (be == null) return;

        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int power = (int) (menu.getEnergyStored() * 52 / LaunchpadSoyuzBlockEntity.MAX_POWER);
        if (power > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 134, topPos + 96 - power, 194, 52 - power, 16, power);
        }

        // Modus-Indikator (Original: cargoMode schiebt die Markierung eine Zelle nach links)
        boolean cargoMode = menu.isCargoMode();
        guiGraphics.blit(TEXTURE, leftPos + 97 - (cargoMode ? 18 : 0), topPos + 52,
                228 - (cargoMode ? 18 : 0), 26, 18, 18);

        blitLed(guiGraphics, leftPos + 157, topPos + 31, be.hasJetFuel());
        blitLed(guiGraphics, leftPos + 175, topPos + 31, be.hasOxidizer());
        blitLed(guiGraphics, leftPos + 139, topPos + 31, menu.getEnergyStored() >= LaunchpadSoyuzBlockEntity.CONSUMPTION);

        int l = be.orbital();
        if (l > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 25, 210 + (l - 1) * 18, 8, 18, 18);
        }

        // Raketen-Steckplatz-Markierung (Original: loadedType)
        if (menu.getLoadedType() == -1) {
            guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 79, 210, 8, 18, 18);
        } else {
            guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 79, 228, 8, 18, 18);
        }

        if (!cargoMode) {
            boolean satPresent = !be.getInventory().getStackInSlot(LaunchpadSoyuzBlockEntity.SLOT_SATELLITE).isEmpty();
            guiGraphics.blit(TEXTURE, leftPos + 97, topPos + 25, satPresent ? 228 : 210, 8, 18, 18);
        } else {
            boolean designatorPresent = !be.getInventory().getStackInSlot(LaunchpadSoyuzBlockEntity.SLOT_DESIGNATOR).isEmpty();
            guiGraphics.blit(TEXTURE, leftPos + 79, topPos + 79, designatorPresent ? 228 : 210, 8, 18, 18);
        }

        if (menu.getStatus() == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING) {
            guiGraphics.blit(TEXTURE, leftPos + 88, topPos + 97, 210, 44, 18, 18);
        }

        // Оригинал: renderTank(x, y НИЗ бака) рисует вверх; порт принимает ВЕРХ области.
        be.getTanks()[0].renderTank(guiGraphics, leftPos + 152, topPos + 44, 16, 52);
        be.getTanks()[1].renderTank(guiGraphics, leftPos + 170, topPos + 44, 16, 52);
    }

    private void blitLed(GuiGraphics guiGraphics, int x, int y, boolean on) {
        guiGraphics.blit(TEXTURE, x, y, on ? 210 : 216, 0, 6, 8);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Оригинал: имя по центру на y=4, инвентарь на x=17.
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 4, 0xFFFFFF, false);
        guiGraphics.drawString(this.font, Component.translatable("container.inventory"),
                this.inventoryLabelX, this.inventoryLabelY, 4210752, false);

        LaunchpadSoyuzBlockEntity.SoyuzStatus status = menu.getStatus();

        if (status == LaunchpadSoyuzBlockEntity.SoyuzStatus.LAUNCHING) {
            // Original: Sekunden:Centesimal (20 Ticks = 1 s)
            int countdown = Math.max(0, menu.getCountdown());
            String secs = String.valueOf(countdown / 20);
            String cents = String.valueOf((countdown % 20) * 5);
            if (secs.length() == 1) secs = "0" + secs;
            if (cents.length() == 1) cents = "0" + cents;
            guiGraphics.drawString(this.font, secs + ":" + cents, 85, 121, 0xFF0000, false);
        } else if (status == LaunchpadSoyuzBlockEntity.SoyuzStatus.ABSENT) {
            drawConstrainedLabel(guiGraphics, Component.translatable("desc.gui.soyuz.idle").getString(), 97, 125, 0xFF0000);
        } else if (status == LaunchpadSoyuzBlockEntity.SoyuzStatus.LOADING) {
            drawConstrainedLabel(guiGraphics, Component.translatable("desc.gui.soyuz.loading").getString(), 97, 125, 0xFF8000);
        } else if (status == LaunchpadSoyuzBlockEntity.SoyuzStatus.FUELING) {
            drawConstrainedLabel(guiGraphics, Component.translatable("desc.gui.soyuz.fueling").getString(), 97, 125, 0xFFFF00);
        } else if (status == LaunchpadSoyuzBlockEntity.SoyuzStatus.READY) {
            drawConstrainedLabel(guiGraphics, Component.translatable("desc.gui.soyuz.ready").getString(), 97, 125, 0x00FF00);
        }
    }

    /** Original {@code GuiInfoContainer.drawConstrainedLabel}: Skalierung auf 22 px Breite, zentriert. */
    private void drawConstrainedLabel(GuiGraphics guiGraphics, String label, int x, int y, int color) {
        float scale = Math.min(1F, 22F / (float) this.font.width(label));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(scale, scale, 1F);
        guiGraphics.drawString(this.font, label,
                (int) (x / scale - this.font.width(label) / 2F),
                (int) (y / scale - (float) this.font.lineHeight / 2F), color, false);
        guiGraphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        LaunchpadSoyuzBlockEntity be = menu.getBlockEntity();
        if (be != null) {
            be.getTanks()[0].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 152, topPos + 44, 16, 52);
            be.getTanks()[1].renderTankInfo(guiGraphics, this.font, mouseX, mouseY, leftPos + 170, topPos + 44, 16, 52);
            renderAreaTooltip(guiGraphics, mouseX, mouseY, 134, 44, 16, 52,
                    Component.literal(menu.getEnergyStored() + " / " + menu.getMaxEnergyStored() + " HE"));
        }

        renderAreaTooltip(guiGraphics, mouseX, mouseY, 79, 52, 18, 18, Component.translatable("desc.gui.soyuz.cargo"));
        renderAreaTooltip(guiGraphics, mouseX, mouseY, 97, 52, 18, 18, Component.translatable("desc.gui.soyuz.satellite"));

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderAreaTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y, int w, int h, Component text) {
        int sx = leftPos + x;
        int sy = topPos + y;
        if (mouseX >= sx && mouseX < sx + w && mouseY >= sy && mouseY < sy + h) {
            guiGraphics.renderTooltip(font, java.util.List.of(text), java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean result = super.mouseClicked(mouseX, mouseY, button);

        // тайл может отсутствовать в реплее Flashback
        LaunchpadSoyuzBlockEntity clickBe = menu.getBlockEntity();
        if (clickBe == null) return result;
        var pos = clickBe.getBlockPos();

        // Оригинальные клик-регионы: (79,52) cargo=true, (97,52) cargo=false, (88,97) launch (nur READY).
        if (inRegion(mouseX, mouseY, 79, 52)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.LAUNCHPAD_SOYUZ_CONTROL,
                    LaunchpadSoyuzControlPacket.setCargo(pos, true));
        } else if (inRegion(mouseX, mouseY, 97, 52)) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.LAUNCHPAD_SOYUZ_CONTROL,
                    LaunchpadSoyuzControlPacket.setCargo(pos, false));
        } else if (inRegion(mouseX, mouseY, 88, 97)
                && menu.getStatus() == LaunchpadSoyuzBlockEntity.SoyuzStatus.READY) {
            playClick();
            ModPacketHandler.sendToServer(ModPacketHandler.LAUNCHPAD_SOYUZ_CONTROL,
                    LaunchpadSoyuzControlPacket.launch(pos));
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
