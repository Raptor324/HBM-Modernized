package com.hbm_m.handler.pollution;

import java.util.UUID;

import com.hbm_m.config.ModClothConfig;
import com.hbm_m.inventory.fluid.trait.PollutionType;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.TickEvent;

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
 * <p><b>Nicht portiert:</b> der Rampant-Modus ({@code rampantTargetSetter},
 * {@code rampantScoutPopulator}) - er setzt Glyphiden voraus, die es im Port nicht gibt.</p>
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
    }

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
        if (!(living instanceof Enemy)) return;

        PollutionData data = PollutionHandler.getPollutionData(level,
                (int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
        if (data == null) return;

        if (data.get(PollutionType.SOOT) <= ModClothConfig.get().buffMobThreshold) return;

        // Original: Operation 1 = MULTIPLY_BASE, Werte 1.0 (doppeltes Leben) und 1.5.
        AttributeInstance health = living.getAttribute(Attributes.MAX_HEALTH);
        if (health != null && health.getModifier(MAX_HEALTH_ID) == null) {
            health.addPermanentModifier(new AttributeModifier(
                    MAX_HEALTH_ID, "Soot Anger Health Increase", 1D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }

        AttributeInstance damage = living.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null && damage.getModifier(ATTACK_DAMAGE_ID) == null) {
            damage.addPermanentModifier(new AttributeModifier(
                    ATTACK_DAMAGE_ID, "Soot Anger Damage Increase", 1.5D,
                    AttributeModifier.Operation.MULTIPLY_BASE));
        }

        living.heal(living.getMaxHealth());
    }
}
