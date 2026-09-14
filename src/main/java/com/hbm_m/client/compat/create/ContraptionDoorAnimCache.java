//? if forge || neoforge {
package com.hbm_m.client.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Map;
import java.util.WeakHashMap;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
//?} elif neoforge {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
*///?}

/**
 * Renderer-owned chase-кэш плавной анимации двери на контрапшене.
 *
 * <p>Проблема: toggling OPEN/PASSABLE blockstate на контрапшене вызывает
 * {@code resetClientContraption} — клиентский DoorBlockEntity пересоздаётся из
 * замороженного NBT (state=0), а значит вести анимацию через {@code be.state}
 * нельзя (мерцание/конвульсия). Вместо этого DoorRenderer на контрапшене читает
 * синкаемое OPEN blockstate-свойство и гонит прогресс к таргету (0/1) здесь,
 * в кэше, привязанном к стабильному (level, pos).
 *
 * <p>Level на контрапшене = VirtualRenderWorld (один экземпляр на ClientContraption,
 * переживает resetRenderLevel), на земле = ClientLevel. pos = локальная позиция BE.
 * Оба стабильны между пересозданиями BE.
 */
@OnlyIn(Dist.CLIENT)
public final class ContraptionDoorAnimCache {

    private static final WeakHashMap<Level, Map<Long, State>> CACHE = new WeakHashMap<>();

    private ContraptionDoorAnimCache() {}

    /**
     * @return прогресс открытия 0..1, плавно догоняющий OPEN-таргет blockstate.
     * @param openTimeTicks время открытия в тиках (DoorDecl.getOpenTime).
     */
    public static float chase(BlockEntity be, int openTimeTicks) {
        Level level = be.getLevel();
        if (level == null) return 0f;
        // Open-target из кэша (VirtualRenderWorld на клиенте, populate packet-applier'ом).
        boolean open = com.hbm_m.compat.ContraptionDoorState.getOpen(level, be.getBlockPos());

        long now = System.currentTimeMillis();
        Map<Long, State> perLevel = CACHE.computeIfAbsent(level, k -> new java.util.HashMap<>());
        long posKey = be.getBlockPos().asLong();
        State st = perLevel.get(posKey);
        if (st == null) {
            st = new State(open ? 1f : 0f, now);
            perLevel.put(posKey, st);
        }

        float dtSec = Math.min(0.25f, (now - st.lastMs) / 1000f);
        st.lastMs = now;
        float totalSec = Math.max(0.05f, openTimeTicks / 20f);
        float delta = dtSec / totalSec;
        if (open) {
            st.value = Math.min(1f, st.value + delta);
        } else {
            st.value = Math.max(0f, st.value - delta);
        }
        return st.value;
    }

    /** Сбросить кэш двери (напр. при разборке). */
    public static void invalidate(BlockEntity be) {
        Level level = be.getLevel();
        if (level == null) return;
        Map<Long, State> perLevel = CACHE.get(level);
        if (perLevel != null) perLevel.remove(be.getBlockPos().asLong());
    }

    private static final class State {
        float value;
        long lastMs;
        State(float value, long lastMs) {
            this.value = value;
            this.lastMs = lastMs;
        }
    }
}
//?}
