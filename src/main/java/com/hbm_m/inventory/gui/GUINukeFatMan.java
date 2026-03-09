package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.entity.bomb.NukeFatManBlockEntity;
import com.hbm_m.inventory.menu.NukeFatManMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * GUI for Fat Man bomb
 * Uses texture fat_man_schematic.png, lens overlays, ready indicator, and info panel with description tooltip.
 */
public class GUINukeFatMan extends GuiInfoScreen<NukeFatManMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/fat_man_schematic.png");

    private final NukeFatManBlockEntity be;

    public GUINukeFatMan(NukeFatManMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        List<Component> text = new ArrayList<>();
        text.add(Component.translatable("gui.hbm_m.nuke_fat_man.desc"));
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 16, 16, 16,
                leftPos - 8, topPos + 16 + 16, text.toArray(new Component[0]));
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        if (be.isReady()) {
            guiGraphics.blit(TEXTURE, this.leftPos + 134, this.topPos + 35, 176, 48, 16, 16);
        }

        for (int index = 0; index < 4; index++) {
            if (be.slots.get(index).getItem() == ModItems.FAT_MAN_EXPLOSIVE.get()) {
                switch (index) {
                    case 0 -> guiGraphics.blit(TEXTURE, this.leftPos + 82, this.topPos + 19, 176, 0, 24, 24);
                    case 1 -> guiGraphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 19, 200, 0, 24, 24);
                    case 2 -> guiGraphics.blit(TEXTURE, this.leftPos + 82, this.topPos + 43, 176, 24, 24, 24);
                    case 3 -> guiGraphics.blit(TEXTURE, this.leftPos + 106, this.topPos + 43, 200, 24, 24, 24);
                }
            }
        }

        this.drawInfoPanel(guiGraphics, -16, 16, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }
}
