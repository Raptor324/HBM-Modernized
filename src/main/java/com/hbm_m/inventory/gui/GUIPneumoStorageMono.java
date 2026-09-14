package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.pneumatic.PneumoStorageMonoBlockEntity;
import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.inventory.menu.PneumoStorageMonoMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PneumoStorageControlC2SPacket;
import com.hbm_m.network.PneumoStorageControlC2SPacket.Control;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.Locale;

/**
 * 1:1-Port von {@code GUIPneumoStorageMono} (1.7.10), 200x181.
 *
 * <p>Links die drei Vorlagenplaetze, daneben je ein Fuellbalken von 124 Bildpunkten Laenge und die
 * Menge samt Prozentwert. Rechts oben die runde Druckluftanzeige, darunter der Druckstufenregler.</p>
 */
public class GUIPneumoStorageMono extends AbstractContainerScreen<PneumoStorageMonoMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_mono.png");

    private static final int SLIDER_X = 174;
    private static final int SLIDER_Y = 36;
    private static final int SLIDER_W = 20;
    private static final int SLIDER_H = 8;
    private static final int GAUGE_X = 184;
    private static final int GAUGE_Y = 25;

    /** Original: der Balken beginnt bei (44, 17) und ist voll 124 Bildpunkte lang. */
    private static final int BAR_X = 44;
    private static final int BAR_Y = 17;
    private static final int BAR_MAX_W = 124;
    private static final int BAR_H = 16;

    public GUIPneumoStorageMono(PneumoStorageMonoMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 200;
        this.imageHeight = 181;
        this.titleLabelX = 176 / 2 - font.width(title) / 2;
        this.titleLabelY = 5;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Fuellbalken je Fach - nur wenn eine Vorlage darin liegt.
        for (int i = 0; i < PneumoStorageMonoBlockEntity.INVENTORY_SIZE; i++) {
            if (menu.slots.get(i).getItem().isEmpty()) continue;

            int bar = (int) (menu.getAmount(i) * (long) BAR_MAX_W / PneumoStorageMonoBlockEntity.CAPACITY);
            if (bar <= 0) continue;

            guiGraphics.blit(TEXTURE, leftPos + BAR_X, topPos + BAR_Y + i * 18, 0, 181, bar, BAR_H);
        }

        var tank = menu.getBlockEntity().getTank();
        guiGraphics.blit(TEXTURE, leftPos + SLIDER_X + 4 * (tank.getPressure() - 1), topPos + SLIDER_Y,
                200, 0, 4, 8);

        double fill = tank.getMaxFill() > 0 ? (double) tank.getFill() / tank.getMaxFill() : 0D;
        GuiGaugeNeedle.draw(guiGraphics, leftPos + GAUGE_X, topPos + GAUGE_Y, fill,
                5, 2, 1, 0xCA6C43, 0xAB4223);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // Original: Menge mit Tausenderpunkten plus Prozentwert, schwarz auf dem Balken.
        for (int i = 0; i < PneumoStorageMonoBlockEntity.INVENTORY_SIZE; i++) {
            if (menu.slots.get(i).getItem().isEmpty()) continue;

            int amount = menu.getAmount(i);
            double percent = ((int) (amount * 1000D / PneumoStorageMonoBlockEntity.CAPACITY)) / 10D;
            String text = String.format(Locale.US, "%,d", amount) + " (" + percent + "%)";

            guiGraphics.drawString(font, text, 50, 22 + i * 18, 0x000000, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (isOverSlider(mouseX, mouseY)) {
            int pressure = menu.getBlockEntity().getTank().getPressure();
            guiGraphics.renderComponentTooltip(font, List.of(
                    Component.translatable("gui.hbm_m.pneumo.pressure", pressure),
                    Component.translatable("gui.hbm_m.pneumo.range",
                            PneumoTubeBlockEntity.getRangeFromPressure(pressure)),
                    Component.translatable("gui.hbm_m.pneumo.pressure.hint")),
                    mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isOverSlider((int) mouseX, (int) mouseY)) {
            PneumoStorageControlC2SPacket.send(menu.getBlockEntity().getBlockPos(), Control.PRESSURE);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isOverSlider(int mouseX, int mouseY) {
        int localX = mouseX - leftPos;
        int localY = mouseY - topPos;
        return localX >= SLIDER_X && localX < SLIDER_X + SLIDER_W
                && localY >= SLIDER_Y && localY < SLIDER_Y + SLIDER_H;
    }
}
