package com.hbm_m.armormod.event;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Броня с модификатором здоровья при снятии опускает максимум здоровья ниже текущего.
 * Ванильный {@code setHealth} сам зажимает значение, поэтому достаточно раз в тик подрезать
 * тех, у кого текущее здоровье выше максимума.
 *
 * <p>Раньше это жило в forge-only подписчике {@code LivingEquipmentChangeEvent} и на NeoForge
 * не работало вовсе. Теперь регистрация одна на обе платформы, как у
 * {@link ArmorModTickHandler}.</p>
 */
public final class ArmorModificationServerEvents {

    private ArmorModificationServerEvents() {}

    public static void init() {
        TickEvent.SERVER_POST.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player.getHealth() > player.getMaxHealth()) {
                    player.setHealth(player.getMaxHealth());
                }
            }
        });
    }
}
