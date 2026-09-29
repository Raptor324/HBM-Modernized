package com.hbm_m.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import com.hbm_m.interfaces.IClientTicker;

/**
 * Реестр фабрик клиентских тикеров станков ({@link IClientTicker}) — общий
 * механизм для ВСЕХ машин фабрики вместо рефлексии Class.forName из common-кода.
 * <p>
 * Наполняется ТОЛЬКО на клиенте (из ClientSetup): клиентский код спокойно
 * импортирует и client-класс тикера, и common-BE. Dedicated server реестр пуст —
 * {@link #create} возвращает null, клиентское состояние просто не создаётся.
 * <p>
 * Новый станок с клиентским тикером: одна строка register(...) в ClientSetup —
 * без новых интерфейсов и без рефлексии.
 */
public final class ClientTickerRegistry {

    private static final Map<BlockEntityType<?>, Function<BlockEntity, IClientTicker>> FACTORIES =
            new HashMap<>();

    private ClientTickerRegistry() {}

    /** Регистрация фабрики тикера для типа BE (вызывать из клиентского сетапа). */
    public static void register(BlockEntityType<?> type, Function<BlockEntity, IClientTicker> factory) {
        FACTORIES.put(type, factory);
    }

    /** Тикер для BE или null (сервер / тип без тикера). Вызывать один раз при первом тике BE. */
    @Nullable
    public static IClientTicker create(BlockEntity be) {
        Function<BlockEntity, IClientTicker> factory = FACTORIES.get(be.getType());
        return factory == null ? null : factory.apply(be);
    }
}
