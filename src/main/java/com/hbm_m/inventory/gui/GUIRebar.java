package com.hbm_m.inventory.gui;

import com.hbm_m.platform.StackNbt;

import java.util.ArrayList;
import java.util.List;

import com.hbm_m.inventory.menu.RebarMenu;
import com.hbm_m.item.tool.ItemRebarPlacer;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * 1:1 {@code ItemRebarPlacer.GUIRebar}: ist der Musterplatz leer, zeigt der Tooltip alle zulaessigen Betonsorten
 * (die gerade durchlaufende rot umrandet) und ihren Namen; ohne gueltige Sorte liegt das Warnfeld bei (87,17).
 */
public class GUIRebar extends AbstractContainerScreen<RebarMenu> {

    private static final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/gui/gui_rebar.png");

    public GUIRebar(RebarMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 182;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float f) {
        com.hbm_m.client.GuiCompat.renderBackground(this, g, mouseX, mouseY, f);
        super.render(g, mouseX, mouseY, f);

        Slot slot = this.menu.getSlot(0);

        if (this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY) && !slot.hasItem()) {

            List<Object[]> lines = new ArrayList<>();
            List<Object> list = new ArrayList<>();
            for (Block conk : ItemRebarPlacer.getAcceptableConk()) list.add(new ItemStack(conk));
            ItemStack selected = (ItemStack) list.get(0);

            if (list.size() > 1) {
                int cycle = (int) ((System.currentTimeMillis() % (1000L * list.size())) / 1000);
                selected = ((ItemStack) list.get(cycle)).copy();
                list.set(cycle, new StackText.Highlighted(selected));
            }

            if (list.size() < 10) {
                lines.add(list.toArray());
            } else if (list.size() < 24) {
                lines.add(list.subList(0, list.size() / 2).toArray());
                lines.add(list.subList(list.size() / 2, list.size()).toArray());
            } else {
                int bound0 = (int) Math.ceil(list.size() / 3D);
                int bound1 = (int) Math.ceil(list.size() / 3D * 2D);
                lines.add(list.subList(0, bound0).toArray());
                lines.add(list.subList(bound0, bound1).toArray());
                lines.add(list.subList(bound1, list.size()).toArray());
            }

            lines.add(new Object[] {selected.getHoverName().getString()});
            StackText.draw(g, this.font, lines, mouseX, mouseY, this.width, this.height);
        } else {
            this.renderTooltip(g, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int i, int j) {
        Component name = StackNbt.hasCustomName(this.menu.rebar.target) ? this.menu.rebar.target.getHoverName() : Component.translatable("container.rebar");
        g.drawString(this.font, name, this.imageWidth / 2 - this.font.width(name) / 2, 6, 4210752, false);
        g.drawString(this.font, Component.translatable("container.inventory"), 8, this.imageHeight - 96 + 2, 4210752, false);
    }

    @Override
    protected void renderBg(GuiGraphics g, float f, int mouseX, int mouseY) {
        g.blit(texture, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        ItemStack conk = this.menu.rebar.getItem(0);
        if (conk.isEmpty() || !ItemRebarPlacer.isValidConk(conk))
            g.blit(texture, leftPos + 87, topPos + 17, 176, 0, 56, 56);
    }

    /** 1:1 {@code GuiInfoContainer.drawStackText}: Tooltip aus Zeilen mit Texten und Gegenstaenden. */
    public static final class StackText {

        /** Im Original ein Stapel mit Anzahl 0: rot umrandet, grau hinterlegt. */
        public record Highlighted(ItemStack stack) { }

        private StackText() { }

        public static void draw(GuiGraphics g, Font font, List<Object[]> lines, int x, int y, int width, int height) {

            if (lines.isEmpty()) return;

            int totalHeight = 0;
            int longestline = 0;

            for (Object[] line : lines) {
                int lineWidth = 0;
                boolean hasStack = false;

                for (Object o : line) {
                    if (o instanceof String s) {
                        lineWidth += font.width(s);
                    } else {
                        lineWidth += 18;
                        hasStack = true;
                    }
                }

                totalHeight += hasStack ? 18 : 10;
                if (lineWidth > longestline) longestline = lineWidth;
            }

            int minX = x + 12;
            int minY = y - 12;

            if (minX + longestline > width) minX -= 28 + longestline;
            if (minY + totalHeight + 6 > height) minY = height - totalHeight - 6;

            g.pose().pushPose();
            g.pose().translate(0, 0, 400);

            int colorBg = 0xF0100010;
            g.fillGradient(minX - 3, minY - 4, minX + longestline + 3, minY - 3, colorBg, colorBg);
            g.fillGradient(minX - 3, minY + totalHeight + 3, minX + longestline + 3, minY + totalHeight + 4, colorBg, colorBg);
            g.fillGradient(minX - 3, minY - 3, minX + longestline + 3, minY + totalHeight + 3, colorBg, colorBg);
            g.fillGradient(minX - 4, minY - 3, minX - 3, minY + totalHeight + 3, colorBg, colorBg);
            g.fillGradient(minX + longestline + 3, minY - 3, minX + longestline + 4, minY + totalHeight + 3, colorBg, colorBg);
            int color0 = 0x505000FF;
            int color1 = (color0 & 0xFEFEFE) >> 1 | color0 & 0xFF000000;
            g.fillGradient(minX - 3, minY - 3 + 1, minX - 3 + 1, minY + totalHeight + 3 - 1, color0, color1);
            g.fillGradient(minX + longestline + 2, minY - 3 + 1, minX + longestline + 3, minY + totalHeight + 3 - 1, color0, color1);
            g.fillGradient(minX - 3, minY - 3, minX + longestline + 3, minY - 3 + 1, color0, color0);
            g.fillGradient(minX - 3, minY + totalHeight + 2, minX + longestline + 3, minY + totalHeight + 3, color1, color1);

            for (int index = 0; index < lines.size(); ++index) {

                Object[] line = lines.get(index);
                int indent = 0;
                boolean hasStack = false;

                for (Object o : line) {
                    if (!(o instanceof String)) hasStack = true;
                }

                for (Object o : line) {

                    if (o instanceof String s) {
                        g.drawString(font, s, minX + indent, minY + (hasStack ? 4 : 0), -1, true);
                        indent += font.width(s) + 2;
                    } else {
                        ItemStack stack;

                        if (o instanceof Highlighted h) {
                            stack = h.stack();
                            g.fill(minX + indent - 1, minY - 1, minX + indent + 17, minY + 17, 0xffff0000);
                            g.fill(minX + indent, minY, minX + indent + 16, minY + 16, 0xffb0b0b0);
                        } else {
                            stack = (ItemStack) o;
                        }

                        g.renderItem(stack, minX + indent, minY);
                        g.renderItemDecorations(font, stack, minX + indent, minY);
                        indent += 18;
                    }
                }

                if (index == 0) minY += 2;

                minY += hasStack ? 18 : 10;
            }

            g.pose().popPose();
        }
    }
}
