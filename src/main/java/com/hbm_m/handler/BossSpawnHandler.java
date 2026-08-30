package com.hbm_m.handler;

import com.hbm_m.block.ModBlocks;
import com.hbm_m.config.ModClothConfig;
import com.hbm_m.entity.ModEntities;
import com.hbm_m.entity.mob.EntityMaskMan;
import com.hbm_m.entity.mob.EntityRADBeast;
import com.hbm_m.lib.RefStrings;
import com.hbm_m.radiation.PlayerHandler;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import dev.architectury.event.events.common.TickEvent;

/**
 * 1:1 port of {@code BossSpawnHandler.rollTheDice} - the parts of it whose mobs exist.
 *
 * <p>Neither boss spawns naturally: both are summoned at a player who has met a specific set of
 * conditions, which is why registering a {@code SpawnPlacement} would have been wrong.</p>
 *
 * <p><b>MaskMan</b> stalks a player who has built an ore acidizer, is carrying at least 50 RAD and
 * is underground. All three have to hold continuously - the timer resets the moment one lapses -
 * and a warning goes out three seconds before he arrives.</p>
 *
 * <p><b>RAD Beasts</b> arrive as a pack when a player carries the {@code radMark} flag, the first
 * of them being the leader. Nothing in this port sets that flag yet, so that half is dormant;
 * see the note on {@link #RAD_MARK}.</p>
 */
public class BossSpawnHandler {

    /** Persistent-data keys, mirroring the original's PERSISTED_NBT_TAG entries. */
    private static final String MASKMAN_TIMER = "hbm_maskManTimer";
    /**
     * {@code radMark}: set by the original when a player survives a serious irradiation event.
     * No port system raises it yet, so the RAD Beast raid never fires - the code is here so that
     * whoever ports that event only has to set the flag.
     */
    public static final String RAD_MARK = "hbm_radMark";

    // The original reads these from MobConfig; the port has no such config section yet, so they
    // sit here as the original's defaults rather than being invented into the config screen.
    private static final int MASKMAN_DELAY = 20 * 60 * 20;   // 20 Minuten
    private static final int MASKMAN_MIN_RAD = 50;
    private static final boolean MASKMAN_UNDERGROUND = true;
    private static final int ELEMENTAL_DELAY = 20 * 60 * 5;
    private static final int ELEMENTAL_CHANCE = 10;
    private static final int ELEMENTAL_AMOUNT = 5;
    private static final double RAID_DISTANCE = 30D;

    /** Registriert den Level-Tick auf dem plattformneutralen Architectury-Event. */
    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(BossSpawnHandler::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        // isSurfaceWorld: no stalking in the Nether or the End.
        if (!level.dimensionType().natural()) return;

        rollMaskMan(level);
        rollRadBeasts(level);
    }

    // ─── MaskMan ─────────────────────────────────────────────────────────────

    private static void rollMaskMan(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) return;

        for (ServerPlayer player : level.players()) {
            if (!qualifies(level, player)) {
                setTimer(player, 0);
                continue;
            }

            int timer = getTimer(player) + 20;
            setTimer(player, timer);

            // Three seconds of warning before he shows up.
            if (timer >= MASKMAN_DELAY - 60 && timer < MASKMAN_DELAY - 40) {
                player.sendSystemMessage(Component.literal("The mask man draws near.")
                        .withStyle(ChatFormatting.RED));
            }

            if (timer >= MASKMAN_DELAY) {
                setTimer(player, 0);
                spawnMaskMan(level, player);
            }
        }
    }

    /** All three conditions of the original, checked together. */
    private static boolean qualifies(ServerLevel level, ServerPlayer player) {
        if (!ModClothConfig.get().enableRadiation) return false;

        // The original tracks whether the acidizer was ever crafted or placed via the stats list.
        var item = ModBlocks.MACHINE_CRYSTALLIZER.get().asItem();
        boolean acidizer = player.getStats().getValue(Stats.ITEM_CRAFTED.get(item)) > 0
                || player.getStats().getValue(Stats.ITEM_USED.get(item)) > 0;
        if (!acidizer) return false;

        if (PlayerHandler.getPlayerRads(player) < MASKMAN_MIN_RAD) return false;

        if (MASKMAN_UNDERGROUND) {
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                    player.getBlockX(), player.getBlockZ());
            if (surface <= player.getY() + 3) return false;
        }

        return true;
    }

    private static void spawnMaskMan(ServerLevel level, ServerPlayer player) {
        double x = player.getX() + level.random.nextGaussian() * 20;
        double z = player.getZ() + level.random.nextGaussian() * 20;
        double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));

        EntityMaskMan man = ModEntities.MASKMAN.get().create(level);
        if (man != null && trySpawn(level, man, x, y, z)) {
            player.sendSystemMessage(Component.literal("The mask man is about to claim another victim.")
                    .withStyle(ChatFormatting.RED));
        } else {
            player.sendSystemMessage(Component.literal("Seems like mask man couldn't come today.")
                    .withStyle(ChatFormatting.BLUE));
        }
    }

    // ─── RAD Beasts ──────────────────────────────────────────────────────────

    private static void rollRadBeasts(ServerLevel level) {
        if (level.getGameTime() % ELEMENTAL_DELAY != 0) return;
        if (level.players().isEmpty()) return;
        if (level.random.nextInt(ELEMENTAL_CHANCE) != 0) return;

        ServerPlayer player = level.players().get(level.random.nextInt(level.players().size()));
        CompoundTag data = persistentData(player);
        if (!data.getBoolean(RAD_MARK)) return;

        player.sendSystemMessage(Component.literal("You hear a faint clicking...")
                .withStyle(ChatFormatting.YELLOW));
        data.putBoolean(RAD_MARK, false);

        for (int i = 0; i < ELEMENTAL_AMOUNT; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double x = player.getX() + Math.cos(angle) * RAID_DISTANCE + level.random.nextGaussian();
            double z = player.getZ() + Math.sin(angle) * RAID_DISTANCE + level.random.nextGaussian();
            double y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));

            EntityRADBeast beast = ModEntities.RAD_BEAST.get().create(level);
            if (beast == null) continue;
            // Only the first of the pack is the leader - and only the leader is worth the
            // advancement, so a raid always contains exactly one.
            if (i == 0) beast.makeLeader();
            trySpawn(level, beast, x, y, z);
        }
    }

    // ─── shared ──────────────────────────────────────────────────────────────

    /** {@code trySpawn}: place it, ask Forge whether it may exist there, then finalise it. */
    private static boolean trySpawn(ServerLevel level, Mob mob, double x, double y, double z) {
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);

        var result = net.minecraftforge.event.ForgeEventFactory.checkSpawnPosition(mob, level,
                MobSpawnType.EVENT);
        if (!result) return false;

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.EVENT, null, null);
        level.addFreshEntity(mob);
        return true;
    }

    private static CompoundTag persistentData(ServerPlayer player) {
        //? if forge {
        return player.getPersistentData();
        //?}
    }

    private static int getTimer(ServerPlayer player) {
        return persistentData(player).getInt(MASKMAN_TIMER);
    }

    private static void setTimer(ServerPlayer player, int value) {
        persistentData(player).putInt(MASKMAN_TIMER, value);
    }

    /** Raises {@link #RAD_MARK} so the next roll can send a RAD Beast pack. */
    public static void markForRadBeasts(ServerPlayer player) {
        persistentData(player).putBoolean(RAD_MARK, true);
    }
}
