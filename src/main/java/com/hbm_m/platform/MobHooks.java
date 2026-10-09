package com.hbm_m.platform;

/**
 * Versionsbruecke fuer Mob-/Entity-APIs (Phase B, Agent D).
 * Reine 1.21.1-Helfer stehen im auskommentierten Zweig.
 */
public final class MobHooks {
    private MobHooks() {}

    /**
     * {@code inst.removeModifier(uuid)}. 1.21.1: Modifier-ID wie in
     * {@link PlatformHooks#attributeModifier(java.util.UUID, String, double, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation)} aus der UUID abgeleitet.
     */
    public static void removeModifier(net.minecraft.world.entity.ai.attributes.AttributeInstance inst, java.util.UUID uuid) {
        //? if < 1.21.1 {
        inst.removeModifier(uuid);
        //?} else {
        /*inst.removeModifier(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm_m.lib.RefStrings.MODID,
                "am_" + uuid.toString().replace('-', '_')));
        *///?}
    }

    //? if >= 1.21.1 {
    /*// Pluenderungsstufe fuer dropCustomDeathLoot: 1.20.1 reicht sie als Parameter durch
    // (Forge getLootingLevel = Pluenderung der Haupthand des Angreifers), 1.21.1 nicht mehr.
    public static int lootingLevel(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (source != null && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            return ItemHooks.getEnchantmentLevel(attacker.getMainHandItem(), level, "minecraft:looting");
        }
        return 0;
    }
    *///?}
}
