package com.hbm_m.powerarmor;

// import java.util.Random;

import com.hbm_m.main.MainRegistry;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerArmorSounds {

    private PowerArmorSounds() {} 

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingTickEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().isClientSide) return;

        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();
        
        // 1. Landing Sound Logic
        boolean wasInAir = player.getPersistentData().getBoolean("wasInAir");
        boolean isGround = player.onGround();
        boolean justLanded = wasInAir && isGround;
        
        player.getPersistentData().putBoolean("wasInAir", !isGround);

        if (justLanded && specs.fallSound != null) {
            if (player.getPersistentData().getBoolean("hbm_hard_landing_occured")) {
                player.getPersistentData().putBoolean("hbm_hard_landing_occured", false);
            } else {
                playFallSound(player, specs.fallSound);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!player.level().isClientSide) return;

        if (!ModPowerArmorItem.hasFSBArmor(player)) return;

        var chestStack = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chestStack.getItem() instanceof ModPowerArmorItem armorItem)) return;

        var specs = armorItem.getSpecs();

        if (specs.jumpSound != null) {
            playJumpSound(player, specs.jumpSound);
        }
    }

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
