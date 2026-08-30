package com.hbm_m.advancement;

import com.hbm_m.lib.RefStrings;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Central grant point for the mod's advancements, standing in for 1.7.10's
 * {@code EntityPlayer.triggerAchievement}.
 *
 * <p>Every advancement under {@code data/hbm_m/advancements} uses a {@code minecraft:impossible}
 * criterion, so none of them can be earned by the vanilla trigger system - they are all awarded
 * from here, which is what lets the original's arbitrary conditions (melt a reactor, drink the
 * wrong coffee, stand near a dying boss) carry over unchanged.</p>
 */
public final class ModAdvancements {

    // ─── Progression ─────────────────────────────────────────────────────────
    public static final String BURNER_PRESS  = "burner_press";
    public static final String BLAST_FURNACE = "blast_furnace";
    public static final String ASSEMBLY      = "assembly";
    public static final String SELENIUM      = "selenium";
    public static final String CHEMPLANT     = "chemplant";
    public static final String CONCRETE      = "concrete";
    public static final String POLYMER       = "polymer";
    public static final String DESH          = "desh";
    public static final String TANTALUM      = "tantalum";
    public static final String GAS_CENT      = "gas_cent";
    public static final String CENTRIFUGE    = "centrifuge";
    public static final String SCHRAB        = "schrab";
    public static final String ACIDIZER      = "acidizer";
    public static final String SILEX         = "silex";
    public static final String TECHNETIUM    = "technetium";
    public static final String CHICAGO_PILE  = "chicago_pile";
    public static final String RADIUM        = "radium";
    public static final String ZIRNOX_BOOM   = "zirnox_boom";
    public static final String WATZ          = "watz";
    public static final String WATZ_BOOM     = "watz_boom";
    public static final String RBMK          = "rbmk";
    public static final String RBMK_BOOM     = "rbmk_boom";
    public static final String BISMUTH       = "bismuth";
    public static final String BREEDING      = "breeding";
    public static final String FUSION        = "fusion";
    public static final String RED_BALLOONS  = "red_balloons";
    public static final String MANHATTAN     = "manhattan";
    public static final String FOEQ          = "foeq";
    public static final String SOYUZ         = "soyuz";
    public static final String SPACE         = "space";

    // ─── Radiation & digamma ─────────────────────────────────────────────────
    public static final String RAD_POISON         = "rad_poison";
    public static final String RAD_DEATH          = "rad_death";
    public static final String DIGAMMA_SEE        = "digamma_see";
    public static final String DIGAMMA_FEEL       = "digamma_feel";
    public static final String DIGAMMA_KNOW       = "digamma_know";
    public static final String DIGAMMA_KAUAI_MOHO = "digamma_kauai_moho";
    public static final String DIGAMMA_UP_ON_TOP  = "digamma_up_on_top";

    // ─── Bosses & satellites ─────────────────────────────────────────────────
    public static final String BOSS_CREEPER   = "boss_creeper";
    public static final String BOSS_MELTDOWN  = "boss_meltdown";
    public static final String BOSS_MASKMAN   = "boss_maskman";
    public static final String BOSS_WORM      = "boss_worm";
    public static final String BOSS_UFO       = "boss_ufo";
    public static final String HORIZONS_START = "horizons_start";
    public static final String HORIZONS_END   = "horizons_end";
    public static final String HORIZONS_BONUS = "horizons_bonus";

    // ─── Oddities ────────────────────────────────────────────────────────────
    public static final String SACRIFICE      = "sacrifice";
    public static final String IMPOSSIBLE     = "impossible";
    public static final String TASTE_OF_BLOOD = "taste_of_blood";
    public static final String POTATO         = "potato";
    public static final String C20_5          = "c20_5";
    public static final String FIEND          = "fiend";
    public static final String FIEND2         = "fiend2";
    public static final String STRATUM        = "stratum";
    public static final String OMEGA12        = "omega12";
    public static final String SLIMEBALL      = "slimeball";
    public static final String SULFURIC       = "sulfuric";
    public static final String GO_FISH        = "go_fish";
    public static final String NO9            = "no9";
    public static final String INFERNO        = "inferno";
    public static final String RED_ROOM       = "red_room";
    public static final String HIDDEN         = "hidden";
    public static final String SOME_WOUNDS    = "some_wounds";

    private ModAdvancements() {}

    /** Awards {@code id} to this player if they do not already have it. Client calls are ignored. */
    public static void grant(Player player, String id) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        // Advancement wurde in 1.21 durch AdvancementHolder ersetzt; PlatformHooks kapselt beide.
        com.hbm_m.platform.PlatformHooks.awardAdvancementIfEligible(
                serverPlayer, ResourceLocation.fromNamespaceAndPath(RefStrings.MODID, id), true);
    }

    /**
     * Awards {@code id} to every player within {@code range} of a point.
     *
     * <p>The original does this a lot - a meltdown, a boss death or a bomb credits everyone who
     * was there to see it, not just whoever pulled the trigger, and in several of those cases
     * there is no "whoever" to credit at all.</p>
     */
    public static void grantNearby(Level level, double x, double y, double z, double range, String id) {
        if (level.isClientSide) return;
        AABB box = new AABB(x, y, z, x, y, z).inflate(range);
        List<Player> players = level.getEntitiesOfClass(Player.class, box);
        for (Player player : players) {
            grant(player, id);
        }
    }

    /** Convenience overload for the common "everyone who saw this entity" case. */
    public static void grantNearby(Entity entity, double range, String id) {
        grantNearby(entity.level(), entity.getX(), entity.getY(), entity.getZ(), range, id);
    }

    /** Awards {@code id} to every player in the level - used by the satellite callbacks. */
    public static void grantAll(Level level, String id) {
        if (level.isClientSide) return;
        for (Player player : level.players()) {
            grant(player, id);
        }
    }
}
