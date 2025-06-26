package com.hbm_m.config;

/**
 * Конфигурация радиационной системы
 */
public class RadiationConfig {
    
    // Основные настройки
    public static boolean enableRadiation = true;       // Включена ли радиация в целом
    public static boolean enableChunkRads = true;       // Включена ли радиация в чанках
    public static boolean worldRadEffects = true;       // Включены ли эффекты радиации на мир (мутации блоков)
    
    // Настройки игрока
    public static float maxPlayerRad = 1000F;           // Максимальный уровень радиации у игрока
    public static float radDecay = 0.01F;               // Скорость распада радиации у игрока
    public static float radDamage = 0.05F;              // Урон от радиации
    public static float radDamageThreshold = 200F;      // Порог, после которого радиация начинает наносить урон
    public static int radSickness = 300;                // Порог для тошноты
    public static int radWater = 500;                   // Порог для негативного эффекта воды
    public static int radConfusion = 700;               // Порог для замешательства
    public static int radBlindness = 900;               // Порог для слепоты
    
    // Настройки чанков
    public static float maxRad = 100_000F;              // Максимальная радиация в чанке
    public static float fogRad = 50F;                   // Порог радиации для появления тумана
    public static int fogCh = 50;                       // Шанс появления тумана (1 из fogCh)
    public static float radChunkDecay = 0.1F;          // Скорость распада радиации в чанке
    public static float radChunkSpreadFactor = 0.2F;  // Фактор распространения радиации между соседними чанками
    public static float radSpreadThreshold = 0.01F;    // Порог, ниже которого радиация не распространяется на соседние чанки
    public static float minRadDecayAmount = 0.01F;     // Минимальное количество радиации, отнимаемое за тик при распаде
    public static float radSourceInfluenceFactor = 0.35F; // Фактор влияния источников радиации на чанк
    public static float radRandomizationFactor = 1.0F; // Фактор рандомизации радиации в чанке (± процент, усилено)
    
    // Модификаторы для типов брони
    public static float hazmatMod = 0.25F;              // Защита от радиации для обычного костюма химзащиты (1.0 - нет защиты)
    public static float advHazmatMod = 0.1F;            // Защита для продвинутого костюма химзащиты
    public static float paaHazmatMod = 0.05F;           // Защита для костюма PAA

    public static boolean usePrismSystem = false; // Если true — PRISM, если false — Simple

    // Настройки отладочного рендера
    public static boolean enableDebugRender = true;    // Включен ли отладочный рендер радиации
    public static float debugRenderTextSize = 0.1F;    // Размер текста отладочного рендера
    public static int debugRenderDistance = 4;         // Дальность прорисовки отладочного рендера (в чанках)
}