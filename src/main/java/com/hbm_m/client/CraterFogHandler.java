//? if forge {
package com.hbm_m.client;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.world.biome.CraterBiomes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

/**
 * Туман в crater биомах. Цвета и плотность — средние значения из 1.7.10
 * {@code BiomeGenCraterBase.skyColor}: inner 0x424A42, mid 0x525A52, outer 0x6B9189.
 */
@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT)
public class CraterFogHandler {

    @SubscribeEvent
    public static void onFogDensity(ViewportEvent.RenderFog event) {
        Level level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        var biomeKey = level.getBiome(player.blockPosition()).unwrapKey().orElse(null);
        if (biomeKey == null) return;

        // Эпицентр — самый плотный туман, к периферии — рассеивается.
        if (biomeKey == CraterBiomes.INNER_CRATER_KEY) {
            event.setNearPlaneDistance(0.1F);
            event.setFarPlaneDistance(140.0F);
            event.setCanceled(true);
        } else if (biomeKey == CraterBiomes.CRATER_KEY) {
            event.setNearPlaneDistance(0.5F);
            event.setFarPlaneDistance(180.0F);
            event.setCanceled(true);
        } else if (biomeKey == CraterBiomes.OUTER_CRATER_KEY) {
            event.setNearPlaneDistance(0.07F);
            event.setFarPlaneDistance(220.0F);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Level level = Minecraft.getInstance().level;
        LocalPlayer player = Minecraft.getInstance().player;
        if (level == null || player == null) return;

        var biomeKey = level.getBiome(player.blockPosition()).unwrapKey().orElse(null);
        if (biomeKey == null) return;

        // Соответствует skyColor каждого биома (как 1.7.10 — туман = sky).
        if (biomeKey == CraterBiomes.INNER_CRATER_KEY) {
            event.setRed(0x42 / 255F);
            event.setGreen(0x4A / 255F);
            event.setBlue(0x42 / 255F);
        } else if (biomeKey == CraterBiomes.CRATER_KEY) {
            event.setRed(0x52 / 255F);
            event.setGreen(0x5A / 255F);
            event.setBlue(0x52 / 255F);
        } else if (biomeKey == CraterBiomes.OUTER_CRATER_KEY) {
            event.setRed(0x6B / 255F);
            event.setGreen(0x91 / 255F);
            event.setBlue(0x89 / 255F);
        }
    }
}
//?}
