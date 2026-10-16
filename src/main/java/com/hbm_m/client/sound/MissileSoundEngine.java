package com.hbm_m.client.sound;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?}
//? if fabric {
/*import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
*///?}

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.hbm_m.client.missile.track.MissileTrackClient;
import com.hbm_m.entity.missile.MissileBaseEntity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Клиентский движок звука баллистических ракет: владеет {@link MissileFlybySoundInstance}
 * по одному на ракету и кормит их снимками позы/скорости каждый тик.
 *
 * Источник данных — в первую очередь сетевой трек ({@link MissileTrackClient}), который
 * живёт независимо от загрузки чанков и vanilla entity tracking'а: именно благодаря ему
 * звук «далеко распространяется» (ракета слышна за сотни блоков, где сущности давно нет).
 * При выключенном сетевом треке (конфиг) берутся видимые {@link MissileBaseEntity} в мире.
 *
 * Все вычисления — в логическом тике ({@code ClientTickEvent.CLIENT_POST}), а не в кадре:
 * стабильный шаг физики при любом FPS.
 */
//? if forge {
@OnlyIn(Dist.CLIENT)
//?}
//? if fabric {
/*@Environment(EnvType.CLIENT)
*///?}
public final class MissileSoundEngine {

    /** Создаём канал только когда игрок в зоне слышимости (с запасом за пределы rolloff). */
    private static final double AUDIBLE_RANGE = 512.0D + 64.0D;

    private static final Map<Integer, MissileFlybySoundInstance> ACTIVE = new HashMap<>();

    private MissileSoundEngine() {}

    /** Хук из {@code MissileTrackClientEvents} — вызывать ПОСЛЕ {@code MissileTrackClient.tick()}. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null || mc.isPaused()) {
            // На паузе SoundManager сам ставит каналы на паузу — просто не пересчитываем физику.
            // Но при потере уровня/игрока (logout) обязаны всё зачистить, иначе звуки «зависнут».
            if (level == null || player == null) {
                clear();
            }
            return;
        }

        Map<Integer, Vec3[]> snapshots = collectSnapshots(level, player);
        updateInstances(snapshots);
    }

    /** Снимки позы/скорости всех слышимых ракет измерения: [0]=pos, [1]=vel. */
    private static Map<Integer, Vec3[]> collectSnapshots(ClientLevel level, LocalPlayer player) {
        Map<Integer, Vec3[]> out = new HashMap<>();

        if (MissileTrackClient.isEnabled()) {
            for (MissileTrackClient.TrackEntry entry : MissileTrackClient.entries()) {
                Vec3 pos = entry.visualPos();
                Vec3 vel = pos.subtract(entry.visualPrevPos());
                if (withinAudible(player, pos)) {
                    out.put(entry.entityId, new Vec3[]{pos, vel});
                }
            }
        } else {
            // Фоллбэк: видимые ракеты в радиусе слышимости (трек-рендер выключен в конфиге).
            AABB box = player.getBoundingBox().inflate(AUDIBLE_RANGE);
            for (Entity entity : level.getEntitiesOfClass(MissileBaseEntity.class, box)) {
                Vec3 pos = entity.position();
                if (withinAudible(player, pos)) {
                    // На клиенте deltaMovement у стабильно лерпующейся ракеты надёжен:
                    // MissileTrackClient может ориентироваться на эту же сущность.
                    Vec3 vel = entity.getDeltaMovement();
                    out.put(entity.getId(), new Vec3[]{pos, vel});
                }
            }
        }
        return out;
    }

    private static boolean withinAudible(LocalPlayer player, Vec3 pos) {
        return player.getEyePosition().distanceToSqr(pos) <= AUDIBLE_RANGE * AUDIBLE_RANGE;
    }

    private static void updateInstances(Map<Integer, Vec3[]> snapshots) {
        Minecraft mc = Minecraft.getInstance();

        // Помечаем живые ракеты: показываем их инстансы и обновляем источник.
        for (Map.Entry<Integer, Vec3[]> snap : snapshots.entrySet()) {
            int entityId = snap.getKey();
            Vec3 pos = snap.getValue()[0];
            Vec3 vel = snap.getValue()[1];

            MissileFlybySoundInstance instance = ACTIVE.get(entityId);
            if (instance == null || instance.isStopped()) {
                instance = new MissileFlybySoundInstance(pos, vel);
                ACTIVE.put(entityId, instance);
                mc.getSoundManager().play(instance);
            } else {
                instance.updateSource(pos, vel);
            }
        }

        // Исчезнувшим ракетам (взрыв/конец трека/вылет из дальности) — плавный fade-out.
        Iterator<Map.Entry<Integer, MissileFlybySoundInstance>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MissileFlybySoundInstance> e = it.next();
            if (!snapshots.containsKey(e.getKey())) {
                e.getValue().updateSource(null, null);
            }
            if (e.getValue().isStopped()) {
                it.remove();
            }
        }
    }

    /** Полная очистка (logout / смена мира) — вызывается вместе с MissileTrackClient.clear(). */
    public static void clear() {
        var soundManager = Minecraft.getInstance().getSoundManager();
        for (MissileFlybySoundInstance instance : ACTIVE.values()) {
            // AbstractTickableSoundInstance.stop() здесь protected — останавливаем канал через движок звука.
            soundManager.stop(instance);
        }
        ACTIVE.clear();
    }
}
