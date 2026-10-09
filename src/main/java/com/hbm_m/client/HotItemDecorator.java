//? if forge || neoforge {
package com.hbm_m.client;

import com.hbm_m.item.special.ItemHot;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
//? if forge {
import net.minecraftforge.client.IItemDecorator;
//?} else {
/*import net.neoforged.neoforge.client.IItemDecorator;
*///?}

/**
 * Original {@code ItemRendererHot} (nur ItemRenderType.INVENTORY): legt das "_hot"-Symbol mit Deckkraft = Hitze
 * ueber den Gegenstand.
 */
public final class HotItemDecorator implements IItemDecorator {

    public static final HotItemDecorator INSTANCE = new HotItemDecorator();

    private HotItemDecorator() {}

    @Override
    public boolean render(GuiGraphics gfx, Font font, ItemStack stack, int x, int y) {
        double h = ItemHot.getHeat(stack);
        if (h > 0 && stack.getItem() instanceof ItemHot hot) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.setShaderColor(1F, 1F, 1F, (float) Math.min(h, 1D));
            gfx.pose().pushPose();
            gfx.pose().translate(0, 0, 200);
            gfx.blit(hot.hotTexture, x, y, 0, 0, 16, 16, 16, 16);
            gfx.pose().popPose();
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            RenderSystem.disableBlend();
        }
        return false;
    }
}
//?}
