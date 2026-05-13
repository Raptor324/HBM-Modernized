package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hbm_m.block.entity.machines.MachineRadarBlockEntity;
import com.hbm_m.inventory.menu.MachineRadarMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateRadarC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class GUIMachineRadar extends GuiInfoScreen<MachineRadarMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_radar.png");

    private static final int MAP_SIZE = 192;
    private static final int MAP_CENTER_X = 108;
    private static final int MAP_CENTER_Y = 117;
    private static final int TOGGLE_X = -10;

    private final MachineRadarBlockEntity radar;
    private final Random noiseRandom = new Random();

    public GUIMachineRadar(MachineRadarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.radar = menu.getBlockEntity();
        this.imageWidth = 216;
        this.imageHeight = 234;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        guiGraphics.blit(TEXTURE, this.leftPos - 14, this.topPos + 94, 216, 198, 14, 46);

        renderModeButtons(guiGraphics);
        renderPowerBar(guiGraphics);

        if (radar.jammed) {
            renderJammedNoise(guiGraphics);
        } else {
            renderRadarContacts(guiGraphics);
        }
    }

    private void renderModeButtons(GuiGraphics guiGraphics) {
        if (radar.scanMissiles || (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + 98, 230, 202, 8, 8);
        }
        if (radar.scanPlayers || (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + 108, 230, 212, 8, 8);
        }
        if (radar.smartMode || (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + 118, 230, 222, 8, 8);
        }
        if (radar.redMode || (radar.jammed && noiseRandom.nextBoolean())) {
            guiGraphics.blit(TEXTURE, leftPos + TOGGLE_X, topPos + 128, 230, 232, 8, 8);
        }
    }

    private void renderPowerBar(GuiGraphics guiGraphics) {
        int bar = (int) radar.getPowerScaled(200);
        if (bar > 0) {
            guiGraphics.blit(TEXTURE, leftPos + 8, topPos + 221, 0, 234, bar, 16);
        }
    }

    private void renderJammedNoise(GuiGraphics guiGraphics) {
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                guiGraphics.blit(TEXTURE, leftPos + 8 + i * 40, topPos + 17 + j * 40, 216, 118 + noiseRandom.nextInt(41), 40, 40);
            }
        }
    }

    private void renderRadarContacts(GuiGraphics guiGraphics) {
        int radarWidth = MachineRadarBlockEntity.RADAR_RANGE * 2 + 1;

        for (int[] contact : radar.nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }

            int px = leftPos + MAP_CENTER_X + (int) (((contact[0] - radar.getBlockPos().getX()) * 1.0D / radarWidth) * MAP_SIZE) - 4;
            int py = topPos + MAP_CENTER_Y + (int) (((contact[2] - radar.getBlockPos().getZ()) * 1.0D / radarWidth) * MAP_SIZE) - 4;

            int iconIndex = Math.max(0, contact[4]);
            guiGraphics.blit(TEXTURE, px, py, 216, 8 * iconIndex, 8, 8);

        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String titleText = this.title.getString();
        guiGraphics.drawString(this.font, titleText, this.imageWidth / 2 - this.font.width(titleText) / 2, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                8, 221, 200, 7,
                radar.getEnergyStored(), radar.getMaxEnergyStored());

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, 98, 8, 8,
                mouseX, mouseY,
            Component.translatable("gui.hbm_m.radar.tooltip.detect_missiles"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, 108, 8, 8,
                mouseX, mouseY,
            Component.translatable("gui.hbm_m.radar.tooltip.detect_players"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, 118, 8, 8,
                mouseX, mouseY,
            Component.translatable("gui.hbm_m.radar.tooltip.smart_mode"),
            Component.translatable("gui.hbm_m.radar.tooltip.smart_mode.desc"));

        drawCustomInfoStat(guiGraphics, mouseX, mouseY,
                TOGGLE_X, 128, 8, 8,
                mouseX, mouseY,
            Component.translatable("gui.hbm_m.radar.tooltip.red_mode"),
            Component.translatable("gui.hbm_m.radar.tooltip.red_mode.on"),
            Component.translatable("gui.hbm_m.radar.tooltip.red_mode.off"));

        renderContactHoverTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderContactHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (radar.jammed || radar.nearbyMissiles.isEmpty()) {
            return;
        }

        int radarWidth = MachineRadarBlockEntity.RADAR_RANGE * 2 + 1;
        for (int[] contact : radar.nearbyMissiles) {
            if (contact == null || contact.length < 5) {
                continue;
            }

            int x = leftPos + MAP_CENTER_X + (int) (((contact[0] - radar.getBlockPos().getX()) * 1.0D / radarWidth) * MAP_SIZE);
            int z = topPos + MAP_CENTER_Y + (int) (((contact[2] - radar.getBlockPos().getZ()) * 1.0D / radarWidth) * MAP_SIZE);

            boolean hovered = mouseX + 5 > x && mouseX - 4 < x && mouseY + 5 > z && mouseY - 4 < z;
            if (!hovered) {
                continue;
            }

            int relX = contact[0] - radar.getBlockPos().getX();
            int relY = contact[1] - radar.getBlockPos().getY();
            int relZ = contact[2] - radar.getBlockPos().getZ();

            int distance = (int) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            int distanceH = (int) Math.sqrt(relX * relX + relZ * relZ);

            List<Component> text = new ArrayList<>();
            text.add(Component.literal(radar.getTargetTypeName(contact[4])));
            text.add(Component.translatable("gui.hbm_m.radar.contact.velocity", contact[3]));
            text.add(Component.translatable("gui.hbm_m.radar.contact.distance", distance));
            text.add(Component.translatable("gui.hbm_m.radar.contact.distance_h", distanceH));
            text.add(Component.translatable("gui.hbm_m.radar.contact.coords", contact[0], contact[1], contact[2]));
            guiGraphics.renderComponentTooltip(this.font, text, x, z);
            return;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isPointInRect(TOGGLE_X, 98, 8, 8, (int) mouseX, (int) mouseY)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR, new UpdateRadarC2SPacket(radar.getBlockPos(), 0));
                return true;
            }
            if (isPointInRect(TOGGLE_X, 108, 8, 8, (int) mouseX, (int) mouseY)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR, new UpdateRadarC2SPacket(radar.getBlockPos(), 1));
                return true;
            }
            if (isPointInRect(TOGGLE_X, 118, 8, 8, (int) mouseX, (int) mouseY)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR, new UpdateRadarC2SPacket(radar.getBlockPos(), 2));
                return true;
            }
            if (isPointInRect(TOGGLE_X, 128, 8, 8, (int) mouseX, (int) mouseY)) {
                playClickSound();
                ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR, new UpdateRadarC2SPacket(radar.getBlockPos(), 3));
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}
