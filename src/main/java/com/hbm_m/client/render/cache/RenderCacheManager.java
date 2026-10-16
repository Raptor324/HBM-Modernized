package com.hbm_m.client.render.cache;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.hbm_m.client.render.LightSampleCache;
import com.hbm_m.client.render.MeshRenderCache;
import com.hbm_m.client.render.MdiGeometryAtlas;
import com.hbm_m.client.render.culling.InstancedRenderFrame;
import com.hbm_m.client.render.culling.OcclusionCullingHelper;
import com.hbm_m.main.MainRegistry;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * ЕДИНАЯ точка инвалидации клиентских рендер-кешей.
 * <p>
 * Раньше списки очистки жили в трёх расходящихся местах (ClientSetup disconnect,
 * ClientSetup reload-листенер, DeferredCacheCleanupReloadListener) — каждый со
 * своим подмножеством кешей, и новые кеши регулярно забывали добавить.
 * Теперь любой код (reload, disconnect, отладка) зовёт
 * {@link #invalidateAll(Reason)}; новые подсистемы рендера регистрируют свою
 * очистку через {@link #register(InvalidationHook)} и больше нигде не упоминаются.
 * <p>
 * Инвалидация всегда выполняется на render thread (deferred через
 * {@link RenderSystem#recordRenderCall}, если вызвана из другого потока) —
 * GL-объекты нельзя удалять вне контекста.
 */
//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
//?} elif fabric {
/*@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
*///?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
*///?}
public final class RenderCacheManager {

    public enum Reason {
        /** F3+T / смена ресурс-пака / перезагрузка шейдеров. */
        RESOURCE_RELOAD,
        /** Выход из мира / disconnect. */
        SESSION_END
    }

    /** Очистка одного кеша; вызывается строго на render thread. */
    @FunctionalInterface
    public interface InvalidationHook {
        void invalidate(Reason reason);
    }

    private static final List<InvalidationHook> HOOKS = new CopyOnWriteArrayList<>();

    private RenderCacheManager() {}

    /**
     * Регистрирует дополнительный инвалидатор. Вызывать на этапе init (не на
     * каждый кадр). Дубликат-защита по имени класса не делается — вызов один
     * раз из static init владельца кеша.
     */
    public static void register(InvalidationHook hook) {
        HOOKS.add(hook);
    }

    /**
     * Полная инвалидация всех кешей. Потокобезопасно: всегда исполняется на
     * render thread (или сразу, или deferred).
     */
    public static void invalidateAll(Reason reason) {
        if (RenderSystem.isOnRenderThread()) {
            invalidateAllNow(reason);
        } else {
            RenderSystem.recordRenderCall(() -> invalidateAllNow(reason));
        }
    }

    /**
     * Немедленная инвалидация. Только render thread!
     * Порядок фиксирован: сначала кадровые/MDI-состояния, затем GPU-атлас,
     * затем кеши рендереров, затем общие mesh/свет/окклюзия.
     */
    public static void invalidateAllNow(Reason reason) {
        try {
            // Кадровые состояния (гейт MDI-фрейма, статистика, отложенные redraw)
            InstancedRenderFrame.clear();

            // GPU-атлас MDI: сессии не должно переживать reload/disconnect
            MdiGeometryAtlas.resetForResourceLifecycle();

            // Кеши конкретных рендереров (инстансеры, DAE, дверные скины и т.д.);
            // фабричные станки чистятся через MachineRenderRegistry.clearAll().
            com.hbm_m.client.render.machine.MachineRenderRegistry.clearAll();
            com.hbm_m.client.render.implementations.DoorRenderer.clearAllCaches();

            // Зарегистрированные инвалидаторы (фабричные спеки, спецэффекты)
            for (InvalidationHook hook : HOOKS) {
                try {
                    hook.invalidate(reason);
                } catch (Throwable t) {
                    MainRegistry.LOGGER.error("RenderCacheManager: hook {} failed", hook, t);
                }
            }

            // Общие компиляторы/кеши движка
            MeshRenderCache.clearAll();
            LightSampleCache.invalidateAll();
            OcclusionCullingHelper.clearCache();

            com.hbm_m.powerarmor.layer.AbstractObjArmorLayer.clearAllCaches();

            MainRegistry.LOGGER.info("Render cache invalidation completed ({})", reason);
        } catch (Throwable t) {
            MainRegistry.LOGGER.error("Error during render cache invalidation", t);
        }
    }
}
