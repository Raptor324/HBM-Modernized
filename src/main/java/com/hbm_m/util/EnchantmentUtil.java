package com.hbm_m.util;

import com.hbm_m.platform.StackNbt;

import net.minecraft.world.entity.player.Player;

/**
 * XP-Teil von {@code com.hbm.util.EnchantmentUtil}. Die Stufenkurve ist die des laufenden Spiels
 * ({@link #xpBarCap}), so wie das Original die seiner Version benutzt - sonst wuerden
 * {@link #getTotalExperience} und {@link #addExperience} verschiedene Kurven mischen.
 */
public final class EnchantmentUtil {

    private EnchantmentUtil() {}

    //? if < 1.21.1 {
    /** Original {@code addEnchantment}: haengt die Verzauberung an (ohne vorhandene zu pruefen). */
    public static void addEnchantment(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment, int level) {
        stack.enchant(enchantment, level);
    }

    /** Original {@code removeEnchantment}: entfernt den ersten Eintrag dieser Verzauberung, egal welche Stufe. */
    public static void removeEnchantment(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        if (StackNbt.read(stack) == null || !StackNbt.read(stack).contains(net.minecraft.world.item.ItemStack.TAG_ENCH, 9)) return;
        net.minecraft.nbt.ListTag list = stack.getEnchantmentTags();
        net.minecraft.resources.ResourceLocation id = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentId(enchantment);
        int i = 0;
        for (; i < list.size(); i++) {
            if (id != null && id.equals(net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentId(list.getCompound(i)))) break;
        }
        if (i < list.size()) list.remove(i);
        if (list.isEmpty()) StackNbt.tag(stack).remove(net.minecraft.world.item.ItemStack.TAG_ENCH);
    }

    public static int getEnchantmentLevel(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.enchantment.Enchantment enchantment) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
    }
    //?} else {
    /*/^ 1.21.1: Vanilla {@code Enchantments.BLOCK_FORTUNE} heisst {@code FORTUNE}. ^/
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> FORTUNE =
            net.minecraft.world.item.enchantment.Enchantments.FORTUNE;

    /^*
     * 1.21.1 haelt Verzauberungen als Map (ein Eintrag je Verzauberung), 1.20.1 als Liste. Ein zweites
     * {@code addEnchantment} derselben Verzauberung legt im Original einen zweiten Listeneintrag an, der erste
     * bleibt wirksam; {@code removeEnchantment} entfernt den ersten, danach gilt der zweite. Dieser zweite
     * Eintrag wird hier im Item-NBT unter {@value #SHADOW} gemerkt.
     ^/
    private static final String SHADOW = "HbmEnchShadow";

    /^* Verzauberungen sind Datapack-Eintraege: Schluessel -> Holder der Registry der laufenden Seite (Server bevorzugt). ^/
    public static net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> holder(
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        net.minecraft.core.HolderLookup.Provider provider = null;
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null && server.isSameThread()) provider = server.registryAccess();
        if (provider == null) provider = com.hbm_m.platform.PlatformHooks.bestEffortProvider();
        if (provider == null) return null;
        return provider.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key).orElse(null);
    }

    /^* Original {@code addEnchantment}: haengt die Verzauberung an (ohne vorhandene zu pruefen). ^/
    public static void addEnchantment(net.minecraft.world.item.ItemStack stack,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key, int level) {
        var h = holder(key);
        if (h == null) return;
        if (net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(h, stack) > 0) {
            net.minecraft.nbt.CompoundTag t = StackNbt.orCreate(stack);
            net.minecraft.nbt.CompoundTag shadow = t.getCompound(SHADOW);
            shadow.putInt(key.location().toString(), level);
            t.put(SHADOW, shadow);
            return;
        }
        stack.enchant(h, level);
    }

    /^* Original {@code removeEnchantment}: entfernt den ersten Eintrag dieser Verzauberung, egal welche Stufe. ^/
    public static void removeEnchantment(net.minecraft.world.item.ItemStack stack,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var h = holder(key);
        if (h == null || net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(h, stack) <= 0) return;
        String id = key.location().toString();
        net.minecraft.nbt.CompoundTag r = StackNbt.read(stack);
        if (r != null && r.getCompound(SHADOW).contains(id)) {
            int next = r.getCompound(SHADOW).getInt(id);
            net.minecraft.nbt.CompoundTag t = StackNbt.orCreate(stack);
            net.minecraft.nbt.CompoundTag shadow = t.getCompound(SHADOW);
            shadow.remove(id);
            if (shadow.isEmpty()) t.remove(SHADOW); else t.put(SHADOW, shadow);
            if (t.isEmpty()) StackNbt.set(stack, null);
            net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, m -> m.set(h, next));
            return;
        }
        net.minecraft.world.item.enchantment.EnchantmentHelper.updateEnchantments(stack, m -> m.set(h, 0));
    }

    public static int getEnchantmentLevel(net.minecraft.world.item.ItemStack stack,
            net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        var h = holder(key);
        return h == null ? 0 : net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(h, stack);
    }
    *///?}

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
