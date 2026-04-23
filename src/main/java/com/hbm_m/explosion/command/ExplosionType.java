package com.hbm_m.explosion.command;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.minecraft.network.chat.Component;

/**
 * Типы ядерных взрывов для команды (строковые id, как в {@link #id()}).
 */
public enum ExplosionType {
    EXPLOSION_NUKE_MK5("explosion_nuke_mk5", true, true, true, true, true, true),
    EXPLOSION_NUKE_FATMAN("explosion_nuke_fatman", true, true, true, true, true, true),
    EXPLOSION_NUKE_GENERIC("explosion_nuke_generic", true, true, true, true, true, false),
    EXPLOSION_NUKE_CHARGE("explosion_nuke_charge", true, true, true, true, true, false),
    EXPLOSION_NUKE_DUD("explosion_nuke_dud", true, true, true, true, true, false),
    EXPLOSION_NUKE_MINE("explosion_nuke_mine", true, true, false, true, true, false),
    EXPLOSION_NUKE_GRENADE("explosion_nuke_grenade", true, true, false, true, true, false);

    private final String id;
    private final boolean supportsCrater;
    private final boolean supportsDamage;
    private final boolean supportsBiomes;
    private final boolean supportsParticles;
    private final boolean supportsAmplifier;
    private final boolean supportsFallout;

    ExplosionType(String id,
                  boolean supportsCrater, boolean supportsDamage, boolean supportsBiomes,
                  boolean supportsParticles, boolean supportsAmplifier, boolean supportsFallout) {
        this.id = id;
        this.supportsCrater = supportsCrater;
        this.supportsDamage = supportsDamage;
        this.supportsBiomes = supportsBiomes;
        this.supportsParticles = supportsParticles;
        this.supportsAmplifier = supportsAmplifier;
        this.supportsFallout = supportsFallout;
    }

    public String id() {
        return id;
    }

    public boolean supportsCrater() {
        return supportsCrater;
    }

    public boolean supportsDamage() {
        return supportsDamage;
    }

    public boolean supportsBiomes() {
        return supportsBiomes;
    }

    public boolean supportsParticles() {
        return supportsParticles;
    }

    public boolean supportsAmplifier() {
        return supportsAmplifier;
    }

    public boolean supportsFallout() {
        return supportsFallout;
    }

    public boolean supportsOptionKey(String key) {
        return switch (key) {
            case "crater" -> supportsCrater;
            case "damage" -> supportsDamage;
            case "biomes" -> supportsBiomes;
            case "particles" -> supportsParticles;
            case "amplifier" -> supportsAmplifier;
            case "sound" -> true;
            case "fallout" -> supportsFallout;
            default -> false;
        };
    }

    private static final SimpleCommandExceptionType UNKNOWN_TYPE =
            new SimpleCommandExceptionType(Component.translatable("commands.hbm_m.explosion.unknown_type"));

    public static ExplosionType parse(String raw) throws CommandSyntaxException {
        String s = raw.toLowerCase(Locale.ROOT);
        for (ExplosionType t : values()) {
            if (t.id.equals(s)) {
                return t;
            }
        }
        throw UNKNOWN_TYPE.create();
    }

    public static Optional<ExplosionType> tryParse(String raw) {
        try {
            return Optional.of(parse(raw));
        } catch (CommandSyntaxException e) {
            return Optional.empty();
        }
    }

    /** Идентификаторы для подсказок Tab (без коротких алиасов). */
    public static String[] allIds() {
        return Arrays.stream(values()).map(ExplosionType::id).toArray(String[]::new);
    }
}
