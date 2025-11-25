package com.hbm_m.api.energy;

public interface IEnergyReceiver extends IEnergyConnector {

    long getEnergyStored();
    long getMaxEnergyStored();
    void setEnergyStored(long energy);

    /**
     * Максимальная скорость приема энергии за тик
     */
    long getReceiveSpeed();

    /**
     * Приоритет получения энергии в сети
     */
    Priority getPriority();

    /**
     * Принять энергию в приемник
     * @param maxReceive Максимальное количество энергии для приема
     * @param simulate Если true, только симулирует операцию без изменения состояния
     * @return Фактически принятое количество энергии
     */
    long receiveEnergy(long maxReceive, boolean simulate);

    /**
     * Может ли этот приемник принимать энергию
     */
    boolean canReceive();

    enum Priority {
        LOW,        // Низкий приоритет
        NORMAL,     // Обычный приоритет (по умолчанию)
        HIGH,       // Высокий приоритет

    }
}