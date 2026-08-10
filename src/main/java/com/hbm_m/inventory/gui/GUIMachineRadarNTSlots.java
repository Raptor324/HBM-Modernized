package com.hbm_m.inventory.gui;
import com.hbm_m.client.GuiCompat;

import java.util.List;

import com.hbm_m.blockentity.machines.MachineRadarBlockEntity;
import com.hbm_m.inventory.menu.MachineRadarSlotsMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ModPacketHandler;
import com.hbm_m.network.UpdateRadarC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI слотов радара (порт GUIMachineRadarNTSlots из 1.7.10, текстура gui_radar_link.png).
 *
 * Размер 176x184. Содержит 10 слотов машины (8 линк-слотов, linker к экрану, батарея)
 * и инвентарь игрока. Кнопка-стрелка в (5,5) возвращает в главный экран радара.
 * Бар энергии: i = power*160/maxPower, source (0,185), позиция (8,64).
 */
public class GUIMachineRadarNTSlots extends GuiInfoScreen<MachineRadarSlotsMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_radar_link.png");

    private static final int BACK_BTN_X = 5;
    private static final int BACK_BTN_Y = 5;

    private final MachineRadarBlockEntity radar;

    public GUIMachineRadarNTSlots(MachineRadarSlotsMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.radar = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 184;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Бар энергии: i = power*160/maxPower, source (0,185).
        int filled = (int) radar.getPowerScaled(160);
        if (filled > 0) {
            guiGraphics.blit(TEXTURE, this.leftPos + 8, this.topPos + 64, 0, 185, filled, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Заголовок и «inventory» как в 1.7.10.
        guiGraphics.drawString(this.font, this.title,
                this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 0x404040, false);
        Component inv = Component.translatable("container.inventory");
        guiGraphics.drawString(this.font, inv, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip кнопки «назад» (порт radar.toggleGui).
        if (isPointInRect(BACK_BTN_X, BACK_BTN_Y, 8, 8, mouseX, mouseY)) {
            guiGraphics.renderComponentTooltip(this.font,
                    java.util.List.of(Component.translatable("gui.hbm_m.radar.tooltip.toggle_gui")),
                    mouseX, mouseY);
        }

        drawElectricityInfo(guiGraphics, mouseX, mouseY,
                8, 64, 160, 16,
                radar.getEnergyStored(), radar.getMaxEnergyStored());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isPointInRect(BACK_BTN_X, BACK_BTN_Y, 8, 8, (int) mouseX, (int) mouseY)) {
            playClickSound();
            ModPacketHandler.sendToServer(ModPacketHandler.UPDATE_RADAR,
                    UpdateRadarC2SPacket.openMain(radar.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
