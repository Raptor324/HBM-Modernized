package com.hbm_m.item.armor;

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

    // --- Резисты (0.0 - 1.0) ---
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

    public PowerArmorSpecs setResistances(float fall, float explosion, float kinetic, float projectile, float fire, float cold, float radiation) {
        this.resFall = fall;
        this.resExplosion = explosion;
        this.resKinetic = kinetic;
        this.resProjectile = projectile;
        this.resFire = fire;
        this.resCold = cold;
        this.resRadiation = radiation;
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