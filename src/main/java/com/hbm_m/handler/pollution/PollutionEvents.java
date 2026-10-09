package com.hbm_m.handler.pollution;

import com.hbm_m.platform.AttributeOps;

import java.util.UUID;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Haengt {@link PollutionHandler} an den Serverlauf - im Original sind das die
 * {@code @SubscribeEvent}-Methoden derselben Klasse.
 *
 * <p>Rampant-Modus: {@code rampantTargetSetter} (Schlafplatz als Ziel) und {@code rampantScoutPopulator}
 * (Spaeher samt Graeber-Eskorte unter freiem Himmel in stark verrusster Oberwelt).</p>
 */
public final class PollutionEvents {

    /** Original: {@code eggTimer < 60} - die Ausbreitung laeuft alle 60 Serverticks. */
    private static final int SPREAD_INTERVAL = 60;

    private static int spreadTimer = 0;
    private static boolean registered = false;

    private PollutionEvents() {}

    public static void init() {
        if (registered) return;
        registered = true;

        // Original: updateSystem laeuft in TickEvent.ServerTickEvent, Phase END. Dort steht die
        // Weltzersetzung vor dem Zaehler, laeuft also jeden Tick, die Ausbreitung nur jeden 60.
        TickEvent.SERVER_POST.register(server -> {
            if (!ModClothConfig.get().enablePollution) return;

            for (ServerLevel level : server.getAllLevels()) {
                PollutionHandler.handleWorldDestruction(level);
            }

            spreadTimer++;
            if (spreadTimer < SPREAD_INTERVAL) return;
            spreadTimer = 0;

            for (ServerLevel level : server.getAllLevels()) {
                PollutionHandler.updateSystem(level);
            }
        });

        // Original: @SubscribeEvent decorateMob(LivingSpawnEvent).
        EntityEvent.LIVING_CHECK_SPAWN.register((entity, world, x, y, z, type, spawner) -> {
            decorateMob(entity, world, x, y, z);
            return EventResult.pass();
        });

        //? if forge {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.entity.player.PlayerSleepInBedEvent event) -> {
            if (com.hbm_m.config.MobConfig.rampantGlyphidGuidance()) {
                BlockPos p = event.getPos();
                PollutionHandler.targetCoords = new net.minecraft.world.phys.Vec3(p.getX(), p.getY(), p.getZ());
            }
        });
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(PollutionEvents::rampantScoutPopulator);
        //?} elif neoforge {
        /*// NeoForge: PlayerSleepInBedEvent heisst CanPlayerSleepEvent
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent event) -> {
            if (com.hbm_m.config.MobConfig.rampantGlyphidGuidance()) {
                BlockPos p = event.getPos();
                PollutionHandler.targetCoords = new net.minecraft.world.phys.Vec3(p.getX(), p.getY(), p.getZ());
            }
        });
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(PollutionEvents::rampantScoutPopulator);
        *///?}
    }

    //? if forge || neoforge {
    /** 1:1 {@code rampantScoutPopulator}. */
    //? if forge {
    private static void rampantScoutPopulator(net.minecraftforge.event.level.LevelEvent.PotentialSpawns event) {
    //?} else {
    /*private static void rampantScoutPopulator(net.neoforged.neoforge.event.level.LevelEvent.PotentialSpawns event) {
    *///?}

        if (com.hbm_m.config.MobConfig.rampantNaturalScoutSpawn() && event.getLevel() instanceof ServerLevel world && world.dimension() == Level.OVERWORLD
                && world.canSeeSky(event.getPos()) && !event.isCanceled()) {

            if (world.random.nextInt(com.hbm_m.config.MobConfig.rampantScoutSpawnChance()) == 0) {

                BlockPos pos = event.getPos();
                float soot = PollutionHandler.getPollution(world, pos.getX(), pos.getY(), pos.getZ(), PollutionType.SOOT);

                if (soot >= com.hbm_m.config.MobConfig.rampantScoutSpawnThresh()) {
                    var scout = new com.hbm_m.entity.mob.glyphid.EntityGlyphidScout(com.hbm_m.entity.ModEntities.GLYPHID_SCOUT.get(), world);
                    scout.moveTo(pos.getX(), pos.getY(), pos.getZ(), world.random.nextFloat() * 360.0F, 0.0F);

                    if (scout.isValidLightLevel()) {
                        //escort for the scout, which can also deal with obstacles
                        var digger = new com.hbm_m.entity.mob.glyphid.EntityGlyphidDigger(com.hbm_m.entity.ModEntities.GLYPHID_DIGGER.get(), world);
                        scout.moveTo(pos.getX(), pos.getY(), pos.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
                        digger.moveTo(pos.getX(), pos.getY(), pos.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
                        if (scout.getCanSpawnHere()) world.addFreshEntity(scout);
                        if (digger.getCanSpawnHere()) world.addFreshEntity(digger);
                    }
                }
            }
        }
    }
    //?}

    // ═══════════════════════════ Mob-Verstaerkung ═══════════════════════════

    public static final UUID MAX_HEALTH_ID = UUID.fromString("25462f6c-2cb2-4ca8-9b47-3a011cc61207");
    public static final UUID ATTACK_DAMAGE_ID = UUID.fromString("8f442d7c-d03f-49f6-a040-249ae742eed9");

    /**
     * 1:1-Port von {@code decorateMob}: wo genug Russ liegt, kommen Monster mit doppeltem Leben
     * und deutlich mehr Schaden aus dem Boden.
     *
     * <p>Aufzurufen, wenn eine Kreatur in der Welt erscheint - im Original haengt das an
     * {@code LivingSpawnEvent}.</p>
     */
    public static void decorateMob(LivingEntity living, LevelAccessor world, double x, double y, double z) {
        if (!ModClothConfig.get().enablePollution) return;
        if (!(world instanceof Level level) || level.isClientSide()) return;
        if (!(living instanceof Enemy) || living instanceof com.hbm_m.entity.mob.glyphid.EntityGlyphid) return;

        PollutionData data = PollutionHandler.getPollutionData(level,
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (data == null) return;

        if (data.get(PollutionType.SOOT) <= ModClothConfig.get().buffMobThreshold) return;

        // Original: Operation 1 = MULTIPLY_BASE, Werte 1.0 (doppeltes Leben) und 1.5.
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && com.hbm_m.platform.AttributeHooks.getModifier(health, MAX_HEALTH_ID) == null) {
            health.addPermanentModifier(com.hbm_m.platform.AttributeHooks.modifier(
                    MAX_HEALTH_ID, "Soot Anger Health Increase", 1D,
                    AttributeOps.MULTIPLY_BASE));
        }

        AttributeInstance damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && com.hbm_m.platform.AttributeHooks.getModifier(damage, ATTACK_DAMAGE_ID) == null) {
            damage.addPermanentModifier(com.hbm_m.platform.AttributeHooks.modifier(
                    ATTACK_DAMAGE_ID, "Soot Anger Damage Increase", 1.5D,
                    AttributeOps.MULTIPLY_BASE));
        }

        living.heal(living.getMaxHealth());
    }
}
