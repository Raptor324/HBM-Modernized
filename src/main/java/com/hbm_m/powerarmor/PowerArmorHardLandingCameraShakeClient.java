package com.hbm_m.powerarmor;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerArmorHardLandingCameraShakeClient {

    private static boolean wasInAir = false;
    private static float maxFall = 0.0F;

    private static long shakeStartMs = 0L;
    private static long shakeDurationMs = 350L; // коротко и приятно
    private static float shakeStrength = 0.0F;  // в градусах
    final static float SHAKE_MIN_FALL = 1.5F;   // старт тряски (как у молота)
    final static float SHAKE_FULL_FALL = 8.0F;  // к этой высоте уже почти максимум

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (p.isSpectator()) return;

        // Проверяем броню
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem)) return;
        if (!armorItem.getSpecs().hasHardLanding) return;

        boolean onGround = p.onGround();

        if (!onGround) {
            wasInAir = true;
            maxFall = Math.max(maxFall, p.fallDistance);
            return;
        }

        if (!wasInAir) return;

        float fall = maxFall;
        wasInAir = false;
        maxFall = 0.0F;

        if (fall <= SHAKE_MIN_FALL) return;
       // порог hardLanding
        if (p.isFallFlying()) return;   // как у молота

        // Интенсивность: растёт с высотой, но мягко ограничена
        float norm = Mth.clamp((fall - SHAKE_MIN_FALL) / (SHAKE_FULL_FALL - SHAKE_MIN_FALL), 0.0F, 1.0F);
        float minStrength = 0.25F; // градусы
        float maxStrength = 1.2F;  // градусы
        shakeStrength = minStrength + (maxStrength - minStrength) * norm;
        shakeStartMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (shakeStartMs == 0L || shakeStrength <= 0.0F) return;

        long now = System.currentTimeMillis();
        float t = (now - shakeStartMs) / (float) shakeDurationMs;
        if (t >= 1.0F) {
            shakeStartMs = 0L;
            shakeStrength = 0.0F;
            return;
        }

        // плавное затухание + синус (без рандома, чтобы не дергало)
        float damp = (1.0F - t);
        damp *= damp;

        float wave = Mth.sin(t * (float)Math.PI * 6.0F); // 3 "качка"
        float offset = wave * damp * shakeStrength;

        event.setPitch(event.getPitch() + offset);
        event.setYaw(event.getYaw() + offset * 0.6F);
        event.setRoll(event.getRoll() + offset * 0.8F); // если roll поддерживается в твоей Forge сборке
    }
}
