package com.hbm_m.util;

import net.minecraft.world.entity.player.Player;

/**
 * XP-Teil von {@code com.hbm.util.EnchantmentUtil}. Die Stufenkurve ist die des laufenden Spiels
 * ({@link #xpBarCap}), so wie das Original die seiner Version benutzt - sonst wuerden
 * {@link #getTotalExperience} und {@link #addExperience} verschiedene Kurven mischen.
 */
public final class EnchantmentUtil {

    private EnchantmentUtil() {}

    /** Original {@code addEnchantment}: haengt die Verzauberung an (ohne vorhandene zu pruefen). */
    public static void addEnchantment(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment, int level) {
        stack.enchant(enchantment, level);
    }

    /** Original {@code removeEnchantment}: entfernt den ersten Eintrag dieser Verzauberung, egal welche Stufe. */
    public static void removeEnchantment(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        if (stack.getTag() == null || !stack.getTag().contains(net.minecraft.world.item.ItemStack.TAG_ENCH, 9)) return;
        net.minecraft.nbt.ListTag list = stack.getEnchantmentTags();
        net.minecraft.resources.ResourceLocation id = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentId(enchantment);
        int i = 0;
        for (; i < list.size(); i++) {
            if (id != null && id.equals(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentId(list.getCompound(i)))) break;
        }
        if (i < list.size()) list.remove(i);
        if (list.isEmpty()) stack.getTag().remove(net.minecraft.world.item.ItemStack.TAG_ENCH);
    }

    public static int getEnchantmentLevel(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
    }

    /** Gleich {@code Player.getXpNeededForNextLevel()} fuer eine beliebige Stufe. */
    public static int xpBarCap(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : (level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2);
    }

    public static int getLevelForExperience(int xp) {
        int level = 0;
        while (true) {
            int xpCap = xpBarCap(level);
            if (xp < xpCap) return level;
            xp -= xpCap;
            level++;
        }
    }

    /** Identical to Player.giveExperiencePoints but without increasing the player's score */
    public static void addExperience(Player player, int xp, boolean silent) {

        int j = Integer.MAX_VALUE - player.totalExperience;

        if (xp > j) {
            xp = j;
        }

        player.experienceProgress += (float) xp / (float) player.getXpNeededForNextLevel();

        for (player.totalExperience += xp; player.experienceProgress >= 1.0F; player.experienceProgress /= (float) player.getXpNeededForNextLevel()) {
            player.experienceProgress = (player.experienceProgress - 1.0F) * (float) player.getXpNeededForNextLevel();

            if (silent) addExperienceLevelSilent(player, 1);
            else player.giveExperienceLevels(1);
        }
    }

    public static void setExperience(Player player, int xp) {
        player.experienceLevel = 0;
        player.experienceProgress = 0.0F;
        player.totalExperience = 0;

        addExperience(player, xp, true);
    }

    public static void addExperienceLevelSilent(Player player, int level) {
        player.experienceLevel += level;

        if (player.experienceLevel < 0) {
            player.experienceLevel = 0;
            player.experienceProgress = 0.0F;
            player.totalExperience = 0;
        }
    }

    /** Fun fact: experienceTotal lies and has no actual purpose other than misleading people! */
    public static int getTotalExperience(Player player) {
        int xp = 0;

        /* count only completed levels */
        for (int i = 0; i < player.experienceLevel; i++) {
            xp += xpBarCap(i);
        }

        xp += (int) (xpBarCap(player.experienceLevel) * player.experienceProgress);

        return xp;
    }
}
