package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMiningLaserBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMiningLaserMenu;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur samt ihrer Overlays: Energiesaeule links,
 * Bohrfortschritt in der Mitte, und der <b>An/Aus-Knopf</b> daneben.
 *
 * <p>Ueber dem Knopf steht die Kantenlaenge des Schachts, damit man beim Einbauen von
 * Reichweiten-Modulen sofort sieht, wie breit der Laser gleich graebt.</p>
 */
public class GUIMachineMiningLaser extends AbstractContainerScreen<MachineMiningLaserMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/machine/gui_laser_miner.png");

    private final MachineMiningLaserBlockEntity blockEntity;

    public GUIMachineMiningLaser(MachineMiningLaserMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 222;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            // Original: der gedrueckte Knopf aus (200,0).
            if (blockEntity.isOn()) {
                guiGraphics.blit(TEXTURE, x + 61, y + 17, 200, 0, 18, 18);
            }

            // Original: die Energiesaeule aus (176, 88-i), von unten wachsend.
            long max = blockEntity.getMaxEnergyStored();
            int e = max > 0 ? (int) (blockEntity.getEnergyStored() * 88L / max) : 0;
            if (e > 0) guiGraphics.blit(TEXTURE, x + 8, y + 106 - e, 176, 88 - e, 16, e);

            // Original: der Bohrfortschritt aus (192,0).
            int progress = blockEntity.getProgressScaled(34);
            if (progress > 0) guiGraphics.blit(TEXTURE, x + 66, y + 36, 192, 0, 8, progress);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
        if (blockEntity != null) {
            // Original: die Kantenlaenge des Schachts, mittig ueber dem Knopf.
            String width = String.valueOf(blockEntity.getWidth());
            guiGraphics.drawString(font, width, 43 - font.width(width) / 2, 26, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Original-Trefferflaeche: (61,17), 18 x 18.
        if (blockEntity != null && isHovering(61, 17, 18, 18, mouseX, mouseY)) {
            com.hbm_m.network.MiningLaserToggleC2SPacket.send(blockEntity.getBlockPos());
            if (minecraft != null) {
                minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
