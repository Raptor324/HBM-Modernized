package com.hbm_m.damagesource;

/**
 * Вспомогательный класс-фабрика для динамического создания DamageSource.
 * НЕ создавайте экземпляры этого класса. Используйте только статические методы.
 */
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageSources {

    // Приватные методы-конструкторы 

    /**
     * Базовый метод для создания DamageSource, у которого нет прямого атакующего.
     * Например: радиация, ядовитое облако, падение обломков.
     * @param level Мир, в котором наносится урон.
     * @param key   Ключ типа урона из ModDamageTypes.
     * @return      Новый экземпляр DamageSource.
     */
    private static DamageSource create(Level level, ResourceKey<DamageType> key) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key));
    }

    /**
     * Базовый метод для создания DamageSource, у которого есть атакующий.
     * @param attacker      Сущность, которая является причиной урона (например, стрелок).
     * @param directEntity  Сущность, которая нанесла урон напрямую (например, пуля). Может быть null.
     * @param key           Ключ типа урона из ModDamageTypes.
     * @return              Новый экземпляр DamageSource.
     */
    private static DamageSource create(Entity attacker, Entity directEntity, ResourceKey<DamageType> key) {
        return new DamageSource(
            attacker.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(key),
            directEntity,
            attacker
        );
    }
    
    // Публичные статические методы для удобного получения DamageSource в коде 

    // Типы урона от окружения (без конкретного атакующего)
    public static DamageSource nuclearBlast(Level level) { return create(level, ModDamageTypes.NUCLEAR_BLAST); }
    public static DamageSource radiation(Level level) { return create(level, ModDamageTypes.RADIATION); }
    public static DamageSource acid(Level level) { return create(level, ModDamageTypes.ACID); }
    public static DamageSource rubble(Level level) { return create(level, ModDamageTypes.RUBBLE); }
    public static DamageSource blackHole(Level level) { return create(level, ModDamageTypes.BLACK_HOLE); }
    public static DamageSource meteorite(Level level) { return create(level, ModDamageTypes.METEORITE); }
    public static DamageSource building(Level level) { return create(level, ModDamageTypes.BUILDING); }
    public static DamageSource cloud(Level level) { return create(level, ModDamageTypes.CLOUD); }
    public static DamageSource electricity(Level level) { return create(level, ModDamageTypes.ELECTRICITY); }
    public static DamageSource exhaust(Level level) { return create(level, ModDamageTypes.EXHAUST); }
    public static DamageSource spikes(Level level) { return create(level, ModDamageTypes.SPIKES); }
    public static DamageSource monoxide(Level level) { return create(level, ModDamageTypes.MONOXIDE); }
    public static DamageSource asbestos(Level level) { return create(level, ModDamageTypes.ASBESTOS); }
    public static DamageSource blacklung(Level level) { return create(level, ModDamageTypes.BLACKLUNG); }
    public static DamageSource vacuum(Level level) { return create(level, ModDamageTypes.VACUUM); }
    public static DamageSource microwave(Level level) { return create(level, ModDamageTypes.MICROWAVE); }
    // ... и другие типы урона, которые могут быть без источника

    // Типы урона от сущностей (атакующий + опционально снаряд)
    // directEntity - это сама пуля, attacker - тот, кто выстрелил.
    public static DamageSource revolverBullet(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.REVOLVER_BULLET); }
    public static DamageSource chopperBullet(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.CHOPPER_BULLET); }
    public static DamageSource tau(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.TAU); }
    public static DamageSource plasma(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.PLASMA); }
    public static DamageSource laser(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.LASER); }
    public static DamageSource flamethrower(Entity directEntity, Entity attacker) { return create(attacker, directEntity, ModDamageTypes.FLAMETHROWER); }

    // Урон, где атакующий и снаряд - это одно и то же (например, урон в ближнем бою)
    public static DamageSource electrified(Entity attacker) { return create(attacker, attacker, ModDamageTypes.ELECTRIFIED); }
    public static DamageSource acidPlayer(Entity attacker) { return create(attacker, attacker, ModDamageTypes.ACID_PLAYER); }
    
}