package com.hbm_m.item.armor;

import net.minecraft.world.effect.MobEffectInstance;
import java.util.ArrayList;
import java.util.List;

public class PowerArmorSpecs {
    public enum EnergyMode {
        CONSTANT_DRAIN, // Тип 1: Тратит постоянно, пока есть энергия - работают резисты
        DRAIN_ON_HIT    // Тип 2: Тратит только при получении урона для его компенсации
    }

    // Основные параметры
    public final EnergyMode mode;
    public final long capacity;
    public final long maxReceive;
    public final long usagePerTick; // Для CONSTANT_DRAIN
    public final long usagePerDamagePoint; // Для DRAIN_ON_HIT (сколько энергии стоит поглощение 1 ед урона)

    // Резисты (значения от 0.0 до 1.0, где 1.0 = 100% защиты)
    public float resFall = 0f;
    public float resExplosion = 0f;
    public float resKinetic = 0f; // Мечи, шипы
    public float resProjectile = 0f; // Пули, стрелы
    public float resFire = 0f; // Огонь, лава
    public float resCold = 0f; // Рыхлый снег
    public float resRadiation = 0f; // Радиация

    // Список эффектов для полного сета
    public List<MobEffectInstance> passiveEffects = new ArrayList<>();

    public PowerArmorSpecs(EnergyMode mode, long capacity, long maxReceive) {
        this.mode = mode;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.usagePerTick = 10; // Дефолт
        this.usagePerDamagePoint = 100; // Дефолт
    }

    // Перегрузка для более детальной настройки расхода
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
}
