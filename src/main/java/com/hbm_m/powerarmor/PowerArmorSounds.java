package com.hbm_m.powerarmor;

// import java.util.Random;

import com.hbm_m.main.MainRegistry;
import com.hbm_m.platform.PlayerPersistentData;

import dev.architectury.event.events.common.TickEvent;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class PowerArmorSounds {

    private PowerArmorSounds() {} 

    private static boolean INITIALIZED = false;

    public static void register() {
        if (INITIALIZED) return;
        INITIALIZED = true;
        TickEvent.PLAYER_POST.register(PowerArmorSounds::onPlayerTickClient);
    }

    private static void onPlayerTickClient(Player player) {
        if (!player.level().isClientSide) return; // client-only sounds

        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();
        
        var pdata = PlayerPersistentData.get(player);

        // 0. Jump Sound Logic (multiloader, client tick based)
        // Jump is: wasOnGround -> !onGround with positive Y velocity.
        // This filters out "walked off a ledge" (usually yVel <= 0 at the transition).
        boolean wasOnGround = pdata.getBoolean("hbm_was_on_ground");
        boolean onGround = player.onGround();
        if (wasOnGround && !onGround && player.getDeltaMovement().y > 0.0D) {
            if (specs.jumpSound != null) {
                playJumpSound(player, specs.jumpSound);
            }
        }
        pdata.putBoolean("hbm_was_on_ground", onGround);

        // 1. Landing Sound Logic
        boolean wasInAir = pdata.getBoolean("wasInAir");
        boolean isGround = player.onGround();
        boolean justLanded = wasInAir && isGround;
        
        pdata.putBoolean("wasInAir", !isGround);

        if (justLanded && specs.fallSound != null) {
            if (pdata.getBoolean("hbm_hard_landing_occured")) {
                pdata.putBoolean("hbm_hard_landing_occured", false);
            } else {
                playFallSound(player, specs.fallSound);
            }
        }
    }

    // Jump sound: Forge event existed, but for multiloader this needs a dedicated hook.
    // TODO: if needed, replicate via client input/tick detection (jump key + onGround transition).

    // ========== UTILITIES ==========

    public static void playJumpSound(Player player, String soundName) {
        SoundEvent soundEvent = getSoundEvent(soundName);
        if (soundEvent != null) {
            Level level = player.level();
            if (level.isClientSide) {
                level.playSound(player, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 0.7F, 1.0F);
            }
        }
    }

    public static void playFallSound(Player player, String soundName) {
        SoundEvent soundEvent = getSoundEvent(soundName);
        if (soundEvent == null) return;

        Level level = player.level();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), soundEvent, SoundSource.PLAYERS, 0.7F, 1.0F);
    }

    private static SoundEvent getSoundEvent(String soundName) {
        if (soundName == null) return null;
        if (soundName.startsWith("hbm_m:")) {
            soundName = soundName.substring(6);
        }

        try {
            return switch (soundName) {
                case "step.powered" -> com.hbm_m.sound.ModSounds.STEP_POWERED.get();
                case "step.metal" -> com.hbm_m.sound.ModSounds.STEP_METAL.get();
                case "step.iron_jump" -> com.hbm_m.sound.ModSounds.STEP_IRON_JUMP.get();
                case "step.iron_land" -> com.hbm_m.sound.ModSounds.STEP_IRON_LAND.get();
                default -> null;
            };
        } catch (Exception e) {
            MainRegistry.LOGGER.error("Failed to get sound event for name: " + soundName, e);
            return null;
        }
    }
}
