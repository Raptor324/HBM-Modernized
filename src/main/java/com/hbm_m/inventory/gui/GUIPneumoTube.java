package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.network.pneumatic.PneumoTubeBlockEntity;
import com.hbm_m.api.pneumatic.PneumaticNet;
import com.hbm_m.inventory.filter.ModulePatternMatcher;
import com.hbm_m.inventory.menu.PneumoTubeMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.PneumoTubeControlC2SPacket;
import com.hbm_m.network.PneumoTubeControlC2SPacket.Control;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.List;

/**
 * 1:1-Port von {@code GUIPneumoTube} (1.7.10), 176×185.
 *
 * <p>Links der Drucklufttank samt Druckstufenregler, darunter der Redstoneschalter. In der Mitte
 * die fuenfzehn Vorlagenplaetze mit dem Weiss/Schwarz-Schalter daneben. Rechts die beiden
 * Reihenfolgeknoepfe - oben fuer die Empfaenger, unten fuer die Plaetze des Quellinventars.</p>
 *
 * <p>Ein Auswurfrohr ohne Einzugsseite bekommt die schmalere Oberflaeche
 * {@code gui_pneumatic_endpoint.png}: dort gibt es nur den Filter, denn Druckstufe und
 * Reihenfolge betreffen allein das Senden.</p>
 */
public class GUIPneumoTube extends AbstractContainerScreen<PneumoTubeMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_pipe.png");
    private static final ResourceLocation TEXTURE_ENDPOINT = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/storage/gui_pneumatic_endpoint.png");

    private final PneumoTubeBlockEntity tube;

    public GUIPneumoTube(PneumoTubeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tube = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 185;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
        this.titleLabelY = 5;
    }

    /** Ein reines Auswurfrohr zeigt nur den Filter - siehe Klassenkommentar. */
    private boolean endpointOnly() {
        return tube != null && !tube.isCompressor();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        ResourceLocation tex = endpointOnly() ? TEXTURE_ENDPOINT : TEXTURE;
        guiGraphics.blit(tex, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        if (tube == null) return;

        // Weiss- oder Schwarzliste: der Zeiger sitzt oben oder unten am Schalter.
        guiGraphics.blit(tex, leftPos + 139, topPos + (tube.isWhitelist() ? 33 : 47), 176, 0, 3, 6);

        if (endpointOnly()) return;

        if (tube.isRedstone()) {
            guiGraphics.blit(tex, leftPos + 7, topPos + 52, 179, 0, 18, 18);
        }
        guiGraphics.blit(tex, leftPos + 151, topPos + 16, 197, 18 * tube.getReceiveOrder(), 18, 18);
        guiGraphics.blit(tex, leftPos + 151, topPos + 52, 215, 18 * tube.getSendOrder(), 18, 18);

        // Druckstufenregler: der Schieber sitzt vier Bildpunkte je Stufe weiter rechts.
        guiGraphics.blit(tex, leftPos + 6 + 4 * (tube.getTank().getPressure() - 1), topPos + 36, 179, 18, 4, 8);

        // Fuellstand des Tanks. Das Original zeichnet hier eine runde Anzeige; hier ein Balken.
        int fill = tube.getTank().getMaxFill() > 0
                ? tube.getTank().getFill() * 16 / tube.getTank().getMaxFill() : 0;
        guiGraphics.fill(leftPos + 8, topPos + 33 - fill, leftPos + 24, topPos + 33, 0xFFCA6C43);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        if (tube == null) return;

        if (!endpointOnly()) {
            if (isHovering(7, 16, 18, 18, mouseX, mouseY)) {
                tooltip(guiGraphics, mouseX, mouseY,
                        Component.literal(tube.getTank().getFill() + " / " + tube.getTank().getMaxFill() + " mB Air"));
            }
            if (isHovering(7, 52, 18, 18, mouseX, mouseY)) {
                tooltip(guiGraphics, mouseX, mouseY, Component.translatable(
                        tube.isRedstone() ? "gui.hbm_m.pneumo.redstone.on" : "gui.hbm_m.pneumo.redstone.off"));
            }
            if (isHovering(6, 36, 20, 8, mouseX, mouseY)) {
                int pressure = tube.getTank().getPressure();
                tooltip(guiGraphics, mouseX, mouseY,
                        Component.translatable("gui.hbm_m.pneumo.pressure", pressure),
                        Component.translatable("gui.hbm_m.pneumo.range",
                                PneumoTubeBlockEntity.getRangeFromPressure(pressure)));
            }
            if (isHovering(151, 16, 18, 18, mouseX, mouseY)) {
                tooltip(guiGraphics, mouseX, mouseY,
                        Component.translatable("gui.hbm_m.pneumo.receive").withStyle(ChatFormatting.YELLOW),
                        Component.translatable(tube.getReceiveOrder() == PneumaticNet.RECEIVE_ROBIN
                                ? "gui.hbm_m.pneumo.receive.robin" : "gui.hbm_m.pneumo.receive.random"));
            }
            if (isHovering(151, 52, 18, 18, mouseX, mouseY)) {
                tooltip(guiGraphics, mouseX, mouseY,
                        Component.translatable("gui.hbm_m.pneumo.send").withStyle(ChatFormatting.YELLOW),
                        Component.translatable(switch (tube.getSendOrder()) {
                            case PneumaticNet.SEND_LAST -> "gui.hbm_m.pneumo.send.last";
                            case PneumaticNet.SEND_RANDOM -> "gui.hbm_m.pneumo.send.random";
                            default -> "gui.hbm_m.pneumo.send.first";
                        }));
            }
        }

        // Vergleichsart der Vorlage unter dem Zeiger, wie im Original ueber dem Platz.
        if (menu.getCarried().isEmpty()) {
            for (int i = 0; i < PneumoTubeBlockEntity.FILTER_SLOTS; i++) {
                Slot slot = menu.slots.get(i);
                if (!slot.hasItem()) continue;
                if (!isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) continue;

                guiGraphics.renderComponentTooltip(font, List.of(
                        Component.translatable("gui.hbm_m.pneumo.filter.hint").withStyle(ChatFormatting.RED),
                        Component.literal(ModulePatternMatcher.getLabel(tube.getMatcher().getMode(i)))),
                        mouseX, mouseY - 30);
                break;
            }
        }
    }

    private void tooltip(GuiGraphics guiGraphics, int mouseX, int mouseY, Component... lines) {
        guiGraphics.renderComponentTooltip(font, List.of(lines), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tube == null) return super.mouseClicked(mouseX, mouseY, button);

        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (!endpointOnly()) {
            if (isHovering(7, 52, 18, 18, mx, my))  { send(Control.REDSTONE); return true; }
            if (isHovering(6, 36, 20, 8, mx, my))   { send(Control.PRESSURE); return true; }
            if (isHovering(151, 16, 18, 18, mx, my)){ send(Control.RECEIVE_ORDER); return true; }
            if (isHovering(151, 52, 18, 18, mx, my)){ send(Control.SEND_ORDER); return true; }
        }
        if (isHovering(128, 30, 14, 26, mx, my))    { send(Control.WHITELIST); return true; }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void send(Control control) {
        PneumoTubeControlC2SPacket.send(tube.getBlockPos(), control);
    }

    private boolean isHovering(int x, int y, int w, int h, int mouseX, int mouseY) {
        int localX = mouseX - this.leftPos;
        int localY = mouseY - this.topPos;
        return localX >= x && localX < x + w && localY >= y && localY < y + h;
    }
}
