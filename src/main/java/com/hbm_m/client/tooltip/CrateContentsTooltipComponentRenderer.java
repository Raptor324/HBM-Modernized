package com.hbm_m.client.tooltip;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Client renderer for crate tooltip rows (item icon + name).
 */
@OnlyIn(Dist.CLIENT)
public class CrateContentsTooltipComponentRenderer implements ClientTooltipComponent {
    private static final int ROW_HEIGHT = 18;
    private static final int TEXT_X_OFFSET = 22;
    private final List<CrateContentsTooltipComponent.Entry> entries;

    public CrateContentsTooltipComponentRenderer(CrateContentsTooltipComponent component) {
        this.entries = component.getEntries();
    }

    @Override
    public int getHeight() {
        return entries.size() * ROW_HEIGHT;
    }

    @Override
    public int getWidth(@Nonnull Font font) {
        int maxTextWidth = 0;
        for (CrateContentsTooltipComponent.Entry entry : entries) {
            maxTextWidth = Math.max(maxTextWidth, font.width(getRowText(entry)));
        }
        return TEXT_X_OFFSET + maxTextWidth;
    }

    @Override
    public void renderImage(@Nonnull Font font, int x, int y, @Nonnull GuiGraphics guiGraphics) {
        for (int i = 0; i < entries.size(); i++) {
            CrateContentsTooltipComponent.Entry entry = entries.get(i);
            ItemStack stack = entry.stack();
            int rowY = y + i * ROW_HEIGHT;

            guiGraphics.renderFakeItem(stack, x, rowY);
            // Показываем суммарное количество возле иконки (включая объединенные стаки).
            guiGraphics.renderItemDecorations(font, stack, x, rowY, Integer.toString(entry.totalCount()));
            guiGraphics.drawString(font, getRowText(entry), x + TEXT_X_OFFSET, rowY + 4, 0x55FFFF, false);
        }
    }

    private Component getRowText(CrateContentsTooltipComponent.Entry entry) {
        ItemStack stack = entry.stack();
        if (entry.totalCount() > 1) {
            return Component.literal(stack.getHoverName().getString() + " x" + entry.totalCount());
        }
        return stack.getHoverName();
    }
}
