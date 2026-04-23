package com.hbm_m.api.fluids;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.api.network.INetworkProvider;

import net.minecraft.world.level.material.Fluid;

/**
 * Фабрика FluidNet, привязанная к конкретному типу жидкости.
 * Хранит по одному экземпляру на Fluid (синглтон), чтобы использоваться как ключ в NodeKey.
 * Аналог FluidNetProvider из 1.7.10 (через FluidType.getNetworkProvider()).
 */
public class FluidNetProvider implements INetworkProvider<FluidNet> {

    private static final Map<Fluid, FluidNetProvider> PROVIDERS = new HashMap<>();

    private final Fluid fluid;

    private FluidNetProvider(Fluid fluid) {
        this.fluid = fluid;
    }

    /**
     * Получить или создать провайдер для данного типа жидкости.
     * Возвращает один и тот же объект при одинаковом Fluid — это ключ для NodeKey.
     */
    public static FluidNetProvider forFluid(Fluid fluid) {
        return PROVIDERS.computeIfAbsent(fluid, FluidNetProvider::new);
    }

    /** Очистка при остановке сервера (опционально, для корректного GC). */
    public static void clearAll() {
        PROVIDERS.clear();
    }

    @Override
    public FluidNet createNetwork() {
        return new FluidNet(fluid);
    }

    public Fluid getFluid() { return fluid; }
}
