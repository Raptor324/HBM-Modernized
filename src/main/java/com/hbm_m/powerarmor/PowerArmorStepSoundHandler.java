package com.hbm_m.powerarmor;

import com.hbm_m.main.MainRegistry;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MainRegistry.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PowerArmorStepSoundHandler {

    private PowerArmorStepSoundHandler() {}
    private static final ThreadLocal<Boolean> HBM_M_SUPPRESS = ThreadLocal.withInitial(() -> false);

    /**
     * Перехватываем звуки шагов и заменяем их на кастомные, если на игроке надет полный сет силовой брони.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlaySound(PlaySoundEvent event) {
        if (Boolean.TRUE.equals(HBM_M_SUPPRESS.get())) {
            return;
        }
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        // Проверяем, что это звук шага (PLAYERS категория)
        if (sound.getSource() != SoundSource.PLAYERS) {
            return;
        }

        ResourceLocation soundId = sound.getLocation();
        if (soundId == null || !isStepSound(soundId)) {
            return;
        }

        float volume = 1.0F;
        float pitch = 1.0F;
        try {
            volume = sound.getVolume();
            pitch = sound.getPitch();
        } catch (NullPointerException ignored) {
            // Некоторые SoundInstance могут быть без backing Sound, используем дефолт
        }

        // Получаем позицию звука и проверяем, что это звук шага игрока
        // Звуки шагов обычно играются на позиции игрока
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;

        Player player = mc.player;
        
        // Проверяем, что звук играется рядом с игроком (в пределах 2 блоков)
        double distance = Math.sqrt(
            Math.pow(sound.getX() - player.getX(), 2) +
            Math.pow(sound.getY() - player.getY(), 2) +
            Math.pow(sound.getZ() - player.getZ(), 2)
        );
        
        if (distance > 2.0) return;

        // Проверяем, что на игроке надет полный сет силовой брони
        if (!ModPowerArmorItem.hasFSBArmor(player)) {
            return;
        }

        var chest = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof ModPowerArmorItem armorItem)) {
            return;
        }

        PowerArmorSpecs specs = armorItem.getSpecs();
        if (specs.stepSound == null || specs.stepSound.isBlank()) {
            return;
        }

        // Проверяем, что игрок на земле и движется
        if (!player.onGround()) {
            return;
        }

        // Получаем кастомный звук шага
        SoundEvent customSound = getSoundEvent(player.level(), specs.stepSound);
        if (customSound != null) {
            // Отменяем оригинальный звук и играем кастомный (с защитой от рекурсии)
            // Уменьшаем громкость шагов (умножаем на 0.6 для более тихого звука)
            float quieterVolume = volume * 0.5F;
            HBM_M_SUPPRESS.set(true);
            try {
                event.setSound(null);
                player.level().playSound(player, player.getX(), player.getY(), player.getZ(), 
                    customSound, SoundSource.PLAYERS, quieterVolume, pitch);
            } finally {
                HBM_M_SUPPRESS.set(false);
            }
        }
    }

    private static SoundEvent getSoundEvent(Level level, String id) {
        // Поддержка двух форматов:
        // 1) "hbm_m:step.powered"
        // 2) "step.powered" (будет считаться как hbm_m:step.powered)
        ResourceLocation rl;
        if (id.contains(":")) {
            rl = ResourceLocation.tryParse(id);
        } else {
            rl = ResourceLocation.fromNamespaceAndPath(MainRegistry.MOD_ID, id);
        }

        if (rl == null) return null;

        return level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.SOUND_EVENT)
                .get(rl);
    }

    private static boolean isStepSound(ResourceLocation soundId) {
        // Избегаем перехвата собственных звуков шага
        if (MainRegistry.MOD_ID.equals(soundId.getNamespace()) && soundId.getPath().startsWith("step")) {
            return false;
        }

        String path = soundId.getPath();
        // Ванильные шаги: block.<block>.step
        return (path.startsWith("block.") && path.endsWith(".step"))
                || path.contains(".step")
                || path.contains("footstep");
    }
}
