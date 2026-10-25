package com.hbm_m.item.weapon.sedna;

import java.util.EnumMap;
import java.util.Locale;

import org.jetbrains.annotations.Nullable;

import com.hbm_m.damagesource.ModDamageTypes;
import com.hbm_m.powerarmor.resist.DamageResistanceHandler.DamageClass;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Port von {@code DamageSourceSednaWithAttacker} / {@code DamageSourceSednaNoAttacker}: je Schadensklasse ein eigener
 * Schadenstyp {@code hbm_m:sedna_<klasse>} mit der Meldungs-ID {@code <klasse>} (klein) - so erkennt der
 * {@code DamageResistanceHandler} die Klasse wie im Original am Namen. Physischer Schaden zaehlt als Projektil,
 * Feuer als Feuer, Sprengstoff als Explosion (Original {@code setProjectile/setFireDamage/setExplosion}).
 * Todesmeldungen: {@code death.attack.<klasse>} und {@code death.attack.<klasse>.player} (= Original
 * {@code death.sedna.<klasse>} / {@code .attacker}).
 */
public final class SednaDamage {

    private SednaDamage() { }

    public static final EnumMap<DamageClass, ResourceKey<DamageType>> KEYS = new EnumMap<>(DamageClass.class);

    static {
        for (DamageClass c : DamageClass.values()) KEYS.put(c, ModDamageTypes.sedna(c.name().toLowerCase(Locale.US)));
    }

    public static DamageSource create(Entity projectile, @Nullable LivingEntity shooter, DamageClass dmgClass) {
        return create(projectile.level(), projectile, shooter, dmgClass);
    }

    /** Original {@code BulletConfig.getDamage(null, shooter, class)}: Projektil darf fehlen (Explosionen). */
    public static DamageSource create(net.minecraft.world.level.Level level, @Nullable Entity projectile, @Nullable LivingEntity shooter, DamageClass dmgClass) {
        var holder = level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(KEYS.get(dmgClass));
        return new DamageSource(holder, projectile != null ? projectile : shooter, shooter);
    }
}
