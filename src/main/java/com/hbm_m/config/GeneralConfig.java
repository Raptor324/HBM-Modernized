package com.hbm_m.config;

/**
 * Общие флаги. Порт {@link com.hbm.config.GeneralConfig} (1.7.10), минимальный набор.
 */
public final class GeneralConfig {

    public static boolean enable528 = false;

    private GeneralConfig() {
    }

    /**
     * Зеркалирует значения схемы (POJO {@link ModClothConfig}) в статические поля-фасады.
     * Аналог NEXT {@code BalanceConfig.loadFrom(store)}: NEXT перегенерирует фасады после
     * каждого применения конфига (NeoForgeHbmConfig.set/reload → NuclearTech.deriveConfigFacades);
     * здесь то же самое — вызов из {@code ConfigSchema.applyAll}, через который проходят
     * все пути применения (JSON-загрузка, GUI, C2S/S2C пакеты).
     */
    public static void syncFrom(ModClothConfig cfg) {
        enable528 = cfg.enable528;
    }
}
