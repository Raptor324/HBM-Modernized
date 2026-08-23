package com.hbm_m.inventory.gui;

import com.hbm_m.blockentity.machines.MachineMicrowaveBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineMicrowaveMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.MicrowaveSpeedC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** GUI der Mikrowelle - Fortschritts-/Energieanzeigen als Fuellrechtecke (siehe
 *  {@code GUIMachineElectricFurnace}) plus Geschwindigkeits-Regler (+/- Buttons), 1:1 zum
 *  Original ({@code handleButtonPacket}). ACHTUNG: Geschwindigkeit 5 (Maximum) laesst die
 *  Maschine explodieren statt schneller zu verarbeiten - absichtliches Originalverhalten. */
public class GUIMachineMicrowave extends AbstractContainerScreen<MachineMicrowaveMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_microwave.png");

    private final MachineMicrowaveBlockEntity blockEntity;

    public GUIMachineMicrowave(MachineMicrowaveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 186;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        int x = leftPos;
        int y = topPos;
        addRenderableWidget(Button.builder(Component.literal("+"), b -> sendSpeed(1))
                .bounds(x + 100, y + 15, 16, 16).build());
        addRenderableWidget(Button.builder(Component.literal("-"), b -> sendSpeed(-1))
                .bounds(x + 100, y + 53, 16, 16).build());
    }

    private void sendSpeed(int delta) {
        if (blockEntity == null) return; // тайл может отсутствовать в реплее Flashback
        MicrowaveSpeedC2SPacket.sendToServer(blockEntity.getBlockPos(), delta);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (blockEntity != null) {
            int cookProgress = blockEntity.getProgressScaled(24);
            if (cookProgress > 0) {
                guiGraphics.fill(x + 90, y + 39, x + 90 + cookProgress, y + 47, 0xFFC0C0C0);
            }

            if (blockEntity.getPowerScaled(1) > 0) {
                guiGraphics.fill(x + 154, y + 20, x + 166, y + 52, 0xFF3080FF);
            }

            int speedHeight = blockEntity.getSpeedScaled(24);
            int warnColor = blockEntity.speed >= MachineMicrowaveBlockEntity.MAX_SPEED ? 0xFFFF3030 : 0xFF30FF60;
            if (speedHeight > 0) {
                guiGraphics.fill(x + 122, y + 44 - speedHeight, x + 132, y + 44, warnColor);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
        if (blockEntity != null) {
            guiGraphics.drawString(font, String.valueOf(blockEntity.speed), 103, 34, 0x404040, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
