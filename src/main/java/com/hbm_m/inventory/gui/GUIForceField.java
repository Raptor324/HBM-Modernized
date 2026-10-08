package com.hbm_m.inventory.gui;

import com.hbm_m.inventory.menu.ForceFieldMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.ToggleForceFieldC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;


/**
 * 1:1-Port von {@code GUIForceField} (1.7.10), Textur {@code gui_field.png}.
 *
 * <p>Links der Energiebalken, daneben die Schildstaerke - beide 16 mal 52 gross und von unten
 * gefuellt. Rechts bei (142, 34) sitzt der Schalter; leuchtet er, laeuft das Feld.</p>
 */
public class GUIForceField extends GuiInfoScreen<ForceFieldMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            RefStrings.MODID, "textures/gui/gui_field.png");

    /** Original: beide Balken sind 52 Bildpunkte hoch und enden auf Hoehe 69. */
    private static final int BAR_HEIGHT = 52;
    private static final int BAR_BOTTOM = 69;

    private static final int BUTTON_X = 142;
    private static final int BUTTON_Y = 34;

    public GUIForceField(ForceFieldMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = this.imageHeight - 96 + 2;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        com.mojang.blaze3d.systems.RenderSystem.setShader(GameRenderer::getPositionTexShader);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Energie: Original {@code getPowerScaled(52)}.
        long power = menu.getBlockEntity().getEnergyStored();
        long maxPower = menu.getBlockEntity().getMaxEnergyStored();
        int i = maxPower > 0 ? (int) (power * BAR_HEIGHT / maxPower) : 0;
        guiGraphics.blit(TEXTURE, leftPos + 8, topPos + BAR_BOTTOM - i, 176, BAR_HEIGHT - i, 16, i);

        // Schild: Original {@code getHealthScaled(52)}.
        int maxHealth = Math.max(1, menu.getMaxHealth());
        int j = menu.getHealth() * BAR_HEIGHT / maxHealth;
        guiGraphics.blit(TEXTURE, leftPos + 62, topPos + BAR_BOTTOM - j, 192, BAR_HEIGHT - j, 16, j);

        if (menu.isOn()) {
            guiGraphics.blit(TEXTURE, leftPos + BUTTON_X, topPos + BUTTON_Y, 176, BAR_HEIGHT, 18, 18);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= leftPos + BUTTON_X && mouseX < leftPos + BUTTON_X + 18
                && mouseY > topPos + BUTTON_Y && mouseY <= topPos + BUTTON_Y + 18) {

            if (minecraft != null) {
                minecraft.getSoundManager().play(
                        SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            ToggleForceFieldC2SPacket.send(menu.getBlockEntity().getBlockPos());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String name = this.title.getString();
        guiGraphics.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Original: drawElectricityInfo(8, 17, 16, 52) und Schild "health / maxHealth HP" (62, 17, 16, 52)
        this.drawElectricityInfo(guiGraphics, mouseX, mouseY, 8, BAR_BOTTOM - BAR_HEIGHT, 16, BAR_HEIGHT,
                menu.getBlockEntity().getEnergyStored(), menu.getBlockEntity().getMaxEnergyStored());
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, 62, BAR_BOTTOM - BAR_HEIGHT, 16, BAR_HEIGHT, mouseX, mouseY,
                Component.literal(menu.getHealth() + " / " + menu.getMaxHealth() + "HP"));
    }

}
