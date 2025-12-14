package com.hbm_m.client.input;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import com.hbm_m.item.custom.grenades_and_activators.MultiDetonatorItem;
import com.hbm_m.client.overlay.GUIMultiDetonator;
import org.lwjgl.glfw.GLFW;

/**
 * Обработчик входных событий для открытия GUI мульти-детонатора по нажатию R
 * ✓ Совместимо с MultiDetonatorItem версии 4 точек
 * ✓ Совместимо с MultiDetonatorScreen версии 4 точек
 */
@Mod.EventBusSubscriber(modid = "hbm_m", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class MultiDetonatorKeyInputHandler {

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;

        // Проверяем, что игрок существует и никакой GUI не открыт
        if (player == null || minecraft.screen != null) {
            return;
        }

        // Используем GLFW код клавиши R
        if (event.getKey() == GLFW.GLFW_KEY_R && event.getAction() == GLFW.GLFW_PRESS) {
            ItemStack mainItem = player.getMainHandItem();
            ItemStack offItem = player.getOffhandItem();

            // Проверяем предмет в основной руке
            if (mainItem.getItem() instanceof MultiDetonatorItem) {
                minecraft.setScreen(new GUIMultiDetonator(mainItem));
                // НЕ вызываем event.setCanceled(true); - это вызовет крах!
                return;
            }

            // Проверяем предмет в руке со щитом
            if (offItem.getItem() instanceof MultiDetonatorItem) {
                minecraft.setScreen(new GUIMultiDetonator(offItem));
                // НЕ вызываем event.setCanceled(true); - это вызовет крах!
                return;
            }
        }
    }
}