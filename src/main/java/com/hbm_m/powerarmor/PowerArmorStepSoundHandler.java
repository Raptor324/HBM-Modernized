package com.hbm_m.powerarmor;

import com.hbm_m.main.MainRegistry;

import dev.architectury.event.events.client.ClientTickEvent;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PowerArmorStepSoundHandler {

    private PowerArmorStepSoundHandler() {}

    private static boolean INITIALIZED = false;

    // Local-player step detection state (client-only).
    private static double lastX;
    private static double lastZ;
    private static boolean hasLastPos = false;
    private static double walkAccum = 0.0D;
    private static int stepCooldownTicks = 0;

    /**
     * Multiloader-safe step sound handler.
     *
     * Forge implementation previously hooked {@code PlaySoundEvent} and replaced vanilla step sounds.
     * That event is loader-specific, so we implement a client-tick based step detector for the local player.
     */
    public static void initClient() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        ClientTickEvent.CLIENT_POST.register(client -> onClientTick());
    }

    private static void onClientTick() {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.isPaused()) return;

        if (stepCooldownTicks > 0) {
            stepCooldownTicks--;
        }

        if (!player.onGround()) {
            // Reset accumulation when airborne.
            walkAccum = 0.0D;
            hasLastPos = false;
            return;
        }

        // Only apply to full-set power armor.
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            hasLastPos = false;
            walkAccum = 0.0D;
            return;
        }

        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem)) return;

        PowerArmorSpecs specs = armorItem.getSpecs();
        if (specs.stepSound == null || specs.stepSound.isBlank()) return;

        // Accumulate horizontal movement.
        double x = player.getX();
        double z = player.getZ();

        if (!hasLastPos) {
            lastX = x;
            lastZ = z;
            hasLastPos = true;
            return;
        }

        double dx = x - lastX;
        double dz = z - lastZ;
        lastX = x;
        lastZ = z;

        double horiz = Math.sqrt(dx * dx + dz * dz);
        walkAccum += horiz;

        // Typical step spacing: ~0.6 blocks. Add a small cooldown so sprinting doesn't spam.
        final double stepDistance = 0.55D;
        if (walkAccum < stepDistance) return;
        if (stepCooldownTicks > 0) return;

        walkAccum = 0.0D;
        stepCooldownTicks = 2;

        SoundEvent customSound = getSoundEvent(player.level(), specs.stepSound);
        if (customSound == null) return;

        // Slightly quieter than vanilla to avoid ear fatigue.
        float volume = 0.5F;
        float pitch = 1.0F;
        player.level().playSound(player, player.getX(), player.getY(), player.getZ(), customSound, SoundSource.PLAYERS, volume, pitch);
    }

    private static SoundEvent getSoundEvent(Level level, String id) {
        // Поддержка двух форматов:
        // 1) "hbm_m:step.powered"
        // 2) "step.powered" (будет считаться как hbm_m:step.powered)
        net.minecraft.resources.ResourceLocation rl;
        if (id.contains(":")) {
            rl = net.minecraft.resources.ResourceLocation.tryParse(id);
        } else {
            //? if fabric && < 1.21.1 {
            /*rl = new net.minecraft.resources.ResourceLocation(MainRegistry.MOD_ID, id);
            *///?} else {
                        rl = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, id);
            //?}

        }

        if (rl == null) return null;

        return level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.SOUND_EVENT)
                .get(rl);
    }
}
