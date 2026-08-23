package com.hbm_m.api.energy;

/**
 * Маркер буферного накопителя энергии (аналог батареи в режиме BUFFER из 1.7.10).
 * В буферном режиме такая батарея становится «проводником»: создает собственный узел
 * (PowerNode), к которому подключаются провода, и подписывает себя как
 * provider+receiver в собственную сеть — энергия протекает сквозь её хранилище.
 *
 * Реализуется только батареями ({@link com.hbm_m.blockentity.machines.MachineBatteryBlockEntity},
 * {@link com.hbm_m.blockentity.machines.BatterySocketBlockEntity}) — не всеми IEnergyModeHolder.
 */
public interface PowerBuffer extends PowerConductor {
}
