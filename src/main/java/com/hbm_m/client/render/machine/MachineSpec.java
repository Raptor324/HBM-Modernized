package com.hbm_m.client.render.machine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Иммутабельное описание рендера станка: набор частей (статические/анимированные/
 * динамические), хуки, резолверы модели и facing. Создаётся через
 * {@link MachineSpecBuilder}; рантайм-кеши VBO/инстансеров живут здесь же и
 * инвалидируются через {@link #clear()} (из RenderCacheManager).
 */
public final class MachineSpec<T extends BlockEntity> {

    /** Описание одной части. {@code name} — уникальный ключ; {@code modelPartName} — имя части модели. */
    record PartDef<T extends BlockEntity>(
            String name,
            String modelPartName,
            @Nullable PartAnimator<T> animator,      // null = статическая
            @Nullable QuadResolver<T> dynamicQuads,  // null = брать часть модели по имени
            @Nullable Function<T, String> dynamicCacheKey,
            int boneId,                               // 0 = не bone-часть; 1..N = chain-группа
            String staticCacheKey                     // предвычисленный "id/name" — без String-аллокаций в hot path
    ) {
        boolean dynamic() { return dynamicQuads != null; }
        boolean animated() { return animator != null; }
    }

    final String id;
    final Class<T> beClass;
    final Function<T, BakedModel> modelResolver;
    final Function<T, Direction> facingResolver;
    final List<PartDef<T>> parts;
    final List<MachineRenderHook<T>> hooks;
    final int viewDistance; // -1 = дефолт по конфигу статики
    /** Стабильный ключ LightSampleCache для одного 8-corner сэмпла на машину за кадр. */
    final long lightSampleKey;
    @Nullable final MachineSpecBuilder.BlockTransform<T> blockTransform; // null = дефолтный setupBlockTransform

    // Runtime: full cache key → GPU-держатель части. Кешируется между кадрами.
    private final Map<String, MachinePartRenderer> partRenderers = new ConcurrentHashMap<>();

    MachineSpec(String id, Class<T> beClass, Function<T, BakedModel> modelResolver,
                Function<T, Direction> facingResolver, List<PartDef<T>> parts,
                List<MachineRenderHook<T>> hooks, int viewDistance,
                @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform) {
        this.id = id;
        this.beClass = beClass;
        this.modelResolver = modelResolver;
        this.facingResolver = facingResolver;
        this.parts = List.copyOf(parts);
        this.hooks = List.copyOf(hooks);
        this.viewDistance = viewDistance;
        this.lightSampleKey = (0x4D4143484C534B4FL) ^ (id.hashCode() * 0x9E3779B97F4A7C15L);
        this.blockTransform = blockTransform;
    }

    @Nullable MachineSpecBuilder.BlockTransform<T> blockTransform() { return blockTransform; }

    String id() { return id; }
    List<PartDef<T>> parts() { return parts; }
    List<MachineRenderHook<T>> hooks() { return hooks; }
    Function<T, BakedModel> modelResolver() { return modelResolver; }
    Function<T, Direction> facingResolver() { return facingResolver; }
    int viewDistance() { return viewDistance; }

    /** Квады/модель части для этого BE (или null, если части нет в модели). */
    @Nullable BakedModel partModel(PartDef<T> part, BakedModel multipartModel) {
        if (part.dynamic()) return null;
        return (multipartModel instanceof com.hbm_m.client.model.AbstractMultipartBakedModel mp)
                ? mp.getPart(part.modelPartName()) : null;
    }

    @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynamicQuads(PartDef<T> part, T be) {
        if (!part.dynamic()) return null;
        try {
            return part.dynamicQuads().resolve(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' resolver failed", id, part.name(), t);
            return null;
        }
    }

    @Nullable String dynamicCacheKeyValue(PartDef<T> part, T be) {
        if (!part.dynamic() || part.dynamicCacheKey() == null) return null;
        try {
            return part.dynamicCacheKey().apply(be);
        } catch (Throwable t) {
            com.hbm_m.main.MainRegistry.LOGGER.error("[MachineRenderers:{}] dynamic part '{}' cacheKey failed", id, part.name(), t);
            return null;
        }
    }

    String cacheKey(PartDef<T> part, @Nullable String dynamicKey) {
        return part.dynamic() ? part.staticCacheKey() + "/" + dynamicKey : part.staticCacheKey();
    }

    /** GPU-держатель части (лениво, на render thread). */
    MachinePartRenderer partRenderer(PartDef<T> part, @Nullable BakedModel partModel,
                                     @Nullable List<net.minecraft.client.renderer.block.model.BakedQuad> dynQuads,
                                     @Nullable String dynamicKey) {
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer existing = partRenderers.get(key);
        if (existing != null && existing.matches(part, key)) {
            existing.ensureBuilt(partModel, dynQuads);
            return existing;
        }
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.boneId(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            raced.ensureBuilt(partModel, dynQuads);
            return raced;
        }
        created.ensureBuilt(partModel, dynQuads);
        return created;
    }

    /**
     * Ленивый вариант {@link #partRenderer}: квад resolver ({@code dynamicQuads}) вызывается
     * ТОЛЬКО когда рендерер ещё не был построен. Иначе (VBO уже в кеше) гора временных
     * BakedQuad создавалась бы каждый кадр впустую — профайлер показывал ~75% времени кадра
     * в retextureAndFixUV/BakedQuad.&lt;init&gt; (танки с жидкостью) + штормmarkSpriteActive у Embeddium.
     */
    MachinePartRenderer partRendererLazy(PartDef<T> part, @Nullable BakedModel partModel,
                                         T be, @Nullable String dynamicKey) {
        String key = cacheKey(part, dynamicKey);
        MachinePartRenderer existing = partRenderers.get(key);
        if (existing != null && existing.matches(part, key)) {
            if (!existing.isAttempted()) {
                existing.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return existing;
        }
        MachinePartRenderer created = new MachinePartRenderer(key, part.name(), part.boneId(), part.dynamic());
        MachinePartRenderer raced = partRenderers.putIfAbsent(key, created);
        if (raced != null) {
            if (!raced.isAttempted()) {
                raced.ensureBuilt(partModel, dynamicQuads(part, be));
            }
            return raced;
        }
        created.ensureBuilt(partModel, dynamicQuads(part, be));
        return created;
    }

    void flush(Matrix4f projection) {
        for (MachinePartRenderer r : partRenderers.values()) {
            r.flush(projection);
        }
    }

    /** Инвалидация GPU-кешей этой спеки (reload/disconnect) — вызывается из RenderCacheManager. */
    void clear() {
        for (MachinePartRenderer r : partRenderers.values()) {
            r.clear();
        }
        partRenderers.clear();
    }

    List<MachinePartRenderer> partRenderersSnapshot() {
        return Collections.unmodifiableList(new ArrayList<>(partRenderers.values()));
    }
}
