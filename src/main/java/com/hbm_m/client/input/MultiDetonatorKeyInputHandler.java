package com.hbm_m.client.input;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.hbm_m.item.MultiDetonatorItem;
import com.hbm_m.client.overlay.MultiDetonatorScreen;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MultiDetonatorKeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        if (player == null || minecraft.screen != null) {
            return;
        }

        // Используем GLFW код клавиши R
        if (event.getKey() == GLFW.GLFW_KEY_R && event.getAction() == GLFW.GLFW_PRESS) {
            ItemStack mainItem = player.getMainHandItem();
            ItemStack offItem = player.getOffhandItem();

            // Проверяем обе руки
            if (mainItem.getItem() instanceof MultiDetonatorItem) {
                minecraft.setScreen(new MultiDetonatorScreen(mainItem));
                // НЕ вызываем event.setCanceled(true); - это вызовет крах!
                return;
            }

            if (offItem.getItem() instanceof MultiDetonatorItem) {
                minecraft.setScreen(new MultiDetonatorScreen(offItem));
                // НЕ вызываем event.setCanceled(true); - это вызовет крах!
                return;
            }
        }
    }
}