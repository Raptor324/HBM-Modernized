package com.hbm_m.api.fluids;

import net.minecraft.core.BlockPos;

/**
 * Позиционная дифференциация fluid-портов мультиблока — порт механики 1.7.10
 * {@code IProxyDelegateProvider} + делегатов вроде {@code DelegateChemicalFactoy}
 * (Chemical Factory): у машины могут быть порты разного назначения, и коннектор
 * на конкретной позиции должен видеть только своё подмножество баков
 * (например, торцы Chemical Factory — выделенный контур охлаждения
 * вода/спент-стим, остальные порты — только рецептурные жидкости).
 *
 * <p>Реализуется контроллером мультиблока. {@link com.hbm_m.blockentity.machines.UniversalMachinePartBlockEntity}
 * спрашивает трансивер для позиции коннектора перед подпиской контроллера в сеть
 * и перед делегированием capability.</p>
 */
public interface IPositionalFluidTransceiver extends IFluidUserMK2 {

    /**
     * Трансивер, представляющий контроллер на конкретном коннекторе.
     *
     * @param connectorPos мировая позиция коннектора (части мультиблока)
     * @return this либо делегат с урезанным набором баков (никогда null)
     */
    IFluidUserMK2 getFluidTransceiverFor(BlockPos connectorPos);
}
