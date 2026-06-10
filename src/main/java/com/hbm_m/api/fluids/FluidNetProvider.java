package com.hbm_m.api.fluids;

import java.util.HashMap;
import java.util.Map;

import com.hbm_m.api.network.INetworkProvider;

import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

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
     * Канонизация ключа сети.
     *
     * Важно: {@link com.hbm_m.api.network.UniNodespace.NodeKey} сравнивает {@link INetworkProvider} по identity,
     * поэтому для \"эквивалентных\" жидкостей (vanilla вода/лава vs HBM варианты, source vs flowing)
     * мы должны возвращать один и тот же ключ.
     */
    private static Fluid canonicalize(Fluid fluid) {
        if (fluid == null) return null;

        // Source/flowing должны жить в одной сети.
        if (fluid instanceof FlowingFluid flowing) {
            fluid = flowing.getSource();
        }

        // Вода/лава: объединяем vanilla и HBM варианты в один net-key.
        if (VanillaFluidEquivalence.isWater(fluid)) {
            return Fluids.WATER;
        }
        if (VanillaFluidEquivalence.isLava(fluid)) {
            return Fluids.LAVA;
        }

        return fluid;
    }

    /**
     * Получить или создать провайдер для данного типа жидкости.
     * Возвращает один и тот же объект при одинаковом Fluid — это ключ для NodeKey.
     */
    public static FluidNetProvider forFluid(Fluid fluid) {
        Fluid key = canonicalize(fluid);
        return PROVIDERS.computeIfAbsent(key, FluidNetProvider::new);
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
