package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.blockentity.bomb.NukeSoliniumBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.NukeSoliniumMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Экран солиниевой бомбы (схема размещения компонентов).
 */
public class GUINukeSolinium extends GuiInfoScreen<NukeSoliniumMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/solinium_schematic.png");

    private final NukeSoliniumBlockEntity be;

    public GUINukeSolinium(NukeSoliniumMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        // Original: Teile-Overlays je Slot, Bereitschaftsanzeige bei (134, 90), kein Info-Panel
        if (be != null) { // тайл может отсутствовать в реплее Flashback
            if (has(0, ModItems.SOLINIUM_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 24, topPos + 84, 0, 222, 22, 14);
            if (has(1, ModItems.SOLINIUM_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 46, topPos + 84, 22, 222, 18, 14);
            if (has(2, ModItems.SOLINIUM_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 76, topPos + 84, 52, 222, 18, 14);
            if (has(3, ModItems.SOLINIUM_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 94, topPos + 84, 70, 222, 22, 14);
            if (has(4, ModItems.SOLINIUM_CORE.get())) guiGraphics.blit(TEXTURE, leftPos + 64, topPos + 84, 40, 222, 12, 28);
            if (has(5, ModItems.SOLINIUM_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 24, topPos + 98, 0, 236, 22, 14);
            if (has(6, ModItems.SOLINIUM_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 46, topPos + 98, 22, 236, 18, 14);
            if (has(7, ModItems.SOLINIUM_PROPELLANT.get())) guiGraphics.blit(TEXTURE, leftPos + 76, topPos + 98, 52, 236, 18, 14);
            if (has(8, ModItems.SOLINIUM_IGNITER.get())) guiGraphics.blit(TEXTURE, leftPos + 94, topPos + 98, 70, 236, 22, 14);
            if (be.isReady()) guiGraphics.blit(TEXTURE, leftPos + 134, topPos + 90, 176, 0, 16, 16);
        }
    }

    private boolean has(int slot, net.minecraft.world.item.Item item) {
        return be.slots.get(slot).is(item);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
