package com.hbm_m.server.missile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.blockentity.machines.LaunchPadBaseBlockEntity;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.missile.MissileBaseEntity;
import com.hbm_m.missile.track.MissileTrackPose;
import com.hbm_m.network.missile.S2CMissileTrackPacket;
import com.hbm_m.network.missile.S2CMissileTrackStopPacket;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Sends authoritative missile poses to every player in the same dimension,
 * independent of vanilla entity tracking / client chunk loading.
 */
public final class MissileTrackBroadcaster {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean TRACK_DEBUG = Boolean.getBoolean("hbm_m.missileTrackDebug");
    /** Active missiles per dimension — not tied to {@link ServerLevel#getAllEntities()} chunk visibility. */
    private static final Map<ResourceLocation, Set<MissileBaseEntity>> ACTIVE_BY_DIMENSION = new ConcurrentHashMap<>();
    private static boolean registered;

    private MissileTrackBroadcaster() {}

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        TickEvent.SERVER_POST.register(server -> {
            if (server == null) {
                return;
            }
            for (ServerLevel level : server.getAllLevels()) {
                if (level != null) {
                    tick(level);
                }
            }
        });
        PlayerEvent.PLAYER_JOIN.register(MissileTrackBroadcaster::syncTracksToPlayer);
        // Singleplayer «save & quit to title» → повторный вход не перезапускает JVM,
        // поэтому эти статические карты переживают перезаход и хранят ссылки на сущности
        // из прежнего экземпляра ServerLevel. Это (а) рисует «замороженные» фантомные
        // контакты на радаре и (б) заставляет нижестоящий duplicate-UUID guard отвергать
        // легитимно восстановленную из NBT ракету. Сбрасываем набор измерения при загрузке уровня.
        LifecycleEvent.SERVER_LEVEL_LOAD.register(level -> {
            Set<MissileBaseEntity> stale = ACTIVE_BY_DIMENSION.remove(level.dimension().location());
            if (stale != null) {
                stale.clear();
            }
        });
    }

    public static void onMissileSpawned(MissileBaseEntity missile) {
        if (missile.level().isClientSide()) {
            return;
        }
        ACTIVE_BY_DIMENSION
                .computeIfAbsent(missile.level().dimension().location(), k -> ConcurrentHashMap.newKeySet())
                .add(missile);
        if (missile.level() instanceof ServerLevel level && ModClothConfig.get().enableMissileNetworkTrack) {
            broadcastTrackNow(level, missile);
        }
    }

    /** First pose packet the same tick as spawn — no gap before vanilla entity drops off at view distance. */
    private static void broadcastTrackNow(ServerLevel level, MissileBaseEntity missile) {
        if (level.getServer() == null || level.getServer().getPlayerList() == null) {
            return;
        }
        S2CMissileTrackPacket packet = buildPacket(level, missile);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) {
                S2CMissileTrackPacket.sendTo(player, packet);
            }
        }
    }

    /**
     * Все активные ракеты в измерении, НЕ привязанные к загрузке чанков
     * (порт глобального реестра IRadarDetectableNT из 1.7.10).
     * Используется радаром для обнаружения ракет в незагруженных чанках.
     */
    public static Set<MissileBaseEntity> getActiveMissiles(ServerLevel level) {
        Set<MissileBaseEntity> set = ACTIVE_BY_DIMENSION.get(level.dimension().location());
        return set != null ? set : Set.of();
    }

    /**
     * Детект «призрака» от {@code PersistentEntitySectionManager}: в наборе уже есть
     * <b>другая</b> живая {@link MissileBaseEntity} с тем же UUID, что и у {@code missile},
     * в том же уровне.
     * <p>
     * Сущности, которые сами форсируют загрузку чанков (region‑тикет в
     * {@link MissileBaseEntity#onAddedToWorld()}), изредка ре‑десериализуются из
     * устаревшего entity‑chunk при полёте вдали от игрока — копия наследует UUID
     * оригинала (загрузка с диска обходит UUID‑проверку PESM). Без этой проверки в
     * воздухе появляется вторая (иногда третья) реальная ракета и второй контакт на радаре.
     * <p>
     * Условие {@code other.level() == level} принципиально: при перезаходе в мир в наборе
     * могут оставаться «осиротевшие» ссылки со старым уровнем — их нужно игнорировать,
     * иначе легитимная восстановленная ракета будет ложно отброшена.
     */
    public static boolean isDuplicateSpawn(ServerLevel level, MissileBaseEntity missile) {
        Set<MissileBaseEntity> set = ACTIVE_BY_DIMENSION.get(level.dimension().location());
        if (set == null || set.isEmpty()) {
            return false;
        }
        UUID uuid = missile.getUUID();
        for (MissileBaseEntity other : set) {
            if (other != missile
                    && !other.isRemoved()
                    && other.level() == level
                    && other.getUUID().equals(uuid)) {
                return true;
            }
        }
        return false;
    }

    public static void onMissileRemoved(MissileBaseEntity missile) {
        if (missile.level().isClientSide()) {
            return;
        }
        ResourceLocation dim = missile.level().dimension().location();
        Set<MissileBaseEntity> set = ACTIVE_BY_DIMENSION.get(dim);
        if (set != null) {
            set.remove(missile);
        }
        if (missile.level() instanceof ServerLevel level) {
            broadcastStop(level, missile.getId());
        }
    }

    private static void tick(ServerLevel level) {
        ModClothConfig cfg = ModClothConfig.get();
        if (!cfg.enableMissileNetworkTrack) {
            return;
        }

        int interval = Math.max(1, cfg.missileTrackInterval);
        if ((level.getGameTime() % interval) != 0L) {
            return;
        }

        int maxRange = cfg.missileTrackMaxRangeBlocks;
        boolean limitByRange = maxRange > 0;
        double maxRangeSq = (double) maxRange * maxRange;
        ResourceLocation dim = level.dimension().location();
        Set<MissileBaseEntity> missiles = ACTIVE_BY_DIMENSION.get(dim);
        if (missiles == null || missiles.isEmpty()) {
            return;
        }

        for (MissileBaseEntity missile : missiles) {
            if (missile.isRemoved() || missile.level() != level) {
                missiles.remove(missile);
                broadcastStop(level, missile.getId()); // Обязательно шлем сигнал об удалении!
            } else {
                broadcastPose(level, missile, limitByRange, maxRangeSq);
            }
        }
    }

    /**
     * Called from {@link MissileBaseEntity#tick()} so poses are sent even when vanilla entity sync stalls
     * in unloaded chunk sections.
     */
    public static void broadcastPoseForMissile(MissileBaseEntity missile) {
        if (!ModClothConfig.get().enableMissileNetworkTrack || missile.isRemoved()) {
            return;
        }
        if (!(missile.level() instanceof ServerLevel level)) {
            return;
        }
        ModClothConfig cfg = ModClothConfig.get();
        int interval = Math.max(1, cfg.missileTrackInterval);
        if ((level.getGameTime() % interval) != 0L) {
            return;
        }
        int maxRange = cfg.missileTrackMaxRangeBlocks;
        broadcastPose(level, missile, maxRange > 0, (double) maxRange * maxRange);
    }

    private static void broadcastPose(ServerLevel level, MissileBaseEntity missile,
                                    boolean limitByRange, double maxRangeSq) {
        if (level.getServer() == null || level.getServer().getPlayerList() == null) {
            return;
        }
        
        S2CMissileTrackPacket packet = buildPacket(level, missile);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() != level) {
                continue;
            }
            // Range gate is for bandwidth only; 0 = unlimited (required for BVR / post-teleport track view).
            if (limitByRange && player.distanceToSqr(missile) > maxRangeSq) {
                if (TRACK_DEBUG) {
                    LOGGER.info("[MissileTrack] Skipping player {} for missile {} - out of range ({} > {})",
                            player.getName().getString(), missile.getId(),
                            (int) Math.sqrt(player.distanceToSqr(missile)), (int) Math.sqrt(maxRangeSq));
                }
                continue;
            }
            S2CMissileTrackPacket.sendTo(player, packet);
            if (TRACK_DEBUG) {
                LOGGER.info("[MissileTrack] Sent track packet for missile {} to {} at {}/{}/{} (dist={})",
                        missile.getId(), player.getName().getString(),
                        (int) missile.getX(), (int) missile.getY(), (int) missile.getZ(),
                        (int) Math.sqrt(player.distanceToSqr(missile)));
            }
        }
    }

    private static void syncTracksToPlayer(ServerPlayer player) {
        ModClothConfig cfg = ModClothConfig.get();
        ServerLevel level = player.serverLevel();
        Set<MissileBaseEntity> missiles = ACTIVE_BY_DIMENSION.get(level.dimension().location());
        if (missiles == null || missiles.isEmpty()) {
            return;
        }
        int maxRange = cfg.missileTrackMaxRangeBlocks;
        boolean limitByRange = maxRange > 0;
        double maxRangeSq = (double) maxRange * maxRange;

        for (MissileBaseEntity missile : missiles) {
            if (missile.isRemoved() || missile.level() != level) {
                continue;
            }
            if (limitByRange && player.distanceToSqr(missile) > maxRangeSq) {
                continue;
            }
            S2CMissileTrackPacket.sendTo(player, buildPacket(level, missile));
        }
    }

    private static S2CMissileTrackPacket buildPacket(ServerLevel level, MissileBaseEntity missile) {
        ResourceLocation entityTypeId = BuiltInRegistries.ENTITY_TYPE.getKey(missile.getType());
        Item launchItem = LaunchPadBaseBlockEntity.getLaunchItemFor(missile.getType());
        ResourceLocation launchItemId = launchItem != null
                ? BuiltInRegistries.ITEM.getKey(launchItem)
                : ResourceLocation.fromNamespaceAndPath("minecraft", "air");

        var motion = missile.getDeltaMovement();
        double mult = missile.getMotionMultiplier();
        MissileTrackPose pose = new MissileTrackPose(
                missile.getX(), missile.getY(), missile.getZ(),
                motion.x * mult, motion.y * mult, motion.z * mult,
                missile.getYRot(), missile.getXRot(),
                missile.getLaunchFacing(),
                entityTypeId, launchItemId,
                missile.getContrailScaleForSync(),
                level.getGameTime(),
                0L);

        return new S2CMissileTrackPacket(
                missile.getId(),
                level.dimension().location(),
                pose);
    }

    private static void broadcastStop(ServerLevel level, int entityId) {
        if (level.getServer() == null || level.getServer().getPlayerList() == null) {
            return;
        }
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level) {
                S2CMissileTrackStopPacket.sendTo(player, entityId);
            }
        }
    }
}
