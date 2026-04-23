package com.hbm_m.api.network;

/**
 * Фабрика сетей конкретного типа.
 * Используется как ключ узловой карты вместе с BlockPos.
 * Каждый тип ресурса (fluid, energy, etc.) создаёт собственный провайдер-синглтон.
 */
public interface INetworkProvider<N extends NodeNet<?, ?, ?>> {

    /** Создать новую пустую сеть этого типа. */
    N createNetwork();
}
