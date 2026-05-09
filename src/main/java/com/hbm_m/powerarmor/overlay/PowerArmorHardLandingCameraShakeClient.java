package com.hbm_m.powerarmor.overlay;

import com.hbm_m.powerarmor.ModPowerArmorItem;
import com.hbm_m.particle.explosions.basic.CameraShakeHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import dev.architectury.event.events.client.ClientTickEvent;

public final class PowerArmorHardLandingCameraShakeClient {

    private static boolean wasInAir = false;
    private static float maxFall = 0.0F;

    final static float SHAKE_MIN_FALL = 1.5F;   // старт тряски (как у молота)
    final static float SHAKE_FULL_FALL = 8.0F;  // к этой высоте уже почти максимум
    private static boolean INITIALIZED = false;

    /** Register client-only tick hook on all loaders (Architectury). */
    public static void initClient() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        ClientTickEvent.CLIENT_POST.register(client -> onClientTick());
    }

    private static void onClientTick() {
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
        float intensity = 0.25F + (1.2F - 0.25F) * norm;

        // На Forge эффект применится через CameraShakeHandler.ForgeHooks (ViewportEvent/RenderGuiEvent).
        // На Fabric это остаётся safe-no-op по визуалу, но логика не ломает компиляцию.
        CameraShakeHandler.addShake(intensity, 10); // 10 тиков, как у ударной волны
    }
}
