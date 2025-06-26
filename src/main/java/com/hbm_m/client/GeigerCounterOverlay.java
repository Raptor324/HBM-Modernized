package com.hbm_m.client;

import com.hbm_m.item.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class GeigerCounterOverlay implements IGuiOverlay {
    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void render(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        //LOGGER.debug("GeigerCounterOverlay render called");
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean hasGeiger = false;
        if (mc.player != null) {
            for (ItemStack stack : mc.player.getInventory().items) {
                if (stack.is(ModItems.GEIGER_COUNTER.get())) {
                    hasGeiger = true;
                    break;
                }
            }
        }

        if (!hasGeiger) {
            // Также проверяем основную и дополнительную руку, если еще не нашли
            ItemStack mainHand = mc.player.getMainHandItem();
            ItemStack offHand = mc.player.getOffhandItem();
            if (mainHand.is(ModItems.GEIGER_COUNTER.get()) || offHand.is(ModItems.GEIGER_COUNTER.get())) {
                hasGeiger = true;
            }
        }

        if (!hasGeiger) return;

        int x = 5;
        int y = screenHeight - 20;
        String text = "Geiger: 0.42";

        guiGraphics.drawString(mc.font, text, x, y, 0xFFFFFF);
    }
}
