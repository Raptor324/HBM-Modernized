package com.hbm_m.client;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.world.biome.CraterBiomes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
public class CraterFogHandler {

    // Этот метод делает туман очень близким и густым в биоме inner_crater
    @SubscribeEvent
    public static void onFogDensity(ViewportEvent.RenderFog event) {
        Level level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        Biome biome = level.getBiome(player.blockPosition()).value();
        // Проверяем, что мы в нужном биоме
        if (level.getBiome(player.blockPosition()).is(CraterBiomes.INNER_CRATER_KEY)) {
            // Настраиваем туман: start - где начинается, end - где заканчивается (чем меньше, тем ближе и гуще)
            event.setNearPlaneDistance(0.1F); // Туман начинается почти у носа
            event.setFarPlaneDistance(140.0F);  // Туман полностью непрозрачный уже в 6 блоках
            event.setCanceled(true); // Отключаем стандартный туман
        }

        if (level.getBiome(player.blockPosition()).is(CraterBiomes.OUTER_CRATER_KEY)) {
            // Настраиваем туман: start - где начинается, end - где заканчивается (чем меньше, тем ближе и гуще)
            event.setNearPlaneDistance(0.07F); // Туман начинается почти у носа
            event.setFarPlaneDistance(180.0F);  // Туман полностью непрозрачный уже в 6 блоках
            event.setCanceled(true); // Отключаем стандартный туман
        }
    }

    // Этот метод задаёт цвет тумана (можно не трогать, если устраивает цвет из биома)
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Level level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        if (level.getBiome(player.blockPosition()).is(CraterBiomes.INNER_CRATER_KEY)) {
            // Цвет тумана (тёмно-серый)
            event.setRed(0.10F);
            event.setGreen(0.10F);
            event.setBlue(0.12F);
        }
    }
}