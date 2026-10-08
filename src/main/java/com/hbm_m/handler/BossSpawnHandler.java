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
    /** {@code radMark}: von Kernschmelzen (z. B. Zirnox) im Umkreis von 100 Bloecken gesetzt, ruft Strahlungs-Elementare. */
    public static final String RAD_MARK = "hbm_radMark";


    /** Registriert den Level-Tick auf dem plattformneutralen Architectury-Event. */
    public static void init() {
        TickEvent.SERVER_LEVEL_POST.register(BossSpawnHandler::onLevelTick);
    }

    private static void onLevelTick(ServerLevel level) {
        // Original rollTheDice: Meteore unabhaengig von Schwierigkeit und Dimension (Spielerfilter unten)
        if (ModClothConfig.get().enableMeteorStrikes) {
            meteorUpdate(level);
        }

        rollGhosts(level);

        if (level.getDifficulty() == Difficulty.PEACEFUL) return;
        // isSurfaceWorld: no stalking in the Nether or the End.
        if (!level.dimensionType().natural()) return;

        if (com.hbm_m.config.MobConfig.enableMaskman()) rollMaskMan(level);
        rollRaids(level);
        if (com.hbm_m.config.MobConfig.enableElementals()) rollRadBeasts(level);
    }

    // ─── Meteore (1:1 BossSpawnHandler.meteorUpdate / spawnMeteorAtPlayer) ──

    private static final java.util.Random meteorRand = new java.util.Random();
    public static int meteorShower = 0;

    private static void meteorUpdate(ServerLevel world) {
        ModClothConfig cfg = ModClothConfig.get();

        if (meteorRand.nextInt(meteorShower > 0 ? cfg.meteorShowerChance : cfg.meteorStrikeChance) == 0) {

            if (!world.players().isEmpty()) {

                ServerPlayer p = world.players().get(meteorRand.nextInt(world.players().size()));

                if (p != null && world.dimension() == net.minecraft.world.level.Level.OVERWORLD) {

                    boolean repell = false;
                    boolean strike = true;

                    for (net.minecraft.world.item.ItemStack armor : p.getInventory().armor) {
                        if (!armor.isEmpty() && com.hbm_m.armormod.util.ArmorModificationHelper.hasMods(armor)) {

                            for (net.minecraft.world.item.ItemStack mod : com.hbm_m.armormod.util.ArmorModificationHelper.pryMods(armor)) {

                                if (mod != null && !mod.isEmpty()) {
                                    if (mod.getItem() == com.hbm_m.item.ModItems.PROTECTION_CHARM.get()) repell = true;
                                    if (mod.getItem() == com.hbm_m.item.ModItems.METEOR_CHARM.get()) strike = false;
                                }
                            }
                        }
                    }

                    // only check if either charm is not present
                    if (!repell || strike) {
                        int x = net.minecraft.util.Mth.floor(p.getX());
                        int z = net.minecraft.util.Mth.floor(p.getZ());

                        com.hbm_m.block.decorations.PedestalBlock.checkPedestalEntries(world.dimension(), world.getGameTime());
                        java.util.List<com.hbm_m.block.decorations.PedestalBlock.PedestalEntry> entries = com.hbm_m.block.decorations.PedestalBlock.getEntriesForDimension(world.dimension());
                        if (entries != null) for (com.hbm_m.block.decorations.PedestalBlock.PedestalEntry entry : entries) {
                            if (Math.abs(entry.pos().getX() - x) <= 100 && Math.abs(entry.pos().getZ() - z) <= 100) {
                                if (entry.type() == com.hbm_m.block.decorations.PedestalBlock.PedestalEntryType.CHARM_OF_PROTECTION) repell = true;
                                if (entry.type() == com.hbm_m.block.decorations.PedestalBlock.PedestalEntryType.METEORITE_CHARM) strike = false;
                            }
                        }
                    }

                    if (strike) spawnMeteorAtPlayer(p, repell);
                }
            }
        }

        if (meteorShower > 0) {
            meteorShower--;
            if (meteorShower == 0 && cfg.enableDebugLogging)
                com.hbm_m.main.MainRegistry.LOGGER.info("Ended meteor shower.");
        }

        if (meteorRand.nextInt(cfg.meteorStrikeChance * 100) == 0 && cfg.enableMeteorShowers) {
            meteorShower = (int) (cfg.meteorShowerDuration * 0.75 + cfg.meteorShowerDuration * 0.25 * meteorRand.nextFloat());

            if (cfg.enableDebugLogging)
                com.hbm_m.main.MainRegistry.LOGGER.info("Started meteor shower! Duration: " + meteorShower);
        }
    }

    public static void spawnMeteorAtPlayer(net.minecraft.world.entity.player.Player player, boolean repell) {

        com.hbm_m.entity.projectile.EntityMeteor meteor = new com.hbm_m.entity.projectile.EntityMeteor(player.level());
        meteor.moveTo(player.getX() + meteorRand.nextInt(201) - 100, 384, player.getZ() + meteorRand.nextInt(201) - 100, 0, 0);

        net.minecraft.world.phys.Vec3 vec;
        if (repell) {
            vec = new net.minecraft.world.phys.Vec3(meteor.getX() - player.getX(), 0, meteor.getZ() - player.getZ()).normalize();
            double vel = meteorRand.nextDouble();
            vec = new net.minecraft.world.phys.Vec3(vec.x * vel, vec.y, vec.z * vel);
            meteor.safe = true;
        } else {
            vec = new net.minecraft.world.phys.Vec3(meteorRand.nextDouble() - 0.5D, 0, 0);
            vec = vec.yRot((float) (Math.PI * meteorRand.nextDouble()));
        }

        meteor.setDeltaMovement(vec.x, -2.5, vec.z);
        player.level().addFreshEntity(meteor);
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

            // Original: Warnung genau 60 Pruefungen (= Sekunden) vor dem Erscheinen
            int maskmanDelay = com.hbm_m.config.MobConfig.maskmanDelay() * 20;
            if (timer == maskmanDelay - 60 * 20) {
                player.sendSystemMessage(Component.literal("The mask man draws near.")
                        .withStyle(ChatFormatting.RED));
            }

            if (timer >= maskmanDelay) {
                setTimer(player, 0);
                spawnMaskMan(level, player);
            }
        }
    }

    /** All three conditions of the original, checked together. */
    private static boolean qualifies(ServerLevel level, ServerPlayer player) {
        if (!ModClothConfig.get().enableRadiation) return false;

        // The original tracks whether the acidizer was ever crafted or placed via the stats list.
        var item = ModBlocks.CRYSTALLIZER.get().asItem();
        boolean acidizer = player.getStats().getValue(Stats.ITEM_CRAFTED.get(item)) > 0
                || player.getStats().getValue(Stats.ITEM_USED.get(item)) > 0;
        if (!acidizer) return false;

        if (PlayerHandler.getPlayerRads(player) < com.hbm_m.config.MobConfig.maskmanMinRad()) return false;

        if (com.hbm_m.config.MobConfig.maskmanUnderground()) {
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
        if (level.getGameTime() % com.hbm_m.config.MobConfig.elementalDelay() != 0) return;
        if (level.players().isEmpty()) return;
        if (level.random.nextInt(com.hbm_m.config.MobConfig.elementalChance()) != 0) return;

        ServerPlayer player = level.players().get(level.random.nextInt(level.players().size()));
        CompoundTag data = persistentData(player);
        if (!data.getBoolean(RAD_MARK)) return;

        player.sendSystemMessage(Component.literal("You hear a faint clicking...")
                .withStyle(ChatFormatting.YELLOW));
        data.putBoolean(RAD_MARK, false);

        // Original: Vektor der Laenge raidAttackDistance, je Elementar weitergedreht
        net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(com.hbm_m.config.MobConfig.raidAttackDistance(), 0, 0);
        for (int i = 0; i < com.hbm_m.config.MobConfig.elementalAmount(); i++) {
            vec = vec.yRot((float) (Math.PI * 2) * level.random.nextFloat());
            double x = player.getX() + vec.x + level.random.nextGaussian();
            double z = player.getZ() + vec.z + level.random.nextGaussian();
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

    /** Original FBI-Razzia: alle {@code raidDelay} Ticks mit 1/{@code raidChance} gegen einen unmarkierten Spieler. */
    private static void rollRaids(ServerLevel world) {
        if (!com.hbm_m.config.MobConfig.enableRaids()) return;
        if (world.getGameTime() % com.hbm_m.config.MobConfig.raidDelay() != 0) return;

        if (world.random.nextInt(com.hbm_m.config.MobConfig.raidChance()) == 0 && !world.players().isEmpty()) {

            net.minecraft.server.level.ServerPlayer player = world.players().get(world.random.nextInt(world.players().size()));

            if (persistentData(player).getLong("fbiMark") < world.getGameTime()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("FBI, OPEN UP!").withStyle(net.minecraft.ChatFormatting.RED));

                net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(com.hbm_m.config.MobConfig.raidAttackDistance(), 0, 0).yRot((float) (Math.PI * 2) * world.random.nextFloat());

                for (int i = 0; i < com.hbm_m.config.MobConfig.raidAmount(); i++) {
                    double spawnX = player.getX() + vec.x + world.random.nextGaussian() * 5;
                    double spawnZ = player.getZ() + vec.z + world.random.nextGaussian() * 5;
                    double spawnY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) spawnX, (int) spawnZ);
                    com.hbm_m.entity.mob.EntityFBI fbi = com.hbm_m.entity.ModEntities.FBI.get().create(world);
                    if (fbi != null) trySpawn(world, fbi, spawnX, spawnY, spawnZ);
                }

                for (int i = 0; i < com.hbm_m.config.MobConfig.raidDrones(); i++) {
                    double spawnX = player.getX() + vec.x + world.random.nextGaussian() * 5;
                    double spawnZ = player.getZ() + vec.z + world.random.nextGaussian() * 5;
                    double spawnY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) spawnX, (int) spawnZ);
                    com.hbm_m.entity.mob.EntityFBIDrone drone = com.hbm_m.entity.ModEntities.FBI_DRONE.get().create(world);
                    if (drone != null) trySpawn(world, drone, spawnX, spawnY + 10, spawnZ);
                }
            }
        }
    }

    /** Original: Spieler mit Digamma sehen alle Sekunde mit 1/5 einen Geist 75 Bloecke entfernt (nur Oberwelten). */
    private static void rollGhosts(ServerLevel world) {
        if (world.getGameTime() % 20 != 0) return;
        if (world.random.nextInt(5) == 0 && !world.players().isEmpty() && world.dimensionType().natural()) {
            net.minecraft.server.level.ServerPlayer player = world.players().get(world.random.nextInt(world.players().size()));

            if (com.hbm_m.extprop.HbmLivingProps.getDigamma(player) > 0) {
                net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(75, 0, 0).yRot((float) (Math.PI * 2) * world.random.nextFloat());
                double spawnX = player.getX() + vec.x + world.random.nextGaussian();
                double spawnZ = player.getZ() + vec.z + world.random.nextGaussian();
                double spawnY = world.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, (int) spawnX, (int) spawnZ);
                com.hbm_m.entity.mob.EntityGhost ghost = com.hbm_m.entity.ModEntities.GHOST.get().create(world);
                if (ghost != null) trySpawn(world, ghost, spawnX, spawnY, spawnZ);
            }
        }
    }

    /** {@code trySpawn}: place it, ask Forge whether it may exist there, then finalise it. */
    private static boolean trySpawn(ServerLevel level, Mob mob, double x, double y, double z) {
        mob.moveTo(x, y, z, level.random.nextFloat() * 360.0F, 0.0F);

        //? if forge {
        var result = net.minecraftforge.event.ForgeEventFactory.checkSpawnPosition(mob, level,
                MobSpawnType.EVENT);
        if (!result) return false;

        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.EVENT, null, null);
        //?} else {
        /*// Kein plattformneutrales Gegenstueck zu checkSpawnPosition; hier nur finalisieren.
        // finalizeSpawn verlor auf 1.21 den abschliessenden CompoundTag-Parameter.
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                MobSpawnType.EVENT, null);
        *///?}
        level.addFreshEntity(mob);
        return true;
    }

    private static CompoundTag persistentData(ServerPlayer player) {
        // getPersistentData() ist Forge-spezifisch; PlayerPersistentData kapselt beide Plattformen.
        return com.hbm_m.platform.PlayerPersistentData.get(player);
    }

    private static int getTimer(ServerPlayer player) {
        return persistentData(player).getInt(MASKMAN_TIMER);
    }

    private static void setTimer(ServerPlayer player, int value) {
        persistentData(player).putInt(MASKMAN_TIMER, value);
    }

    /** Original {@code markFBI}: Razzia-Markierung fuer die naechsten 20 Minuten (Radiolyse-Kammer, Radiobox). */
    public static void markFBI(ServerPlayer player) {
        persistentData(player).putLong("fbiMark", player.level().getGameTime() + 20 * 60 * 20);
    }

    /** Raises {@link #RAD_MARK} so the next roll can send a RAD Beast pack. */
    public static void markForRadBeasts(ServerPlayer player) {
        persistentData(player).putBoolean(RAD_MARK, true);
    }
}
