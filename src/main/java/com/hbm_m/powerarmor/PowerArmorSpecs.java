package com.hbm_m.powerarmor;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;

public class PowerArmorSpecs {
    public enum EnergyMode {
        CONSTANT_DRAIN,
        DRAIN_ON_HIT
    }

    // --- Основные параметры ---
    public final EnergyMode mode;
    public final long capacity;
    public final long maxReceive;
    public final long usagePerTick;
    public final long usagePerDamagePoint;

    // --- Damage Threshold (DT) - полное поглощение урона ---
    public float dtFall = 0f;
    public float dtExplosion = 0f;
    public float dtKinetic = 0f;
    public float dtProjectile = 0f;
    public float dtFire = 0f;
    public float dtCold = 0f;
    public float dtRadiation = 0f;
    public float dtEnergy = 0f; // для лазеров, электричества и т.д.

    // --- Damage Resistance (DR) - процентное снижение (0.0 - 1.0) ---
    public float drFall = 0f;
    public float drExplosion = 0f;
    public float drKinetic = 0f;
    public float drProjectile = 0f;
    public float drFire = 0f;
    public float drCold = 0f;
    public float drRadiation = 0f;
    public float drEnergy = 0f;

    // --- Другие резисты (для обратной совместимости) ---
    public float resFall = 0f;
    public float resExplosion = 0f;
    public float resKinetic = 0f;
    public float resProjectile = 0f;
    public float resFire = 0f;
    public float resCold = 0f;
    public float resRadiation = 0f;

    // --- Спецэффекты (Флаги из ArmorFSB) ---
    public boolean hasVats = false;         // Красный текст в тултипе
    public boolean hasThermal = false;      // Красный текст + эффект (в будущем)
    public boolean hasNightVision = false;  // (Опционально)
    public boolean hasGeigerSound = false;  // Треск радиации
    public boolean hasCustomGeiger = false; // HUD геигер
    public boolean hasHardLanding = false;  // AOE урон при падении
    public boolean noHelmetRequired = false;// Сет работает без шлема

    // --- Движение ---
    public float stepHeight = 0f;           // Авто-прыжок (было setStepSize)
    public int stepSize = 0;                // Старое название для совместимости
    public int dashCount = 0;               // Рывки (нужен отдельный хендлер пакетов)

    // --- Звуки ---
    public String stepSound = null;         // Звук шага
    public String jumpSound = null;         // Звук прыжка
    public String fallSound = null;         // Звук падения

    // --- Оверлеи ---
    public String overlay = null;           // Путь к текстуре оверлея шлема

    public List<MobEffectInstance> passiveEffects = new ArrayList<>();

    public PowerArmorSpecs(EnergyMode mode, long capacity, long maxReceive) {
        this(mode, capacity, maxReceive, 10);
    }

    public PowerArmorSpecs(EnergyMode mode, long capacity, long maxReceive, long usage) {
        this.mode = mode;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        if (mode == EnergyMode.CONSTANT_DRAIN) {
            this.usagePerTick = usage;
            this.usagePerDamagePoint = 0;
        } else {
            this.usagePerTick = 0;
            this.usagePerDamagePoint = usage;
        }
    }

    // Конструктор для CONSTANT_DRAIN режима с drain и consumption
    public PowerArmorSpecs(EnergyMode mode, long capacity, long maxReceive, long drain, long consumption) {
        this.mode = mode;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.usagePerTick = drain;
        this.usagePerDamagePoint = consumption;
    }

    public PowerArmorSpecs setResistances(float fall, float explosion, float kinetic, float projectile, float fire, float cold, float radiation) {
        // Для обратной совместимости - устанавливаем только DR
        this.drFall = fall;
        this.drExplosion = explosion;
        this.drKinetic = kinetic;
        this.drProjectile = projectile;
        this.drFire = fire;
        this.drCold = cold;
        this.drRadiation = radiation;

        // Синхронизируем с legacy полями
        this.resFall = fall;
        this.resExplosion = explosion;
        this.resKinetic = kinetic;
        this.resProjectile = projectile;
        this.resFire = fire;
        this.resCold = cold;
        this.resRadiation = radiation;

        return this;
    }

    /**
     * Установить Damage Threshold (DT) значения для разных типов урона
     */
    public PowerArmorSpecs setDT(float fall, float explosion, float kinetic, float projectile, float fire, float cold, float radiation, float energy) {
        this.dtFall = fall;
        this.dtExplosion = explosion;
        this.dtKinetic = kinetic;
        this.dtProjectile = projectile;
        this.dtFire = fire;
        this.dtCold = cold;
        this.dtRadiation = radiation;
        this.dtEnergy = energy;
        return this;
    }

