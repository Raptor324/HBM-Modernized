package com.hbm_m.inventory.gui;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.blockentity.machines.MachineDifurnaceRtgBlockEntity;
import com.hbm_m.client.GuiCompat;
import com.hbm_m.inventory.menu.MachineDifurnaceRtgMenu;
import com.hbm_m.item.machine.ItemRTGPellet;
import com.hbm_m.lib.RefStrings;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 1:1 {@code GUIMachineDiFurnaceRTG}: Waermeanzeige (ab 15 leuchtend), Fortschrittspfeil, zwei Info-Felder (Beschreibung,
 * Pelletliste mit Waerme) und der Hinweis, von welcher Seite die Eingaenge befuellt werden.
 */
public class GUIMachineDifurnaceRtg extends GuiInfoScreen<MachineDifurnaceRtgMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/processing/gui_rtg_difurnace.png");

    private final MachineDifurnaceRtgBlockEntity bFurnace;

    public GUIMachineDifurnaceRtg(MachineDifurnaceRtgMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.bFurnace = menu.getBlockEntity();
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    private static Component[] lines(String key, Object... args) {
        String[] split = Component.translatable(key, args).getString().split("\\$");
        Component[] out = new Component[split.length];
        for (int i = 0; i < split.length; i++) out[i] = Component.literal(split[i]);
        return out;
    }

    /** Original {@code ForgeDirection.getOrientation(dir)}-Name. */
    private static String dirName(byte dir) {
        return dir >= 0 && dir < 6 ? Direction.from3DDataValue(dir).getName().toUpperCase(java.util.Locale.ROOT) : "UNKNOWN";
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        GuiCompat.renderBackground(this, g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);

        if (bFurnace != null) {
            drawCustomInfoStat(g, mouseX, mouseY, -15, 36 + 16, 16, 16, leftPos - 8, topPos + 36 + 16, lines("desc.gui.rtgBFurnace.desc"));
            drawCustomInfoStat(g, mouseX, mouseY, 58, 36, 18, 16, mouseX, mouseY, lines("desc.gui.rtg.heat", bFurnace.getPower()));

            List<Component> pelletText = new ArrayList<>();
            pelletText.add(Component.translatable("desc.gui.rtg.pellets"));
            for (ItemRTGPellet pellet : ItemRTGPellet.PELLETS) {
                pelletText.add(Component.translatable("desc.gui.rtg.pelletHeat", Component.translatable(pellet.getDescriptionId()), pellet.getHeat()));
            }
            drawCustomInfoStat(g, mouseX, mouseY, -15, 36, 16, 16, leftPos - 8, topPos + 36 + 16, pelletText.toArray(new Component[0]));

            if (this.menu.getCarried().isEmpty()) {
                for (int i = 0; i < 2; i++) {
                    Slot slot = this.menu.slots.get(i);
                    if (isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                        byte dir = i == 0 ? bFurnace.sideUpper : bFurnace.sideLower;
                        g.renderTooltip(this.font, Component.literal("Accepts items from: " + dirName(dir)).withStyle(ChatFormatting.YELLOW),
                                mouseX, mouseY - (slot.hasItem() ? 15 : 0));
                        return;
                    }
                }
            }
        }

        this.renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(this.font, this.title, this.imageWidth / 2 - this.font.width(this.title) / 2, 6, 4210752, false);
        g.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (bFurnace == null) return; // тайл может отсутствовать в реплее Flashback

        if (bFurnace.getPower() >= 15) g.blit(TEXTURE, leftPos + 58, topPos + 36, 176, 31, 18, 16);

        int p = bFurnace.getDiFurnaceProgressScaled(24);
        g.blit(TEXTURE, leftPos + 101, topPos + 35, 176, 14, p + 1, 17);

        drawInfoPanel(g, -15, 36, PanelType.LARGE_BLUE_INFO);
        drawInfoPanel(g, -15, 36 + 16, PanelType.LARGE_GREEN_INFO);
    }
}
