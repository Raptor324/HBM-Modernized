package com.hbm_m.inventory.gui;

import org.jetbrains.annotations.NotNull;

import com.hbm_m.block.bomb.LargeNukeType;
import com.hbm_m.blockentity.bomb.LargeNukeBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.LargeNukeMenu;
import com.hbm_m.item.ModItems;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;

/**
 * Gemeinsamer Bildschirm der grossen Bomben, je Typ 1:1 {@code GUINukeGadget}/{@code GUINukeBoy}/
 * {@code GUINukeMike}/{@code GUINukeTsar}: Schaltbild, Teile-Overlays, Bereitschaftsanzeige, Info-Panel.
 */
public class GUINukeLarge extends GuiInfoScreen<LargeNukeMenu> {

    /** Original {@code GUINukeTsar.textureMike}: die Tsar-Overlays liegen auf dem Ivy-Mike-Schaltbild. */
    private static final ResourceLocation TEXTURE_MIKE = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/weapon/ivy_mike_schematic.png");

    private final LargeNukeBlockEntity be;
    private final LargeNukeType type;

    public GUINukeLarge(LargeNukeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.be = menu.be;
        this.type = menu.type;
        this.imageWidth = type.guiWidth();
        this.imageHeight = type.guiHeight();
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Original I18nUtil.resolveKeyArray: "$" trennt die Zeilen
        String[] split = Component.translatable(type.descKey()).getString().split("\\$");
        Component[] descText = new Component[split.length];
        for (int i = 0; i < split.length; i++) descText[i] = Component.literal(split[i]);
        this.drawCustomInfoStat(guiGraphics, mouseX, mouseY, -16, 16, 16, 16, leftPos - 8, topPos + 16 + 16, descText);
    }

    private boolean has(int slot, Item item) {
        return be.slots.get(slot).is(item);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        ResourceLocation tex = type.schematic();
        g.blit(tex, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight);

        if (be != null) { // tile may be missing in a Flashback replay
            switch (type) {
                case GADGET -> {
                    if (has(1, ModItems.FAT_MAN_EXPLOSIVE.get())) g.blit(tex, leftPos + 82, topPos + 19, 176, 0, 24, 24);
                    if (has(2, ModItems.FAT_MAN_EXPLOSIVE.get())) g.blit(tex, leftPos + 106, topPos + 19, 200, 0, 24, 24);
                    if (has(3, ModItems.FAT_MAN_EXPLOSIVE.get())) g.blit(tex, leftPos + 82, topPos + 43, 176, 24, 24, 24);
                    if (has(4, ModItems.FAT_MAN_EXPLOSIVE.get())) g.blit(tex, leftPos + 106, topPos + 43, 200, 24, 24, 24);
                    if (be.isReady()) g.blit(tex, leftPos + 134, topPos + 35, 176, 48, 16, 16);
                }
                case BOY -> {
                    if (be.isReady()) g.blit(tex, leftPos + 142, topPos + 90, 176, 0, 16, 16);
                    if (has(0, ModItems.BOY_SHIELDING.get())) g.blit(tex, leftPos + 27, topPos + 87, 176, 16, 21, 22);
                    if (has(1, ModItems.BOY_TARGET.get())) g.blit(tex, leftPos + 27, topPos + 89, 176, 38, 21, 18);
                    if (has(2, ModItems.BOY_BULLET.get())) g.blit(tex, leftPos + 74, topPos + 94, 176, 57, 19, 8);
                    if (has(3, ModItems.BOY_PROPELLANT.get())) g.blit(tex, leftPos + 92, topPos + 95, 176, 66, 12, 6);
                    if (has(4, ModItems.BOY_IGNITER.get())) g.blit(tex, leftPos + 107, topPos + 91, 176, 75, 16, 14);
                }
                case MIKE -> {
                    if (be.isReady() && !be.isFilled()) g.blit(tex, leftPos + 5, topPos + 35, 177, 1, 16, 16);
                    if (be.isReady() && be.isFilled()) g.blit(tex, leftPos + 5, topPos + 35, 177, 19, 16, 16);
                    if (has(5, ModItems.MIKE_CORE.get())) g.blit(tex, leftPos + 75, topPos + 25, 176, 49, 80, 36);
                    if (has(6, ModItems.MIKE_DEUT.get())) g.blit(tex, leftPos + 79, topPos + 30, 180, 88, 58, 26);
                    if (has(7, ModItems.MIKE_COOLING_UNIT.get())) g.blit(tex, leftPos + 140, topPos + 30, 240, 88, 12, 26);
                    for (int i = 0; i < 4; i++) {
                        if (!has(i, ModItems.EXPLOSIVE_LENSES.get())) continue;
                        switch (i) {
                            case 0 -> g.blit(tex, leftPos + 24, topPos + 20, 209, 1, 23, 23);
                            case 2 -> g.blit(tex, leftPos + 47, topPos + 20, 232, 1, 23, 23);
                            case 1 -> g.blit(tex, leftPos + 24, topPos + 43, 209, 24, 23, 23);
                            case 3 -> g.blit(tex, leftPos + 47, topPos + 43, 232, 24, 23, 23);
                        }
                    }
                }
                case TSAR -> {
                    if (be.isFilled()) g.blit(TEXTURE_MIKE, leftPos + 18, topPos + 50, 176, 18, 16, 16);
                    else if (be.isReady()) g.blit(TEXTURE_MIKE, leftPos + 18, topPos + 50, 176, 0, 16, 16);
                    for (int i = 0; i < 4; i++) {
                        if (!has(i, ModItems.EXPLOSIVE_LENSES.get())) continue;
                        switch (i) {
                            case 0 -> g.blit(TEXTURE_MIKE, leftPos + 24 + 16, topPos + 20 + 16, 209, 1, 23, 23);
                            case 2 -> g.blit(TEXTURE_MIKE, leftPos + 47 + 16, topPos + 20 + 16, 232, 1, 23, 23);
                            case 1 -> g.blit(TEXTURE_MIKE, leftPos + 24 + 16, topPos + 43 + 16, 209, 24, 23, 23);
                            case 3 -> g.blit(TEXTURE_MIKE, leftPos + 47 + 16, topPos + 43 + 16, 232, 24, 23, 23);
                        }
                    }
                    if (has(5, ModItems.TSAR_CORE.get())) g.blit(TEXTURE_MIKE, leftPos + 75 + 16, topPos + 25 + 16, 176, 220, 80, 36);
                }
            }
        }

        this.drawInfoPanel(g, -16, 16, PanelType.LARGE_BLUE_INFO);
    }

    @Override
    protected void renderLabels(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Original: Ivy Mike setzt den Titel auf y=4, alle anderen auf y=6
        int titleY = type == LargeNukeType.MIKE ? 4 : 6;
        guiGraphics.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, titleY, 4210752, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, type.inventoryX(), this.imageHeight - 96 + 2, 4210752, false);
    }
}
