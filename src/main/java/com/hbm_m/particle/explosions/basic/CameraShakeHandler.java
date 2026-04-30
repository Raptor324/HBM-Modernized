package com.hbm_m.particle.explosions.basic;

import net.minecraft.client.Minecraft;
import dev.architectury.event.events.client.ClientTickEvent;

/**
 *  ОБРАБОТЧИК ТРЯСКИ КАМЕРЫ И GUI
 *
 * Управляет эффектом тряски камеры и интерфейса при воздействии ударной волны
 * Создаёт резкие импульсные толчки вместо мелкого дрожания
 */
public class CameraShakeHandler {

    // ┌─────────────────────────────────────────────────────────────┐
    // │ ПАРАМЕТРЫ ТРЯСКИ                                            │
    // └─────────────────────────────────────────────────────────────┘

    private static float shakeIntensity = 0.0F;
    private static int shakeDuration = 0;

    // Текущие смещения камеры
    private static float shakeOffsetX = 0.0F;
    private static float shakeOffsetY = 0.0F;
    private static float shakeOffsetRoll = 0.0F;

    // Целевые смещения (для плавной интерполяции)
    private static float targetOffsetX = 0.0F;
    private static float targetOffsetY = 0.0F;
    private static float targetOffsetRoll = 0.0F;

    //  УВЕЛИЧЕНА АМПЛИТУДА: было 5.0F, стало 12.0F
    private static final float VISUAL_MULTIPLIER = 12.0F;

    //  МНОЖИТЕЛЬ ДЛЯ GUI (меньше чем для камеры, чтобы не было слишком сильно)
    @SuppressWarnings("unused")
    private static final float GUI_MULTIPLIER = 2.5F;

    //  СКОРОСТЬ СМЕНЫ НАПРАВЛЕНИЯ: чем больше - тем быстрее меняется
    private static final float INTERPOLATION_SPEED = 0.4F; // 0.4 = быстрая смена

    //  ЧАСТОТА ОБНОВЛЕНИЯ ЦЕЛЕВЫХ ЗНАЧЕНИЙ (в тиках)
    private static int updateCounter = 0;
    private static final int UPDATE_FREQUENCY = 2; // Каждые 2 тика = 10 раз в секунду

    private static boolean INITIALIZED = false;

    /** Register client tick updates on all loaders (Architectury). */
    public static void initClient() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        ClientTickEvent.CLIENT_PRE.register(client -> {
            if (Minecraft.getInstance().level != null) {
                tick();
            }
        });
    }

    /**
     *  ДОБАВИТЬ ЭФФЕКТ ТРЯСКИ
     */
    public static void addShake(float intensity, int duration) {
        if (intensity > shakeIntensity) {
            shakeIntensity = intensity;
            shakeDuration = duration;
            generateNewTarget();
        } else if (shakeDuration > 0) {
            shakeDuration = Math.max(shakeDuration, duration);
            shakeIntensity = Math.min(shakeIntensity + intensity * 0.3F, 2.0F);
        } else {
            shakeIntensity = intensity;
            shakeDuration = duration;
            generateNewTarget();
        }
    }

    /**
     *  ГЕНЕРАЦИЯ НОВОЙ ЦЕЛЕВОЙ ПОЗИЦИИ
     */
    private static void generateNewTarget() {
        targetOffsetX = (float) (Math.random() - 0.5) * 2.0F * shakeIntensity * VISUAL_MULTIPLIER;
        targetOffsetY = (float) (Math.random() - 0.5) * 2.0F * shakeIntensity * VISUAL_MULTIPLIER;
        targetOffsetRoll = (float) (Math.random() - 0.5) * shakeIntensity * VISUAL_MULTIPLIER * 0.3F;
    }

    /**
     *  ОБНОВЛЕНИЕ ТРЯСКИ
     */
    public static void tick() {
        if (shakeDuration > 0) {
            shakeDuration--;
            shakeIntensity *= 0.92F;

            updateCounter++;

            if (updateCounter >= UPDATE_FREQUENCY) {
                generateNewTarget();
                updateCounter = 0;
            }

            // Плавная интерполяция к целевой позиции
            shakeOffsetX += (targetOffsetX - shakeOffsetX) * INTERPOLATION_SPEED;
            shakeOffsetY += (targetOffsetY - shakeOffsetY) * INTERPOLATION_SPEED;
            shakeOffsetRoll += (targetOffsetRoll - shakeOffsetRoll) * INTERPOLATION_SPEED;

            if (shakeIntensity < 0.01F) {
                shakeIntensity = 0.0F;
                shakeDuration = 0;
                shakeOffsetX = 0.0F;
                shakeOffsetY = 0.0F;
                shakeOffsetRoll = 0.0F;
                targetOffsetX = 0.0F;
                targetOffsetY = 0.0F;
                targetOffsetRoll = 0.0F;
                updateCounter = 0;
            }
        } else {
            shakeIntensity = 0.0F;
            shakeOffsetX *= 0.8F;
            shakeOffsetY *= 0.8F;
            shakeOffsetRoll *= 0.8F;

            if (Math.abs(shakeOffsetX) < 0.01F && Math.abs(shakeOffsetY) < 0.01F) {
                shakeOffsetX = 0.0F;
                shakeOffsetY = 0.0F;
                shakeOffsetRoll = 0.0F;
            }
        }
    }

    // --- Loader-specific hooks ---
    //
    // Forge provides direct camera+GUI hooks via events (ViewportEvent / RenderGuiEvent).
    // On Fabric these hooks are not available here; we keep the core shake state/tick
    // loader-agnostic so it can be driven by client tick, and optionally integrated
    // via mixins or platform hooks later.

    //? if forge {
    /*@net.minecraftforge.fml.common.Mod.EventBusSubscriber(
            modid = com.hbm_m.lib.RefStrings.MODID,
            value = net.minecraftforge.api.distmarker.Dist.CLIENT
    )
    public static final class ForgeHooks {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onCameraSetup(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
            if (shakeIntensity > 0.0F || Math.abs(shakeOffsetX) > 0.01F) {
                event.setYaw(event.getYaw() + shakeOffsetX);
                event.setPitch(event.getPitch() + shakeOffsetY);
                event.setRoll(event.getRoll() + shakeOffsetRoll);
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public static void onRenderGuiPre(net.minecraftforge.client.event.RenderGuiEvent.Pre event) {
            if (shakeIntensity > 0.0F || Math.abs(shakeOffsetX) > 0.01F) {
                com.mojang.blaze3d.vertex.PoseStack poseStack = event.getGuiGraphics().pose();
                poseStack.pushPose();

                float guiOffsetX = shakeOffsetX * GUI_MULTIPLIER;
                float guiOffsetY = shakeOffsetY * GUI_MULTIPLIER;

                int screenWidth = event.getGuiGraphics().guiWidth();
                int screenHeight = event.getGuiGraphics().guiHeight();
                float centerX = screenWidth / 2.0F;
                float centerY = screenHeight / 2.0F;

                poseStack.translate(centerX, centerY, 0);
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(shakeOffsetRoll * 0.2F));
                poseStack.translate(guiOffsetX, guiOffsetY, 0);
                poseStack.translate(-centerX, -centerY, 0);
            }
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void onRenderGuiPost(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
            if (shakeIntensity > 0.0F || Math.abs(shakeOffsetX) > 0.01F) {
                com.mojang.blaze3d.vertex.PoseStack poseStack = event.getGuiGraphics().pose();
                poseStack.popPose();
            }
        }
    }
    *///?}
}
