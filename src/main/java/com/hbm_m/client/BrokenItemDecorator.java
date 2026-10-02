//? if forge {
package com.hbm_m.client;

import com.hbm_m.item.BrokenItem;
import com.hbm_m.lib.RefStrings;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.IItemDecorator;

/** Original {@code BrokenItem.getIcon}: Pass 0 = Symbol des gebrochenen Gegenstands, Pass 1 = Riss. */
public final class BrokenItemDecorator implements IItemDecorator {

    public static final BrokenItemDecorator INSTANCE = new BrokenItemDecorator();
    private static final ResourceLocation CRACK = ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, "textures/item/broken_item.png");

    private BrokenItemDecorator() {}

    @Override
    public boolean render(GuiGraphics gfx, Font font, ItemStack stack, int x, int y) {
        ItemStack inner = BrokenItem.getBrokenStack(stack);
        if (inner.isEmpty()) return false;
        gfx.renderFakeItem(inner, x, y);
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 200);
        gfx.blit(CRACK, x, y, 0, 0, 16, 16, 16, 16);
        gfx.pose().popPose();
        return false;
    }
}
//?}
