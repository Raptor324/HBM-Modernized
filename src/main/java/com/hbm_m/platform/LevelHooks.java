package com.hbm_m.platform;

import net.minecraft.world.level.Level;

/**
 * Высотные аксессоры уровня + звук/чанк-тикеты/рейкаст/SavedData.
 */
public final class LevelHooks {

    private LevelHooks() {}

    public static int minY(Level level) {
        return level.getMinBuildHeight();
    }

    public static int maxY(Level level) {
        return level.getMaxBuildHeight();
    }

    public static int minSectionY(Level level) {
        return level.getMinSection();
    }

    /** Округлённая вверх до секций высота мира. */
    public static int worldHeight(Level level) {
        int raw = maxY(level) - minY(level) + 1;
        return (raw + 15) & ~15;
    }

    // =====================================================================================
    //  Звук: Object (SoundEvent / Holder / Supplier-RegistrySupplier) -> SoundEvent.
    // =====================================================================================

    /** Разрешение «любого» звука в {@link net.minecraft.sounds.SoundEvent}. */
    private static net.minecraft.sounds.SoundEvent resolveSound(Object sound) {
        if (sound instanceof net.minecraft.core.Holder<?> holder) {
            return (net.minecraft.sounds.SoundEvent) holder.value();
        }
        if (sound instanceof net.minecraft.sounds.SoundEvent s) {
            return s;
        }
        if (sound instanceof java.util.function.Supplier<?> sup && sup.get() instanceof net.minecraft.sounds.SoundEvent s2) {
            // RegistrySupplier<SoundEvent> — Supplier, но не Holder на 1.20.1 (без этой ветки звук молча терялся)
            return s2;
        }
        return null;
    }

    /** Кросс-версионное проигрывание звука (скрывает SoundEvent vs Holder<SoundEvent>). */
    public static void playSound(Level level, net.minecraft.core.BlockPos pos, Object sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        net.minecraft.sounds.SoundEvent se = resolveSound(sound);
        if (se != null) {
            level.playSound(null, pos, se, source, volume, pitch);
        }
    }

    /** Кросс-версионное проигрывание звука по координатам. */
    public static void playSound(Level level, double x, double y, double z, Object sound, net.minecraft.sounds.SoundSource source, float volume, float pitch) {
        net.minecraft.sounds.SoundEvent se = resolveSound(sound);
        if (se != null) {
            level.playSound(null, x, y, z, se, source, volume, pitch);
        }
    }

    /**
     * Block-collider ray context без сущности. Конструктор 1.20.1 принимает nullable Entity;
     * в 1.21.1 есть перегрузка с CollisionContext, из-за которого голый {@code null} неоднозначен.
     */
    public static net.minecraft.world.level.ClipContext clipContext(net.minecraft.world.phys.Vec3 from, net.minecraft.world.phys.Vec3 to,
                                                                    net.minecraft.world.level.ClipContext.Fluid fluid) {
        //? if < 1.21.1 {
        return new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER, fluid, (net.minecraft.world.entity.Entity) null);
        //?} else {
        /*return new net.minecraft.world.level.ClipContext(from, to, net.minecraft.world.level.ClipContext.Block.COLLIDER, fluid,
                net.minecraft.world.phys.shapes.CollisionContext.empty());
        *///?}
    }

    /**
     * {@code DimensionDataStorage.computeIfAbsent} на обеих версиях: 1.20.1 принимает loader/creator,
     * 1.21.1 — {@code SavedData.Factory}, чей loader дополнительно получает реестры (игнорируем).
     */
    public static <T extends net.minecraft.world.level.saveddata.SavedData> T getOrCreateSavedData(
            net.minecraft.server.level.ServerLevel level, String name,
            java.util.function.Function<net.minecraft.nbt.CompoundTag, T> loader, java.util.function.Supplier<T> creator) {
        //? if < 1.21.1 {
        return level.getDataStorage().computeIfAbsent(loader::apply, creator::get, name);
        //?} else {
        /*return level.getDataStorage().computeIfAbsent(
                new net.minecraft.world.level.saveddata.SavedData.Factory<>(creator, (nbt, provider) -> loader.apply(nbt), null),
                name);
        *///?}
    }

    /**
     * Событие детонации взрыва: даёт чужим модам (защита территорий и т.п.) отфильтровать
     * список задетых сущностей. Раньше звалось только на Forge.
     */
    public static void fireExplosionDetonate(net.minecraft.world.level.Level level,
            net.minecraft.world.level.Explosion explosion,
            java.util.List<net.minecraft.world.entity.Entity> entities, double size) {
        //? if forge {
        net.minecraftforge.event.ForgeEventFactory.onExplosionDetonate(level, explosion, entities, size);
        //?} elif neoforge {
        /*net.neoforged.neoforge.event.EventHooks.onExplosionDetonate(level, explosion, entities, size);
        *///?}
    }

    // ════════════════════════════════════════════════════════════════
    // Chunk loading & ticket management
    // ════════════════════════════════════════════════════════════════

    /** Один тикет на ЧАНК (не на блок): TicketType<ChunkPos>, иначе ≤256 тикетов на чанк. */
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TEMPORARY_CHUNK_TICKET =
            net.minecraft.server.level.TicketType.create("hbm_m_temporary", java.util.Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong), 100);

    /**
     * Ensures the chunk containing the specified block position is loaded on the server.
     * Uses a temporary ticket (lifespan of 100 ticks / 5 seconds) to prevent premature unloading.
     *
     * <p>CONTRACT: this is a FORCE-LOAD, not an availability check. The trailing
     * {@code getChunk} is a deliberately BLOCKING full chunk load — callers
     * (e.g. {@code DetonatorItem.triggerDetonation}) immediately read block state
     * and block entities at {@code pos} afterwards, so the chunk must be fully
     * loaded before this returns. Do NOT replace with {@code getChunkNow}:
     * that would only succeed if the chunk already happens to be loaded.</p>
     *
     * @return true if the chunk is (now) loaded and accessible; false if out of world bounds or client-side not loaded
     */
    public static boolean loadChunkTemporary(Level level, net.minecraft.core.BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.world.level.ChunkPos chunkPos = new net.minecraft.world.level.ChunkPos(pos);
            // Быстрый путь: чанк уже тикает вместе с сущностями — тикет не нужен
            // (каждый addRegionTicket провоцирует пересчёт графа загрузки чанков).
            if (serverLevel.isPositionEntityTicking(pos)) {
                return true;
            }
            serverLevel.getChunkSource().addRegionTicket(TEMPORARY_CHUNK_TICKET, chunkPos, 2, chunkPos);
            return serverLevel.getChunk(chunkPos.x, chunkPos.z) != null;
        }
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4);
    }

    /** Adds a chunk loading region ticket via the server chunk cache. */
    public static <T> void addChunkTicket(Level level, net.minecraft.server.level.TicketType<T> type, net.minecraft.world.level.ChunkPos pos, int radius, T value) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().addRegionTicket(type, pos, radius, value);
        }
    }

    /** Removes a chunk loading region ticket via the server chunk cache. */
    public static <T> void removeChunkTicket(Level level, net.minecraft.server.level.TicketType<T> type, net.minecraft.world.level.ChunkPos pos, int radius, T value) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            serverLevel.getChunkSource().removeRegionTicket(type, pos, radius, value);
        }
    }
}