    /**
     * Установить Damage Resistance (DR) значения для разных типов урона
     */
    public PowerArmorSpecs setDR(float fall, float explosion, float kinetic, float projectile, float fire, float cold, float radiation, float energy) {
        this.drFall = fall;
        this.drExplosion = explosion;
        this.drKinetic = kinetic;
        this.drProjectile = projectile;
        this.drFire = fire;
        this.drCold = cold;
        this.drRadiation = radiation;
        this.drEnergy = energy;

        // Синхронизируем с legacy полями
        this.resFall = fall;
        this.resExplosion = explosion;
        this.resKinetic = kinetic;
        this.resProjectile = projectile;
        this.resFire = fire;
        this.resCold = cold;
        this.resRadiation = radiation;

        return this;
    }

    /**
     * Универсальный метод для установки DT и DR одновременно
     */
    public PowerArmorSpecs setProtection(float dtFall, float drFall, float dtExplosion, float drExplosion,
                                       float dtKinetic, float drKinetic, float dtProjectile, float drProjectile,
                                       float dtFire, float drFire, float dtCold, float drCold,
                                       float dtRadiation, float drRadiation, float dtEnergy, float drEnergy) {
        this.dtFall = dtFall;
        this.drFall = drFall;
        this.dtExplosion = dtExplosion;
        this.drExplosion = drExplosion;
        this.dtKinetic = dtKinetic;
        this.drKinetic = drKinetic;
        this.dtProjectile = dtProjectile;
        this.drProjectile = drProjectile;
        this.dtFire = dtFire;
        this.drFire = drFire;
        this.dtCold = dtCold;
        this.drCold = drCold;
        this.dtRadiation = dtRadiation;
        this.drRadiation = drRadiation;
        this.dtEnergy = dtEnergy;
        this.drEnergy = drEnergy;

        // Синхронизируем с legacy полями
        this.resFall = drFall;
        this.resExplosion = drExplosion;
        this.resKinetic = drKinetic;
        this.resProjectile = drProjectile;
        this.resFire = drFire;
        this.resCold = drCold;
        this.resRadiation = drRadiation;

        return this;
    }

    public PowerArmorSpecs addEffect(MobEffectInstance effect) {
        this.passiveEffects.add(effect);
        return this;
    }

    public PowerArmorSpecs setFeatures(boolean vats, boolean thermal, boolean hardLanding, boolean geiger) {
        this.hasVats = vats;
        this.hasThermal = thermal;
        this.hasHardLanding = hardLanding;
        this.hasGeigerSound = geiger;
        return this;
    }

    public PowerArmorSpecs setFeatures(boolean vats, boolean thermal, boolean hardLanding, boolean geiger, boolean customGeiger) {
        this.hasVats = vats;
        this.hasThermal = thermal;
        this.hasHardLanding = hardLanding;
        this.hasGeigerSound = geiger;
        this.hasCustomGeiger = customGeiger;
        return this;
    }
    
    public PowerArmorSpecs setMovement(float stepHeight, int dashCount) {
        this.stepHeight = stepHeight;
        this.dashCount = dashCount;
        return this;
    }

    public PowerArmorSpecs setNoHelmet(boolean noHelmet) {
        this.noHelmetRequired = noHelmet;
        return this;
    }

    public PowerArmorSpecs setGeigerHUD(boolean customGeiger) {
        this.hasCustomGeiger = customGeiger;
        return this;
    }

    public PowerArmorSpecs setStepSize(int stepSize) {
        this.stepSize = stepSize;
        this.stepHeight = stepSize; // Синхронизируем для совместимости
        return this;
    }

    public PowerArmorSpecs setSounds(String step, String jump, String fall) {
        this.stepSound = step;
        this.jumpSound = jump;
        this.fallSound = fall;
        return this;
    }

    public PowerArmorSpecs setStepSound(String step) {
        this.stepSound = step;
        return this;
    }

    public PowerArmorSpecs setJumpSound(String jump) {
        this.jumpSound = jump;
        return this;
    }

    public PowerArmorSpecs setFallSound(String fall) {
        this.fallSound = fall;
        return this;
    }

    public PowerArmorSpecs setOverlay(String path) {
        this.overlay = path;
        return this;
    }
}