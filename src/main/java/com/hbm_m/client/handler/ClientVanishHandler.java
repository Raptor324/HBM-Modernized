package com.hbm_m.client.handler;

import com.hbm_m.lib.RefStrings;
import net.minecraft.world.entity.Entity;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
*///?}

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//? if forge {
@net.minecraftforge.api.distmarker.OnlyIn(net.minecraftforge.api.distmarker.Dist.CLIENT)
@Mod.EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
//?} elif neoforge {
/*@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
@EventBusSubscriber(modid = RefStrings.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
*///?}
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