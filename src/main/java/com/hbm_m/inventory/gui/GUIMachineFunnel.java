package com.hbm_m.inventory.gui;

import java.util.List;
import com.hbm_m.client.GuiCompat;

import com.hbm_m.blockentity.machines.MachineFunnelBlockEntity;
import com.hbm_m.inventory.menu.MachineFunnelMenu;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.network.FunnelModeC2SPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Verwendet die portierte Original-Panel-Textur (nur der statische Rahmen). Der grafische Modus-
 * Umschalt-Knopf des Originals (10x10-Icon aus der Textur-Leiste) wird durch einen normalen
 * Text-Button ersetzt (keine geratenen Icon-Koordinaten aus der unbekannten Textur - siehe
 * etablierte GUI-Konvention dieser Session).
 */
public class GUIMachineFunnel extends AbstractContainerScreen<MachineFunnelMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_funnel.png");

    private static final List<String> MODE_LABELS = List.of("3x3+2x2", "3x3", "2x2");

    private final MachineFunnelBlockEntity blockEntity;
    private Button modeButton;

    public GUIMachineFunnel(MachineFunnelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 168;
        this.inventoryLabelY = imageHeight - 96 + 2;
    }

    @Override
    protected void init() {
        super.init();
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (blockEntity == null) return; // тайл может отсутствовать в реплее Flashback
        modeButton = Button.builder(Component.literal(MODE_LABELS.get(blockEntity.getMode())), b -> {
            FunnelModeC2SPacket.sendToServer(blockEntity.getBlockPos());
            b.setMessage(Component.literal(MODE_LABELS.get((blockEntity.getMode() + 1) % 3)));
        }).bounds(x + 130, y + 74, 40, 14).build();
        addRenderableWidget(modeButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if (modeButton != null && blockEntity != null) { // тайл может отсутствовать в реплее Flashback
            modeButton.setMessage(Component.literal(MODE_LABELS.get(blockEntity.getMode())));
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, imageWidth / 2 - font.width(title) / 2, 6, 0x404040, false);
        guiGraphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        com.hbm_m.client.GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
