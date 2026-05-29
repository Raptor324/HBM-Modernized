package com.hbm_m.client.missile.track;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.missile.track.MissileTrackPose;
import com.hbm_m.network.missile.S2CMissileTrackPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side store of server missile tracks (pose + contrail), independent of chunk entity sync.
 */
public final class MissileTrackClient {

    private static final Map<Integer, TrackEntry> TRACKS = new ConcurrentHashMap<>();
    /** Drop track if no packet for this long (ms). Must exceed brief network stalls. */
    private static final long STALE_MS = 15_000L;

    private MissileTrackClient() {}

    public static boolean isEnabled() {
        return ModClothConfig.get().enableMissileNetworkTrack;
    }

    public static void onTrack(S2CMissileTrackPacket msg) {
        if (!isEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !level.dimension().location().equals(msg.dimensionId)) {
            return;
        }
        TRACKS.compute(msg.entityId, (id, existing) -> {
            TrackEntry entry = existing != null ? existing : new TrackEntry(id, msg.dimensionId);
            entry.push(new MissileTrackPose(
                    msg.x, msg.y, msg.z,
                    msg.vx, msg.vy, msg.vz,
                    msg.yaw, msg.pitch,
                    msg.launchFacing,
                    msg.entityTypeId, msg.launchItemId,
                    msg.contrailScale, msg.worldTick,
                    System.nanoTime()));
            return entry;
        });
    }

    public static void onStop(int entityId) {
        TRACKS.remove(entityId);
    }

    public static void clear() {
        TRACKS.clear();
    }

