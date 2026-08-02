package com.hbm_m.client.handler;

import com.hbm_m.lib.RefStrings;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Скрывает рендер «мёртвых» сущностей на клиенте, чтобы эффект скелетонизации
 * (ParticleSkeletonNT) не дублировался с нормальным рендером трупа.
 *
 * Порт {@code ClientProxy.vanish/isVanished} из HBM 1.7.10.
 *
 * Сущность помечается "vanished" на 2 секунды по умолчанию
 * (этого хватает, пока сервер не удалит мёртвую сущность из списка трекинга).
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientVanishHandler {

    private static final Map<Integer, Long> VANISHED = new ConcurrentHashMap<>();
    private static final long DEFAULT_DURATION_MS = 2000L;

    public static void vanish(int entityId) {
        vanish(entityId, (int) DEFAULT_DURATION_MS);
    }

    public static void vanish(int entityId, int durationMs) {
        VANISHED.put(entityId, System.currentTimeMillis() + durationMs);
    }

    public static boolean isVanished(Entity entity) {
        if (entity == null) return false;
        Long until = VANISHED.get(entity.getId());
        return until != null && until > System.currentTimeMillis();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        if (isVanished(event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
