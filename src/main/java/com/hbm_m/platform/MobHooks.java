package com.hbm_m.platform;

/**
 * Versionsbruecke fuer Mob-/Entity-/Player-APIs.
 * Reine 1.21.1-Helfer stehen im auskommentierten Zweig.
 */
public final class MobHooks {
    private MobHooks() {}

    //? if >= 1.21.1 {
    /*// Pluenderungsstufe fuer dropCustomDeathLoot: 1.20.1 reicht sie als Parameter durch
    // (Forge getLootingLevel = Pluenderung der Haupthand des Angreifers), 1.21.1 nicht mehr.
    public static int lootingLevel(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source) {
        if (source != null && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
            return ItemHooks.getEnchantmentLevel(attacker.getMainHandItem(), level,
                    net.minecraft.world.item.enchantment.Enchantments.LOOTING);
        }
        return 0;
    }
    *///?}

    /** Кросс-версионная проверка и выдача достижения игроку. */
    public static void awardAdvancementIfEligible(net.minecraft.server.level.ServerPlayer player, net.minecraft.resources.ResourceLocation id, boolean eligible) {
        if (!eligible) return;
        var server = player.getServer();
        if (server == null) return;
        // Гейтится ТОЛЬКО различие доступа (getAdvancement vs get); общий блок
        // с циклом по критериям одинаково компилируется через var на обеих версиях.
        var advancementManager = server.getAdvancements();
        var adv =
        //? if < 1.21.1 {
        advancementManager.getAdvancement(id);
        //?} else {
        /*advancementManager.get(id);
        *///?}
        if (adv != null) {
            net.minecraft.advancements.AdvancementProgress progress = player.getAdvancements().getOrStartProgress(adv);
            if (!progress.isDone()) {
                for (String criterion : progress.getRemainingCriteria()) {
                    player.getAdvancements().award(adv, criterion);
                }
            }
        }
    }

    /** Кросс-версионное поджигание сущности. */
    public static void setSecondsOnFire(net.minecraft.world.entity.Entity entity, int seconds) {
        //? if < 1.21.1 {
        entity.setSecondsOnFire(seconds);
        //?} else {
        /*entity.igniteForSeconds((float) seconds);
        *///?}
    }

    /**
     * Кросс-версионный расчёт отбрасывания взрывом с учётом защитных зачарований.
     * ProtectionEnchantment (1.20.1) -> EnchantmentHelper (1.21.1).
     * ИЗВЕСТНОЕ ОТКЛОНЕНИЕ: на 1.21.1 учитывается только атрибут EXPLOSION_KNOCKBACK_RESISTANCE
     * (Blast Protection в 1.21 не влияет на knockback) — не баг, изменение механики ванили.
     */
    public static double getExplosionKnockbackAfterDampener(net.minecraft.world.entity.LivingEntity entity, double knockback) {
        //? if < 1.21.1 {
        return net.minecraft.world.item.enchantment.ProtectionEnchantment.getExplosionKnockbackAfterDampener(entity, knockback);
        //?} else {
        /*double res = entity.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.EXPLOSION_KNOCKBACK_RESISTANCE);
        return knockback * (1.0 - res);
        *///?}
    }

    /**
     * Вертикальное смещение седока (1.20.1 {@code Entity.getMyRidingOffset()}).
     * 1.21.1: смещение считается от точки крепления к ТРАНСПОРТУ (EntityAttachments);
     * без транспорта смещения нет.
     */
    public static double getMyRidingOffset(net.minecraft.world.entity.Entity entity) {
        //? if < 1.21.1 {
        return entity.getMyRidingOffset();
        //?} else {
        /*var vehicle = entity.getVehicle();
        if (vehicle != null) {
            return entity.getVehicleAttachmentPoint(vehicle).y;
        }
        return 0.0D;
        *///?}
    }

    /**
     * {@code Player.canInteractWithBlock(pos, slop)} (1.21.1); на 1.20.1 — тот же eye-to-AABB
     * тест против Forge reach-атрибута. На 1.21.1 ванильный вызов оставлен намеренно:
     * Sable хукает его для блоков на кораблях, самодельная дистанция игнорировала бы позу корабля.
     */
    public static boolean canInteractWithBlock(net.minecraft.world.entity.player.Player player,
                                               net.minecraft.core.BlockPos pos, double slop) {
        //? if < 1.21.1 {
        double reach = player.getBlockReach() + slop;
        return new net.minecraft.world.phys.AABB(pos).distanceToSqr(player.getEyePosition()) < reach * reach;
        //?} else {
        /*return player.canInteractWithBlock(pos, slop);
        *///?}
    }

    /** Модовый fake player (Mts/Prepare и т.п.) — лоадер-специфичный класс. */
    public static boolean isFakePlayer(net.minecraft.world.entity.player.Player player) {
        //? if < 1.21.1 {
        return player instanceof net.minecraftforge.common.util.FakePlayer;
        //?} else {
        /*return player instanceof net.neoforged.neoforge.common.util.FakePlayer;
        *///?}
    }

    /** {@code player.getPersistentData()} — форджевское расширенное NBT игрока (бывший PlayerPersistentData). */
    public static net.minecraft.nbt.CompoundTag getPersistentData(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData();
    }
}