    public static void tick() {
        if (!isEnabled()) {
            TRACKS.clear();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        // SP pause menu: client tick still runs; do not predict ahead of frozen server pose.
        if (mc.isPaused()) {
            return;
        }
        tickStale();

        ClientLevel level = mc.level;

        for (TrackEntry entry : TRACKS.values()) {
            entry.tick(level);
        }
    }

    private static void tickStale() {
        long now = System.currentTimeMillis();
        ClientLevel level = Minecraft.getInstance().level;
        Iterator<Map.Entry<Integer, TrackEntry>> it = TRACKS.entrySet().iterator();
        while (it.hasNext()) {
            TrackEntry entry = it.next().getValue();
            if (level != null && !level.dimension().location().equals(entry.dimensionId)) {
                it.remove();
                continue;
            }
            if (entry.curr != null && now - entry.lastPacketMillis > STALE_MS) {
                it.remove();
            }
        }
    }

    public static Iterable<TrackEntry> entries() {
        return TRACKS.values();
    }

    public static TrackEntry get(int entityId) {
        return TRACKS.get(entityId);
    }

    public static boolean hasActiveTrack(int entityId) {
        return isEnabled() && TRACKS.containsKey(entityId);
    }

    /**
     * Authoritative track mesh (and track contrail) instead of vanilla entity draw.
     * Vanilla only while the client still has a nearby entity in the spawn chunk, or during ascent
     * within view distance — otherwise track (teleport / unloaded entity / far flight).
     */
    public static boolean shouldUseTrackWorldRender(int entityId) {
        if (!isEnabled()) {
            return false;
        }
        TrackEntry entry = TRACKS.get(entityId);
        if (entry == null || entry.curr == null) {
            return false;
        }
        return !shouldPreferVanillaEntityRender(entry);
    }

    /**
     * Keep vanilla entity lerp for launch wobble when the client entity is actually present and close.
     * If the entity is unloaded (spectator teleport, tracking range) or far away, track must draw instead.
     */
    public static boolean shouldPreferVanillaEntityRender(TrackEntry entry) {
        if (entry == null || entry.curr == null) {
            return false;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        Entity entity = level.getEntity(entry.entityId);
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (!entry.crossedChunkBoundary) {
            return true;
        }
        if (!entry.isLaunchPhase()) {
            return false;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double dx = entity.getX() - camera.x;
        double dy = entity.getY() - camera.y;
        double dz = entity.getZ() - camera.z;
        double distSq = dx * dx + dy * dy + dz * dz;
        double maxDist = MissileTrackWorldRender.maxSafeRenderDistanceBlocks() * 0.85D;
        return distSq <= maxDist * maxDist;
    }

    public static final class TrackEntry {
        public final int entityId;
        public final ResourceLocation dimensionId;
        private MissileTrackPose prev;
        private MissileTrackPose curr;
        private long lastPacketMillis;

        // Физические координаты для сглаживания (Logical Tick)
        private double x, y, z;
        private double xo, yo, zo;
        private float yaw, pitch;
        private float yRotO, xRotO;

        private boolean hasLastContrail;
        private int clientTicksSinceUpdate;

        private int spawnChunkX;
        private int spawnChunkZ;
        private boolean crossedChunkBoundary;
        private boolean wasTrackWorldRender;

        TrackEntry(int entityId, ResourceLocation dimensionId) {
            this.entityId = entityId;
            this.dimensionId = dimensionId;
        }

        void push(MissileTrackPose pose) {
            lastPacketMillis = System.currentTimeMillis();
            long nowNanos = System.nanoTime();

            if (curr == null) {
                ChunkPos spawnChunk = new ChunkPos(BlockPos.containing(pose.x(), pose.y(), pose.z()));
                spawnChunkX = spawnChunk.x;
                spawnChunkZ = spawnChunk.z;
                crossedChunkBoundary = false;
                hasLastContrail = false;
                syncVisualStateFromEntityOrPose(pose);
            } else {
                prev = curr;
            }

            curr = pose.withReceiveNanos(nowNanos);
            clientTicksSinceUpdate = 0;
        }

        MissileTrackPose latest() {
            return curr;
        }

        void tick(ClientLevel level) {
            if (curr == null) {
                return;
            }

            updateChunkBoundary(level);

            boolean useTrack = shouldUseTrackWorldRender(entityId);
            if (!useTrack) {
                Entity entity = level.getEntity(entityId);
                if (entity != null) {
                    syncVisualStateFromEntity(entity);
                }
                wasTrackWorldRender = false;
                clientTicksSinceUpdate++;
                return;
            }

            if (!wasTrackWorldRender) {
                Entity entity = level.getEntity(entityId);
                if (entity != null) {
                    syncVisualStateFromEntity(entity);
                } else {
                    syncVisualStateFromEntityOrPose(curr);
                }
                hasLastContrail = false;
            }
            wasTrackWorldRender = true;

            xo = x;
            yo = y;
            zo = z;
            yRotO = yaw;
            xRotO = pitch;

            clientTicksSinceUpdate++;

            // 1. Предсказываем ГДЕ ракета должна быть прямо сейчас на сервере (Цель)
            double targetX = curr.x() + curr.vx() * clientTicksSinceUpdate;
            double targetY = curr.y() + curr.vy() * clientTicksSinceUpdate;
            double targetZ = curr.z() + curr.vz() * clientTicksSinceUpdate;

            // 2. Добавляем базовую скорость к нашим текущим визуальным координатам
            x += curr.vx();
            y += curr.vy();
            z += curr.vz();

            // 3. Мягко "дотягиваем" визуальные координаты до идеальной цели.
            // При ускорении ракета будет сглаживать ошибку предсказания без рывков.
            double lerpPos = 0.33D;
            x = Mth.lerp(lerpPos, x, targetX);
            y = Mth.lerp(lerpPos, y, targetY);
            z = Mth.lerp(lerpPos, z, targetZ);

            // То же самое для вращения
            float targetYaw = curr.yaw();
            float targetPitch = curr.pitch();
            float yawVel = 0;
            float pitchVel = 0;

            if (prev != null) {
                int interval = Math.max(1, ModClothConfig.get().missileTrackInterval);
                yawVel = Mth.wrapDegrees(curr.yaw() - prev.yaw()) / interval;
                pitchVel = (curr.pitch() - prev.pitch()) / interval;

                targetYaw += yawVel * clientTicksSinceUpdate;
                targetPitch += pitchVel * clientTicksSinceUpdate;
            }

            yaw += yawVel;
            pitch += pitchVel;

            yaw = Mth.rotLerp(0.33F, yaw, targetYaw);
            pitch = Mth.rotLerp(0.33F, pitch, targetPitch);

            // 4. Генерируем партиклы следа (Контрейлы)
            // Это должно происходить ИМЕННО в тиках для идеально ровного интервала спавна
            if (level != null) {
                tickContrail(level);
                tickNozzleFlare(level);
            }
        }

        private void updateChunkBoundary(ClientLevel level) {
            if (crossedChunkBoundary) {
                return;
            }
            Entity entity = level.getEntity(entityId);
            double checkX = entity != null ? entity.getX() : x;
            double checkZ = entity != null ? entity.getZ() : z;
            ChunkPos currentChunk = new ChunkPos(BlockPos.containing(checkX, 0.0D, checkZ));
            int chunkX = currentChunk.x;
            int chunkZ = currentChunk.z;
            if (chunkX != spawnChunkX || chunkZ != spawnChunkZ) {
                crossedChunkBoundary = true;
                if (entity != null) {
                    syncVisualStateFromEntity(entity);
                }
            }
        }

        /** Ascent phase — vanilla entity lerp gives smooth launch like 1.7.10 {@code EntityThrowableInterp}. */
        boolean isLaunchPhase() {
            return curr != null && curr.vy() > 0.0D;
        }

        private void syncVisualStateFromEntityOrPose(MissileTrackPose pose) {
            Minecraft mc = Minecraft.getInstance();
            ClientLevel level = mc.level;
            Entity entity = level != null ? level.getEntity(entityId) : null;
            if (entity != null) {
                syncVisualStateFromEntity(entity);
            } else {
                x = xo = pose.x();
                y = yo = pose.y();
                z = zo = pose.z();
                yaw = yRotO = pose.yaw();
                pitch = xRotO = pose.pitch();
            }
        }

        private void syncVisualStateFromEntity(Entity entity) {
            x = xo = entity.getX();
            y = yo = entity.getY();
            z = zo = entity.getZ();
            yaw = yRotO = entity.getYRot();
            pitch = xRotO = entity.getXRot();
        }

        private void tickContrail(ClientLevel level) {
            if (hasLastContrail) {
                MissileTrackContrail.spawn(
                        level,
                        xo, yo, zo, // Строго от предыдущего тика
                        x, y, z,    // Строго к текущему
                        curr.contrailScale());
            } else {
                hasLastContrail = true;
            }
        }

        private void tickNozzleFlare(ClientLevel level) {
            Vec3 step = new Vec3(x - xo, y - yo, z - zo);
            MissileNozzleFlare.spawn(level, x, y, z, pitch, yaw, step, curr.contrailScale());
        }

        public InterpolatedPose interpolate(float partialTick) {
            if (curr == null) {
                return null;
            }

            // Рендер теперь только интерполирует между двумя логическими кадрами клиента (xo -> x)
            // Никакой экстраполяции во время рендера — абсолютная плавность движения (500+ FPS)
            double interpX = Mth.lerp(partialTick, xo, x);
            double interpY = Mth.lerp(partialTick, yo, y);
            double interpZ = Mth.lerp(partialTick, zo, z);

            float interpYaw = Mth.rotLerp(partialTick, yRotO, yaw);
            float interpPitch = Mth.rotLerp(partialTick, xRotO, pitch);

            return new InterpolatedPose(
                    curr, prev != null ? prev : curr,
                    interpX, interpY, interpZ, interpYaw, interpPitch);
        }
    }

    public record InterpolatedPose(
            MissileTrackPose current,
            MissileTrackPose previous,
            double x, double y, double z,
            float yaw, float pitch
    ) {}
}