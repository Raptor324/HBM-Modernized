package com.hbm_m.config;

/**
 * Конфигурация машин.
 * Значения загружаются из файла конфигурации и могут быть изменены пользователем.
 */
public class MachineConfig {

    /** Original 9.00_scaleRTGPower: RTG-/Betavoltaik-Leistung sinkt mit dem Zerfall (Lesen ueber VersatileConfig). */
    public static boolean scaleRTGPower = false;
    /** Original 9.01_doRTGsDecay: RTG-Pellets zerfallen ueberhaupt (wirkt auf Pelletwerte - Neustart). */
    public static boolean doRTGsDecay = true;

    //=====================================================================================//
    // КЛАСС КОНФИГУРАЦИИ ДЛЯ ПЕРЕДАЧИ В BLOCKENTITY
    //=====================================================================================//

    public static class FrackingTowerConfig {
        public long maxPower;
        public long consumption;
        public int solutionRequired;
        public int delay;
        public int oilPerDeposit;
        public int gasPerDepositMin;
        public int gasPerDepositMax;
        public double drainChance;
        public int oilPerBedrockDeposit;
        public int gasPerBedrockDepositMin;
        public int gasPerBedrockDepositMax;
        public int destructionRange;

        public FrackingTowerConfig() {
            ModClothConfig.FrackingTowerSettings cfg = ModClothConfig.get().frackingTower;
            this.maxPower = cfg.maxPower;
            this.consumption = cfg.consumption;
            this.solutionRequired = cfg.solutionRequired;
            this.delay = cfg.delay;
            this.oilPerDeposit = cfg.oilPerDeposit;
            this.gasPerDepositMin = cfg.gasPerDepositMin;
            this.gasPerDepositMax = cfg.gasPerDepositMax;
            this.drainChance = cfg.drainChance;
            this.oilPerBedrockDeposit = cfg.oilPerBedrockDeposit;
            this.gasPerBedrockDepositMin = cfg.gasPerBedrockDepositMin;
            this.gasPerBedrockDepositMax = cfg.gasPerBedrockDepositMax;
            this.destructionRange = cfg.destructionRange;
        }
    }

    /**
     * Получение конфигурации Fracking Tower.
     */
    public static FrackingTowerConfig getFrackingTowerConfig() {
        return new FrackingTowerConfig();
    }
}
